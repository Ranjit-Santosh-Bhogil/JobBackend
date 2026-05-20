package com.ranjit.jobportal.specification;

import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.enums.JobType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    // Base filter for public job browsing: only active listings are shown.
    public static Specification<Job> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Job> titleOrDescriptionContains(String q) {
        if (q == null || q.isBlank()) {
            return alwaysTrue();
        }
        String pattern = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("companyName")), pattern)
        );
    }

    public static Specification<Job> locationContains(String location) {
        if (location == null || location.isBlank()) {
            return alwaysTrue();
        }
        String pattern = "%" + location.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("location")), pattern);
    }

    public static Specification<Job> hasJobType(JobType jobType) {
        if (jobType == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.equal(root.get("jobType"), jobType);
    }

    public static Specification<Job> minSalaryAtLeast(BigDecimal minSalary) {
        if (minSalary == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("minSalary"), minSalary);
    }

    private static Specification<Job> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }
}
