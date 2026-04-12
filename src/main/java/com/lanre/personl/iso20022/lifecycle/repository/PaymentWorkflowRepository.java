package com.lanre.personl.iso20022.lifecycle.repository;

import com.lanre.personl.iso20022.lifecycle.entity.PaymentWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentWorkflowRepository extends JpaRepository<PaymentWorkflow, Long> {
    Optional<PaymentWorkflow> findByEndToEndId(String endToEndId);
}
