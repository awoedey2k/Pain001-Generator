package com.lanre.personl.iso20022.routing.strategy;

import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class HighValueAdapter implements MarketInfrastructureAdapter {
    
    // Simulating a High-Value BIC list
    private static final Set<String> HIGH_VALUE_BICS = Set.of("CHASUS33XXX", "CITIUS33XXX");

    @Override
    public String getName() { return "HIGH-VALUE-PRIORITY-QUEUE"; }

    @Override
    public boolean supports(String currency, String receiverBic) {
        return HIGH_VALUE_BICS.contains(receiverBic);
    }

    @Override
    public void route(MxPacs00800110 message) {
        log.info("[HIGH-VALUE ADAPTER] Prioritizing payment {} in High-Value Settlement Queue.", 
            message.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId());
    }
}
