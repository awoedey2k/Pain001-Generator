package com.lanre.personl.iso20022.pacs008.controller;

import com.lanre.personl.iso20022.logging.LoggingContext;
import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import com.lanre.personl.iso20022.routing.service.Pacs002Service;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/v1/validate/pacs008")
@RequiredArgsConstructor
@Tag(name = "validation", description = "Inbound ISO 20022 validation and status-report generation.")
@SecurityRequirement(name = "basicAuth")
public class Pacs008ValidationController {

    private final Pacs008Service pacs008Service;
    private final Pacs002Service statusGeneratorService;

    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Validate pacs.008.001.10 XML",
            description = "Runs validation checks and returns a pacs.002 status report aligned to FI-to-FI validation flows."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = {
                    @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "pacs008Xml",
                                    value = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.10\">...</Document>"
                            )),
                    @Content(mediaType = MediaType.TEXT_XML_VALUE,
                            schema = @Schema(type = "string"))
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "pacs.002 validation status report",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Message validation failed",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks WRITER or ADMIN role"),
            @ApiResponse(responseCode = "413", description = "Payload exceeds configured request-size limit")
    })
    public ResponseEntity<String> validatePacs008(@RequestBody String xmlPayload) {
        try {
            MxPacs00800110 parsedMessage = pacs008Service.validateAndParse(xmlPayload);
            String msgId = parsedMessage.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId();
            String endToEndId = extractEndToEndId(parsedMessage);

            try (LoggingContext.Scope ignored = LoggingContext.withIdentifiers(msgId, endToEndId)) {
                log.info("Received pacs.008 validation request.");
            
                // Generate ACCP status as pacs.002 for FI-to-FI validation flows
                ValidationReport report = ValidationReport.builder()
                        .valid(true)
                        .errorMessages(Collections.emptyList())
                        .build();

                String responseXml = statusGeneratorService.generateValidationStatusReport(msgId, report);
                return ResponseEntity.ok(responseXml);
            }
            
        } catch (ValidationException ve) {
            try (LoggingContext.Scope ignored = LoggingContext.withMsgId("UNKNOWN-PACS-ID")) {
                log.error("Gatekeeper bounced pacs.008 message at stage: {}", ve.getStage());
            
                ValidationReport report = ValidationReport.builder()
                        .valid(false)
                        .failedStage(ve.getStage())
                        .errorMessages(ve.getErrors())
                        .build();

                String responseXml = statusGeneratorService.generateValidationStatusReport("UNKNOWN-PACS-ID", report);
                return ResponseEntity.badRequest().body(responseXml);
            }
            
        } catch (Exception e) {
            try (LoggingContext.Scope ignored = LoggingContext.withMsgId("UNKNOWN-PACS-ID")) {
                log.error("Unknown critical error during validation", e);
                ValidationReport report = ValidationReport.builder()
                        .valid(false)
                        .failedStage("SYSTEM_ERROR")
                        .errorMessages(Collections.singletonList(e.getMessage()))
                        .build();
                return ResponseEntity.internalServerError().body(statusGeneratorService.generateValidationStatusReport("UNKNOWN-PACS-ID", report));
            }
        }
    }

    private String extractEndToEndId(MxPacs00800110 parsedMessage) {
        try {
            return parsedMessage.getFIToFICstmrCdtTrf()
                    .getCdtTrfTxInf()
                    .get(0)
                    .getPmtId()
                    .getEndToEndId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
