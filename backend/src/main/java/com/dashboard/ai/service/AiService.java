package com.dashboard.ai.service;

import com.dashboard.ai.dto.AiSuggestResponse;
import com.dashboard.common.exception.BusinessException;
import com.dashboard.domain.Task;
import com.dashboard.domain.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final TaskRepository taskRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;

    public AiService(TaskRepository taskRepository,
                     SimpMessagingTemplate messagingTemplate,
                     ObjectMapper objectMapper,
                     @Value("${app.ai.api-key:}") String apiKey) {
        this.taskRepository = taskRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    public AiSuggestResponse suggest(String description) {
        if (description == null || description.trim().length() < 10) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "任务描述太短");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return mockSuggest(description);
        }

        try {
            String prompt = "分析以下任务描述，生成最多5个相关标签和预估工时（小时数）。返回JSON格式：{\"tags\":[\"标签1\",\"标签2\"],\"estimatedHours\":数字}。任务描述：" + description;

            String response = callOpenAi(prompt);

            JsonNode jsonNode = objectMapper.readTree(extractJson(response));

            List<String> tags = new ArrayList<>();
            if (jsonNode.has("tags")) {
                jsonNode.get("tags").forEach(tag -> tags.add(tag.asText()));
            }

            Double estimatedHours = jsonNode.has("estimatedHours")
                    ? jsonNode.get("estimatedHours").asDouble()
                    : null;

            return new AiSuggestResponse(tags, estimatedHours);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 建议暂时不可用，请稍后重试");
        }
    }

    public Object executeCommand(String command, Long projectId) {
        if (command == null || command.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请输入指令");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return mockExecuteCommand(command, projectId);
        }

        try {
            String prompt = "解析以下自然语言指令，转换为看板任务批量操作。返回JSON：{\"action\":\"操作类型\",\"filter\":{\"field\":\"字段\",\"operator\":\"操作符\",\"value\":\"值\"},\"updates\":{\"field\":\"要更新的字段\",\"value\":\"新值\"}}。指令：" + command;

            String response = callOpenAi(prompt);

            JsonNode parsed = objectMapper.readTree(extractJson(response));

            String action = parsed.has("action") ? parsed.get("action").asText() : null;

            if ("DELETE".equalsIgnoreCase(action)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "该操作将删除任务，请确认后再执行。如需继续，请使用更明确的操作指令。");
            }

            List<Task> tasks = findTasksByFilter(parsed, projectId);

            if (tasks.isEmpty()) {
                return "没有找到符合条件的任务";
            }

            int updatedCount = applyUpdates(tasks, parsed);

            if (projectId != null) {
                Map<String, Object> wsMsg = new HashMap<>();
                wsMsg.put("type", "TASKS_UPDATED");
                wsMsg.put("count", updatedCount);
                messagingTemplate.convertAndSend("/topic/board/" + projectId, wsMsg);
            }

            String summary = generateSummary(action, updatedCount, parsed);
            return summary;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法理解该指令，请尝试更明确的描述");
        }
    }

    private String callOpenAi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", entity, String.class);

        String responseBody = responseEntity.getBody();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("choices") && root.get("choices").size() > 0) {
                JsonNode firstChoice = root.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    private List<Task> findTasksByFilter(JsonNode parsed, Long projectId) {
        if (parsed == null || !parsed.has("filter")) {
            if (projectId != null) {
                return taskRepository.findByColumn_ProjectIdOrderBySortOrderAsc(projectId);
            }
            return Collections.emptyList();
        }

        JsonNode filter = parsed.get("filter");
        String field = filter.has("field") ? filter.get("field").asText() : null;
        String value = filter.has("value") ? filter.get("value").asText() : null;

        if (projectId != null) {
            return taskRepository.findByColumn_ProjectIdOrderBySortOrderAsc(projectId);
        }

        return Collections.emptyList();
    }

    private int applyUpdates(List<Task> tasks, JsonNode parsed) {
        if (parsed == null || !parsed.has("updates")) {
            return tasks.size();
        }

        JsonNode updates = parsed.get("updates");
        String field = updates.has("field") ? updates.get("field").asText() : null;
        String value = updates.has("value") ? updates.get("value").asText() : null;

        int count = 0;
        for (Task task : tasks) {
            boolean updated = false;

            if ("priority".equalsIgnoreCase(field) && value != null) {
                try {
                    task.setPriority(com.dashboard.domain.enums.TaskPriority.valueOf(value));
                    updated = true;
                } catch (IllegalArgumentException ignored) {
                }
            } else if ("title".equalsIgnoreCase(field) && value != null) {
                task.setTitle(value);
                updated = true;
            }

            if (updated) {
                taskRepository.save(task);
                count++;
            }
        }

        return count;
    }

    private String generateSummary(String action, int count, JsonNode parsed) {
        if (parsed != null && parsed.has("updates")) {
            JsonNode updates = parsed.get("updates");
            String field = updates.has("field") ? updates.get("field").asText() : "";
            String value = updates.has("value") ? updates.get("value").asText() : "";
            return "已将 " + count + " 个任务标记为" + value;
        }
        return "已处理 " + count + " 个任务";
    }

    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }
        response = response.trim();
        int jsonStart = response.indexOf('{');
        int jsonEnd = response.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        return response;
    }

    private AiSuggestResponse mockSuggest(String description) {
        List<String> tags = new ArrayList<>();
        String lower = description.toLowerCase();

        if (lower.contains("bug") || lower.contains("修复") || lower.contains("问题")) {
            tags.add("Bug修复");
            return new AiSuggestResponse(tags, 2.0);
        }
        if (lower.contains("功能") || lower.contains("开发") || lower.contains("feature")) {
            tags.add("功能开发");
            return new AiSuggestResponse(tags, 8.0);
        }
        if (lower.contains("文档") || lower.contains("doc")) {
            tags.add("文档");
            return new AiSuggestResponse(tags, 1.0);
        }
        if (lower.contains("设计") || lower.contains("ui") || lower.contains("界面")) {
            tags.add("UI设计");
            return new AiSuggestResponse(tags, 4.0);
        }
        if (lower.contains("测试") || lower.contains("test")) {
            tags.add("测试");
            return new AiSuggestResponse(tags, 3.0);
        }

        tags.add("待分类");
        return new AiSuggestResponse(tags, 2.0);
    }

    private Object mockExecuteCommand(String command, Long projectId) {
        return "AI 服务未配置，请在 application.yml 中配置 app.ai.api-key 后重试。";
    }
}
