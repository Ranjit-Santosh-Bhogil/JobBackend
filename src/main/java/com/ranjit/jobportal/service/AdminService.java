package com.ranjit.jobportal.service;

import com.ranjit.jobportal.dto.request.UpdateUserStatusRequest;
import com.ranjit.jobportal.dto.response.StatsResponse;
import com.ranjit.jobportal.dto.response.UserResponse;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.ApplicationStatus;
import com.ranjit.jobportal.enums.Role;
import com.ranjit.jobportal.exception.ApiException;
import com.ranjit.jobportal.repository.JobApplicationRepository;
import com.ranjit.jobportal.repository.JobRepository;
import com.ranjit.jobportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;

    public StatsResponse getPlatformStats() {
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            usersByRole.put(role.name(), userRepository.countByRole(role));
        }

        return StatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalJobs(jobRepository.count())
                .totalApplications(applicationRepository.count())
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.PENDING))
                .usersByRole(usersByRole)
                .build();
    }

    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(request.getEnabled());
        return UserResponse.from(userRepository.save(user));
    }

    public Map<String, Object> getReports() {
        return Map.of(
                "platformStats", getPlatformStats(),
                "message", "Reports generated successfully"
        );
    }
}
