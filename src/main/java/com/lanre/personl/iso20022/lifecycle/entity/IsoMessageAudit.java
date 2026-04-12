package com.lanre.personl.iso20022.lifecycle.entity;

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
public class IsoMessageAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String messageType; // pain.001, pacs.008, pacs.002, camt.053

    private String messageId;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String payload;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "workflow_id")
    private PaymentWorkflow workflow;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
