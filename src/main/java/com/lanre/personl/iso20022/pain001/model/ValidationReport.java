package com.lanre.personl.iso20022.pain001.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Normalized outcome of technical, semantic, and business validation stages.")
public class ValidationReport {
    @Schema(example = "true", description = "Whether the inbound message passed validation.")
    private boolean valid;

    @Schema(example = "SCHEMA_VALIDATION", nullable = true, description = "Validation stage that failed, when applicable.")
    private String failedStage;

    @ArraySchema(schema = @Schema(example = "Invalid BICFI code"))
    private List<String> errorMessages;
}
