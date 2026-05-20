package com.ranjit.jobportal.config;

import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.entity.User;
import com.ranjit.jobportal.enums.JobType;
import com.ranjit.jobportal.enums.Role;
import com.ranjit.jobportal.repository.JobRepository;
import com.ranjit.jobportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed predictable demo accounts so the frontend can be tested immediately.
        ensureUser("Admin User", "admin@jobportal.com", "admin123", Role.ADMIN);
        User recruiter = ensureUser("Recruiter Demo", "recruiter@jobportal.com", "recruiter123", Role.RECRUITER);
        ensureUser("Candidate Demo", "candidate@jobportal.com", "candidate123", Role.CANDIDATE);

        if (jobRepository.count() == 0) {
            // Add starter jobs only on an empty database to avoid duplicate sample data.
            jobRepository.save(Job.builder()
                    .title("Senior React Developer")
                    .description("Build modern job portal UIs with React, Vite, and Tailwind CSS.")
                    .companyName("TechNova Labs")
                    .location("Remote")
                    .jobType(JobType.REMOTE)
                    .minSalary(new BigDecimal("90000"))
                    .maxSalary(new BigDecimal("130000"))
                    .active(true)
                    .postedBy(recruiter)
                    .build());

            jobRepository.save(Job.builder()
                    .title("Java Spring Boot Engineer")
                    .description("Develop secure REST APIs with Spring Boot, JWT, and PostgreSQL.")
                    .companyName("CloudHire")
                    .location("New York, NY")
                    .jobType(JobType.FULL_TIME)
                    .minSalary(new BigDecimal("100000"))
                    .maxSalary(new BigDecimal("150000"))
                    .active(true)
                    .postedBy(recruiter)
                    .build());

            log.info("Seeded sample jobs");
        }

        log.info("Demo accounts ready:");
        log.info("  Admin     -> admin@jobportal.com / admin123");
        log.info("  Recruiter -> recruiter@jobportal.com / recruiter123");
        log.info("  Candidate -> candidate@jobportal.com / candidate123");
    }

    private User ensureUser(String name, String email, String rawPassword, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .enabled(true)
                    .build();
            log.info("Created default {} user: {}", role, email);
            return userRepository.save(user);
        });
    }
}
