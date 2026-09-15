package com.smartstock.backend.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer hibernate6ModuleCustomizer() {
        Hibernate6Module module = new Hibernate6Module();
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return builder -> builder.modulesToInstall(modules -> modules.add(module));
    }
}
