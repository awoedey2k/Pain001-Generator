package com.lanre.personl.iso20022.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Iso20022MetricsService {

    private final MeterRegistry meterRegistry;

    public Iso20022MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementRequest(String method, String path, int status) {
        meterRegistry.counter(
                "iso20022.requests.total",
                List.of(
                        Tag.of("method", normalize(method, "UNKNOWN")),
                        Tag.of("path", normalize(path, "UNKNOWN")),
                        Tag.of("status", String.valueOf(status))
                )
        ).increment();
    }

    public void incrementValidationFailure(String messageFamily, String stage) {
        meterRegistry.counter(
                "iso20022.validation.failures",
                List.of(
                        Tag.of("messageFamily", normalize(messageFamily, "unknown")),
                        Tag.of("stage", normalize(stage, "unknown"))
                )
        ).increment();
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
