package com.lanre.personl.iso20022.routing.repository;

import com.lanre.personl.iso20022.routing.entity.PaymentRoutingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRoutingRepository extends JpaRepository<PaymentRoutingAudit, Long> {
    List<PaymentRoutingAudit> findByMsgId(String msgId);
}
