package com.lanre.personl.iso20022.lifecycle.controller;

import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.lanre.personl.iso20022.pain001.service.Pain001GeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class AuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Pain001GeneratorService pain001GeneratorService;

    @Autowired
    private Pacs008Service pacs008Service;

    @Test
    @DisplayName("Should return lightweight workflow summaries without embedded audit payloads")
    void shouldReturnLightweightWorkflowSummaries() throws Exception {
        PaymentRequest request = buildRequest();
        pain001GeneratorService.generatePain001Xml(request);
        pacs008Service.generatePacs008Xml(request);

        mockMvc.perform(get("/api/v1/lifecycle/all")
                        .with(httpBasic("api-auditor", "changeit-auditor-password")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(request.getEndToEndId())))
                .andExpect(content().string(not(containsString("\"auditLogs\""))))
                .andExpect(content().string(not(containsString("\"payload\""))));
    }

    @Test
    @DisplayName("Should return compact audit metadata without payload or workflow recursion")
    void shouldReturnCompactAuditMetadata() throws Exception {
        PaymentRequest request = buildRequest();
        pain001GeneratorService.generatePain001Xml(request);
        pacs008Service.generatePacs008Xml(request);

        mockMvc.perform(get("/api/v1/lifecycle/{endToEndId}", request.getEndToEndId())
                        .with(httpBasic("api-auditor", "changeit-auditor-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endToEndId").value(request.getEndToEndId()))
                .andExpect(jsonPath("$.auditLogCount").value(2))
                .andExpect(jsonPath("$.auditLogs", hasSize(2)))
                .andExpect(jsonPath("$.auditLogs[0].messageType").exists())
                .andExpect(jsonPath("$.auditLogs[0].payload").doesNotExist())
                .andExpect(jsonPath("$.auditLogs[0].workflow").doesNotExist());
    }

    private PaymentRequest buildRequest() {
        return PaymentRequest.builder()
                .debtorName("Lifecycle Endpoint Corp")
                .debtorIban("DE89370400440532013000")
                .debtorBic("COBADEFFXXX")
                .creditorName("Compact Audit Ltd")
                .creditorIban("FR7630006000011234567890189")
                .creditorBic("AGRIFRPPXXX")
                .amount(new BigDecimal("275.15"))
                .currency("EUR")
                .endToEndId("E2E-" + UUID.randomUUID().toString().substring(0, 8))
                .remittanceInfo("Lifecycle endpoint shape test")
                .build();
    }
}
