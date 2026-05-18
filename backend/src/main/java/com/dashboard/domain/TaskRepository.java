package com.dashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByColumnIdOrderBySortOrderAsc(Long columnId);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByDueDateBetweenAndAssigneeIsNotNull(LocalDateTime start, LocalDateTime end);

    List<Task> findByColumn_ProjectIdOrderBySortOrderAsc(Long projectId);
}