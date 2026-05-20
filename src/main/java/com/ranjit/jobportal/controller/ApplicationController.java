package com.ranjit.jobportal.controller;

import com.ranjit.jobportal.dto.request.ApplyJobRequest;
import com.ranjit.jobportal.dto.request.UpdateApplicationStatusRequest;
import com.ranjit.jobportal.dto.response.ApplicationResponse;
import com.ranjit.jobportal.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    // Connects candidates to jobs and lets recruiters move applications through statuses.
    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable Long jobId,
            @RequestBody(required = false) ApplyJobRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(jobId, request));
    }

    @GetMapping("/applications/me")
    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getMyApplications(pageable));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<Page<ApplicationResponse>> getApplicantsByJob(
            @PathVariable Long jobId,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getApplicantsByJob(jobId, pageable));
    }

    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request
    ) {
        return ResponseEntity.ok(applicationService.updateStatus(applicationId, request));
    }
}
