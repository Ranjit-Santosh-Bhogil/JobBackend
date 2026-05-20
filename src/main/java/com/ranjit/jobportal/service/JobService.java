package com.ranjit.jobportal.service;

import com.ranjit.jobportal.dto.request.JobRequest;
import com.ranjit.jobportal.dto.response.JobResponse;
import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.JobType;
import com.ranjit.jobportal.enums.Role;
import com.ranjit.jobportal.exception.ApiException;
import com.ranjit.jobportal.repository.JobRepository;
import com.ranjit.jobportal.repository.UserRepository;
import com.ranjit.jobportal.security.SecurityUtils;
import com.ranjit.jobportal.security.UserPrincipal;
import com.ranjit.jobportal.specification.JobSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(
            String q,
            String location,
            JobType jobType,
            BigDecimal minSalary,
            Pageable pageable
    ) {
        Specification<Job> spec = Specification.allOf(
                JobSpecifications.isActive(),
                JobSpecifications.titleOrDescriptionContains(q),
                JobSpecifications.locationContains(location),
                JobSpecifications.hasJobType(jobType),
                JobSpecifications.minSalaryAtLeast(minSalary)
        );

        return jobRepository.findAll(spec, pageable).map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getMyJobs(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User user = getUser(principal.getId());
        return jobRepository.findByPostedBy(user, pageable).map(JobResponse::from);
    }

    @Transactional
    public JobResponse createJob(JobRequest request) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User user = getUser(principal.getId());

        if (user.getRole() != Role.RECRUITER && user.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only recruiters can post jobs");
        }

        Job job = mapRequest(new Job(), request);
        job.setPostedBy(user);
        job.setActive(request.getActive() == null || request.getActive());
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = getJobForModification(id);
        mapRequest(job, request);
        if (request.getActive() != null) {
            job.setActive(request.getActive());
        }
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public void deleteJob(Long id) {
        Job job = getJobForModification(id);
        jobRepository.delete(job);
    }

    private Job getJobForModification(Long id) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));

        if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return job;
        }

        if (!job.getPostedBy().getId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only modify your own jobs");
        }
        return job;
    }

    private Job mapRequest(Job job, JobRequest request) {
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompanyName(request.getCompanyName());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setMinSalary(request.getMinSalary());
        job.setMaxSalary(request.getMaxSalary());
        return job;
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
