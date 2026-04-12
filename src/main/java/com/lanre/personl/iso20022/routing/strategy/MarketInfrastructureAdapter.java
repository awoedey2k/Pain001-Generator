package com.lanre.personl.iso20022.routing.strategy;

import com.prowidesoftware.swift.model.mx.MxPacs00800110;

/**
 * Strategy Interface for Market Infrastructure (MI) Adapters.
 * <p>
 * Implementations of this interface represent specialized clearing systems
 * (e.g., SEPA, Fedwire) or priority settlement queues.
 * </p>
 */
public interface MarketInfrastructureAdapter {
    String getName();
    boolean supports(String currency, String receiverBic);
    void route(MxPacs00800110 message);
}
