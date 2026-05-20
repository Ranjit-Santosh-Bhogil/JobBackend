package com.ranjit.jobportal.dto.response;

import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.enums.JobType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private String companyName;
    private String company;
    private String location;
    private JobType jobType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private boolean active;
    private Long postedById;
    private LocalDateTime createdAt;
    private LocalDateTime postedAt;

    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .companyName(job.getCompanyName())
                .company(job.getCompanyName())
                .location(job.getLocation())
                .jobType(job.getJobType())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .active(job.isActive())
                .postedById(job.getPostedBy().getId())
                .createdAt(job.getCreatedAt())
                .postedAt(job.getCreatedAt())
                .build();
    }
}
