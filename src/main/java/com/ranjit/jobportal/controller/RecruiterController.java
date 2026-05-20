package com.ranjit.jobportal.controller;

import com.ranjit.jobportal.dto.response.ApplicationResponse;
import com.ranjit.jobportal.dto.response.StatsResponse;
import com.ranjit.jobportal.service.ApplicationService;
import com.ranjit.jobportal.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    // Recruiter dashboard summaries and recent applicant feeds.
    private final RecruiterService recruiterService;
    private final ApplicationService applicationService;

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(recruiterService.getDashboardStats());
    }

    @GetMapping("/applicants/recent")
    public ResponseEntity<Page<ApplicationResponse>> getRecentApplicants(
            @PageableDefault(size = 5, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getRecentApplicants(pageable));
    }
}
