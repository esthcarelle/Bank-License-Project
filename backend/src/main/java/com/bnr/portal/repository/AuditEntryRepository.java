package com.bnr.portal.repository;

import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Append-only use: only {@link #save} and reads. Application code never calls delete/update on audit.
 */
public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    @Query("select e from AuditEntry e join fetch e.actor where e.application = :app order by e.createdAt asc")
    List<AuditEntry> findForApplicationWithActor(@Param("app") Application app);
}
