package com.lanre.personl.iso20022.lifecycle.service;

import com.lanre.personl.iso20022.lifecycle.entity.PaymentWorkflow;
import com.lanre.personl.iso20022.lifecycle.repository.PaymentWorkflowRepository;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.lanre.personl.iso20022.pain001.service.Pain001GeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "iso20022.security.audit-payload-storage-mode=HASH_ONLY"
})
class HashOnlyPayloadLifecycleIntegrationTest {

    @Autowired
    private Pain001GeneratorService pain001Service;

    @Autowired
    private PaymentWorkflowRepository workflowRepository;

    @Test
    @DisplayName("Should persist only hash metadata when hash-only payload storage is enabled")
    void shouldPersistOnlyHashMetadata() {
        String endToEndId = "E2E-HASH-ONLY-001";
        PaymentRequest request = PaymentRequest.builder()
                .debtorName("Hash Only Corp")
                .debtorIban("DE12345")
                .creditorName("Audit Archive Ltd")
                .creditorIban("FR67890")
                .amount(new BigDecimal("45.67"))
                .currency("EUR")
                .endToEndId(endToEndId)
                .remittanceInfo("Hash only audit test")
                .build();

        pain001Service.generatePain001Xml(request);

        PaymentWorkflow workflow = workflowRepository.findByEndToEndId(endToEndId).orElseThrow();
        assertEquals(1, workflow.getAuditLogs().size());
        assertNull(workflow.getAuditLogs().get(0).getPayload());
        assertNotNull(workflow.getAuditLogs().get(0).getPayloadHash());
        assertEquals("HASH_ONLY", workflow.getAuditLogs().get(0).getPayloadStorageType());
        assertNull(workflow.getAuditLogs().get(0).getPayloadReference());
    }
}
