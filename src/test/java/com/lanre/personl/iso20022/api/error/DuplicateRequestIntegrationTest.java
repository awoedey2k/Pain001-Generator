package com.lanre.personl.iso20022.api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class DuplicateRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String DUPLICATE_E2E = "E2E-DUPLICATE-001";

    private static final String PAYMENT_REQUEST = """
            {
              "debtorName": "Acme Corporation",
              "debtorIban": "DE89370400440532013000",
              "debtorBic": "COBADEFFXXX",
              "creditorName": "Widget Supplies Ltd",
              "creditorIban": "GB29NWBK60161331926819",
              "creditorBic": "NWBKGB2L",
              "amount": 1500.00,
              "currency": "EUR",
              "endToEndId": "%s",
              "remittanceInfo": "Invoice 42"
            }
            """;

    @Test
    @DisplayName("Should return pain.002 duplicate rejection when pain.001 is submitted twice for the same EndToEndId")
    void shouldReturnPain002DuplicateRejection() throws Exception {
        mockMvc.perform(post("/api/v1/pain001")
                        .with(httpBasic("api-admin", "changeit-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST.formatted(DUPLICATE_E2E)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pain001")
                        .with(httpBasic("api-admin", "changeit-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST.formatted(DUPLICATE_E2E)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("pain.002")))
                .andExpect(content().string(containsString("RJCT")))
                .andExpect(content().string(containsString("DUPL")))
                .andExpect(content().string(containsString(DUPLICATE_E2E)));
    }

    @Test
    @DisplayName("Should return pacs.002 duplicate rejection when pacs.008 is submitted twice for the same EndToEndId")
    void shouldReturnPacs002DuplicateRejection() throws Exception {
        String e2e = "E2E-DUPLICATE-002";

        mockMvc.perform(post("/api/v1/pain001")
                        .with(httpBasic("api-admin", "changeit-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST.formatted(e2e)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pacs008")
                        .with(httpBasic("api-admin", "changeit-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST.formatted(e2e)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pacs008")
                        .with(httpBasic("api-admin", "changeit-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST.formatted(e2e)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("pacs.002")))
                .andExpect(content().string(containsString("RJCT")))
                .andExpect(content().string(containsString("DUPL")))
                .andExpect(content().string(containsString(e2e)));
    }
}

