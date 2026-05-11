package com.bnr.portal.repository;

import com.bnr.portal.domain.ApplicationStage;
import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @EntityGraph(attributePaths = {"applicant"})
    List<Application> findByApplicantOrderByCreatedAtDesc(User applicant);

    @EntityGraph(attributePaths = {"applicant"})
    List<Application> findAllByOrderByCreatedAtDesc();

    List<Application> findByStateOrderByCreatedAtDesc(ApplicationStage stage);

    @Query("select distinct a from Application a join fetch a.applicant left join fetch a.reviewedBy where a.id = ?1")
    Optional<Application> findDetailedById(Long id);
}
