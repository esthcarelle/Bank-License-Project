package com.bnr.portal.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Which moves are legal from each stage, and who may press the button.
 */
public final class StageTransitions {

    private static final Map<ApplicationStage, Set<CaseMove>> LEGAL_NEXT_STEPS =
            new EnumMap<>(ApplicationStage.class);

    static {
        LEGAL_NEXT_STEPS.put(ApplicationStage.SUBMITTED, EnumSet.of(CaseMove.START_REVIEW));
        LEGAL_NEXT_STEPS.put(
                ApplicationStage.UNDER_REVIEW,
                EnumSet.of(CaseMove.REQUEST_ADDITIONAL_INFO, CaseMove.COMPLETE_REVIEW));
        LEGAL_NEXT_STEPS.put(
                ApplicationStage.ADDITIONAL_INFO_REQUESTED,
                EnumSet.of(CaseMove.RESUBMIT));
        LEGAL_NEXT_STEPS.put(
                ApplicationStage.PENDING_APPROVAL,
                EnumSet.of(CaseMove.APPROVE, CaseMove.REJECT));
        LEGAL_NEXT_STEPS.put(ApplicationStage.APPROVED, EnumSet.noneOf(CaseMove.class));
        LEGAL_NEXT_STEPS.put(ApplicationStage.REJECTED, EnumSet.noneOf(CaseMove.class));
    }

    private static final Map<CaseMove, ApplicationStage> WHERE_IT_GOES = Map.of(
            CaseMove.START_REVIEW, ApplicationStage.UNDER_REVIEW,
            CaseMove.REQUEST_ADDITIONAL_INFO, ApplicationStage.ADDITIONAL_INFO_REQUESTED,
            CaseMove.RESUBMIT, ApplicationStage.SUBMITTED,
            CaseMove.COMPLETE_REVIEW, ApplicationStage.PENDING_APPROVAL,
            CaseMove.APPROVE, ApplicationStage.APPROVED,
            CaseMove.REJECT, ApplicationStage.REJECTED
    );

    private static final Map<CaseMove, UserRole> WHO_DOES_THIS = Map.of(
            CaseMove.START_REVIEW, UserRole.REVIEW_OFFICER,
            CaseMove.REQUEST_ADDITIONAL_INFO, UserRole.REVIEW_OFFICER,
            CaseMove.RESUBMIT, UserRole.APPLICANT,
            CaseMove.COMPLETE_REVIEW, UserRole.REVIEW_OFFICER,
            CaseMove.APPROVE, UserRole.APPROVAL_OFFICER,
            CaseMove.REJECT, UserRole.APPROVAL_OFFICER
    );

    private StageTransitions() {
    }

    public static boolean allowedFrom(ApplicationStage now, CaseMove move) {
        Set<CaseMove> moves = LEGAL_NEXT_STEPS.get(now);
        return moves != null && moves.contains(move);
    }

    public static ApplicationStage stageAfter(CaseMove move) {
        return WHERE_IT_GOES.get(move);
    }

    public static UserRole roleThatMustMakeThisMove(CaseMove move) {
        return WHO_DOES_THIS.get(move);
    }

    public static boolean thisRoleMayMakeMove(UserRole role, CaseMove move) {
        UserRole needed = WHO_DOES_THIS.get(move);
        return needed != null && needed == role;
    }
}
