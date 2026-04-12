package com.lanre.personl.iso20022.pacs008.service;

import com.lanre.personl.iso20022.pain001.exception.ValidationException;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import com.prowidesoftware.swift.model.mx.dic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Pacs008ServiceTest {

    private Pacs008Service pacs008Service;

    @Mock
    private com.lanre.personl.iso20022.lifecycle.service.LifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        pacs008Service = new Pacs008Service(lifecycleService);
    }

    private String getValidPacs008XmlString() {
        MxPacs00800110 mx = new MxPacs00800110();
        FIToFICustomerCreditTransferV10 pacs008 = new FIToFICustomerCreditTransferV10();
        
        GroupHeader96 grpHdr = new GroupHeader96();
        grpHdr.setMsgId("MTMX-" + UUID.randomUUID().toString().substring(0, 8));
        
        // Proper BIC11
        BranchAndFinancialInstitutionIdentification6 instgAgt = new BranchAndFinancialInstitutionIdentification6();
        FinancialInstitutionIdentification18 finId1 = new FinancialInstitutionIdentification18();
        finId1.setBICFI("CITIUS33XXX"); // Correct format: 11 chars
        instgAgt.setFinInstnId(finId1);
        grpHdr.setInstgAgt(instgAgt);

        BranchAndFinancialInstitutionIdentification6 instdAgt = new BranchAndFinancialInstitutionIdentification6();
        FinancialInstitutionIdentification18 finId2 = new FinancialInstitutionIdentification18();
        finId2.setBICFI("BOFAGB2L"); // Correct format: 8 chars
        instdAgt.setFinInstnId(finId2);
        grpHdr.setInstdAgt(instdAgt);
        
        pacs008.setGrpHdr(grpHdr);
        mx.setFIToFICstmrCdtTrf(pacs008);
        
        return mx.message();
    }

    @Test
    @DisplayName("Should pass pacs.008 validation with valid BIC formats")
    void shouldPassValidPacs008() {
        String xml = getValidPacs008XmlString();
        assertDoesNotThrow(() -> {
            MxPacs00800110 parsed = pacs008Service.validateAndParse(xml);
            assertEquals("CITIUS33XXX", parsed.getFIToFICstmrCdtTrf().getGrpHdr().getInstgAgt().getFinInstnId().getBICFI());
        });
    }

    @Test
    @DisplayName("Should detect business violation on invalid BIC length/structure")
    void shouldDetectInvalidBICFormats() {
        String xml = getValidPacs008XmlString();
        
        // Mutate the valid BIC to be an invalid string natively
        String invalidXml = xml.replace("CITIUS33XXX", "INVALIDBIC999"); // 13 chars, violates regex

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            pacs008Service.validateAndParse(invalidXml);
        });

        assertEquals("STAGE_2_BUSINESS", exception.getStage());
        assertTrue(exception.getErrors().get(0).contains("violates BIC11 requirements"));
    }
}
