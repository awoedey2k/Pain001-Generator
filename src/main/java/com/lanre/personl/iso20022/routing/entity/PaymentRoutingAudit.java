package com.lanre.personl.iso20022.routing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_routing_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRoutingAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String msgId;

    private String currency;

    private String receiverBic;

    private String destinationRoute;

    private boolean highValue;

    private LocalDateTime processedAt;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String decisionReason;
}
