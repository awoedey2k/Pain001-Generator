package com.lanre.personl.iso20022.lifecycle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Compact lifecycle audit entry without the stored payload body.")
public record LifecycleAuditEntryResponse(
        @Schema(example = "12")
        Long id,
        @Schema(example = "pacs.008")
        String messageType,
        @Schema(example = "PACS8-1f4b79d0")
        String messageId,
        @Schema(example = "2026-04-16T10:15:30")
        LocalDateTime timestamp
) {
}
