package com.lanre.personl.iso20022.routing.strategy;

import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SepaMockAdapter implements MarketInfrastructureAdapter {
    @Override
    public String getName() { return "SEPA-MOCK-SERVICE"; }

    @Override
    public boolean supports(String currency, String receiverBic) {
        return "EUR".equalsIgnoreCase(currency);
    }

    @Override
    public void route(MxPacs00800110 message) {
        log.info("[SEPA ADAPTER] Routing payment {} to SEPA Instant Clearing.", 
            message.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId());
    }
}
