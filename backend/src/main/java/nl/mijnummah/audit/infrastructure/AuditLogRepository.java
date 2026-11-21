package nl.mijnummah.audit.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.audit.domain.AuditLogEntry;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    Page<AuditLogEntry> findByMosqueId(UUID mosqueId, Pageable pageable);

    @Modifying
    @Query("delete from AuditLogEntry e where e.occurredAt < :cutoff")
    int deleteOccurredBefore(@Param("cutoff") Instant cutoff);
}
