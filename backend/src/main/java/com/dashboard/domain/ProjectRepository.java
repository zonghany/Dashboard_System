package com.dashboard.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p JOIN ProjectMember pm ON p.id = pm.project.id WHERE pm.user.id = :userId ORDER BY p.updatedAt DESC")
    Page<Project> findByMemberUserId(@Param("userId") Long userId, Pageable pageable);
}