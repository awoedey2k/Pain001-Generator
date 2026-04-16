package com.lanre.personl.iso20022.lifecycle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "iso_message_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lifecycle audit entry for a generated, validated, routed, or reconciled ISO 20022 message.")
public class IsoMessageAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1")
    private Long id;

    @Schema(example = "pain.001", description = "ISO 20022 message family recorded in the lifecycle.")
    private String messageType; // pain.001, pacs.008, pacs.002, camt.053

    @Schema(example = "MSGID-20260412123300-4fe65c60", description = "Domain message identifier for the payload.")
    private String messageId;

    @Lob
    @Column(columnDefinition = "CLOB")
    @Schema(description = "Protected payload snapshot as stored after redaction or encryption policy is applied.")
    private String payload;

    @Schema(example = "2026-04-16T10:15:30", description = "Timestamp when the audit entry was created.")
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "workflow_id")
    @JsonIgnore
    @Schema(hidden = true)
    private PaymentWorkflow workflow;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
