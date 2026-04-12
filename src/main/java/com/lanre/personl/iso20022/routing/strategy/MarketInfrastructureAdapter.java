package com.lanre.personl.iso20022.routing.strategy;

import com.prowidesoftware.swift.model.mx.MxPacs00800110;

public interface MarketInfrastructureAdapter {
    String getName();
    boolean supports(String currency, String receiverBic);
    void route(MxPacs00800110 message);
}
