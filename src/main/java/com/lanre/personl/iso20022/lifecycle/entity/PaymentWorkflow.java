package com.lanre.personl.iso20022.lifecycle.entity;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_workflows")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Correlated payment lifecycle view keyed by EndToEndId.")
public class PaymentWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1")
    private Long id;

    @Column(unique = true, nullable = false)
    @Schema(example = "INV-2026-00042", description = "Unique end-to-end transaction reference shared across message types.")
    private String endToEndId;

    @Schema(example = "SETTLING", description = "Current workflow status such as PENDING, SETTLING, RECONCILED, or FAILED.")
    private String status; // PENDING, SETTLING, RECONCILED, FAILED

    @Schema(example = "1500.00")
    private BigDecimal amount;

    @Schema(example = "EUR")
    private String currency;

    @Schema(example = "Acme Corporation")
    private String debtorName;

    @Schema(example = "Widget Supplies Ltd")
    private String creditorName;

    @Schema(example = "Invoice 2026-00042 payment")
    private String remittanceInformation;

    @Schema(example = "2026-04-16T10:10:00")
    private LocalDateTime createdAt;

    @Schema(example = "2026-04-16T10:15:30")
    private LocalDateTime lastUpdatedAt;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    @ArraySchema(schema = @Schema(implementation = IsoMessageAudit.class))
    private List<IsoMessageAudit> auditLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
