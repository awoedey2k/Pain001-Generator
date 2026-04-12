package com.lanre.personl.iso20022.pain001.service;

import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Pain001GeneratorService}.
 * Validates that the generated XML conforms to pain.001.001.11 structure.
 */
@ExtendWith(MockitoExtension.class)
class Pain001GeneratorServiceTest {

    private Pain001GeneratorService service;

    @Mock
    private com.lanre.personl.iso20022.lifecycle.service.LifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        service = new Pain001GeneratorService(lifecycleService);
    }

    private PaymentRequest sampleRequest() {
        return PaymentRequest.builder()
                .debtorName("Acme Corporation")
                .debtorIban("DE89370400440532013000")
                .debtorBic("COBADEFFXXX")
                .creditorName("Widget Supplies Ltd")
                .creditorIban("GB29NWBK60161331926819")
                .creditorBic("NWBKGB2L")
                .amount(new BigDecimal("1500.00"))
                .currency("EUR")
                .endToEndId("INV-2026-00042")
                .remittanceInfo("Invoice 2026-00042 payment")
                .build();
    }

    @Test
    @DisplayName("Should generate valid XML containing Document root element")
    void shouldGenerateXmlWithDocumentRoot() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertNotNull(xml);
        assertTrue(xml.contains("<Doc:Document"), "XML must contain <Document> root element");
        assertTrue(xml.contains("CstmrCdtTrfInitn"), "XML must contain CstmrCdtTrfInitn element");
    }

    @Test
    @DisplayName("Should contain GroupHeader with MsgId and CreDtTm")
    void shouldContainGroupHeader() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertTrue(xml.contains("<Doc:GrpHdr>"), "XML must contain <GrpHdr>");
        assertTrue(xml.contains("<Doc:MsgId>"), "XML must contain <MsgId>");
        assertTrue(xml.contains("<Doc:CreDtTm>"), "XML must contain <CreDtTm>");
        assertTrue(xml.contains("<Doc:NbOfTxs>1</Doc:NbOfTxs>"), "NbOfTxs should be 1");
    }

    @Test
    @DisplayName("Should map debtor information correctly")
    void shouldMapDebtorInfo() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertTrue(xml.contains("<Doc:Dbtr>"), "XML must contain <Dbtr>");
        assertTrue(xml.contains("<Doc:Nm>Acme Corporation</Doc:Nm>"), "Debtor name must be present");
        assertTrue(xml.contains("<Doc:IBAN>DE89370400440532013000</Doc:IBAN>"), "Debtor IBAN must be present");
        assertTrue(xml.contains("<Doc:BICFI>COBADEFFXXX</Doc:BICFI>"), "Debtor BIC must be present");
    }

    @Test
    @DisplayName("Should map creditor information correctly")
    void shouldMapCreditorInfo() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertTrue(xml.contains("<Doc:Cdtr>"), "XML must contain <Cdtr>");
        assertTrue(xml.contains("<Doc:Nm>Widget Supplies Ltd</Doc:Nm>"), "Creditor name must be present");
        assertTrue(xml.contains("<Doc:IBAN>GB29NWBK60161331926819</Doc:IBAN>"), "Creditor IBAN must be present");
        assertTrue(xml.contains("<Doc:BICFI>NWBKGB2L</Doc:BICFI>"), "Creditor BIC must be present");
    }

    @Test
    @DisplayName("Should map amount and currency correctly")
    void shouldMapAmountAndCurrency() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertTrue(xml.contains("1500.00"), "Amount must be present");
        assertTrue(xml.contains("EUR"), "Currency must be present");
    }

    @Test
    @DisplayName("Should include payment method TRF")
    void shouldIncludePaymentMethodTRF() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertTrue(xml.contains("<Doc:PmtMtd>TRF</Doc:PmtMtd>"), "Payment method should be TRF");
    }

    @Test
    @DisplayName("Should include EndToEndId from request")
    void shouldIncludeEndToEndId() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertTrue(xml.contains("<Doc:EndToEndId>INV-2026-00042</Doc:EndToEndId>"),
                "EndToEndId from request must be preserved");
    }

    @Test
    @DisplayName("Should include remittance information")
    void shouldIncludeRemittanceInfo() {
        String xml = service.generatePain001Xml(sampleRequest());

        assertTrue(xml.contains("<Doc:Ustrd>Invoice 2026-00042 payment</Doc:Ustrd>"),
                "Unstructured remittance info must be present");
    }

    @Test
    @DisplayName("Should auto-generate EndToEndId when not provided")
    void shouldAutoGenerateEndToEndIdWhenMissing() {
        PaymentRequest request = sampleRequest();
        request.setEndToEndId(null);

        String xml = service.generatePain001Xml(request);

        assertTrue(xml.contains("<Doc:EndToEndId>E2E-"),
                "Auto-generated EndToEndId should have E2E- prefix");
    }

    @Test
    @DisplayName("Should handle missing remittance info gracefully")
    void shouldHandleMissingRemittanceInfo() {
        PaymentRequest request = sampleRequest();
        request.setRemittanceInfo(null);

        String xml = service.generatePain001Xml(request);

        assertNotNull(xml);
        assertFalse(xml.contains("<Doc:RmtInf>"), "RmtInf should be absent when not provided");
    }

    @Test
    @DisplayName("Generated XML should be well-formed (contains XML declaration or namespace)")
    void shouldBeWellFormedXml() {
        String xml = service.generatePain001Xml(sampleRequest());

        // Prowide generates the XML with proper namespace declarations
        assertTrue(xml.contains("xmlns") || xml.contains("<?xml"),
                "XML should contain namespace declarations or XML declaration");
    }
}
