package com.bnr.portal.service;

import com.bnr.portal.domain.ApplicationStage;
import com.bnr.portal.domain.UserRole;
import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.User;
import com.bnr.portal.repository.ApplicationRepository;
import com.bnr.portal.repository.UserRepository;
import com.bnr.portal.security.SignedInUser;
import com.bnr.portal.service.api.PermissionDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationRecords {

    private final ApplicationRepository applications;
    private final UserRepository users;
    private final AuditTrail auditTrail;

    public ApplicationRecords(
            ApplicationRepository applications,
            UserRepository users,
            AuditTrail auditTrail) {
        this.applications = applications;
        this.users = users;
        this.auditTrail = auditTrail;
    }

    @Transactional
    public Application registerNew(String institutionName, SignedInUser caller) {
        if (caller.getRole() != UserRole.APPLICANT) {
            throw new PermissionDeniedException("Only applicants can create applications");
        }
        User applicant = users.findById(caller.getId()).orElseThrow();
        Application fresh = new Application();
        fresh.setApplicant(applicant);
        fresh.setInstitutionName(institutionName);
        fresh.setState(ApplicationStage.SUBMITTED);
        fresh.setCreatedAt(Instant.now());
        fresh.setUpdatedAt(Instant.now());
        Application saved = applications.save(fresh);

        auditTrail.record(applicant, saved, "APPLICATION_CREATED",
                "NONE", ApplicationStage.SUBMITTED.name(),
                Map.of("institutionName", institutionName));
        return saved;
    }

    @Transactional(readOnly = true)
    public Application fetchOneIfAllowed(long applicationId, SignedInUser caller) {
        Application row = applications.findDetailedById(applicationId)
                .orElseThrow(() -> new PermissionDeniedException("Application not found or access denied"));
        if (!viewerMayOpen(caller, row)) {
            throw new PermissionDeniedException("Application not found or access denied");
        }
        return row;
    }

    @Transactional(readOnly = true)
    public List<Application> everythingVisibleTo(SignedInUser caller) {
        User me = users.findById(caller.getId()).orElseThrow();
        return switch (caller.getRole()) {
            case APPLICANT -> applications.findByApplicantOrderByCreatedAtDesc(me);
            case REVIEW_OFFICER, APPROVAL_OFFICER -> applications.findAllByOrderByCreatedAtDesc();
        };
    }

    public boolean viewerMayOpen(SignedInUser caller, Application row) {
        return switch (caller.getRole()) {
            case APPLICANT -> row.getApplicant().getId().equals(caller.getId());
            case REVIEW_OFFICER, APPROVAL_OFFICER -> true;
        };
    }
}
