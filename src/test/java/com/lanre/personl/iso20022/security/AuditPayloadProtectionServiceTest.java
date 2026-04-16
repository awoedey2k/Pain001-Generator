package com.lanre.personl.iso20022.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditPayloadProtectionServiceTest {

    @Test
    @DisplayName("Should redact configured sensitive XML tags by default")
    void shouldRedactSensitiveTags() {
        Iso20022SecurityProperties properties = new Iso20022SecurityProperties();
        AuditPayloadProtectionService service = new AuditPayloadProtectionService(properties);

        String payload = "<Doc:Nm>Alice</Doc:Nm><Doc:IBAN>GB123456</Doc:IBAN><Doc:MsgId>MSG-1</Doc:MsgId>";

        String protectedPayload = service.protect(payload);

        assertFalse(protectedPayload.contains("Alice"));
        assertFalse(protectedPayload.contains("GB123456"));
        assertTrue(protectedPayload.contains("<Doc:Nm>[REDACTED]</Doc:Nm>"));
        assertTrue(protectedPayload.contains("<Doc:IBAN>[REDACTED]</Doc:IBAN>"));
        assertTrue(protectedPayload.contains("<Doc:MsgId>MSG-1</Doc:MsgId>"));
    }

    @Test
    @DisplayName("Should encrypt payload when encryption mode is enabled")
    void shouldEncryptPayload() {
        Iso20022SecurityProperties properties = new Iso20022SecurityProperties();
        properties.setAuditPayloadMode(Iso20022SecurityProperties.PayloadProtectionMode.ENCRYPT);
        properties.setEncryptionKey("demo-encryption-key");
        AuditPayloadProtectionService service = new AuditPayloadProtectionService(properties);

        String payload = "<Doc:Nm>Alice</Doc:Nm>";

        String protectedPayload = service.protect(payload);

        assertNotEquals(payload, protectedPayload);
        assertTrue(protectedPayload.startsWith("enc:v1:"));
        assertFalse(protectedPayload.contains("Alice"));
    }
}
