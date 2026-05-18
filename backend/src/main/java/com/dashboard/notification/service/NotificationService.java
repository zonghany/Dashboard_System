package com.dashboard.notification.service;

import com.dashboard.common.exception.BusinessException;
import com.dashboard.config.AppConfig;
import com.dashboard.domain.Notification;
import com.dashboard.domain.NotificationRepository;
import com.dashboard.domain.Task;
import com.dashboard.domain.TaskRepository;
import com.dashboard.domain.User;
import com.dashboard.domain.UserRepository;
import com.dashboard.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;
    private final AppConfig appConfig;

    public List<NotificationResponse> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "通知不存在"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Scheduled(fixedRateString = "${app.notification.scan-interval}")
    @Transactional
    public void checkDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(appConfig.getNotification().getReminderHours());

        List<Task> tasks = taskRepository.findByDueDateBetweenAndAssigneeIsNotNull(now, deadline);

        for (Task task : tasks) {
            User assignee = task.getAssignee();
            if (assignee == null) {
                continue;
            }

            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            boolean alreadyNotified = notificationRepository
                    .existsByUserIdAndRelatedTaskIdAndTypeAndCreatedAtAfter(
                            assignee.getId(), task.getId(), "REMINDER", twentyFourHoursAgo);

            if (alreadyNotified) {
                continue;
            }

            Duration duration = Duration.between(now, task.getDueDate());
            long hoursUntilDeadline = duration.toHours();

            String taskTitle = task.getTitle() != null && !task.getTitle().isEmpty()
                    ? task.getTitle()
                    : "未命名任务";

            String message = "任务「" + taskTitle + "」将在 " + hoursUntilDeadline + " 小时后截止";

            if (assignee.getEmail() != null) {
                try {
                    SimpleMailMessage mailMessage = new SimpleMailMessage();
                    mailMessage.setTo(assignee.getEmail());
                    mailMessage.setSubject("任务截止提醒");
                    mailMessage.setText("您的任务「" + taskTitle + "」即将在 " + hoursUntilDeadline + " 小时后截止，请及时处理。");
                    mailSender.send(mailMessage);
                } catch (Exception e) {
                }
            }

            Notification notification = new Notification();
            notification.setUser(assignee);
            notification.setMessage(message);
            notification.setType("REMINDER");
            notification.setRelatedTaskId(task.getId());
            notification.setIsRead(false);
            notificationRepository.save(notification);

            Map<String, Object> wsMsg = new HashMap<>();
            wsMsg.put("type", "NEW_NOTIFICATION");
            wsMsg.put("data", message);
            messagingTemplate.convertAndSend("/topic/user/" + assignee.getId(), wsMsg);
        }
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getRelatedTaskId(),
                notification.getType(),
                notification.getCreatedAt()
        );
    }
}