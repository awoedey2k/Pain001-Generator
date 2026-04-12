package com.lanre.personl.iso20022.pain001.exception;

import lombok.Getter;
import java.util.List;

@Getter
public class ValidationException extends RuntimeException {
    private final String stage;
    private final List<String> errors;

    public ValidationException(String stage, List<String> errors) {
        super("Validation failed at stage: " + stage);
        this.stage = stage;
        this.errors = errors;
    }
}
