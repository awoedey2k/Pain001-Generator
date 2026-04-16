package com.lanre.personl.iso20022.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuditPayloadProtectionService {

    private static final String ENCRYPTED_PREFIX = "enc:v1:";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final Iso20022SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String protect(String payload) {
        if (payload == null) {
            return null;
        }

        return switch (securityProperties.getAuditPayloadMode()) {
            case PLAINTEXT -> payload;
            case REDACT -> redact(payload);
            case ENCRYPT -> encrypt(payload);
        };
    }

    private String redact(String payload) {
        String redacted = payload;
        List<String> sensitiveTags = securityProperties.getSensitiveXmlTags();
        for (String tag : sensitiveTags) {
            Pattern pattern = Pattern.compile("(<(?:\\w+:)?" + Pattern.quote(tag) + ">)(.*?)(</(?:\\w+:)?" + Pattern.quote(tag) + ">)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(redacted);
            redacted = matcher.replaceAll("$1" + Matcher.quoteReplacement(securityProperties.getRedactionPlaceholder()) + "$3");
        }
        return redacted;
    }

    private String encrypt(String payload) {
        String configuredKey = securityProperties.getEncryptionKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("Audit payload encryption is enabled but iso20022.security.encryption-key is not configured.");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(configuredKey), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            return ENCRYPTED_PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt audit payload.", e);
        }
    }

    private SecretKeySpec buildKey(String configuredKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
