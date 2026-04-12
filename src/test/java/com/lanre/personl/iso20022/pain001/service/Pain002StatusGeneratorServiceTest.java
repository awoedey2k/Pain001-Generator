package com.lanre.personl.iso20022.pain001.service;

import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class Pain002StatusGeneratorServiceTest {

    private Pain002StatusGeneratorService statusGeneratorService;

    @BeforeEach
    void setUp() {
        statusGeneratorService = new Pain002StatusGeneratorService();
    }

    @Test
    @DisplayName("Should generate ACCP Status Report for Valid Messages")
    void shouldGenerateAccectedStatus() {
        ValidationReport validReport = ValidationReport.builder()
                .valid(true)
                .failedStage(null)
                .errorMessages(Collections.emptyList())
                .build();

        String responseXml = statusGeneratorService.generateStatusReport("ORIGINAL-ID-123", validReport);

        assertNotNull(responseXml);
        assertTrue(responseXml.contains("<Doc:CstmrPmtStsRpt>"));
        assertTrue(responseXml.contains("<Doc:OrgnlMsgId>ORIGINAL-ID-123</Doc:OrgnlMsgId>"));
        assertTrue(responseXml.contains("<Doc:GrpSts>ACCP</Doc:GrpSts>"));
        assertFalse(responseXml.contains("<Doc:StsRsnInf>"), "Accepted messages should not have Error Reason blocks");
    }

    @Test
    @DisplayName("Should generate RJCT Status Report and append validation exceptions for Invalid Messages")
    void shouldGenerateRejectedStatusWithReasons() {
        ValidationReport invalidReport = ValidationReport.builder()
                .valid(false)
                .failedStage("STAGE_2_SEMANTIC")
                .errorMessages(Arrays.asList("Invalid ISO 4217 Currency Code", "Another generic error"))
                .build();

        String responseXml = statusGeneratorService.generateStatusReport("ORIGINAL-ID-999", invalidReport);

        assertNotNull(responseXml);
        assertTrue(responseXml.contains("<Doc:GrpSts>RJCT</Doc:GrpSts>"));
        assertTrue(responseXml.contains("<Doc:Rsn>"));
        assertTrue(responseXml.contains("<Doc:Prtry>VALIDATION_FAILED</Doc:Prtry>"));
        
        assertTrue(responseXml.contains("STAGE_2_SEMANTIC: Invalid ISO 4217 Currency Code"));
        assertTrue(responseXml.contains("STAGE_2_SEMANTIC: Another generic error"));
    }
}
