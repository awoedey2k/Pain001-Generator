package com.lanre.personl.iso20022.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "iso20022.security")
public class Iso20022SecurityProperties {

    private PayloadProtectionMode auditPayloadMode = PayloadProtectionMode.REDACT;
    private String encryptionKey = "";
    private String redactionPlaceholder = "[REDACTED]";
    private List<String> sensitiveXmlTags = new ArrayList<>(
            List.of("Nm", "IBAN", "BICFI", "Ustrd", "AdrLine", "EndToEndId", "InstrId", "TxId")
    );

    public enum PayloadProtectionMode {
        PLAINTEXT,
        REDACT,
        ENCRYPT
    }
}
