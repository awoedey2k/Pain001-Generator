package com.lanre.personl.iso20022.routing.service;

import com.lanre.personl.iso20022.routing.config.RoutingProperties;
import com.lanre.personl.iso20022.routing.strategy.MarketInfrastructureAdapter;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoutingRuleEvaluator {

    private final List<RoutingProperties.Rule> orderedRules;
    private final Map<String, MarketInfrastructureAdapter> adaptersByName;

    public RoutingRuleEvaluator(
            RoutingProperties routingProperties,
            List<MarketInfrastructureAdapter> adapters,
            RoutingConfigurationValidator routingConfigurationValidator
    ) {
        this.adaptersByName = adapters.stream()
                .collect(Collectors.toMap(MarketInfrastructureAdapter::getName, Function.identity()));
        routingConfigurationValidator.validate(routingProperties, adaptersByName.keySet());

        this.orderedRules = routingProperties.getRules().stream()
                .sorted(Comparator.comparingInt(RoutingProperties.Rule::getOrder))
                .toList();
    }

    public Optional<RoutingDecision> evaluate(String currency, String receiverBic) {
        return orderedRules.stream()
                .filter(rule -> rule.matches(currency, receiverBic))
                .findFirst()
                .map(rule -> new RoutingDecision(
                        rule.getId(),
                        rule.getOrder(),
                        adaptersByName.get(rule.getAdapter()),
                        rule.matchesReceiverBic(receiverBic)
                ));
    }

    public record RoutingDecision(
            String ruleId,
            int order,
            MarketInfrastructureAdapter adapter,
            boolean highValue
    ) {
    }
}
