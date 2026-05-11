package com.bnr.portal.service;

import com.bnr.portal.domain.ApplicationStage;
import com.bnr.portal.domain.CaseMove;
import com.bnr.portal.domain.StageTransitions;
import com.bnr.portal.domain.UserRole;
import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.User;
import com.bnr.portal.repository.ApplicationRepository;
import com.bnr.portal.repository.UserRepository;
import com.bnr.portal.security.SignedInUser;
import com.bnr.portal.service.api.InvalidOperationException;
import com.bnr.portal.service.api.PermissionDeniedException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkflowRunner {

    private final ApplicationRepository applications;
    private final UserRepository users;
    private final AuditTrail auditTrail;
    private final ApplicationRecords applicationRecords;

    public WorkflowRunner(
            ApplicationRepository applications,
            UserRepository users,
            AuditTrail auditTrail,
            ApplicationRecords applicationRecords) {
        this.applications = applications;
        this.users = users;
        this.auditTrail = auditTrail;
        this.applicationRecords = applicationRecords;
    }

    @Transactional
    public Application runTransition(
            long applicationId,
            CaseMove move,
            SignedInUser caller,
            long expectedRowVersion,
            String rejectionReason
    ) {
        Application row = applications.findDetailedById(applicationId)
                .orElseThrow(() -> new PermissionDeniedException("Application not found or access denied"));

        if (!applicationRecords.viewerMayOpen(caller, row)) {
            throw new PermissionDeniedException("Application not found or access denied");
        }

        if (row.getState().finished()) {
            throw new InvalidOperationException("Application is in a final state and cannot be changed");
        }

        if (row.getVersion() != expectedRowVersion) {
            throw new ObjectOptimisticLockingFailureException(Application.class, applicationId);
        }

        User actor = users.findById(caller.getId())
                .orElseThrow(() -> new PermissionDeniedException("User not found"));

        if (!StageTransitions.thisRoleMayMakeMove(actor.getRole(), move)) {
            throw new PermissionDeniedException("Your role cannot perform this action");
        }

        if (!StageTransitions.allowedFrom(row.getState(), move)) {
            throw new InvalidOperationException("Illegal transition: " + move + " from " + row.getState());
        }

        if (move == CaseMove.RESUBMIT) {
            if (!row.getApplicant().getId().equals(actor.getId())) {
                throw new PermissionDeniedException("Only the applicant may resubmit");
            }
        }

        if (move == CaseMove.APPROVE || move == CaseMove.REJECT) {
            if (actor.getRole() != UserRole.APPROVAL_OFFICER) {
                throw new PermissionDeniedException("Only an approval officer may finalize the decision");
            }
            if (row.getReviewedBy() == null) {
                throw new InvalidOperationException("Application has no recorded reviewing officer");
            }
            if (row.getReviewedBy().getId().equals(actor.getId())) {
                throw new PermissionDeniedException(
                        "Separation of duties: the officer who reviewed this application cannot grant final approval or rejection");
            }
            if (move == CaseMove.REJECT && (rejectionReason == null || rejectionReason.isBlank())) {
                throw new InvalidOperationException("Rejection reason is required");
            }
        }

        ApplicationStage before = row.getState();
        ApplicationStage after = StageTransitions.stageAfter(move);

        if (move == CaseMove.COMPLETE_REVIEW) {
            row.setReviewedBy(actor);
        }
        if (move == CaseMove.REJECT) {
            row.setLastRejectionReason(rejectionReason);
        }

        row.setState(after);
        row.setUpdatedAt(Instant.now());

        Application saved = applications.save(row);

        Map<String, Object> details = new HashMap<>();
        details.put("action", move.name());
        if (rejectionReason != null) {
            details.put("rejectionReason", rejectionReason);
        }

        auditTrail.record(actor, saved, move.name(),
                before.name(), after.name(), details);

        return saved;
    }
}
