package com.bnr.portal.service;

import com.bnr.portal.domain.ApplicationStage;
import com.bnr.portal.domain.CaseMove;
import com.bnr.portal.domain.UserRole;
import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.User;
import com.bnr.portal.repository.ApplicationRepository;
import com.bnr.portal.repository.UserRepository;
import com.bnr.portal.security.SignedInUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentApprovalIntegrationTest {

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
    private User approverA;
    private User approverB;
    private long applicationId;
    private long version;

    @BeforeEach
    void setup() {
        applicant = users.save(person("c-app@test.rw", UserRole.APPLICANT));
        reviewer = users.save(person("c-rev@test.rw", UserRole.REVIEW_OFFICER));
        approverA = users.save(person("c-appA@test.rw", UserRole.APPROVAL_OFFICER));
        approverB = users.save(person("c-appB@test.rw", UserRole.APPROVAL_OFFICER));

        Application row = new Application();
        row.setApplicant(applicant);
        row.setInstitutionName("Concurrent Bank");
        row.setState(ApplicationStage.PENDING_APPROVAL);
        row.setReviewedBy(reviewer);
        row.setCreatedAt(Instant.now());
        row.setUpdatedAt(Instant.now());
        row = applications.save(row);
        applicationId = row.getId();
        version = row.getVersion();
    }

    private User person(String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName("C");
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode("p"));
        return u;
    }

    @Test
    void twoPeopleApproveAtOnce_onlyOneWinsTheRace() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<Throwable> errA = new AtomicReference<>();
        AtomicReference<Throwable> errB = new AtomicReference<>();

        Thread th1 = new Thread(() -> {
            try {
                start.await();
                workflowRunner.runTransition(
                        applicationId,
                        CaseMove.APPROVE,
                        new SignedInUser(approverA),
                        version,
                        null
                );
            } catch (Throwable t) {
                errA.set(t);
            } finally {
                done.countDown();
            }
        });
        Thread th2 = new Thread(() -> {
            try {
                start.await();
                workflowRunner.runTransition(
                        applicationId,
                        CaseMove.APPROVE,
                        new SignedInUser(approverB),
                        version,
                        null
                );
            } catch (Throwable t) {
                errB.set(t);
            } finally {
                done.countDown();
            }
        });

        th1.start();
        th2.start();
        start.countDown();
        done.await();

        long conflicts = 0;
        if (isOptimisticConflict(errA.get())) {
            conflicts++;
        }
        if (isOptimisticConflict(errB.get())) {
            conflicts++;
        }
        assertEquals(1, conflicts, "Exactly one concurrent approval should fail optimistic locking");
        Application fresh = applications.findById(applicationId).orElseThrow();
        assertEquals(ApplicationStage.APPROVED, fresh.getState());
    }

    private static boolean isOptimisticConflict(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof ObjectOptimisticLockingFailureException) {
            return true;
        }
        return t.getCause() != null && isOptimisticConflict(t.getCause());
    }
}
