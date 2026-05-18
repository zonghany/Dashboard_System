package com.dashboard.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {

    private Long id;

    private String message;

    private boolean isRead;

    private Long relatedTaskId;

    private String type;

    private LocalDateTime createdAt;
}