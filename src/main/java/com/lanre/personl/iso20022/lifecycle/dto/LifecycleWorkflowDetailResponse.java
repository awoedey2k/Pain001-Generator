package com.lanre.personl.iso20022.lifecycle.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Detailed lifecycle view with compact audit metadata.")
public record LifecycleWorkflowDetailResponse(
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
        @Schema(example = "3")
        int auditLogCount,
        @ArraySchema(schema = @Schema(implementation = LifecycleAuditEntryResponse.class))
        List<LifecycleAuditEntryResponse> auditLogs
) {
}
