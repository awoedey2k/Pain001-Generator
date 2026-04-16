package com.lanre.personl.iso20022.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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
    @DisplayName("Should store protected payload in database mode with hash metadata")
    void shouldPrepareDatabasePayloadRecord() {
        Iso20022SecurityProperties properties = new Iso20022SecurityProperties();
        AuditPayloadProtectionService service = new AuditPayloadProtectionService(properties);

        AuditPayloadRecord record = service.protectForStorage("<Doc:Nm>Alice</Doc:Nm>");

        assertNotNull(record.payload());
        assertNotNull(record.payloadHash());
        assertEquals("DATABASE", record.payloadStorageType());
        assertNull(record.payloadReference());
        assertTrue(record.payload().contains("[REDACTED]"));
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

    @Test
    @DisplayName("Should store only payload hash when hash-only mode is enabled")
    void shouldPrepareHashOnlyPayloadRecord() {
        Iso20022SecurityProperties properties = new Iso20022SecurityProperties();
        properties.setAuditPayloadStorageMode(Iso20022SecurityProperties.PayloadStorageMode.HASH_ONLY);
        AuditPayloadProtectionService service = new AuditPayloadProtectionService(properties);

        AuditPayloadRecord record = service.protectForStorage("<Doc:Nm>Alice</Doc:Nm>");

        assertNull(record.payload());
        assertNotNull(record.payloadHash());
        assertEquals(64, record.payloadHash().length());
        assertEquals("HASH_ONLY", record.payloadStorageType());
        assertNull(record.payloadReference());
    }

    @Test
    @DisplayName("Should write protected payload to filesystem when blob-style storage is enabled")
    void shouldPrepareFilesystemPayloadRecord(@TempDir Path tempDir) throws Exception {
        Iso20022SecurityProperties properties = new Iso20022SecurityProperties();
        properties.setAuditPayloadStorageMode(Iso20022SecurityProperties.PayloadStorageMode.FILESYSTEM);
        properties.getBlobStorage().setEnabled(true);
        properties.getBlobStorage().setBasePath(tempDir.toString());
        AuditPayloadProtectionService service = new AuditPayloadProtectionService(properties);

        AuditPayloadRecord record = service.protectForStorage("<Doc:Nm>Alice</Doc:Nm>");

        assertNull(record.payload());
        assertNotNull(record.payloadHash());
        assertEquals("FILESYSTEM", record.payloadStorageType());
        assertNotNull(record.payloadReference());

        String persisted = Files.readString(Path.of(record.payloadReference()));
        assertTrue(persisted.contains("[REDACTED]"));
        assertFalse(persisted.contains("Alice"));
    }
}
