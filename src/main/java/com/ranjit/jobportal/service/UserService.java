package com.ranjit.jobportal.service;

import com.ranjit.jobportal.dto.request.UpdateProfileRequest;
import com.ranjit.jobportal.dto.response.UserResponse;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.exception.ApiException;
import com.ranjit.jobportal.repository.UserRepository;
import com.ranjit.jobportal.security.SecurityUtils;
import com.ranjit.jobportal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getProfile() {
        User user = getCurrentUserEntity();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        return UserResponse.from(userRepository.save(user));
    }

    private User getCurrentUserEntity() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
