package com.lanre.personl.iso20022.reconciliation.controller;

import com.lanre.personl.iso20022.reconciliation.service.Camt053Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Bank-to-Customer Statement Reconciliation.
 * Allows systems to upload bank statement reports (camt.053) to reconcile
 * existing payment instructions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final Camt053Service camt053Service;

    /**
     * Accepts a raw camt.053 XML statement and triggers the reconciliation logic.
     * This will scan the statement for EndToEndIds matching open 'SETTLING' payments.
     *
     * @param xml the raw ISO 20022 camt.053.001.10 XML payload
     * @return a message confirming the processing start
     */
    @PostMapping("/statement")
    public ResponseEntity<String> reconcileStatement(@RequestBody String xml) {
        log.info("Received statement upload for reconciliation.");
        try {
            camt053Service.processStatement(xml);
            return ResponseEntity.ok("Statement processed successfully. Reconciliation transitions logged.");
        } catch (Exception e) {
            log.error("Failed to process reconciliation statement", e);
            return ResponseEntity.internalServerError().body("Failed to process statement: " + e.getMessage());
        }
    }
}
