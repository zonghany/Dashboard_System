package com.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfig {
    private InvitationConfig invitation;
    private String uploadDir;
    private NotificationConfig notification;
    private CorsConfigProps cors;

    @Getter
    @Setter
    public static class InvitationConfig {
        private int expirationHours;
    }

    @Getter
    @Setter
    public static class NotificationConfig {
        private long scanInterval;
        private int reminderHours;
    }

    @Getter
    @Setter
    public static class CorsConfigProps {
        private String allowedOrigins;
    }
}