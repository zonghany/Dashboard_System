package com.dashboard.ai.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiCommandRequest {

    @NotBlank
    private String command;

    private Long projectId;
}