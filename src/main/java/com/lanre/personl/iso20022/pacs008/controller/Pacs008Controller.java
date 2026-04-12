package com.lanre.personl.iso20022.pacs008.controller;

import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/pacs008")
@RequiredArgsConstructor
public class Pacs008Controller {

    private final Pacs008Service pacs008Service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generatePacs008(@RequestBody PaymentRequest request) {
        log.info("Received request to generate pacs.008 XML for: {} -> {}", request.getDebtorName(), request.getCreditorName());
        try {
            String xml = pacs008Service.generatePacs008Xml(request);
            return ResponseEntity.ok(xml);
        } catch (Exception e) {
            log.error("Failed to generate pacs.008 message", e);
            return ResponseEntity.internalServerError().body("<Error>" + e.getMessage() + "</Error>");
        }
    }
}
