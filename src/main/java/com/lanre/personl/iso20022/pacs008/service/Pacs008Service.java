package com.lanre.personl.iso20022.pacs008.service;

import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ISO 20022 Interbank Settlement Engine (pacs.008).
 * <p>
 * This service handles the generation and gatekeeper validation of FI-to-FI Customer
 * Credit Transfer messages (pacs.008.001.10).
 * </p>
 *
 * <p><b>Gatekeeper Validation (3-Stage):</b></p>
 * <ol>
 *   <li><b>Technical</b>: Schema validation against official ISO 20022 XSDs.</li>
 *   <li><b>Business</b>: Strict BIC11 format verification and field presence.</li>
 * </ol>
 */
@Slf4j
@Service
public class Pacs008Service {

    private final com.lanre.personl.iso20022.lifecycle.service.LifecycleService lifecycleService;
    private final Validator xsdValidator;
    private static final Pattern BIC11_PATTERN = Pattern.compile("^[A-Z]{6,6}[A-Z2-9][A-NP-Z0-9]([A-Z0-9]{3,3})?$");

    public Pacs008Service(com.lanre.personl.iso20022.lifecycle.service.LifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
        Validator temp = null;
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            ClassPathResource xsdResource = new ClassPathResource("xsd/pacs.008.001.10.xsd");
            try (var xsdStream = xsdResource.getInputStream()) {
                StreamSource xsdSource = new StreamSource(xsdStream);
                xsdSource.setSystemId(xsdResource.getURL().toExternalForm());
                Schema schema = factory.newSchema(xsdSource);
                temp = schema.newValidator();
            }
        } catch (Exception e) {
            log.warn("pacs.008.001.10.xsd validation fallback ignored due to absence. Operating without strict XSD.");
        }
        this.xsdValidator = temp;
    }

    /**
     * Validates a raw pacs.008 Interbank Settlement string payload.
     * Enforces XSD checking (Technical) and BIC11 standards (Business).
     */
    public MxPacs00800110 validateAndParse(String xml) {
        log.info("Starting pacs.008 Gatekeeper Validation...");

        // 1. Stage 1: Technical XSD check
        if (xsdValidator != null) {
            try {
                List<String> exceptions = new ArrayList<>();
                xsdValidator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                    public void warning(SAXParseException e) { exceptions.add(e.getMessage()); }
                    public void error(SAXParseException e) { exceptions.add(e.getMessage()); }
                    public void fatalError(SAXParseException e) { exceptions.add(e.getMessage()); }
                });
                xsdValidator.validate(new StreamSource(new StringReader(xml)));

                if (!exceptions.isEmpty()) {
                    throw new ValidationException("STAGE_1_TECHNICAL", exceptions);
                }
            } catch (ValidationException ve) {
                throw ve;
            } catch (Exception e) {
                throw new ValidationException("STAGE_1_TECHNICAL", List.of("XSD Evaluation Failed: " + e.getMessage()));
            }
        }

        // 2. Map Payload securely
        MxPacs00800110 parsedMessage = MxPacs00800110.parse(xml);
        FIToFICustomerCreditTransferV10 transfer = parsedMessage.getFIToFICstmrCdtTrf();

        if (transfer == null) {
            throw new ValidationException("STAGE_1_TECHNICAL", List.of("Invalid document structure: Missing FIToFICstmrCdtTrf payload."));
        }

        // 3. Stage 2: Business Checks (BIC11 formats on InstgAgt and InstdAgt)
        List<String> businessErrors = new ArrayList<>();
        GroupHeader96 grpHdr = transfer.getGrpHdr();
        
        if (grpHdr != null) {
            if (grpHdr.getInstgAgt() != null && grpHdr.getInstgAgt().getFinInstnId() != null) {
                String instgBic = grpHdr.getInstgAgt().getFinInstnId().getBICFI();
                if (!isValidBic11(instgBic)) {
                    businessErrors.add("InstgAgt BICFI violates BIC11 requirements: " + instgBic);
                }
            }

            if (grpHdr.getInstdAgt() != null && grpHdr.getInstdAgt().getFinInstnId() != null) {
                String instdBic = grpHdr.getInstdAgt().getFinInstnId().getBICFI();
                if (!isValidBic11(instdBic)) {
                    businessErrors.add("InstdAgt BICFI violates BIC11 requirements: " + instdBic);
                }
            }
        }

        if (!businessErrors.isEmpty()) {
            throw new ValidationException("STAGE_2_BUSINESS", businessErrors);
        }

        return parsedMessage;
    }

    private boolean isValidBic11(String bic) {
        if (bic == null) return false;
        // Allows exactly 8 or exactly 11 characters conforming to SWIFT BIC constraints
        return BIC11_PATTERN.matcher(bic).matches() && (bic.length() == 8 || bic.length() == 11);
    }

    /**
     * Generates a valid pacs.008.001.10 XML from a PaymentRequest.
     */
    public String generatePacs008Xml(com.lanre.personl.iso20022.pain001.model.PaymentRequest request) {
        log.info("Generating pacs.008.001.10 XML for interbank settlement: {} -> {}", request.getDebtorName(), request.getCreditorName());
        
        MxPacs00800110 mx = new MxPacs00800110();
        FIToFICustomerCreditTransferV10 pacs = new FIToFICustomerCreditTransferV10();
        
        // 1. Group Header
        GroupHeader96 grpHdr = new GroupHeader96();
        grpHdr.setMsgId("PACS8-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        try {
            grpHdr.setCreDtTm(javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(new java.util.GregorianCalendar()));
        } catch (Exception ignored) {}
        grpHdr.setNbOfTxs("1");
        
        SettlementInstruction11 sttlmInf = new SettlementInstruction11();
        sttlmInf.setSttlmMtd(SettlementMethod1Code.CLRG); // Clearing
        grpHdr.setSttlmInf(sttlmInf);

        ActiveCurrencyAndAmount ttlAmt = new ActiveCurrencyAndAmount();
        ttlAmt.setValue(request.getAmount());
        ttlAmt.setCcy(request.getCurrency());
        grpHdr.setTtlIntrBkSttlmAmt(ttlAmt);
        
        // Instructing/Instructed agents (Using BICs or defaults)
        grpHdr.setInstgAgt(createAgent(request.getDebtorBic() != null ? request.getDebtorBic() : "CITIUS33XXX"));
        grpHdr.setInstdAgt(createAgent(request.getCreditorBic() != null ? request.getCreditorBic() : "BOFAGB2L"));
        
        pacs.setGrpHdr(grpHdr);
        
        // 2. Transaction Information
        CreditTransferTransaction50 tx = new CreditTransferTransaction50();
        PaymentIdentification13 pmtId = new PaymentIdentification13();
        
        String e2eId = request.getEndToEndId();
        if (e2eId == null || e2eId.trim().isEmpty()) {
            e2eId = "E2E-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            request.setEndToEndId(e2eId);
        }
        pmtId.setEndToEndId(e2eId);
        pmtId.setTxId("TX-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        tx.setPmtId(pmtId);
        
        ActiveCurrencyAndAmount intrBkAmt = new ActiveCurrencyAndAmount();
        intrBkAmt.setValue(request.getAmount());
        intrBkAmt.setCcy(request.getCurrency());
        tx.setIntrBkSttlmAmt(intrBkAmt);

        ActiveOrHistoricCurrencyAndAmount instdAmt = new ActiveOrHistoricCurrencyAndAmount();
        instdAmt.setValue(request.getAmount());
        instdAmt.setCcy(request.getCurrency());
        tx.setInstdAmt(instdAmt);
        
        tx.setChrgBr(ChargeBearerType1Code.SLEV); // Shared
        
        // Debtor
        tx.setDbtr(createParty(request.getDebtorName()));
        tx.setDbtrAcct(createAccount(request.getDebtorIban()));
        tx.setDbtrAgt(createAgent(request.getDebtorBic() != null ? request.getDebtorBic() : "CITIUS33XXX"));
        
        // Creditor
        tx.setCdtr(createParty(request.getCreditorName()));
        tx.setCdtrAcct(createAccount(request.getCreditorIban()));
        tx.setCdtrAgt(createAgent(request.getCreditorBic() != null ? request.getCreditorBic() : "BOFAGB2L"));
        
        pacs.addCdtTrfTxInf(tx);
        mx.setFIToFICstmrCdtTrf(pacs);
        
        String xml = mx.message();

        // ── 3. Lifecycle Tracking ───────────────────────────────────────
        lifecycleService.markAsSettling(request.getEndToEndId(), xml, grpHdr.getMsgId());
        
        return xml;
    }

    private BranchAndFinancialInstitutionIdentification6 createAgent(String bic) {
        BranchAndFinancialInstitutionIdentification6 agent = new BranchAndFinancialInstitutionIdentification6();
        FinancialInstitutionIdentification18 finId = new FinancialInstitutionIdentification18();
        finId.setBICFI(bic);
        agent.setFinInstnId(finId);
        return agent;
    }

    private PartyIdentification135 createParty(String name) {
        PartyIdentification135 party = new PartyIdentification135();
        party.setNm(name);
        return party;
    }

    private CashAccount40 createAccount(String iban) {
        CashAccount40 account = new CashAccount40();
        AccountIdentification4Choice id = new AccountIdentification4Choice();
        id.setIBAN(iban);
        account.setId(id);
        return account;
    }
}
