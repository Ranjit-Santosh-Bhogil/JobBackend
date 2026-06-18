package com.ranjit.jobportal.config;

import com.ranjit.jobportal.security.CustomUserDetailsService;
import com.ranjit.jobportal.security.JwtAuthenticationFilter;
import com.ranjit.jobportal.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            if (authentication == null
                                    || !authentication.isAuthenticated()
                                    || authentication instanceof AnonymousAuthenticationToken) {
                                authenticationEntryPoint.commence(
                                        request,
                                        response,
                                        new org.springframework.security.authentication.InsufficientAuthenticationException(
                                                "Authentication required"
                                        )
                                );
                                return;
                            }
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\"}"
                            );
                        }))
                .authorizeHttpRequests(auth -> auth
                        // Authentication endpoints are public because they create or refresh JWTs.
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh").permitAll()

                        // Put specific job sub-routes before public /jobs matchers so roles are enforced.
                        .requestMatchers(HttpMethod.POST, "/jobs/*/apply").hasRole("CANDIDATE")
                        .requestMatchers(HttpMethod.GET, "/jobs/*/applications").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers("/jobs/my/**", "/jobs/my").hasAnyRole("RECRUITER", "ADMIN")

                        // Public job discovery: candidates and visitors can list jobs and open details.
                        .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/*").permitAll()

                        // Admin and recruiter dashboards expose private operational data.
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/recruiter/**").hasAnyRole("RECRUITER", "ADMIN")

                        // Only recruiters/admins can create, edit, or delete job postings.
                        .requestMatchers(HttpMethod.POST, "/jobs", "/jobs/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/jobs/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/jobs/**").hasAnyRole("RECRUITER", "ADMIN")

                        // Application records require a logged-in user; services enforce ownership.
                        .requestMatchers("/applications/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allows the Vite frontend to call the API during local development.
        configuration.setAllowedOriginPatterns(buildAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> buildAllowedOriginPatterns() {
        List<String> patterns = new java.util.ArrayList<>(Arrays.asList(allowedOrigins.split(",")));
        patterns.add("http://localhost:*");
        patterns.add("http://127.0.0.1:*");
        return patterns;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
