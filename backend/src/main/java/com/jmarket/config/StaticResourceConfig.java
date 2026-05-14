package com.jmarket.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = resolveUploadRoot().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    private Path resolveUploadRoot() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if (workingDirectory.getFileName() != null && "backend".equals(workingDirectory.getFileName().toString())) {
            return workingDirectory.resolve("uploads").normalize();
        }
        return workingDirectory.resolve(Path.of("backend", "uploads")).normalize();
    }
}
