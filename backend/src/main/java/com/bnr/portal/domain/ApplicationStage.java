package com.bnr.portal.domain;

/**
 * Application stage
 */
public enum ApplicationStage {
    SUBMITTED,
    UNDER_REVIEW,
    ADDITIONAL_INFO_REQUESTED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED;

    public boolean finished() {
        return this == APPROVED || this == REJECTED;
    }
}
