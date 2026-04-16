package com.lanre.personl.iso20022.routing.service;

import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.lanre.personl.iso20022.routing.entity.PaymentRoutingAudit;
import com.lanre.personl.iso20022.routing.repository.PaymentRoutingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentRouterServiceIntegrationTest {

    @Autowired
    private PaymentRouterService routerService;

    @Autowired
    private Pacs008Service pacs008Service;

    @Autowired
    private PaymentRoutingRepository repository;

    @Test
    @DisplayName("Should route EUR payment to SEPA Mock Service and Audit entry")
    void shouldRouteSepaAndAudit() {
        PaymentRequest request = PaymentRequest.builder()
            .debtorName("Switch User")
            .debtorIban("DE12345")
            .creditorName("EU Recipient")
            .creditorIban("FR67890")
            .amount(new BigDecimal("2500.00"))
            .currency("EUR")
            .debtorBic("NORMALBICXX")
            .build();

        String pacs008Xml = pacs008Service.generatePacs008Xml(request);

        String result = routerService.processAndRoute(pacs008Xml);

        // Assert BAH wrapping
        assertTrue(result.contains("AppHdrAndMsg"));
        assertTrue(result.contains("urn:iso:std:iso:20022:tech:xsd:head.001.001.03"));
        assertTrue(result.contains("pacs.008.001.10"));

        // Assert Persistence
        List<PaymentRoutingAudit> audits = repository.findAll();
        assertFalse(audits.isEmpty());
        PaymentRoutingAudit latest = audits.get(audits.size() - 1);
        assertEquals("EUR", latest.getCurrency());
        assertEquals("SEPA-MOCK-SERVICE", latest.getDestinationRoute());
    }

    @Test
    @DisplayName("Should prioritize High-Value BICs over Currency rules")
    void shouldPrioritizeHighValue() {
        PaymentRequest request = PaymentRequest.builder()
            .debtorName("Switch User")
            .debtorIban("US12345")
            .creditorName("Priority Recipient")
            .creditorIban("US67890")
            .amount(new BigDecimal("50000.00"))
            .currency("USD")
            .creditorBic("CHASUS33XXX") // High Value BIC
            .build();

        String pacs008Xml = pacs008Service.generatePacs008Xml(request);

        String result = routerService.processAndRoute(pacs008Xml);

        assertTrue(result.contains("urn:iso:std:iso:20022:tech:xsd:head.001.001.03"));
        List<PaymentRoutingAudit> audits = repository.findAll();
        PaymentRoutingAudit latest = audits.get(audits.size() - 1);
        assertEquals("HIGH-VALUE-PRIORITY-QUEUE", latest.getDestinationRoute());
        assertTrue(latest.isHighValue());
    }

    @Test
    @DisplayName("Should generate pacs.002 rejection report for unsupported currency")
    void shouldRejectUnsupportedCurrency() {
        PaymentRequest request = PaymentRequest.builder()
            .debtorName("Switch User")
            .debtorIban("GB12345")
            .creditorName("UK Recipient")
            .creditorIban("GB67890")
            .amount(new BigDecimal("100.00"))
            .currency("GBP") // Unsupported currency
            .debtorBic("NORMALBICXX")
            .build();

        String pacs008Xml = pacs008Service.generatePacs008Xml(request);

        String result = routerService.processAndRoute(pacs008Xml);

        // Assert pacs.002 rejection response
        assertTrue(result.contains("FIToFIPmtStsRpt"));
        assertTrue(result.contains("NoRoutingRule"));
        
        // Assert Persistence shows NONE for destination
        List<PaymentRoutingAudit> audits = repository.findAll();
        PaymentRoutingAudit latest = audits.get(audits.size() - 1);
        assertEquals("NONE", latest.getDestinationRoute());
    }
}
