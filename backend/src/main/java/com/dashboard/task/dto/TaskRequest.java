package com.dashboard.task.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRequest {

    private String title;

    private String description;

    private String priority;

    private LocalDateTime dueDate;

    private Long assigneeId;

    private Double estimatedHours;

    private Integer sortOrder;

    private Long columnId;
}