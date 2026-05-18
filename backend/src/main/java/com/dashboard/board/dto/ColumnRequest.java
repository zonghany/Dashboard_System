package com.dashboard.board.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ColumnRequest {

    @NotBlank
    private String name;

    private Integer sortOrder;
}