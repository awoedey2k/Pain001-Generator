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

@Slf4j
@Service
public class Pacs008Service {

    private final Validator xsdValidator;
    private static final Pattern BIC11_PATTERN = Pattern.compile("^[A-Z]{6,6}[A-Z2-9][A-NP-Z0-9]([A-Z0-9]{3,3})?$");

    public Pacs008Service() {
        Validator temp = null;
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new ClassPathResource("xsd/pacs.008.001.10.xsd").getFile());
            temp = schema.newValidator();
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
}
