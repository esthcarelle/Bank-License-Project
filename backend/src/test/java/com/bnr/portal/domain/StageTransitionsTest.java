package com.bnr.portal.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StageTransitionsTest {

    @Test
    void freshSubmission_canStartReview() {
        assertTrue(StageTransitions.allowedFrom(
                ApplicationStage.SUBMITTED, CaseMove.START_REVIEW));
    }

    @Test
    void freshSubmission_cannotApproveOutOfOrder() {
        assertFalse(StageTransitions.allowedFrom(
                ApplicationStage.SUBMITTED, CaseMove.APPROVE));
    }

    @Test
    void onceDecided_nothingElseHappens() {
        for (CaseMove move : CaseMove.values()) {
            assertFalse(StageTransitions.allowedFrom(ApplicationStage.APPROVED, move));
            assertFalse(StageTransitions.allowedFrom(ApplicationStage.REJECTED, move));
        }
    }

    @Test
    void afterMoreInfoRequested_applicantSendsBackToSubmitted() {
        assertTrue(StageTransitions.allowedFrom(
                ApplicationStage.ADDITIONAL_INFO_REQUESTED, CaseMove.RESUBMIT));
        assertEquals(ApplicationStage.SUBMITTED,
                StageTransitions.stageAfter(CaseMove.RESUBMIT));
    }

    @Test
    void whoIsAllowedToDoWhat() {
        assertEquals(UserRole.REVIEW_OFFICER,
                StageTransitions.roleThatMustMakeThisMove(CaseMove.START_REVIEW));
        assertEquals(UserRole.APPROVAL_OFFICER,
                StageTransitions.roleThatMustMakeThisMove(CaseMove.APPROVE));
        assertTrue(StageTransitions.thisRoleMayMakeMove(UserRole.APPLICANT, CaseMove.RESUBMIT));
        assertFalse(StageTransitions.thisRoleMayMakeMove(UserRole.APPLICANT, CaseMove.APPROVE));
    }
}
