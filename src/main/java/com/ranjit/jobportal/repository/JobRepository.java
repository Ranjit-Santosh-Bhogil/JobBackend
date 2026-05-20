package com.ranjit.jobportal.repository;

import com.ranjit.jobportal.entity.Job;
import com.ranjit.jobportal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Page<Job> findByPostedBy(User postedBy, Pageable pageable);

    Page<Job> findByActiveTrue(Pageable pageable);

    long countByActiveTrue();
}
