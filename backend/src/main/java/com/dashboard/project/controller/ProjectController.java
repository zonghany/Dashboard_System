package com.dashboard.project.controller;

import com.dashboard.auth.util.AuthContext;
import com.dashboard.common.dto.PageResponse;
import com.dashboard.project.dto.InviteRequest;
import com.dashboard.project.dto.MemberResponse;
import com.dashboard.project.dto.ProjectRequest;
import com.dashboard.project.dto.ProjectResponse;
import com.dashboard.project.service.ProjectService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        Long userId = AuthContext.getCurrentUserId();
        ProjectResponse response = projectService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> getUserProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthContext.getCurrentUserId();
        PageResponse<ProjectResponse> response = projectService.getUserProjects(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectDetail(@PathVariable Long id) {
        ProjectResponse response = projectService.getProjectDetail(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        Long userId = AuthContext.getCurrentUserId();
        ProjectResponse response = projectService.updateProject(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<String> inviteMember(
            @PathVariable Long id,
            @Valid @RequestBody InviteRequest request) {
        Long userId = AuthContext.getCurrentUserId();
        String message = projectService.inviteMember(id, userId, request);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> getProjectMembers(@PathVariable Long id) {
        List<MemberResponse> members = projectService.getProjectMembers(id);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<MemberResponse> changeMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestBody Map<String, String> body) {
        Long userId = AuthContext.getCurrentUserId();
        String role = body.get("role");
        MemberResponse response = projectService.changeMemberRole(id, userId, memberId, role);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<String> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId) {
        Long userId = AuthContext.getCurrentUserId();
        String message = projectService.removeMember(id, userId, memberId);
        return ResponseEntity.ok(message);
    }
}