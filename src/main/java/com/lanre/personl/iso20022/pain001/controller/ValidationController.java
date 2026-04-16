package com.lanre.personl.iso20022.pain001.controller;

import com.lanre.personl.iso20022.logging.LoggingContext;
import com.lanre.personl.iso20022.metrics.Iso20022MetricsService;
import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import com.lanre.personl.iso20022.pain001.service.Iso20022ValidationService;
import com.lanre.personl.iso20022.pain001.service.Pain002StatusGeneratorService;
import com.prowidesoftware.swift.model.mx.MxPain00100111;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * Gatekeeper REST endpoint providing ISO 20022 validation services.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validate")
@RequiredArgsConstructor
@Tag(name = "validation", description = "Inbound ISO 20022 validation and status-report generation.")
@SecurityRequirement(name = "basicAuth")
public class ValidationController {

    private final Iso20022ValidationService validationService;
    private final Pain002StatusGeneratorService statusGeneratorService;
    private final Iso20022MetricsService metricsService;

    /**
     * Validates an incoming pain.001.001.11 message and responds with 
     * a pain.002.001.10 message natively wrapped around the validation state.
     *
     * @param xml The raw string payload representing the incoming XML file
     * @return pain.002 XML structured Response
     */
    @PostMapping(value = "/pain001", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Validate pain.001.001.11 XML",
            description = "Runs technical, semantic, and business validation checks, then returns a pain.002 status report."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = {
                    @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "pain001Xml",
                                    value = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.11\">...</Document>"
                            )),
                    @Content(mediaType = MediaType.TEXT_XML_VALUE,
                            schema = @Schema(type = "string"))
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "pain.002 validation status report",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks WRITER or ADMIN role"),
            @ApiResponse(responseCode = "413", description = "Payload exceeds configured request-size limit")
    })
    public ResponseEntity<String> validatePain001(@RequestBody String xml) {
        ValidationReport report;
        String originalMsgId = null;

        try {
            // Attempt an optimistic fast message ID extraction before failure context
            try {
                // Not ideal for severe schema failures, but attempts to extract OriginalMsgId for rejection wrapper
                MxPain00100111 rawFetch = MxPain00100111.parse(xml);
                if (rawFetch != null && rawFetch.getCstmrCdtTrfInitn() != null && rawFetch.getCstmrCdtTrfInitn().getGrpHdr() != null) {
                   originalMsgId = rawFetch.getCstmrCdtTrfInitn().getGrpHdr().getMsgId();
                }
            } catch (Exception ignored) { }

            try (LoggingContext.Scope ignored = LoggingContext.withMsgId(originalMsgId)) {
                log.info("Received pain.001 validation request.");

                // Engage Core Validation Engine (Technical -> Semantic -> Business)
                validationService.validateIncomingMessage(xml);

                log.info("Message validated successfully.");
                report = ValidationReport.builder()
                    .valid(true)
                    .failedStage(null)
                    .errorMessages(Collections.emptyList())
                    .build();
            }
        } catch (ValidationException e) {
            try (LoggingContext.Scope ignored = LoggingContext.withMsgId(originalMsgId)) {
                metricsService.incrementValidationFailure("pain.001", e.getStage());
                log.warn("Validation Engine intercepted an invalid message at stage [{}]: {}", e.getStage(), e.getErrors());
                report = ValidationReport.builder()
                    .valid(false)
                    .failedStage(e.getStage())
                    .errorMessages(e.getErrors())
                    .build();
            }
        } catch (Exception e) {
            try (LoggingContext.Scope ignored = LoggingContext.withMsgId(originalMsgId)) {
                metricsService.incrementValidationFailure("pain.001", "CRITICAL_SYSTEM_ERROR");
                log.error("Gatekeeper Critical Exception during Validation Pipeline", e);
                 report = ValidationReport.builder()
                    .valid(false)
                    .failedStage("CRITICAL_SYSTEM_ERROR")
                    .errorMessages(Collections.singletonList("Unknown generic processing failure internally."))
                    .build();
            }
        }

        // Generate final Standard ISO Acknowledgement (pain.002.001.10) wrapper
        String responseXml;
        try (LoggingContext.Scope ignored = LoggingContext.withMsgId(originalMsgId)) {
            responseXml = statusGeneratorService.generateStatusReport(originalMsgId, report);
        }

        // A valid response is always returned to the downstream sender
        // HTTP Status 400 could be used strictly for false, however, XML ACK with RJCT natively handles it.
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(MediaType.APPLICATION_XML_VALUE))
            .body(responseXml);
    }
}
