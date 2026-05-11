package com.bnr.portal.domain;

/**
 * Something a user might try to do that moves the case forward
 */
public enum CaseMove {
    START_REVIEW,
    REQUEST_ADDITIONAL_INFO,
    RESUBMIT,
    COMPLETE_REVIEW,
    APPROVE,
    REJECT
}
