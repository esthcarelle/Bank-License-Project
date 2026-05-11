package com.bnr.portal.web.dto;

import java.time.Instant;

public record AuditEntryResponse(
        long id,
        String actorEmail,
        String action,
        String stateBefore,
        String stateAfter,
        String detailsJson,
        Instant createdAt
) {}
