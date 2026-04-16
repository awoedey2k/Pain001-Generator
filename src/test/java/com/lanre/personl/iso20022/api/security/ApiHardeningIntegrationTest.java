package com.lanre.personl.iso20022.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "iso20022.api.request-limits.xml-bytes=100",
        "iso20022.api.request-limits.json-bytes=2048",
        "iso20022.api.request-limits.text-bytes=128",
        "iso20022.api.rate-limit.requests-per-window=2",
        "iso20022.api.rate-limit.window-seconds=60"
})
class ApiHardeningIntegrationTest {

    private static final String VALID_PAYMENT_REQUEST = """
            {
              "debtorName": "Acme Corporation",
              "debtorIban": "DE89370400440532013000",
              "debtorBic": "COBADEFFXXX",
              "creditorName": "Widget Supplies Ltd",
              "creditorIban": "GB29NWBK60161331926819",
              "creditorBic": "NWBKGB2L",
              "amount": 1500.00,
              "currency": "EUR",
              "endToEndId": "E2E-AUTH-001",
              "remittanceInfo": "Invoice 42"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void resetRateLimiter() {
        rateLimiter.clear();
    }

    @Test
    @DisplayName("Should require HTTP Basic authentication for API writes")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/pain001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYMENT_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow writer credentials on payment generation endpoint")
    void shouldAllowWriterToPost() throws Exception {
        mockMvc.perform(post("/api/v1/pain001")
                        .with(httpBasic("api-writer", "changeit-writer-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYMENT_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should restrict lifecycle reads to auditor and admin roles")
    void shouldRestrictLifecycleReads() throws Exception {
        mockMvc.perform(get("/api/v1/lifecycle/UNKNOWN-E2E-ID")
                        .with(httpBasic("api-writer", "changeit-writer-password")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/lifecycle/UNKNOWN-E2E-ID")
                        .with(httpBasic("api-auditor", "changeit-auditor-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject XML payloads above the configured size limit")
    void shouldRejectOversizedXmlPayload() throws Exception {
        String oversizedXml = "<Document>" + "x".repeat(150) + "</Document>";

        mockMvc.perform(post("/api/v1/validate/pain001")
                        .with(httpBasic("api-writer", "changeit-writer-password"))
                        .contentType(MediaType.APPLICATION_XML)
                        .content(oversizedXml))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @DisplayName("Should return 429 when the per-client rate limit is exceeded")
    void shouldRateLimitBurstTraffic() throws Exception {
        mockMvc.perform(get("/api/v1/lifecycle/UNKNOWN-E2E-ID")
                        .with(httpBasic("api-auditor", "changeit-auditor-password")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/lifecycle/UNKNOWN-E2E-ID")
                        .with(httpBasic("api-auditor", "changeit-auditor-password")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/lifecycle/UNKNOWN-E2E-ID")
                        .with(httpBasic("api-auditor", "changeit-auditor-password")))
                .andExpect(status().isTooManyRequests());
    }
}
