package com.dashboard.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiSuggestResponse {

    private List<String> tags;

    private Double estimatedHours;
}