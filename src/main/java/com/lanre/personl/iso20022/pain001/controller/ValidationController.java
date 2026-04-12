package com.lanre.personl.iso20022.pain001.controller;

import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import com.lanre.personl.iso20022.pain001.service.Iso20022ValidationService;
import com.lanre.personl.iso20022.pain001.service.Pain002StatusGeneratorService;
import com.prowidesoftware.swift.model.mx.MxPain00100111;
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
public class ValidationController {

    private final Iso20022ValidationService validationService;
    private final Pain002StatusGeneratorService statusGeneratorService;

    /**
     * Validates an incoming pain.001.001.11 message and responds with 
     * a pain.002.001.10 message natively wrapped around the validation state.
     *
     * @param xml The raw string payload representing the incoming XML file
     * @return pain.002 XML structured Response
     */
    @PostMapping(value = "/pain001", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> validatePain001(@RequestBody String xml) {
        log.info("Received pain.001 validation request.");
        
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

            // Engage Core Validation Engine (Technical -> Semantic -> Business)
            validationService.validateIncomingMessage(xml);

            log.info("Message validated successfully.");
            report = ValidationReport.builder()
                .valid(true)
                .failedStage(null)
                .errorMessages(Collections.emptyList())
                .build();
                
        } catch (ValidationException e) {
            log.warn("Validation Engine intercepted an invalid message at stage [{}]: {}", e.getStage(), e.getErrors());
            report = ValidationReport.builder()
                .valid(false)
                .failedStage(e.getStage())
                .errorMessages(e.getErrors())
                .build();
        } catch (Exception e) {
            log.error("Gatekeeper Critical Exception during Validation Pipeline", e);
             report = ValidationReport.builder()
                .valid(false)
                .failedStage("CRITICAL_SYSTEM_ERROR")
                .errorMessages(Collections.singletonList("Unknown generic processing failure internally."))
                .build();
        }

        // Generate final Standard ISO Acknowledgement (pain.002.001.10) wrapper
        String responseXml = statusGeneratorService.generateStatusReport(originalMsgId, report);

        // A valid response is always returned to the downstream sender
        // HTTP Status 400 could be used strictly for false, however, XML ACK with RJCT natively handles it.
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(responseXml);
    }
}
