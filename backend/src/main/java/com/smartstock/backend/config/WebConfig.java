package com.smartstock.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String local = uploadsDir.endsWith("/") ? uploadsDir : uploadsDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + local);
    }
}
