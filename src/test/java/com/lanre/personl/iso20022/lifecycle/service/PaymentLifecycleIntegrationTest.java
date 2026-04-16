package com.lanre.personl.iso20022.lifecycle.service;

import com.lanre.personl.iso20022.lifecycle.entity.PaymentWorkflow;
import com.lanre.personl.iso20022.lifecycle.repository.PaymentWorkflowRepository;
import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.lanre.personl.iso20022.pain001.service.Pain001GeneratorService;
import com.lanre.personl.iso20022.reconciliation.service.Camt053Service;
import com.prowidesoftware.swift.model.mx.MxCamt05300110;
import com.prowidesoftware.swift.model.mx.dic.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentLifecycleIntegrationTest {

    @Autowired
    private Pain001GeneratorService pain001Service;

    @Autowired
    private Pacs008Service pacs008Service;

    @Autowired
    private Camt053Service camt053Service;

    @Autowired
    private PaymentWorkflowRepository workflowRepository;

    @Test
    @DisplayName("Should track payment through full E2E lifecycle (INIT -> SETTLE -> RECONCILE)")
    void shouldTrackFullLifecycle() {
        String e2eId = "E2E-" + UUID.randomUUID().toString().substring(0, 8);
        PaymentRequest request = PaymentRequest.builder()
                .debtorName("Lifecycle Corp")
                .debtorIban("DE12345")
                .creditorName("Statement LLC")
                .creditorIban("FR67890")
                .amount(new BigDecimal("123.45"))
                .currency("EUR")
                .endToEndId(e2eId)
                .remittanceInfo("Full Cycle Test")
                .build();

        // 1. INITIATED
        pain001Service.generatePain001Xml(request);
        
        Optional<PaymentWorkflow> workflowInit = workflowRepository.findByEndToEndId(e2eId);
        assertTrue(workflowInit.isPresent());
        assertEquals("PENDING", workflowInit.get().getStatus());
        assertEquals(1, workflowInit.get().getAuditLogs().size());
        assertNotNull(workflowInit.get().getAuditLogs().get(0).getPayload());
        assertTrue(workflowInit.get().getAuditLogs().get(0).getPayload().contains("[REDACTED]"));
        assertFalse(workflowInit.get().getAuditLogs().get(0).getPayload().contains("Lifecycle Corp"));

        // 2. SETTLING
        pacs008Service.generatePacs008Xml(request);
        
        Optional<PaymentWorkflow> workflowSettling = workflowRepository.findByEndToEndId(e2eId);
        assertEquals("SETTLING", workflowSettling.get().getStatus());
        assertEquals(2, workflowSettling.get().getAuditLogs().size());

        // 3. RECONCILED (camt.053)
        String camt053Xml = generateMockCamt053(e2eId, request.getAmount(), request.getCurrency());
        camt053Service.processStatement(camt053Xml);

        Optional<PaymentWorkflow> workflowReconciled = workflowRepository.findByEndToEndId(e2eId);
        assertEquals("RECONCILED", workflowReconciled.get().getStatus());
        assertEquals(3, workflowReconciled.get().getAuditLogs().size());
        
        // Verify RmtInf was carried through
        assertEquals("Full Cycle Test", workflowReconciled.get().getRemittanceInformation());
    }

    private String generateMockCamt053(String e2eId, BigDecimal amount, String ccy) {
        MxCamt05300110 mx = new MxCamt05300110();
        BankToCustomerStatementV10 b2c = new BankToCustomerStatementV10();
        
        AccountStatement11 stmt = new AccountStatement11();
        ReportEntry12 entry = new ReportEntry12();
        
        EntryDetails11 details = new EntryDetails11();
        EntryTransaction12 tx = new EntryTransaction12();
        TransactionReferences6 refs = new TransactionReferences6();
        refs.setEndToEndId(e2eId);
        tx.setRefs(refs);
        details.getTxDtls().add(tx);
        entry.getNtryDtls().add(details);
        
        stmt.getNtry().add(entry);
        b2c.getStmt().add(stmt);
        mx.setBkToCstmrStmt(b2c);
        
        return mx.message();
    }
}
