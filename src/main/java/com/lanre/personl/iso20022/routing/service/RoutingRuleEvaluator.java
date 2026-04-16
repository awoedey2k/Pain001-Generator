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

    public RoutingRuleEvaluator(RoutingProperties routingProperties, List<MarketInfrastructureAdapter> adapters) {
        this.orderedRules = routingProperties.getRules().stream()
                .sorted(Comparator.comparingInt(RoutingProperties.Rule::getOrder))
                .toList();
        this.adaptersByName = adapters.stream()
                .collect(Collectors.toMap(MarketInfrastructureAdapter::getName, Function.identity()));

        validateConfiguration();
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

    private void validateConfiguration() {
        if (orderedRules.isEmpty()) {
            throw new IllegalStateException("At least one routing rule must be configured under iso20022.routing.rules.");
        }

        long distinctOrders = orderedRules.stream()
                .map(RoutingProperties.Rule::getOrder)
                .distinct()
                .count();
        if (distinctOrders != orderedRules.size()) {
            throw new IllegalStateException("Routing rule orders must be unique so evaluation order stays explicit.");
        }

        for (RoutingProperties.Rule rule : orderedRules) {
            if (!adaptersByName.containsKey(rule.getAdapter())) {
                throw new IllegalStateException("Routing rule '" + rule.getId()
                        + "' references unknown adapter '" + rule.getAdapter() + "'.");
            }
        }
    }

    public record RoutingDecision(
            String ruleId,
            int order,
            MarketInfrastructureAdapter adapter,
            boolean highValue
    ) {
    }
}
