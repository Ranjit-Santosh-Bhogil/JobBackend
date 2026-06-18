package com.ranjit.jobportal;

import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.Role;
import com.ranjit.jobportal.repository.RefreshTokenRepository;
import com.ranjit.jobportal.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @AfterEach
    void cleanup() {
        deleteUser(user);
    }

    @Test
    void unauthenticatedProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/applications/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void refreshTokenCanBeRotatedMultipleTimes() throws Exception {
        user = createUser(Role.CANDIDATE);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = extractJsonField(loginResult.getResponse().getContentAsString(), "refreshToken");

        for (int attempt = 0; attempt < 3; attempt++) {
            MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andReturn();

            refreshToken = extractJsonField(refreshResult.getResponse().getContentAsString(), "refreshToken");
        }
    }

    private User createUser(Role role) {
        String id = UUID.randomUUID().toString();
        User created = User.builder()
                .name(role.name() + " Auth Test")
                .email(role.name().toLowerCase() + "-" + id + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(created);
    }

    private void deleteUser(User userToDelete) {
        if (userToDelete != null && userToDelete.getId() != null) {
            refreshTokenRepository.deleteByUser(userToDelete);
            userRepository.deleteById(userToDelete.getId());
        }
    }

    private String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Missing field " + field + " in " + json);
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
