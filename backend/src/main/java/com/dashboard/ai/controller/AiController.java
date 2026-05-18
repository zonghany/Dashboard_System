package com.dashboard.ai.controller;

import com.dashboard.ai.dto.AiCommandRequest;
import com.dashboard.ai.dto.AiSuggestRequest;
import com.dashboard.ai.dto.AiSuggestResponse;
import com.dashboard.ai.service.AiService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/suggest")
    public AiSuggestResponse suggest(@Valid @RequestBody AiSuggestRequest request) {
        return aiService.suggest(request.getDescription());
    }

    @PostMapping("/command")
    public Object executeCommand(@Valid @RequestBody AiCommandRequest request) {
        return aiService.executeCommand(request.getCommand(), request.getProjectId());
    }
}