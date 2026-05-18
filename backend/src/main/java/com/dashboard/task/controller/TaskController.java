package com.dashboard.task.controller;

import com.dashboard.task.dto.TaskRequest;
import com.dashboard.task.dto.TaskResponse;
import com.dashboard.task.service.AttachmentService;
import com.dashboard.task.service.TaskService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AttachmentService attachmentService;

    @GetMapping
    public List<TaskResponse> getTasks(@RequestParam Long columnId) {
        return taskService.getTasksByColumn(columnId);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestParam Long columnId) {
        Long userId = com.dashboard.auth.util.AuthContext.getCurrentUserId();
        TaskResponse response = taskService.createTask(columnId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<Void> moveTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long targetColumnId = body.get("targetColumnId") != null
                ? ((Number) body.get("targetColumnId")).longValue()
                : null;
        Integer sortOrder = body.get("sortOrder") != null
                ? ((Number) body.get("sortOrder")).intValue()
                : null;

        if (targetColumnId == null) {
            throw new com.dashboard.common.exception.BusinessException(
                    HttpStatus.BAD_REQUEST, "targetColumnId 不能为空");
        }

        taskService.moveTask(id, targetColumnId, sortOrder);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<?> uploadAttachment(@PathVariable Long id, @RequestParam MultipartFile file) {
        return ResponseEntity.ok(attachmentService.uploadAttachment(id, file));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<?> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getAttachments(id));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}