package com.lanre.personl.iso20022.routing.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Should reject malformed currency and BIC values in routing rules")
    void shouldRejectMalformedCurrencyAndBicValues() {
        RoutingProperties properties = new RoutingProperties();
        RoutingProperties.Rule rule = new RoutingProperties.Rule();
        rule.setId("bad-rule");
        rule.setOrder(1);
        rule.setAdapter("SEPA-MOCK-SERVICE");
        rule.setCurrencies(List.of("eur"));
        rule.setReceiverBics(List.of("BAD-BIC"));
        properties.setRules(List.of(rule));

        var violations = validator.validate(properties);

        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getMessage().contains("uppercase three-letter ISO 4217")));
        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getMessage().contains("8 or 11 uppercase characters")));
    }
}
