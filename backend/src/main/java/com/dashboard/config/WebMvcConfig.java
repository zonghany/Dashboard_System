package com.dashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppConfig appConfig;

    public WebMvcConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (appConfig.getCors() != null && appConfig.getCors().getAllowedOrigins() != null) {
            registry.addMapping("/api/**")
                .allowedOrigins(appConfig.getCors().getAllowedOrigins().split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = appConfig.getUploadDir();
        if (uploadDir != null && !uploadDir.isEmpty()) {
            registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
        }
    }
}