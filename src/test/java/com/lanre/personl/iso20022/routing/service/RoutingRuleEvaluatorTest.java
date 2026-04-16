package com.lanre.personl.iso20022.routing.service;

import com.lanre.personl.iso20022.routing.config.RoutingProperties;
import com.lanre.personl.iso20022.routing.strategy.MarketInfrastructureAdapter;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingRuleEvaluatorTest {

    @Test
    @DisplayName("Should prefer the lower ordered high-value rule over currency mapping")
    void shouldPreferLowerOrderedHighValueRule() {
        RoutingRuleEvaluator evaluator = new RoutingRuleEvaluator(
                properties(
                        rule("high-value-priority", 1, "HIGH-VALUE-PRIORITY-QUEUE", List.of(), List.of("CHASUS33XXX")),
                        rule("fedwire-usd", 2, "FEDWIRE-MOCK-SERVICE", List.of("USD"), List.of())
                ),
                adapters()
        );

        var decision = evaluator.evaluate("USD", "CHASUS33XXX");

        assertTrue(decision.isPresent());
        assertEquals("high-value-priority", decision.get().ruleId());
        assertEquals("HIGH-VALUE-PRIORITY-QUEUE", decision.get().adapter().getName());
        assertTrue(decision.get().highValue());
    }

    @Test
    @DisplayName("Should honor configured order when currency rule is placed before high-value rule")
    void shouldHonorConfiguredRuleOrder() {
        RoutingRuleEvaluator evaluator = new RoutingRuleEvaluator(
                properties(
                        rule("fedwire-usd", 1, "FEDWIRE-MOCK-SERVICE", List.of("USD"), List.of()),
                        rule("high-value-priority", 2, "HIGH-VALUE-PRIORITY-QUEUE", List.of(), List.of("CHASUS33XXX"))
                ),
                adapters()
        );

        var decision = evaluator.evaluate("USD", "CHASUS33XXX");

        assertTrue(decision.isPresent());
        assertEquals("fedwire-usd", decision.get().ruleId());
        assertEquals("FEDWIRE-MOCK-SERVICE", decision.get().adapter().getName());
    }

    private RoutingProperties properties(RoutingProperties.Rule... rules) {
        RoutingProperties properties = new RoutingProperties();
        properties.setRules(List.of(rules));
        return properties;
    }

    private RoutingProperties.Rule rule(
            String id,
            int order,
            String adapter,
            List<String> currencies,
            List<String> receiverBics
    ) {
        RoutingProperties.Rule rule = new RoutingProperties.Rule();
        rule.setId(id);
        rule.setOrder(order);
        rule.setAdapter(adapter);
        rule.setCurrencies(currencies);
        rule.setReceiverBics(receiverBics);
        return rule;
    }

    private List<MarketInfrastructureAdapter> adapters() {
        return List.of(
                namedAdapter("HIGH-VALUE-PRIORITY-QUEUE"),
                namedAdapter("FEDWIRE-MOCK-SERVICE"),
                namedAdapter("SEPA-MOCK-SERVICE")
        );
    }

    private MarketInfrastructureAdapter namedAdapter(String name) {
        return new MarketInfrastructureAdapter() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public void route(MxPacs00800110 message) {
                // No-op for rule evaluation tests.
            }
        };
    }
}
