package com.lanre.personl.iso20022.routing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "iso20022.routing")
public class RoutingProperties {

    private List<Rule> rules = new ArrayList<>();

    @Getter
    @Setter
    public static class Rule {
        private String id;
        private int order;
        private String adapter;
        private List<String> currencies = new ArrayList<>();
        private List<String> receiverBics = new ArrayList<>();

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
