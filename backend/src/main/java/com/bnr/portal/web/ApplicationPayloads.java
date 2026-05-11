package com.bnr.portal.web;

import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.ApplicationDocument;
import com.bnr.portal.entity.AuditEntry;
import com.bnr.portal.web.dto.ApplicationResponse;
import com.bnr.portal.web.dto.AuditEntryResponse;
import com.bnr.portal.web.dto.DocumentMetadataResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationPayloads {

    /** Shape we send back to the browser for one application (includes audit + files list). */
    public ApplicationResponse packForBrowser(
            Application row,
            List<AuditEntry> auditLinesOldestFirst,
            List<ApplicationDocument> filesNewestFirst
    ) {
        List<AuditEntryResponse> audit = auditLinesOldestFirst.stream()
                .map(line -> new AuditEntryResponse(
                        line.getId(),
                        line.getActor().getEmail(),
                        line.getAction(),
                        line.getStateBefore(),
                        line.getStateAfter(),
                        line.getDetailsJson(),
                        line.getCreatedAt()
                ))
                .toList();

        List<DocumentMetadataResponse> docs = filesNewestFirst.stream()
                .map(d -> new DocumentMetadataResponse(
                        d.getId(),
                        d.getRevision(),
                        d.getOriginalFilename(),
                        d.getSizeBytes(),
                        d.getContentType(),
                        d.getUploadedBy().getEmail(),
                        d.getUploadedAt()
                ))
                .toList();

        String reviewerEmail = row.getReviewedBy() != null ? row.getReviewedBy().getEmail() : null;

        return new ApplicationResponse(
                row.getId(),
                row.getInstitutionName(),
                row.getState(),
                row.getVersion(),
                row.getApplicant().getEmail(),
                row.getApplicant().getFullName(),
                reviewerEmail,
                row.getLastRejectionReason(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                audit,
                docs
        );
    }
}
