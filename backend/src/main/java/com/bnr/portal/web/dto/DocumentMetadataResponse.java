package com.bnr.portal.web.dto;

import java.time.Instant;

public record DocumentMetadataResponse(
        long id,
        int revision,
        String originalFilename,
        long sizeBytes,
        String contentType,
        String uploadedByEmail,
        Instant uploadedAt
) {}
