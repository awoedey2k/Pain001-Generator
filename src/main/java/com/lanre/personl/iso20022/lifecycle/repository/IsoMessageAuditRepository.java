package com.lanre.personl.iso20022.lifecycle.repository;

import com.lanre.personl.iso20022.lifecycle.dto.LifecycleAuditEntryResponse;
import com.lanre.personl.iso20022.lifecycle.entity.IsoMessageAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    boolean existsByWorkflowEndToEndIdAndMessageType(String endToEndId, String messageType);

    @Query("""
            select a.messageId
            from IsoMessageAudit a
            where a.workflow.endToEndId = :endToEndId
              and a.messageType = :messageType
            order by a.timestamp desc
            """)
    List<String> findMessageIdsByWorkflowEndToEndIdAndMessageType(String endToEndId, String messageType);

    default Optional<String> findLatestMessageIdByWorkflowEndToEndIdAndMessageType(String endToEndId, String messageType) {
        List<String> ids = findMessageIdsByWorkflowEndToEndIdAndMessageType(endToEndId, messageType);
        if (ids == null || ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ids.get(0));
    }
}
