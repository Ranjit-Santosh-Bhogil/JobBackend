package com.ranjit.jobportal;

import com.ranjit.jobportal.config.DatabaseEnsurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Properties;

@SpringBootApplication
public class JobPortalApplication {

    public static void main(String[] args) {
        // Prepare PostgreSQL before Spring creates the datasource.
        ensureDatabaseFromConfig();
        SpringApplication.run(JobPortalApplication.class, args);
    }

    private static void ensureDatabaseFromConfig() {
        try {
            // Read the same datasource values that Spring Boot will use at runtime.
            Properties properties = new Properties();
            try (InputStream input = new ClassPathResource("application.properties").getInputStream()) {
                properties.load(input);
            }

            String url = properties.getProperty("spring.datasource.url");
            String username = properties.getProperty("spring.datasource.username");
            String password = properties.getProperty("spring.datasource.password");

            if (url != null && username != null && password != null) {
                DatabaseEnsurer.ensureDatabaseExists(url, username, password);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not prepare PostgreSQL database before startup", ex);
        }
    }
}
