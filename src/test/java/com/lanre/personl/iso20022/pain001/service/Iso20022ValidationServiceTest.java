package com.lanre.personl.iso20022.pain001.service;

import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.prowidesoftware.swift.model.mx.MxPain00100111;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Iso20022ValidationServiceTest {

    private Iso20022ValidationService validationService;
    private Pain001GeneratorService generatorService;

    @Mock
    private com.lanre.personl.iso20022.lifecycle.service.LifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        validationService = new Iso20022ValidationService();
        generatorService = new Pain001GeneratorService(lifecycleService);
    }

    private PaymentRequest createValidRequest() {
        return PaymentRequest.builder()
                .debtorName("Valid Corp")
                .debtorIban("DE89370400440532013000")
                .debtorBic("COBADEFFXXX")
                .creditorName("Valid LLC")
                .creditorIban("GB29NWBK60161331926819")
                .creditorBic("NWBKGB2L")
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .endToEndId("E2E-TESTING-001")
                .build();
    }

    @Test
    @DisplayName("Should pass all stages for a perfectly valid pain.001 XML")
    void shouldPassValidMessage() {
        String validXml = generatorService.generatePain001Xml(createValidRequest());
        
        // This execution date will be today by default in the generator
        // Today could be a weekend! We need to ensure we trick the XML if today is a weekend.

        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String safeDateStr = today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
            validXml = validXml.replace("<Doc:Dt>" + todayStr + "</Doc:Dt>", "<Doc:Dt>" + safeDateStr + "</Doc:Dt>");
        }

        MxPain00100111 result = validationService.validateIncomingMessage(validXml);
        assertNotNull(result);
        assertEquals("Valid Corp", result.getCstmrCdtTrfInitn().getGrpHdr().getInitgPty().getNm());
    }

    @Test
    @DisplayName("STAGE 1: Should throw exception for technically invalid XML (XSD violation)")
    void shouldFailTechnicalXsdValidation() {
        String validXml = generatorService.generatePain001Xml(createValidRequest());
        // Mutate XML to break XSD validation: Remove the absolutely required <Doc:MsgId> tag
        String invalidXml = validXml.replaceAll("<Doc:MsgId>[^<]*</Doc:MsgId>", "");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            validationService.validateIncomingMessage(invalidXml);
        });

        assertEquals("STAGE_1_TECHNICAL_XSD", exception.getStage());
        assertTrue(exception.getErrors().stream().anyMatch(err -> err.contains("Invalid content was found")));
    }

    @Test
    @DisplayName("STAGE 2: Should throw exception for invalid ISO 4217 Currency Code")
    void shouldFailSemanticCurrencyValidation() {
        PaymentRequest request = createValidRequest();
        request.setCurrency("XYZ"); // xyz is not a valid ISO 4217 code
        String invalidXml = generatorService.generatePain001Xml(request);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            validationService.validateIncomingMessage(invalidXml);
        });

        // The XYZ currency might fail XSD validation first if the XSD restricts Ccy lengths or patterns. 
        // XSD specifies pattern [A-Z]{3}. XYZ matches pattern, so XSD passes. Stage 2 catches semantics.
        assertEquals("STAGE_2_SEMANTIC", exception.getStage());
        assertTrue(exception.getErrors().get(0).contains("Invalid ISO 4217 Currency Code directly declared on Amount: XYZ"));
    }

    @Test
    @DisplayName("STAGE 3: Should throw exception for Execution Date in the past")
    void shouldFailBusinessPastExecutionDate() {
        String validXml = generatorService.generatePain001Xml(createValidRequest());
        
        LocalDate pastDate = LocalDate.now().minusDays(5);
        String pastDateStr = pastDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        String invalidXml = validXml.replaceAll("<Doc:Dt>[^<]*</Doc:Dt>", "<Doc:Dt>" + pastDateStr + "</Doc:Dt>");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            validationService.validateIncomingMessage(invalidXml);
        });

        assertEquals("STAGE_3_BUSINESS", exception.getStage());
        assertTrue(exception.getErrors().get(0).contains("Requested Execution Date cannot be in the past"));
    }

    @Test
    @DisplayName("STAGE 3: Should throw exception for Execution Date falling on a weekend")
    void shouldFailBusinessWeekendExecutionDate() {
        String validXml = generatorService.generatePain001Xml(createValidRequest());
        
        // Find next Saturday
        LocalDate futureDate = LocalDate.now();
        while (futureDate.getDayOfWeek() != DayOfWeek.SATURDAY) {
            futureDate = futureDate.plusDays(1);
        }
        
        String weekendDateStr = futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String invalidXml = validXml.replaceAll("<Doc:Dt>[^<]*</Doc:Dt>", "<Doc:Dt>" + weekendDateStr + "</Doc:Dt>");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            validationService.validateIncomingMessage(invalidXml);
        });

        assertEquals("STAGE_3_BUSINESS", exception.getStage());
        assertTrue(exception.getErrors().get(0).contains("Requested Execution Date cannot fall on a weekend"));
    }
}
