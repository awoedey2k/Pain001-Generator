package com.lanre.personl.iso20022.reconciliation.service;

import com.lanre.personl.iso20022.lifecycle.service.LifecycleService;
import com.prowidesoftware.swift.model.mx.MxCamt05300110;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ISO 20022 camt.053 Reconciliation Engine.
 * <p>
 * This service parses Bank-to-Customer Statement (camt.053) messages (SRU2023)
 * to perform transaction reconciliation. It deep-scans account entries and
 * associated transaction details to extract the {@code EndToEndId}.
 * </p>
 *
 * <p><b>Reconciliation Logic:</b></p>
 * Each matched EndToEndId is reported back to the {@link LifecycleService}
 * to flip the payment status to 'RECONCILED'.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Camt053Service {

    private final LifecycleService lifecycleService;

    /**
     * Parses a camt.053.001.10 Bank-to-Customer Statement and reconciles entries.
     */
    public void processStatement(String xml) {
        log.info("Processing Bank-to-Customer Statement (camt.053)...");
        
        MxCamt05300110 mx = MxCamt05300110.parse(xml);
        BankToCustomerStatementV10 statementReport = mx.getBkToCstmrStmt();
        String msgId = mx.getAppHdr() != null ? mx.getAppHdr().messageName() : "CAMT053-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        for (AccountStatement11 stmt : statementReport.getStmt()) {
            for (ReportEntry12 entry : stmt.getNtry()) {
                processEntry(entry, xml, msgId);
            }
        }
    }

    private void processEntry(ReportEntry12 entry, String fullXml, String msgId) {
        for (EntryDetails11 details : entry.getNtryDtls()) {
            for (EntryTransaction12 tx : details.getTxDtls()) {
                TransactionReferences6 refs = tx.getRefs();
                if (refs != null && refs.getEndToEndId() != null) {
                    String e2eId = refs.getEndToEndId();
                    log.info("Found reconciliation candidate: EndToEndId={}", e2eId);
                    
                    // Trigger Lifecycle update
                    lifecycleService.markAsReconciled(e2eId, fullXml, msgId);
                }
            }
        }
    }
}
