package com.smartstock.backend.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.smartstock.backend.storage.Armazenamento;
import com.smartstock.backend.storage.LocalStorage;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<LocalStorage> localStorage;

    public WebConfig(ObjectProvider<LocalStorage> localStorage) {
        this.localStorage = localStorage;
    }

    /**
     * Só no perfil "h2" (armazenamento local): o backend serve as fotos dos itens em /arquivos/pastilhas/...
     * No uso normal as fotos vêm direto do Supabase Storage e não passam pelo backend.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        LocalStorage local = localStorage.getIfAvailable();
        if (local != null) {
            registry.addResourceHandler("/arquivos/" + Armazenamento.PASTILHAS + "/**")
                    .addResourceLocations(local.raiz().resolve(Armazenamento.PASTILHAS).toUri().toString());
        }
    }
}
