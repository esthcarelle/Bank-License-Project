package com.bnr.portal.repository;

import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {

    @Query("select d from ApplicationDocument d join fetch d.uploadedBy where d.application = :app order by d.revision desc, d.uploadedAt desc")
    List<ApplicationDocument> findByApplicationWithUploader(@Param("app") Application application);

    @Query("select coalesce(max(d.revision), 0) from ApplicationDocument d where d.application = ?1")
    int maxRevisionByApplication(Application application);
}
