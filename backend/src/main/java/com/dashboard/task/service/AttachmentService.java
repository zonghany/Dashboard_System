package com.dashboard.task.service;

import com.dashboard.common.exception.BusinessException;
import com.dashboard.config.AppConfig;
import com.dashboard.domain.Task;
import com.dashboard.domain.TaskAttachment;
import com.dashboard.domain.TaskAttachmentRepository;
import com.dashboard.domain.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskRepository taskRepository;
    private final AppConfig appConfig;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp", "pdf", "doc", "docx", "xls", "xlsx", "txt"
    ));

    @Transactional
    public TaskAttachment uploadAttachment(Long taskId, MultipartFile file) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "任务不存在"));

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "文件大小不能超过10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "文件名不能为空");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "不支持的文件类型，支持的格式：图片（PNG/JPG/GIF/WebP）和文档（PDF/DOC/DOCX/XLS/XLSX/TXT）");
        }

        String storedFilename = UUID.randomUUID().toString() + (dotIndex >= 0 ? originalFilename.substring(dotIndex) : "");
        Path uploadDir = Paths.get(appConfig.getUploadDir(), taskId.toString());
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法创建上传目录");
        }

        Path filePath = uploadDir.resolve(storedFilename);
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTask(task);
        attachment.setFileName(originalFilename);
        attachment.setFilePath(filePath.toString());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment = taskAttachmentRepository.save(attachment);

        return attachment;
    }

    public List<TaskAttachment> getAttachments(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "任务不存在"));
        return taskAttachmentRepository.findByTaskId(taskId);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "附件不存在"));

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件删除失败");
        }

        taskAttachmentRepository.delete(attachment);
    }
}