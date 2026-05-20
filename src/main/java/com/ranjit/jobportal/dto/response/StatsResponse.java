package com.ranjit.jobportal.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class StatsResponse {

    private long totalJobs;
    private long totalApplications;
    private long totalUsers;
    private long pendingApplications;
    private Map<String, Long> usersByRole;
}
