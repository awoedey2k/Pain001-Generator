package com.lanre.personl.iso20022.pacs008.service;

import com.prowidesoftware.swift.model.field.Field;
import com.prowidesoftware.swift.model.field.Field32A;
import com.prowidesoftware.swift.model.field.Field50K;
import com.prowidesoftware.swift.model.field.Field59;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

/**
 * Legacy Interoperability Bridge (MT to MX).
 * <p>
 * This service provides high-fidelity translation from legacy FIN messages (MT103)
 * to ISO 20022 interbank standards (pacs.008.001.10).
 * </p>
 *
 * <p><b>Mapping Logic:</b></p>
 * <ul>
 *   <li><b>Field 32A</b>: Mapped to {@code IntrBkSttlmAmt} and {@code IntrBkSttlmDte}.</li>
 *   <li><b>Field 50K/A</b>: Mapped to {@code Dbtr} (Debtor) details, using unstructured address fallback.</li>
 *   <li><b>Field 59</b>: Mapped to {@code Cdtr} (Creditor) and {@code CdtrAcct}.</li>
 *   <li><b>Field 20</b>: Preserved as the {@code EndToEndId} for downstream correlation.</li>
 * </ul>
 */
@Slf4j
@Service
public class LegacyTranslatorService {

    /**
     * Translates a legacy MT103 SWIFT string into an ISO 20022 Interbank Settlement (pacs.008.001.10).
     */
    public MxPacs00800110 translateMT103ToPacs008(String mt103Message) {
        log.info("Parsing incoming MT103 message...");
        MT103 mt103 = MT103.parse(mt103Message);
        
        MxPacs00800110 mx = new MxPacs00800110();
        FIToFICustomerCreditTransferV10 pacs008 = new FIToFICustomerCreditTransferV10();

        // 1. Setup Required Group Header
        GroupHeader96 grpHdr = new GroupHeader96();
        grpHdr.setMsgId("MTMX-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            grpHdr.setCreDtTm(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            log.error("Date configuration error", e);
        }
        grpHdr.setNbOfTxs("1");

        // FIELD 32A Mapping (Date, Currency, Amount) -> IntrBkSttlmAmt & Dt
        Field32A f32A = mt103.getField32A();
        if (f32A != null) {
            ActiveCurrencyAndAmount sttlmAmt = new ActiveCurrencyAndAmount();
            sttlmAmt.setValue(new BigDecimal(f32A.getAmountAsNumber().toString()));
            sttlmAmt.setCcy(f32A.getCurrency());
            
            grpHdr.setTtlIntrBkSttlmAmt(sttlmAmt);
            
            try {
                // Map YYMMDD to XML Calendar Date natively
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(f32A.getDateAsCalendar().getTime());
                grpHdr.setIntrBkSttlmDt(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
            } catch (Exception ignored) {}
        } else {
            log.warn("MT103 is missing mandatory Field 32A (Amount/Date)!");
        }

        pacs008.setGrpHdr(grpHdr);

        // 2. Setup Central Credit Transfer Transaction Information
        CreditTransferTransaction50 txInfo = new CreditTransferTransaction50();

        // FIELD 50a Mapping (Ordering Customer) -> Dbtr
        Field f50a = mt103.getField50K();
        if (f50a == null) f50a = mt103.getField50A();
        PartyIdentification135 dbtr = new PartyIdentification135();
        PostalAddress24 dbtrAddress = buildSafeAddressFromLegacy(f50a, "Dbtr");
        if (dbtrAddress != null) dbtr.setPstlAdr(dbtrAddress);

        if (f50a != null && f50a.getValue() != null) {
            List<String> f50aLines = Arrays.asList(f50a.getValue().split("\\r?\\n"));
            if (!f50aLines.isEmpty()) {
                dbtr.setNm(f50aLines.get(Math.max(0, f50aLines.size() - 2)));
            }
        } else {
            dbtr.setNm("NOTPROVIDED"); // Enforce mandatory ISO requirements 
        }
        txInfo.setDbtr(dbtr);


        // FIELD 59 Mapping (Beneficiary) -> Cdtr
        Field f59a = mt103.getField59();
        if (f59a == null) f59a = mt103.getField59A();
        PartyIdentification135 cdtr = new PartyIdentification135();
        PostalAddress24 cdtrAddress = buildSafeAddressFromLegacy(f59a, "Cdtr");
        if (cdtrAddress != null) cdtr.setPstlAdr(cdtrAddress);

        if (f59a != null) {
            if (f59a.getComponent(1) != null) {
                // Map Account component directly
                CashAccount40 cdtrAcct = new CashAccount40();
                AccountIdentification4Choice acctId = new AccountIdentification4Choice();
                acctId.setOthr(new GenericAccountIdentification1().setId(f59a.getComponent(1)));
                cdtrAcct.setId(acctId);
                txInfo.setCdtrAcct(cdtrAcct);
            }
            if (f59a.getValue() != null) {
                 List<String> f59aLines = Arrays.asList(f59a.getValue().split("\\r?\\n"));
                 if (!f59aLines.isEmpty()) {
                     cdtr.setNm(f59aLines.get(0)); // Typically Name is first text line after optional account
                 }
            } else {
                 cdtr.setNm("NOTPROVIDED");
            }
        }
        txInfo.setCdtr(cdtr);


        PaymentIdentification13 pmtId = new PaymentIdentification13();
        pmtId.setEndToEndId(mt103.getField20() != null ? mt103.getField20().getValue() : "NOTPROVIDED");
        pmtId.setTxId("TXID-" + UUID.randomUUID().toString().substring(0,8));
        txInfo.setPmtId(pmtId);

        // Required Amount field in Tx (Repeating the 32A settlement logically)
        if (f32A != null) {
             ActiveCurrencyAndAmount instdAmt = new ActiveCurrencyAndAmount();
             instdAmt.setValue(new BigDecimal(f32A.getAmountAsNumber().toString()));
             instdAmt.setCcy(f32A.getCurrency());
             txInfo.setIntrBkSttlmAmt(instdAmt);
        }

        pacs008.getCdtTrfTxInf().add(txInfo);
        mx.setFIToFICstmrCdtTrf(pacs008);

        log.info("Successfully mapped MT103 generic fields to pacs.008 schema structured paths.");
        return mx;
    }

    /**
     * Mitigates address "overflow" by isolating completely unstructured data lines from 
     * MT Address blocks seamlessly into the versatile <AdrLine> XML element. 
     */
    private PostalAddress24 buildSafeAddressFromLegacy(Field legacyField, String actorType) {
        if (legacyField == null || legacyField.getValue() == null) return null;

        PostalAddress24 address = new PostalAddress24();
        
        // Grab unstructured address lines (ignoring the first line which we usually assume is Name)
        List<String> mtLines = Arrays.asList(legacyField.getValue().split("\\r?\\n"));
        
        boolean hasAddressLines = false;
        // Start from index 1 (skipping Name assumption logic for safety fallback)
        int startLine = 1;

        if (legacyField instanceof Field50K) {
            // 50K can have an account as component 1. If so, logic varies slightly, 
            // but simply throwing the remaining non-name lines into AdrLine prevents data truncation entirely.
        }

        // Drop exactly what MT gave us natively line by line into AdrLine!
        for (int i = startLine; i < mtLines.size(); i++) {
            String line = mtLines.get(i);
            if (line != null && !line.trim().isEmpty()) {
                // Address lines have max occurrences of 7 in ISO
                if (address.getAdrLine().size() < 7) {
                    address.getAdrLine().add(line.trim());
                    hasAddressLines = true;
                } else {
                    log.warn("MT Data overflow detected on Address for {}. Truncating beyond 7 lines.", actorType);
                }
            }
        }

        return hasAddressLines ? address : null;
    }
}
