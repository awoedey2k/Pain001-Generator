package com.lanre.personl.iso20022.lifecycle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Lightweight lifecycle summary returned by default from list endpoints.")
public record LifecycleWorkflowSummaryResponse(
        @Schema(example = "1")
        Long id,
        @Schema(example = "INV-2026-00042")
        String endToEndId,
        @Schema(example = "SETTLING")
        String status,
        @Schema(example = "1500.00")
        BigDecimal amount,
        @Schema(example = "EUR")
        String currency,
        @Schema(example = "Acme Corporation")
        String debtorName,
        @Schema(example = "Widget Supplies Ltd")
        String creditorName,
        @Schema(example = "Invoice 2026-00042 payment")
        String remittanceInformation,
        @Schema(example = "2026-04-16T10:10:00")
        LocalDateTime createdAt,
        @Schema(example = "2026-04-16T10:15:30")
        LocalDateTime lastUpdatedAt,
        @Schema(example = "3", description = "Number of stored lifecycle audit entries for the workflow.")
        int auditLogCount
) {
}
