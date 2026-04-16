package com.lanre.personl.iso20022.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class ActuatorMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Should expose health and readiness actuator endpoints")
    void shouldExposeHealthAndReadinessEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Should expose the custom request-count metric through actuator")
    void shouldExposeRequestCountMetric() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics/iso20022.requests.total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iso20022.requests.total"));

        Counter counter = meterRegistry.find("iso20022.requests.total")
                .tag("method", "GET")
                .tag("path", "/actuator/health")
                .counter();
        assertNotNull(counter);
        assertTrue(counter.count() >= 1.0d);
    }

    @Test
    @DisplayName("Should count validation failures by message family and stage")
    void shouldCountValidationFailuresByStage() throws Exception {
        String invalidPainXml = """
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.11">
                  <CstmrCdtTrfInitn>
                    <GrpHdr>
                      <CreDtTm>2026-04-16T12:00:00</CreDtTm>
                    </GrpHdr>
                  </CstmrCdtTrfInitn>
                </Document>
                """;

        mockMvc.perform(post("/api/v1/validate/pain001")
                        .with(httpBasic("api-writer", "changeit-writer-password"))
                        .contentType(MediaType.parseMediaType(MediaType.APPLICATION_XML_VALUE))
                        .content(invalidPainXml))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics/iso20022.validation.failures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iso20022.validation.failures"));

        Counter counter = meterRegistry.find("iso20022.validation.failures")
                .tag("messageFamily", "pain.001")
                .tag("stage", "STAGE_1_TECHNICAL_XSD")
                .counter();
        assertNotNull(counter);
        assertTrue(counter.count() >= 1.0d);
    }
}
