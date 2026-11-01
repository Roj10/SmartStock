package com.smartstock.backend.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

/**
 * <b>Em palavras simples:</b> Ajuste de como os objetos do sistema viram texto JSON nas respostas da API.
 *
 * "Jackson" é a biblioteca que transforma objetos Java em JSON (o formato que o navegador entende).
 * Este ajuste ensina o Jackson a lidar com dados que o banco carrega "sob demanda" (relacionamentos
 * preguiçosos do Hibernate), evitando erros ao montar a resposta.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer hibernate6ModuleCustomizer() {
        Hibernate6Module module = new Hibernate6Module();
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return builder -> builder.modulesToInstall(modules -> modules.add(module));
    }
}
