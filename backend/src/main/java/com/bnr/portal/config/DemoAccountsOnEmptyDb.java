package com.bnr.portal.config;

import com.bnr.portal.domain.ApplicationStage;
import com.bnr.portal.domain.UserRole;
import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.User;
import com.bnr.portal.repository.ApplicationRepository;
import com.bnr.portal.repository.UserRepository;
import com.bnr.portal.service.AuditTrail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Component
@Profile("!test")
public class DemoAccountsOnEmptyDb implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "tentativepassword";

    private static final Logger log = LoggerFactory.getLogger(DemoAccountsOnEmptyDb.class);

    private final UserRepository users;
    private final ApplicationRepository applications;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrail auditTrail;

    public DemoAccountsOnEmptyDb(
            UserRepository users,
            ApplicationRepository applications,
            PasswordEncoder passwordEncoder,
            AuditTrail auditTrail) {
        this.users = users;
        this.applications = applications;
        this.passwordEncoder = passwordEncoder;
        this.auditTrail = auditTrail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        log.info(
                "Seeding demo users and sample microfinance applications (password for all: {})",
                DEMO_PASSWORD);

        User applicant = savePerson("applicant@bnr.rw", "Ikaze Applicant", UserRole.APPLICANT);
        User reviewer = savePerson("reviewer@bnr.rw", "Umuhuzabikorwa Reviewer", UserRole.REVIEW_OFFICER);
        savePerson("approver@bnr.rw", "Umuyobozi Approver", UserRole.APPROVAL_OFFICER);

        Application urwego = createSubmittedApplication(applicant, "Urwego Bank");
        moveToUnderReview(urwego, reviewer);

        Application letshego = createSubmittedApplication(applicant, "Letshego");
        moveToUnderReview(letshego, reviewer);
        moveToPendingApproval(letshego, reviewer);

        createSubmittedApplication(applicant, "Unguka Bank");
        createSubmittedApplication(applicant, "AB Bank");
        createSubmittedApplication(applicant, "CoPEDU");
    }

    private Application createSubmittedApplication(User applicant, String institutionName) {
        Application a = new Application();
        a.setApplicant(applicant);
        a.setInstitutionName(institutionName);
        a.setState(ApplicationStage.SUBMITTED);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        a = applications.save(a);
        auditTrail.record(
                applicant, a, "APPLICATION_CREATED",
                "NONE", ApplicationStage.SUBMITTED.name(),
                Map.of("seed", true, "institutionName", a.getInstitutionName()));
        return a;
    }

    private void moveToUnderReview(Application app, User reviewer) {
        app.setState(ApplicationStage.UNDER_REVIEW);
        app.setUpdatedAt(Instant.now());
        app = applications.save(app);
        auditTrail.record(
                reviewer, app, "START_REVIEW",
                ApplicationStage.SUBMITTED.name(), ApplicationStage.UNDER_REVIEW.name(),
                Map.of("seed", true));
    }

    private void moveToPendingApproval(Application app, User reviewer) {
        app.setState(ApplicationStage.PENDING_APPROVAL);
        app.setReviewedBy(reviewer);
        app.setUpdatedAt(Instant.now());
        applications.save(app);
        auditTrail.record(
                reviewer, app, "COMPLETE_REVIEW",
                ApplicationStage.UNDER_REVIEW.name(), ApplicationStage.PENDING_APPROVAL.name(),
                Map.of("seed", true));
    }

    private User savePerson(String email, String name, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        return users.save(u);
    }
}
