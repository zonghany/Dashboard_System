package com.dashboard.task.service;

import com.dashboard.common.exception.BusinessException;
import com.dashboard.domain.BoardColumn;
import com.dashboard.domain.BoardColumnRepository;
import com.dashboard.domain.Notification;
import com.dashboard.domain.NotificationRepository;
import com.dashboard.domain.Task;
import com.dashboard.domain.TaskAttachmentRepository;
import com.dashboard.domain.TaskRepository;
import com.dashboard.domain.User;
import com.dashboard.domain.UserRepository;
import com.dashboard.domain.enums.TaskPriority;
import com.dashboard.task.dto.TaskRequest;
import com.dashboard.task.dto.TaskResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public List<TaskResponse> getTasksByColumn(Long columnId) {
        List<Task> tasks = taskRepository.findByColumnIdOrderBySortOrderAsc(columnId);
        return tasks.stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse createTask(Long columnId, Long userId) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "看板列不存在"));

        List<Task> existingTasks = taskRepository.findByColumnIdOrderBySortOrderAsc(columnId);
        int maxSort = existingTasks.stream()
                .mapToInt(t -> t.getSortOrder() != null ? t.getSortOrder() : 0)
                .max()
                .orElse(0);

        Task task = new Task();
        task.setColumn(column);
        task.setTitle("");
        task.setSortOrder(maxSort + 1);
        task = taskRepository.save(task);

        TaskResponse response = toTaskResponse(task);

        Long projectId = column.getProject().getId();
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "TASK_CREATED");
        wsMsg.put("data", response);
        messagingTemplate.convertAndSend("/topic/board/" + projectId, wsMsg);

        return response;
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "任务不存在"));

        Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;

        if (req.getTitle() != null) {
            task.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            task.setDescription(req.getDescription());
        }
        if (req.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(req.getPriority()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "无效的优先级值");
            }
        }
        if (req.getDueDate() != null) {
            task.setDueDate(req.getDueDate());
        }
        if (req.getEstimatedHours() != null) {
            task.setEstimatedHours(req.getEstimatedHours());
        }

        if (req.getAssigneeId() != null) {
            User assignee = userRepository.findById(req.getAssigneeId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
            task.setAssignee(assignee);
        }

        if (req.getColumnId() != null && !req.getColumnId().equals(task.getColumn().getId())) {
            BoardColumn newColumn = boardColumnRepository.findById(req.getColumnId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "目标列不存在"));
            task.setColumn(newColumn);
            if (req.getSortOrder() != null) {
                task.setSortOrder(req.getSortOrder());
            }
        } else if (req.getSortOrder() != null) {
            task.setSortOrder(req.getSortOrder());
        }

        task = taskRepository.save(task);

        if (task.getAssignee() != null
                && (oldAssigneeId == null || !oldAssigneeId.equals(task.getAssignee().getId()))) {
            Notification notification = new Notification();
            notification.setUser(task.getAssignee());
            notification.setMessage("您被分配了新任务「" + task.getTitle() + "」");
            notification.setType("ASSIGNMENT");
            notification.setRelatedTaskId(task.getId());
            notification.setIsRead(false);
            notificationRepository.save(notification);

            Map<String, Object> notifMsg = new HashMap<>();
            notifMsg.put("type", "NEW_NOTIFICATION");
            notifMsg.put("data", notification.getMessage());
            messagingTemplate.convertAndSend("/topic/user/" + task.getAssignee().getId(), notifMsg);
        }

        TaskResponse response = toTaskResponse(task);

        Long projectId = task.getColumn().getProject().getId();
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "TASK_UPDATED");
        wsMsg.put("data", response);
        messagingTemplate.convertAndSend("/topic/board/" + projectId, wsMsg);

        return response;
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "任务不存在"));

        Long projectId = task.getColumn().getProject().getId();

        taskRepository.delete(task);

        Map<String, Object> delMsg = new HashMap<>();
        delMsg.put("type", "TASK_DELETED");
        delMsg.put("taskId", taskId);
        messagingTemplate.convertAndSend("/topic/board/" + projectId, delMsg);
    }

    @Transactional
    public void moveTask(Long taskId, Long targetColumnId, Integer sortOrder) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "任务不存在"));

        Long projectId = task.getColumn().getProject().getId();

        BoardColumn targetColumn = boardColumnRepository.findById(targetColumnId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "目标列不存在"));

        task.setColumn(targetColumn);

        if (sortOrder != null) {
            List<Task> tasksInColumn = taskRepository.findByColumnIdOrderBySortOrderAsc(targetColumnId);
            int idx = 0;
            for (Task t : tasksInColumn) {
                if (!t.getId().equals(taskId)) {
                    if (idx == sortOrder) {
                        idx++;
                    }
                    t.setSortOrder(idx++);
                }
            }
            task.setSortOrder(sortOrder);
        }

        task = taskRepository.save(task);

        TaskResponse response = toTaskResponse(task);

        Map<String, Object> moveMsg = new HashMap<>();
        moveMsg.put("type", "TASK_MOVED");
        moveMsg.put("data", response);
        messagingTemplate.convertAndSend("/topic/board/" + projectId, moveMsg);
    }

    private TaskResponse toTaskResponse(Task task) {
        List<String> tags = new ArrayList<>();
        if (task.getMetadata() != null) {
            try {
                JsonNode metadataNode = objectMapper.readTree(task.getMetadata());
                if (metadataNode.has("tags")) {
                    metadataNode.get("tags").forEach(tag -> tags.add(tag.asText()));
                }
            } catch (Exception e) {
                tags.clear();
            }
        }

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority() != null ? task.getPriority().name() : null,
                task.getDueDate(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getUsername() : null,
                task.getEstimatedHours(),
                task.getSortOrder(),
                task.getColumn().getId(),
                task.getColumn().getName(),
                tags,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}