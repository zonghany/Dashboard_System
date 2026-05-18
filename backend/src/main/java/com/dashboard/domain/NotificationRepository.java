package com.dashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    boolean existsByUserIdAndRelatedTaskIdAndTypeAndCreatedAtAfter(Long userId, Long relatedTaskId, String type, LocalDateTime after);
}