package com.lanre.personl.iso20022.lifecycle.repository;

import com.lanre.personl.iso20022.lifecycle.dto.LifecycleWorkflowSummaryResponse;
import com.lanre.personl.iso20022.lifecycle.entity.PaymentWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentWorkflowRepository extends JpaRepository<PaymentWorkflow, Long> {
    Optional<PaymentWorkflow> findByEndToEndId(String endToEndId);
    boolean existsByEndToEndId(String endToEndId);

    @Query("""
            select new com.lanre.personl.iso20022.lifecycle.dto.LifecycleWorkflowSummaryResponse(
                w.id,
                w.endToEndId,
                w.status,
                w.amount,
                w.currency,
                w.debtorName,
                w.creditorName,
                w.remittanceInformation,
                w.createdAt,
                w.lastUpdatedAt,
                size(w.auditLogs)
            )
            from PaymentWorkflow w
            where w.endToEndId = :endToEndId
            """)
    Optional<LifecycleWorkflowSummaryResponse> findSummaryByEndToEndId(String endToEndId);

    @Query("""
            select new com.lanre.personl.iso20022.lifecycle.dto.LifecycleWorkflowSummaryResponse(
                w.id,
                w.endToEndId,
                w.status,
                w.amount,
                w.currency,
                w.debtorName,
                w.creditorName,
                w.remittanceInformation,
                w.createdAt,
                w.lastUpdatedAt,
                size(w.auditLogs)
            )
            from PaymentWorkflow w
            order by w.lastUpdatedAt desc
            """)
    List<LifecycleWorkflowSummaryResponse> findAllSummaries();
}
