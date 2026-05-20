package com.ranjit.jobportal.service;

import com.ranjit.jobportal.dto.request.ApplyJobRequest;
import com.ranjit.jobportal.dto.request.UpdateApplicationStatusRequest;
import com.ranjit.jobportal.dto.response.ApplicationResponse;
import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.entity.JobApplication;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.ApplicationStatus;
import com.ranjit.jobportal.enums.Role;
import com.ranjit.jobportal.exception.ApiException;
import com.ranjit.jobportal.repository.JobApplicationRepository;
import com.ranjit.jobportal.repository.JobRepository;
import com.ranjit.jobportal.repository.UserRepository;
import com.ranjit.jobportal.security.SecurityUtils;
import com.ranjit.jobportal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApplicationResponse apply(Long jobId, ApplyJobRequest request) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User candidate = getUser(principal.getId());

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only candidates can apply for jobs");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!job.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Job is not active");
        }

        if (applicationRepository.existsByJobAndCandidate(job, candidate)) {
            throw new ApiException(HttpStatus.CONFLICT, "Already applied to this job");
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .candidate(candidate)
                .status(ApplicationStatus.PENDING)
                .coverLetter(request != null ? request.getCoverLetter() : null)
                .build();

        return ApplicationResponse.from(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User candidate = getUser(principal.getId());
        return applicationRepository.findByCandidate(candidate, pageable).map(ApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicantsByJob(Long jobId, Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!canManageJob(principal, job)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return applicationRepository.findByJob(job, pageable).map(ApplicationResponse::from);
    }

    @Transactional
    public ApplicationResponse updateStatus(Long applicationId, UpdateApplicationStatusRequest request) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!canManageJob(principal, application.getJob())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        application.setStatus(request.getStatus());
        return ApplicationResponse.from(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getRecentApplicants(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User recruiter = getUser(principal.getId());
        return applicationRepository.findByJobPostedBy(recruiter, pageable).map(ApplicationResponse::from);
    }

    private boolean canManageJob(UserPrincipal principal, Job job) {
        if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        return job.getPostedBy().getId().equals(principal.getId());
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
