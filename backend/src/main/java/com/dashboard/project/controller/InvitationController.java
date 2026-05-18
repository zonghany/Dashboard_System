package com.dashboard.project.controller;

import com.dashboard.auth.util.AuthContext;
import com.dashboard.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final ProjectService projectService;

    @GetMapping("/{token}")
    public ResponseEntity<Void> acceptInvitation(@PathVariable String token) {
        Long userId = AuthContext.getCurrentUserId();
        Long projectId = projectService.acceptInvitation(token, userId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/projects/" + projectId))
                .build();
    }
}