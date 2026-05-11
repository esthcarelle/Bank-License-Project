package com.bnr.portal.service;

import com.bnr.portal.domain.ApplicationStage;
import com.bnr.portal.domain.CaseMove;
import com.bnr.portal.domain.UserRole;
import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.User;
import com.bnr.portal.repository.ApplicationRepository;
import com.bnr.portal.repository.UserRepository;
import com.bnr.portal.security.SignedInUser;
import com.bnr.portal.service.api.PermissionDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApplicationWorkflowAuthorizationTest {

    @Autowired
    WorkflowRunner workflowRunner;
    @Autowired
    ApplicationRepository applications;
    @Autowired
    UserRepository users;
    @Autowired
    PasswordEncoder passwordEncoder;

    private User applicant;
    private User reviewer;
    private User approverOther;

    @BeforeEach
    void setup() {
        applicant = users.save(person("applicant-auth@test.rw", UserRole.APPLICANT));
        reviewer = users.save(person("reviewer-auth@test.rw", UserRole.REVIEW_OFFICER));
        approverOther = users.save(person("approver-other@test.rw", UserRole.APPROVAL_OFFICER));
    }

    private User person(String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName("Test");
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode("p"));
        return u;
    }

    @Test
    void reviewerWhoSignedOffCannotAlsoApprove() {
        Application row = new Application();
        row.setApplicant(applicant);
        row.setInstitutionName("Test Bank");
        row.setState(ApplicationStage.PENDING_APPROVAL);
        row.setReviewedBy(reviewer);
        row.setCreatedAt(Instant.now());
        row.setUpdatedAt(Instant.now());
        Application saved = applications.save(row);

        SignedInUser reviewerSession = new SignedInUser(reviewer);

        final long id = saved.getId();
        final long ver = saved.getVersion();
        assertThrows(PermissionDeniedException.class, () ->
                workflowRunner.runTransition(
                        id,
                        CaseMove.APPROVE,
                        reviewerSession,
                        ver,
                        null
                ));
    }

    @Test
    void aDifferentApproverCanCloseTheCase() {
        Application row = new Application();
        row.setApplicant(applicant);
        row.setInstitutionName("Other Bank");
        row.setState(ApplicationStage.PENDING_APPROVAL);
        row.setReviewedBy(reviewer);
        row.setCreatedAt(Instant.now());
        row.setUpdatedAt(Instant.now());
        Application saved = applications.save(row);

        SignedInUser approverSession = new SignedInUser(approverOther);
        workflowRunner.runTransition(
                saved.getId(),
                CaseMove.APPROVE,
                approverSession,
                saved.getVersion(),
                null
        );
    }
}
