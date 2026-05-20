package com.ranjit.jobportal.repository;

import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.entity.JobApplication;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    @EntityGraph(attributePaths = {"job", "candidate", "job.postedBy"})
    Page<JobApplication> findByCandidate(User candidate, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "candidate", "job.postedBy"})
    Page<JobApplication> findByJob(Job job, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "candidate", "job.postedBy"})
    Page<JobApplication> findByJobPostedBy(User recruiter, Pageable pageable);

    boolean existsByJobAndCandidate(Job job, User candidate);

    @EntityGraph(attributePaths = {"job", "candidate", "job.postedBy"})
    Optional<JobApplication> findById(Long id);

    @EntityGraph(attributePaths = {"job", "candidate", "job.postedBy"})
    Optional<JobApplication> findByIdAndJobPostedBy(Long id, User recruiter);

    long countByJobPostedBy(User recruiter);

    long countByJobPostedByAndStatus(User recruiter, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);
}
