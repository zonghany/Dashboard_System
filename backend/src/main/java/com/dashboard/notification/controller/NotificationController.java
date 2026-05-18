package com.dashboard.notification.controller;

import com.dashboard.auth.util.AuthContext;
import com.dashboard.notification.dto.NotificationResponse;
import com.dashboard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getNotifications() {
        Long userId = AuthContext.getCurrentUserId();
        return notificationService.getNotifications(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        Long userId = AuthContext.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    @PutMapping("/{id}/read")
    public Map<String, String> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        return result;
    }
}