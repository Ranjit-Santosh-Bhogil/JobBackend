package com.ranjit.jobportal;

import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.JobType;
import com.ranjit.jobportal.enums.Role;
import com.ranjit.jobportal.repository.JobApplicationRepository;
import com.ranjit.jobportal.repository.JobRepository;
import com.ranjit.jobportal.repository.RefreshTokenRepository;
import com.ranjit.jobportal.repository.UserRepository;
import com.ranjit.jobportal.security.JwtService;
import com.ranjit.jobportal.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Job job;
    private User candidate;
    private User recruiter;

    @AfterEach
    void cleanup() {
        if (job != null && job.getId() != null) {
            applicationRepository.deleteByJob(job);
            jobRepository.deleteById(job.getId());
        }
        deleteUser(candidate);
        deleteUser(recruiter);
    }

    @Test
    void candidateCanApplyWithCoverLetter() throws Exception {
        recruiter = createUser(Role.RECRUITER);
        candidate = createUser(Role.CANDIDATE);
        job = createJob(recruiter);

        mockMvc.perform(post("/jobs/{jobId}/apply", job.getId())
                        .header("Authorization", "Bearer " + tokenFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverLetter\":\"I am excited to apply for this role.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId", is(job.getId().intValue())))
                .andExpect(jsonPath("$.candidateId", is(candidate.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.coverLetter", is("I am excited to apply for this role.")));
    }

    @Test
    void recruiterCannotApplyToAJob() throws Exception {
        recruiter = createUser(Role.RECRUITER);
        job = createJob(recruiter);

        mockMvc.perform(post("/jobs/{jobId}/apply", job.getId())
                        .header("Authorization", "Bearer " + tokenFor(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverLetter\":\"This should not be accepted.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCanCreateJob() throws Exception {
        recruiter = createUser(Role.RECRUITER);

        String body = """
                {
                  "title": "Frontend Engineer",
                  "description": "Build candidate-facing job portal screens.",
                  "companyName": "Hiring Labs",
                  "location": "Remote",
                  "jobType": "FULL_TIME",
                  "minSalary": 80000,
                  "maxSalary": 120000,
                  "active": true
                }
                """;

        String response = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + tokenFor(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Frontend Engineer")))
                .andExpect(jsonPath("$.postedById", is(recruiter.getId().intValue())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long createdJobId = Long.valueOf(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
        job = jobRepository.findById(createdJobId).orElseThrow();
    }

    @Test
    void candidateCannotCreateJob() throws Exception {
        candidate = createUser(Role.CANDIDATE);

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + tokenFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Invalid\",\"description\":\"Candidates cannot post jobs.\"}"))
                .andExpect(status().isForbidden());
    }

    private User createUser(Role role) {
        String id = UUID.randomUUID().toString();
        User user = User.builder()
                .name(role.name() + " Test User")
                .email(role.name().toLowerCase() + "-" + id + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    private Job createJob(User postedBy) {
        Job newJob = Job.builder()
                .title("Backend Test Engineer")
                .description("Build and test Spring Boot APIs.")
                .companyName("Test Company")
                .location("Remote")
                .jobType(JobType.REMOTE)
                .minSalary(new BigDecimal("70000"))
                .maxSalary(new BigDecimal("90000"))
                .active(true)
                .postedBy(postedBy)
                .build();
        return jobRepository.save(newJob);
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(UserPrincipal.from(user));
    }

    private void deleteUser(User user) {
        if (user != null && user.getId() != null) {
            refreshTokenRepository.deleteByUser(user);
            userRepository.deleteById(user.getId());
        }
    }
}
