package com.lanre.personl.iso20022.lifecycle.service;

import com.lanre.personl.iso20022.lifecycle.entity.IsoMessageAudit;
import com.lanre.personl.iso20022.lifecycle.entity.PaymentWorkflow;
import com.lanre.personl.iso20022.lifecycle.repository.PaymentWorkflowRepository;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.lanre.personl.iso20022.security.AuditPayloadProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Stateful Lifecycle Orchestrator for ISO 20022 Payments.
 * <p>
 * This service manages the "Golden Thread" of a transaction by correlating multiple message types
 * (pain.001, pacs.008, camt.053, etc.) under a single {@link PaymentWorkflow} entity.
 * It uses the ISO-standard {@code EndToEndId} as the primary correlation key.
 * </p>
 *
 * <h3>Key Stages:</h3>
 * <ul>
 *   <li><b>PENDING</b>: Initiated via pain.001</li>
 *   <li><b>SETTLING</b>: Interbank settlement instruction (pacs.008) triggered</li>
 *   <li><b>RECONCILED</b>: Confirmed via bank statement (camt.053)</li>
 *   <li><b>FAILED</b>: Rejection reported via status return (pacs.002)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleService {

    private final PaymentWorkflowRepository workflowRepository;
    private final AuditPayloadProtectionService auditPayloadProtectionService;

    /**
     * Step 1: Initialize the workflow from a pain.001 request.
     */
    @Transactional
    public void startWorkflow(PaymentRequest request, String xml, String msgId) {
        log.info("Starting lifecycle for EndToEndId: {}", request.getEndToEndId());
        
        PaymentWorkflow workflow = PaymentWorkflow.builder()
                .endToEndId(request.getEndToEndId())
                .status("PENDING")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .debtorName(request.getDebtorName())
                .creditorName(request.getCreditorName())
                .remittanceInformation(request.getRemittanceInfo())
                .build();

        addAuditLog(workflow, "pain.001", msgId, xml);
        workflowRepository.save(workflow);
    }

    /**
     * Step 2: Update workflow to SETTLING when pacs.008 is generated.
     */
    @Transactional
    public void markAsSettling(String endToEndId, String xml, String msgId) {
        log.info("Transitioning workflow to SETTLING for EndToEndId: {}", endToEndId);
        
        workflowRepository.findByEndToEndId(endToEndId).ifPresent(workflow -> {
            workflow.setStatus("SETTLING");
            addAuditLog(workflow, "pacs.008", msgId, xml);
            workflowRepository.save(workflow);
        });
    }

    /**
     * Step 3: Update workflow to RECONCILED when camt.053 matches.
     */
    @Transactional
    public void markAsReconciled(String endToEndId, String xml, String msgId) {
        log.info("Transitioning workflow to RECONCILED for EndToEndId: {}", endToEndId);
        
        Optional<PaymentWorkflow> workflowOpt = workflowRepository.findByEndToEndId(endToEndId);
        if (workflowOpt.isPresent()) {
            PaymentWorkflow workflow = workflowOpt.get();
            workflow.setStatus("RECONCILED");
            addAuditLog(workflow, "camt.053", msgId, xml);
            workflowRepository.save(workflow);
        } else {
            log.warn("Reconciliation entry for unknown EndToEndId: {}. Logging as orphaned.", endToEndId);
            // In a production app, we would create an 'Orphaned' record here.
        }
    }

    /**
     * Mark as FAILED due to pacs.002 status report.
     */
    @Transactional
    public void markAsFailed(String endToEndId, String xml, String msgId, String reason) {
        log.info("Transitioning workflow to FAILED for EndToEndId: {}. Reason: {}", endToEndId, reason);
        
        workflowRepository.findByEndToEndId(endToEndId).ifPresent(workflow -> {
            workflow.setStatus("FAILED");
            addAuditLog(workflow, "pacs.002", msgId, xml);
            workflowRepository.save(workflow);
        });
    }

    private void addAuditLog(PaymentWorkflow workflow, String type, String msgId, String payload) {
        IsoMessageAudit audit = IsoMessageAudit.builder()
                .messageType(type)
                .messageId(msgId)
                .payload(auditPayloadProtectionService.protect(payload))
                .workflow(workflow)
                .build();
        workflow.getAuditLogs().add(audit);
    }
}
