package com.lanre.personl.iso20022.pacs008.controller;

import com.lanre.personl.iso20022.pacs008.service.LegacyTranslatorService;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
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
@RequestMapping("/api/v1/translator")
@RequiredArgsConstructor
public class LegacyTranslatorController {

    private final LegacyTranslatorService translatorService;

    @PostMapping(value = "/mt103", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> translateMT103(@RequestBody String mt103Payload) {
        log.info("Received request to translate MT103 payload.");
        try {
            MxPacs00800110 pacs008 = translatorService.translateMT103ToPacs008(mt103Payload);
            return ResponseEntity.ok(pacs008.message());
        } catch (Exception e) {
            log.error("Failed to translate MT103 message", e);
            return ResponseEntity.badRequest()
                    .body("<Error>Failed to parse or translate MT103 snippet: " + e.getMessage() + "</Error>");
        }
    }
}
