package com.dashboard.project.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectRequest {
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称不能超过100个字符")
    private String name;

    private String description;
}