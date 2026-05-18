package com.dashboard.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnResponse {

    private Long id;

    private String name;

    private int sortOrder;

    private int taskCount;
}