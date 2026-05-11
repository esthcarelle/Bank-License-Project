package com.bnr.portal.web.dto;

import com.bnr.portal.domain.ApplicationStage;

import java.time.Instant;

public record ApplicationSummaryResponse(
        long id,
        String institutionName,
        ApplicationStage state,
        long version,
        String applicantEmail,
        Instant createdAt
) {}
