package com.lanre.personl.iso20022.security;

public record AuditPayloadRecord(
        String payload,
        String payloadHash,
        String payloadStorageType,
        String payloadReference
) {
}
