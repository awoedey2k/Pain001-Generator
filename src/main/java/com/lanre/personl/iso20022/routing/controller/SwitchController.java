package com.lanre.personl.iso20022.routing.controller;

import com.lanre.personl.iso20022.routing.service.PaymentRouterService;
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
@RequestMapping("/api/v1/switch")
@RequiredArgsConstructor
public class SwitchController {

    private final PaymentRouterService routerService;

    /**
     * Entry point for the modern Interbank Clearing Switch.
     * Routes pacs.008 messages across different simulating Market Infrastructures.
     */
    @PostMapping(value = "/route", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> routePayment(@RequestBody String xmlPayload) {
        log.info("Switch controller received routing request.");
        try {
            String response = routerService.processAndRoute(xmlPayload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Switch failed to process payment", e);
            // In a real switch, we'd return a system fault pacs.002
            return ResponseEntity.badRequest().body("<Error>" + e.getMessage() + "</Error>");
        }
    }
}
