package com.dashboard.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private String priority;

    private LocalDateTime dueDate;

    private Long assigneeId;

    private String assigneeName;

    private Double estimatedHours;

    private Integer sortOrder;

    private Long columnId;

    private String columnName;

    private List<String> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}