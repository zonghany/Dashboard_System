package com.dashboard.auth.service;

import com.dashboard.auth.dto.AuthResponse;
import com.dashboard.auth.dto.LoginRequest;
import com.dashboard.auth.dto.RegisterRequest;
import com.dashboard.auth.util.JwtUtil;
import com.dashboard.common.exception.BusinessException;
import com.dashboard.config.AppConfig;
import com.dashboard.domain.User;
import com.dashboard.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final AppConfig appConfig;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT, "该邮箱已被注册");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        sendVerificationEmail(user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getUsername());
    }

    public String refreshToken(Long userId, String email) {
        return jwtUtil.generateToken(userId, email);
    }

    private void sendVerificationEmail(String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("欢迎注册 Dashboard Kanban 系统");
            message.setText("感谢您注册 Dashboard Kanban 系统。您的账号已成功创建，请使用您的邮箱和密码登录。");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("邮件发送失败，已跳过: " + e.getMessage());
        }
    }
}