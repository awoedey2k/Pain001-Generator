package com.lanre.personl.iso20022.routing.service;

import com.lanre.personl.iso20022.routing.config.RoutingProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RoutingConfigurationValidator {

    public void validate(RoutingProperties routingProperties, Collection<String> adapterNames) {
        List<String> issues = new ArrayList<>();
        List<RoutingProperties.Rule> rules = routingProperties.getRules();

        if (rules == null || rules.isEmpty()) {
            issues.add("At least one routing rule must be configured under iso20022.routing.rules.");
        } else {
            Set<String> ids = new HashSet<>();
            Set<Integer> orders = new HashSet<>();

            for (int index = 0; index < rules.size(); index++) {
                RoutingProperties.Rule rule = rules.get(index);
                String ruleLabel = describeRule(rule, index);

                if (isBlank(rule.getId())) {
                    issues.add("Rule #" + (index + 1) + " must define a non-blank id.");
                } else if (!ids.add(rule.getId())) {
                    issues.add("Routing rule id '" + rule.getId() + "' is duplicated.");
                }

                if (rule.getOrder() <= 0) {
                    issues.add(ruleLabel + " must define an order greater than zero.");
                } else if (!orders.add(rule.getOrder())) {
                    issues.add("Routing rule order '" + rule.getOrder() + "' is duplicated.");
                }

                if (isBlank(rule.getAdapter())) {
                    issues.add(ruleLabel + " must define a target adapter.");
                } else if (!adapterNames.contains(rule.getAdapter())) {
                    issues.add(ruleLabel + " references unknown adapter '" + rule.getAdapter() + "'.");
                }

                if ((rule.getCurrencies() == null || rule.getCurrencies().isEmpty())
                        && (rule.getReceiverBics() == null || rule.getReceiverBics().isEmpty())) {
                    issues.add(ruleLabel + " must define at least one currency or receiver BIC match condition.");
                }
            }
        }

        if (!issues.isEmpty()) {
            throw new IllegalStateException("Invalid routing configuration:\n - " + String.join("\n - ", issues));
        }
    }

    private String describeRule(RoutingProperties.Rule rule, int index) {
        if (rule == null || isBlank(rule.getId())) {
            return "Rule #" + (index + 1);
        }
        return "Routing rule '" + rule.getId() + "'";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
