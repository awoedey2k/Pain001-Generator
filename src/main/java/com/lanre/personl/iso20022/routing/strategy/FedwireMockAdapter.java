package com.lanre.personl.iso20022.routing.strategy;

import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FedwireMockAdapter implements MarketInfrastructureAdapter {
    @Override
    public String getName() { return "FEDWIRE-MOCK-SERVICE"; }

    @Override
    public void route(MxPacs00800110 message) {
        log.info("[FEDWIRE ADAPTER] Routing payment {} to Fedwire Funds Service.", 
            message.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId());
    }
}
