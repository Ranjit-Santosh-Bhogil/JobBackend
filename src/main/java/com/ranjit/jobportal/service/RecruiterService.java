package com.ranjit.jobportal.service;

import com.ranjit.jobportal.dto.response.StatsResponse;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.ApplicationStatus;
import com.ranjit.jobportal.exception.ApiException;
import com.ranjit.jobportal.repository.JobApplicationRepository;
import com.ranjit.jobportal.repository.JobRepository;
import com.ranjit.jobportal.repository.UserRepository;
import com.ranjit.jobportal.security.SecurityUtils;
import com.ranjit.jobportal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public StatsResponse getDashboardStats() {
        User recruiter = getCurrentRecruiter();
        long myJobs = jobRepository.findByPostedBy(recruiter, org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements();

        return StatsResponse.builder()
                .totalJobs(myJobs)
                .totalApplications(applicationRepository.countByJobPostedBy(recruiter))
                .pendingApplications(applicationRepository.countByJobPostedByAndStatus(
                        recruiter, ApplicationStatus.PENDING))
                .build();
    }

    private User getCurrentRecruiter() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
