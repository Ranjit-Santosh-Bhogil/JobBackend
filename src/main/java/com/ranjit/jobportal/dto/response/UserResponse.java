package com.ranjit.jobportal.dto.response;

import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private List<String> roles;
    private boolean enabled;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(List.of(user.getRole().name()))
                .enabled(user.isEnabled())
                .build();
    }
}
