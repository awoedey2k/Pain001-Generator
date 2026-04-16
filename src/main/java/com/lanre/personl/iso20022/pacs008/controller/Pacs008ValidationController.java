package com.lanre.personl.iso20022.pacs008.controller;

import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import com.lanre.personl.iso20022.routing.service.Pacs002Service;
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
public class Pacs008ValidationController {

    private final Pacs008Service pacs008Service;
    private final Pacs002Service statusGeneratorService;

    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> validatePacs008(@RequestBody String xmlPayload) {
        log.info("Received pacs.008 validation request.");
        
        try {
            MxPacs00800110 parsedMessage = pacs008Service.validateAndParse(xmlPayload);
            
            // Generate ACCP status as pacs.002 for FI-to-FI validation flows
            ValidationReport report = ValidationReport.builder()
                    .valid(true)
                    .errorMessages(Collections.emptyList())
                    .build();
                    
            String msgId = parsedMessage.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId();
            String responseXml = statusGeneratorService.generateValidationStatusReport(msgId, report);
            return ResponseEntity.ok(responseXml);
            
        } catch (ValidationException ve) {
            log.error("Gatekeeper bounced pacs.008 message at stage: {}", ve.getStage());
            
            ValidationReport report = ValidationReport.builder()
                    .valid(false)
                    .failedStage(ve.getStage())
                    .errorMessages(ve.getErrors())
                    .build();
                    
            String responseXml = statusGeneratorService.generateValidationStatusReport("UNKNOWN-PACS-ID", report);
            return ResponseEntity.badRequest().body(responseXml);
            
        } catch (Exception e) {
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
