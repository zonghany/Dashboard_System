package com.dashboard.board.service;

import com.dashboard.board.dto.ColumnRequest;
import com.dashboard.board.dto.ColumnResponse;
import com.dashboard.common.exception.BusinessException;
import com.dashboard.domain.BoardColumn;
import com.dashboard.domain.BoardColumnRepository;
import com.dashboard.domain.Task;
import com.dashboard.domain.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<ColumnResponse> getColumns(Long projectId) {
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        return columns.stream()
                .map(column -> {
                    int taskCount = taskRepository.findByColumnIdOrderBySortOrderAsc(column.getId()).size();
                    return new ColumnResponse(column.getId(), column.getName(), column.getSortOrder(), taskCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ColumnResponse createColumn(Long projectId, ColumnRequest req) {
        List<BoardColumn> existing = boardColumnRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        boolean duplicate = existing.stream().anyMatch(c -> c.getName().equals(req.getName()));
        if (duplicate) {
            throw new BusinessException(HttpStatus.CONFLICT, "该列名已存在");
        }

        BoardColumn column = new BoardColumn();
        column.setName(req.getName());

        if (req.getSortOrder() != null) {
            column.setSortOrder(req.getSortOrder());
        } else {
            int maxSort = existing.stream().mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0).max().orElse(-1);
            column.setSortOrder(maxSort + 1);
        }

        column.setProject(existing.isEmpty() ? null : existing.get(0).getProject());
        if (column.getProject() == null) {
            com.dashboard.domain.Project project = new com.dashboard.domain.Project();
            project.setId(projectId);
            column.setProject(project);
        }

        column = boardColumnRepository.save(column);

        ColumnResponse response = new ColumnResponse(column.getId(), column.getName(), column.getSortOrder(), 0);
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "COLUMN_CREATED");
        wsMsg.put("data", response);
        messagingTemplate.convertAndSend("/topic/board/" + projectId, wsMsg);

        return response;
    }

    @Transactional
    public ColumnResponse renameColumn(Long columnId, ColumnRequest req) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "看板列不存在"));

        column.setName(req.getName());
        column = boardColumnRepository.save(column);

        int taskCount = taskRepository.findByColumnIdOrderBySortOrderAsc(column.getId()).size();
        ColumnResponse response = new ColumnResponse(column.getId(), column.getName(), column.getSortOrder(), taskCount);

        Long projectId = column.getProject().getId();
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "COLUMN_UPDATED");
        wsMsg.put("data", response);
        messagingTemplate.convertAndSend("/topic/board/" + projectId, wsMsg);

        return response;
    }

    @Transactional
    public void reorderColumns(Long projectId, List<ColumnRequest> columns) {
        List<BoardColumn> existing = boardColumnRepository.findByProjectIdOrderBySortOrderAsc(projectId);

        for (int i = 0; i < columns.size(); i++) {
            ColumnRequest req = columns.get(i);
            BoardColumn target = existing.stream()
                    .filter(c -> c.getName().equals(req.getName()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "列不存在: " + req.getName()));
            target.setSortOrder(i);
            boardColumnRepository.save(target);
        }

        Map<String, Object> reorderMsg = new HashMap<>();
        reorderMsg.put("type", "COLUMNS_REORDERED");
        messagingTemplate.convertAndSend("/topic/board/" + projectId, reorderMsg);
    }

    @Transactional
    public void deleteColumn(Long projectId, Long columnId) {
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        if (columns.size() <= 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "项目中至少需要保留一个看板列");
        }

        BoardColumn columnToDelete = columns.stream()
                .filter(c -> c.getId().equals(columnId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "看板列不存在"));

        BoardColumn targetColumn = columns.stream()
                .filter(c -> !c.getId().equals(columnId) && "待办".equals(c.getName()))
                .findFirst()
                .orElse(columns.stream()
                        .filter(c -> !c.getId().equals(columnId))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "没有可用的目标列")));

        List<Task> tasksToMove = taskRepository.findByColumnIdOrderBySortOrderAsc(columnId);
        int maxSort = taskRepository.findByColumnIdOrderBySortOrderAsc(targetColumn.getId()).stream()
                .mapToInt(t -> t.getSortOrder() != null ? t.getSortOrder() : 0)
                .max().orElse(0);

        for (int i = 0; i < tasksToMove.size(); i++) {
            Task task = tasksToMove.get(i);
            task.setColumn(targetColumn);
            task.setSortOrder(maxSort + 1 + i);
            taskRepository.save(task);
        }

        boardColumnRepository.delete(columnToDelete);

        Map<String, Object> delMsg = new HashMap<>();
        delMsg.put("type", "COLUMN_DELETED");
        delMsg.put("columnId", columnId);
        messagingTemplate.convertAndSend("/topic/board/" + projectId, delMsg);
    }
}