package com.dashboard.domain;

import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task_attachments")
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    private String fileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}