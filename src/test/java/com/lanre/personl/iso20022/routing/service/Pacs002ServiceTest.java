package com.lanre.personl.iso20022.routing.service;

import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Pacs002ServiceTest {

    private Pacs002Service pacs002Service;

    @BeforeEach
    void setUp() {
        pacs002Service = new Pacs002Service();
    }

    @Test
    @DisplayName("Should generate ACCP pacs.002 status report for valid pacs.008 validation")
    void shouldGenerateAcceptedValidationStatus() {
        ValidationReport validReport = ValidationReport.builder()
                .valid(true)
                .errorMessages(List.of())
                .build();

        String responseXml = pacs002Service.generateValidationStatusReport("PACS-ORIGINAL-123", validReport);

        assertNotNull(responseXml);
        assertTrue(responseXml.contains("<Doc:FIToFIPmtStsRpt>"));
        assertTrue(responseXml.contains("<Doc:OrgnlMsgId>PACS-ORIGINAL-123</Doc:OrgnlMsgId>"));
        assertTrue(responseXml.contains("<Doc:OrgnlMsgNmId>pacs.008.001.10</Doc:OrgnlMsgNmId>"));
        assertTrue(responseXml.contains("<Doc:GrpSts>ACCP</Doc:GrpSts>"));
        assertFalse(responseXml.contains("<Doc:StsRsnInf>"));
    }

    @Test
    @DisplayName("Should generate RJCT pacs.002 status report with validation reasons")
    void shouldGenerateRejectedValidationStatus() {
        ValidationReport invalidReport = ValidationReport.builder()
                .valid(false)
                .failedStage("STAGE_2_BUSINESS")
                .errorMessages(List.of("Invalid BICFI"))
                .build();

        String responseXml = pacs002Service.generateValidationStatusReport("PACS-ORIGINAL-999", invalidReport);

        assertNotNull(responseXml);
        assertTrue(responseXml.contains("<Doc:GrpSts>RJCT</Doc:GrpSts>"));
        assertTrue(responseXml.contains("<Doc:Prtry>VALIDATION_FAILED</Doc:Prtry>"));
        assertTrue(responseXml.contains("STAGE_2_BUSINESS: Invalid BICFI"));
    }
}
