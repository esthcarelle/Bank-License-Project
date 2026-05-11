package com.bnr.portal.web.dto;

import com.bnr.portal.domain.ApplicationStage;

import java.time.Instant;
import java.util.List;

public record ApplicationResponse(
        long id,
        String institutionName,
        ApplicationStage state,
        long version,
        String applicantEmail,
        String applicantName,
        String reviewedByEmail,
        String lastRejectionReason,
        Instant createdAt,
        Instant updatedAt,
        List<AuditEntryResponse> auditTrail,
        List<DocumentMetadataResponse> documents
) {}
