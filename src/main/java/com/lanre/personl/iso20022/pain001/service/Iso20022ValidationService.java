package com.lanre.personl.iso20022.pain001.service;

import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.prowidesoftware.swift.model.mx.MxPain00100111;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV11;
import com.prowidesoftware.swift.model.mx.dic.PaymentInstruction40;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransaction54;
import com.prowidesoftware.swift.model.mx.dic.PartyIdentification135;
import com.prowidesoftware.swift.model.mx.dic.PostalAddress24;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * ISO 20022 Gatekeeper Validation Service.
 * <p>
 * This service implements a rigorous 3-stage validation pipeline for inbound
 * pain.001.001.11 messages (SRU2023):
 * </p>
 *
 * <ol>
 *   <li><b>STAGE 1: Technical XSD</b> - Schema conformity check.</li>
 *   <li><b>STAGE 2: Semantic</b> - ISO code list verification (Currency, Countries).</li>
 *   <li><b>STAGE 3: Business</b> - Logical constraints (Execution dates, weekend checks).</li>
 * </ol>
 */
@Slf4j
@Service
public class Iso20022ValidationService {

    private final Schema xsdSchema;

    public Iso20022ValidationService() {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            ClassPathResource xsdResource = new ClassPathResource("xsd/pain.001.001.11.xsd");
            try (var xsdStream = xsdResource.getInputStream()) {
                StreamSource xsdSource = new StreamSource(xsdStream);
                xsdSource.setSystemId(xsdResource.getURL().toExternalForm());
                this.xsdSchema = factory.newSchema(xsdSource);
            }
        } catch (Exception e) {
            log.error("Failed to initialize XSD Validator: ", e);
            throw new RuntimeException("Validation Engine could not start. Missing or invalid XSD.");
        }
    }

    /**
     * Executes the full pipeline: Stage 1 (Technical), Stage 2 (Semantic), Stage 3 (Business).
     * @param xml Incoming raw ISO 20022 Message
     * @return MxPain00100111 The completely verified and parsed Java object model
     */
    public MxPain00100111 validateIncomingMessage(String xml) {
        log.info("Starting Gatekeeper Validation Pipeline...");

        // ======================================
        // STAGE 1: Technical (XSD Validation)
        // ======================================
        performStage1TechnicalValidation(xml);

        // Parse to completely typed models via Prowide now that its XML structure is guaranteed.
        MxPain00100111 parsedMessage = MxPain00100111.parse(xml);
        CustomerCreditTransferInitiationV11 document = parsedMessage.getCstmrCdtTrfInitn();
        
        if (document == null || document.getPmtInf().isEmpty()) {
            throw new ValidationException("STAGE_1_TECHNICAL", 
                 List.of("The document lacks a valid CustomerCreditTransferInitiationV11 payload or PaymentInformation block."));
        }

        // ======================================
        // STAGE 2: Semantic (ISO Standards)
        // ======================================
        performStage2SemanticValidation(document);

        // ======================================
        // STAGE 3: Business (Custom Constraints)
        // ======================================
        performStage3BusinessValidation(document);

        log.info("Gatekeeper Check: PASSED. Message is perfectly valid.");
        return parsedMessage;
    }

    private void performStage1TechnicalValidation(String xml) {
        log.debug("Executing Stage 1 (Technical XSD Check)");
        try {
            Validator xsdValidator = xsdSchema.newValidator();

            // Using a thread-safe custom ErrorHandler to collect all SAX errors instead of failing at the first drop.
            List<String> exceptions = new ArrayList<>();
            xsdValidator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                public void warning(SAXParseException e) { exceptions.add("Line " + e.getLineNumber() + ": " + e.getMessage()); }
                public void error(SAXParseException e) { exceptions.add("Line " + e.getLineNumber() + ": " + e.getMessage()); }
                public void fatalError(SAXParseException e) { exceptions.add("Fatal [Line " + e.getLineNumber() + "]: " + e.getMessage()); }
            });
            
            // Perform schema validation
            xsdValidator.validate(new StreamSource(new StringReader(xml)));

            if (!exceptions.isEmpty()) {
                throw new ValidationException("STAGE_1_TECHNICAL_XSD", exceptions);
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new ValidationException("STAGE_1_TECHNICAL_XSD", List.of("General parsing failure: " + e.getMessage()));
        }
    }

    private void performStage2SemanticValidation(CustomerCreditTransferInitiationV11 document) {
        log.debug("Executing Stage 2 (Semantic Data Verification)");
        List<String> errors = new ArrayList<>();
        Set<String> validCountries = new HashSet<>(Arrays.asList(Locale.getISOCountries()));

        for (PaymentInstruction40 instruction : document.getPmtInf()) {
            
            // Check Dbtr Country Code if postal address exists
            PartyIdentification135 debtor = instruction.getDbtr();
            if (debtor != null && debtor.getPstlAdr() != null) {
                PostalAddress24 address = debtor.getPstlAdr();
                if (address.getCtry() != null && !validCountries.contains(address.getCtry())) {
                    errors.add("Invalid ISO 3166 County Code in Dbtr/PstlAdr: " + address.getCtry());
                }
            }

            // Check every single transaction currency
            for (CreditTransferTransaction54 tx : instruction.getCdtTrfTxInf()) {
                if (tx.getAmt() != null && tx.getAmt().getInstdAmt() != null) {
                    String currency = tx.getAmt().getInstdAmt().getCcy();
                    if (!isValidCurrency(currency)) {
                        errors.add("Invalid ISO 4217 Currency Code directly declared on Amount: " + currency);
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("STAGE_2_SEMANTIC", errors);
        }
    }

    private void performStage3BusinessValidation(CustomerCreditTransferInitiationV11 document) {
        log.debug("Executing Stage 3 (Business & Network Requirements)");
        List<String> errors = new ArrayList<>();

        for (PaymentInstruction40 instruction : document.getPmtInf()) {
            if (instruction.getReqdExctnDt() != null && instruction.getReqdExctnDt().getDt() != null) {
                
                // Get Gregorgian Calendar Date from XML and convert to modern java.time
                LocalDate exctnDt = LocalDate.of(
                    instruction.getReqdExctnDt().getDt().getYear(),
                    instruction.getReqdExctnDt().getDt().getMonth(),
                    instruction.getReqdExctnDt().getDt().getDay()
                );

                // Rule 1: Not in Past
                if (exctnDt.isBefore(LocalDate.now())) {
                    errors.add("Requested Execution Date cannot be in the past. Date provided: " + exctnDt);
                }

                // Rule 2: No Weekends
                DayOfWeek day = exctnDt.getDayOfWeek();
                if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                    errors.add("Requested Execution Date cannot fall on a weekend (" + day + ").");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("STAGE_3_BUSINESS", errors);
        }
    }

    private boolean isValidCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) return false;
        try {
            Currency.getInstance(currencyCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
