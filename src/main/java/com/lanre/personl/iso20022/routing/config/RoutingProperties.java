package com.lanre.personl.iso20022.routing.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "iso20022.routing")
public class RoutingProperties {

    @Valid
    @NotEmpty(message = "iso20022.routing.rules must contain at least one rule.")
    private List<Rule> rules = new ArrayList<>();

    @Getter
    @Setter
    public static class Rule {
        @NotBlank(message = "Routing rule id must not be blank.")
        private String id;

        @Positive(message = "Routing rule order must be greater than zero.")
        private int order;

        @NotBlank(message = "Routing rule adapter must not be blank.")
        private String adapter;

        private List<@Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Routing currencies must be uppercase three-letter ISO 4217 codes."
        ) String> currencies = new ArrayList<>();

        private List<@Pattern(
                regexp = "^[A-Z0-9]{8}([A-Z0-9]{3})?$",
                message = "Routing receiver BICs must be 8 or 11 uppercase characters."
        ) String> receiverBics = new ArrayList<>();

        public boolean matches(String currency, String receiverBic) {
            boolean currencyMatch = currencies.isEmpty() || containsIgnoreCase(currencies, currency);
            boolean bicMatch = receiverBics.isEmpty() || containsIgnoreCase(receiverBics, receiverBic);
            return currencyMatch && bicMatch;
        }

        public boolean matchesReceiverBic(String receiverBic) {
            return !receiverBics.isEmpty() && containsIgnoreCase(receiverBics, receiverBic);
        }

        private boolean containsIgnoreCase(List<String> values, String candidate) {
            if (candidate == null) {
                return false;
            }

            String normalizedCandidate = normalize(candidate);
            return values.stream().anyMatch(value -> normalize(value).equals(normalizedCandidate));
        }

        private String normalize(String value) {
            return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        }
    }
}
