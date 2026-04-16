package com.lanre.personl.iso20022.lifecycle.repository;

import com.lanre.personl.iso20022.lifecycle.dto.LifecycleAuditEntryResponse;
import com.lanre.personl.iso20022.lifecycle.entity.IsoMessageAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IsoMessageAuditRepository extends JpaRepository<IsoMessageAudit, Long> {

    @Query("""
            select new com.lanre.personl.iso20022.lifecycle.dto.LifecycleAuditEntryResponse(
                a.id,
                a.messageType,
                a.messageId,
                a.timestamp
            )
            from IsoMessageAudit a
            where a.workflow.endToEndId = :endToEndId
            order by a.timestamp asc
            """)
    List<LifecycleAuditEntryResponse> findMetadataByWorkflowEndToEndId(String endToEndId);
}
