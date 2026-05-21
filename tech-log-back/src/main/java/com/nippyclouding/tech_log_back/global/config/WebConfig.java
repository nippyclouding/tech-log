package com.nippyclouding.tech_log_back.global.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ImageStorageProperties imageStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Path.of(imageStorageProperties.uploadDir()).toAbsolutePath().normalize();
        registry.addResourceHandler(imageStorageProperties.publicPath() + "/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
