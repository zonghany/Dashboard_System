package com.dashboard.ai.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiSuggestRequest {

    @NotBlank
    private String description;
}