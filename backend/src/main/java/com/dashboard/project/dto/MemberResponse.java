package com.dashboard.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private Long userId;
    private String username;
    private String email;
    private String role;
}