package com.lanre.personl.iso20022.pain001.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReport {
    private boolean valid;
    private String failedStage;
    private List<String> errorMessages;
}
