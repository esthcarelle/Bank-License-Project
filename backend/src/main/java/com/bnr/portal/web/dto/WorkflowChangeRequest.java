package com.bnr.portal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** Body for POST that tells us which move you want and which row version you saw. */
public record WorkflowChangeRequest(
        @NotNull String action,
        @NotNull Long expectedVersion,
        @JsonProperty("rejectionReason")
        String rejectionReason
) {}
