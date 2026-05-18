package com.dashboard.project.service;

import com.dashboard.common.dto.PageResponse;
import com.dashboard.common.exception.BusinessException;
import com.dashboard.config.AppConfig;
import com.dashboard.domain.BoardColumn;
import com.dashboard.domain.BoardColumnRepository;
import com.dashboard.domain.Invitation;
import com.dashboard.domain.InvitationRepository;
import com.dashboard.domain.Project;
import com.dashboard.domain.ProjectMember;
import com.dashboard.domain.ProjectMemberRepository;
import com.dashboard.domain.ProjectRepository;
import com.dashboard.domain.User;
import com.dashboard.domain.UserRepository;
import com.dashboard.domain.enums.InvitationStatus;
import com.dashboard.domain.enums.MemberRole;
import com.dashboard.project.dto.InviteRequest;
import com.dashboard.project.dto.MemberResponse;
import com.dashboard.project.dto.ProjectRequest;
import com.dashboard.project.dto.ProjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final JavaMailSender mailSender;
    private final AppConfig appConfig;

    @Transactional
    public ProjectResponse createProject(Long userId, ProjectRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        Project project = new Project();
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project = projectRepository.save(project);

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(MemberRole.管理员);
        projectMemberRepository.save(member);

        createDefaultColumn(project, "待办", 1);
        createDefaultColumn(project, "进行中", 2);
        createDefaultColumn(project, "已完成", 3);

        return toProjectResponse(project, 1);
    }

    public PageResponse<ProjectResponse> getUserProjects(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<Project> projectPage = projectRepository.findByMemberUserId(userId, pageRequest);

        List<ProjectResponse> responses = projectPage.getContent().stream()
                .map(project -> {
                    int memberCount = projectMemberRepository.findByProjectId(project.getId()).size();
                    return toProjectResponse(project, memberCount);
                })
                .collect(Collectors.toList());

        return new PageResponse<>(
                responses,
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.getTotalElements(),
                projectPage.getTotalPages()
        );
    }

    public ProjectResponse getProjectDetail(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "项目不存在"));

        int memberCount = projectMemberRepository.findByProjectId(projectId).size();
        return toProjectResponse(project, memberCount);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, Long userId, ProjectRequest req) {
        requireAdmin(projectId, userId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "项目不存在"));

        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project = projectRepository.save(project);

        int memberCount = projectMemberRepository.findByProjectId(projectId).size();
        return toProjectResponse(project, memberCount);
    }

    @Transactional
    public String inviteMember(Long projectId, Long userId, InviteRequest req) {
        requireAdmin(projectId, userId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "项目不存在"));

        userRepository.findByEmail(req.getEmail()).ifPresent(existingUser -> {
            projectMemberRepository.findByProjectIdAndUserId(projectId, existingUser.getId())
                    .ifPresent(member -> {
                        throw new BusinessException(HttpStatus.CONFLICT, "该用户已是项目成员");
                    });
        });

        Invitation invitation = new Invitation();
        invitation.setProject(project);
        invitation.setEmail(req.getEmail());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(appConfig.getInvitation().getExpirationHours()));
        invitationRepository.save(invitation);

        sendInvitationEmail(req.getEmail(), project.getName(), invitation.getToken());

        return "邀请已发送";
    }

    @Transactional
    public Long acceptInvitation(String token, Long userId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "无效的邀请链接"));

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邀请链接已过期");
        }

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "您已加入该项目");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        projectMemberRepository.findByProjectIdAndUserId(invitation.getProject().getId(), userId)
                .ifPresent(member -> {
                    throw new BusinessException(HttpStatus.CONFLICT, "您已是该项目成员");
                });

        ProjectMember member = new ProjectMember();
        member.setProject(invitation.getProject());
        member.setUser(user);
        member.setRole(MemberRole.普通成员);
        projectMemberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return invitation.getProject().getId();
    }

    @Transactional
    public MemberResponse changeMemberRole(Long projectId, Long userId, Long targetUserId, String role) {
        requireAdmin(projectId, userId);

        MemberRole newRole;
        try {
            newRole = MemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无效的角色");
        }

        if (userId.equals(targetUserId) && newRole != MemberRole.管理员) {
            long adminCount = projectMemberRepository.countByProjectIdAndRole(projectId, MemberRole.管理员);
            if (adminCount <= 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "项目中至少需要保留一名管理员");
            }
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "成员不存在"));

        member.setRole(newRole);
        projectMemberRepository.save(member);

        return new MemberResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail(),
                member.getRole().name()
        );
    }

    @Transactional
    public String removeMember(Long projectId, Long userId, Long targetUserId) {
        requireAdmin(projectId, userId);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "成员不存在"));

        if (member.getRole() == MemberRole.管理员) {
            long adminCount = projectMemberRepository.countByProjectIdAndRole(projectId, MemberRole.管理员);
            if (adminCount <= 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "项目中至少需要保留一名管理员");
            }
        }

        projectMemberRepository.delete(member);

        return "成员已移除";
    }

    public List<MemberResponse> getProjectMembers(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "项目不存在"));

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        return members.stream()
                .map(member -> new MemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getEmail(),
                        member.getRole().name()
                ))
                .collect(Collectors.toList());
    }

    private void requireAdmin(Long projectId, Long userId) {
        if (!isAdmin(projectId, userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有管理员可以执行此操作");
        }
    }

    private boolean isAdmin(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(member -> member.getRole() == MemberRole.管理员)
                .orElse(false);
    }

    private ProjectResponse toProjectResponse(Project project, int memberCount) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                memberCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private void createDefaultColumn(Project project, String name, int sortOrder) {
        BoardColumn column = new BoardColumn();
        column.setProject(project);
        column.setName(name);
        column.setSortOrder(sortOrder);
        boardColumnRepository.save(column);
    }

    private void sendInvitationEmail(String email, String projectName, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("邀请加入项目：" + projectName);
        message.setText("您被邀请加入项目「" + projectName + "」。\n\n"
                + "请点击以下链接接受邀请：\n"
                + appConfig.getCors().getAllowedOrigins() + "/invitations/" + token + "\n\n"
                + "该邀请链接将在" + appConfig.getInvitation().getExpirationHours() + "小时后过期。");
        mailSender.send(message);
    }
}