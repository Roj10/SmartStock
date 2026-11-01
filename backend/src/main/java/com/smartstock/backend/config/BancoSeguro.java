package com.smartstock.backend.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Proteções da conexão com o banco no Supabase:
 * 1. Conexão criptografada e com o servidor verificado: se o certificado do Supabase estiver em
 *    backend/supabase-ca.crt, a conexão passa de "require" (só criptografa) para "verify-full" (criptografa e
 *    confere que do outro lado é mesmo o Supabase, impedindo interceptação).
 * 2. Row Level Security ligado em todas as tabelas do schema "smartstock" e nenhum acesso para os papéis públicos
 *    do Supabase (anon/authenticated): mesmo que alguém exponha o schema na API REST do Supabase por engano, os
 *    dados não ficam acessíveis por lá. O backend continua com acesso normal (é o dono das tabelas).
 */
@Configuration
public class BancoSeguro {

    private static final Logger log = LoggerFactory.getLogger(BancoSeguro.class);

    @Bean
    public static BeanPostProcessor conexaoVerificada(@Value("${app.supabase.db-ca:supabase-ca.crt}") String certificado) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nome) {
                if (bean instanceof DataSourceProperties props && props.getUrl() != null
                        && props.getUrl().startsWith("jdbc:postgresql:")) {
                    Path ca = Path.of(certificado).toAbsolutePath();
                    if (Files.isReadable(ca)) {
                        props.setUrl(comVerificacao(props.getUrl(), ca));
                        log.info("Conexão com o banco: TLS com verificação do servidor (verify-full, certificado {})", ca);
                    } else {
                        log.warn("Conexão com o banco: TLS sem verificar o servidor (sslmode=require). Para verificar, "
                                + "baixe o certificado do Supabase (Database Settings > SSL Configuration) e salve como {}", ca);
                    }
                }
                return bean;
            }
        };
    }

    static String comVerificacao(String url, Path ca) {
        String semSsl = url.replaceAll("([?&])(sslmode|sslrootcert)=[^&]*", "$1").replaceAll("&&+", "&")
                .replace("?&", "?").replaceAll("[?&]$", "");
        String sep = semSsl.contains("?") ? "&" : "?";
        return semSsl + sep + "sslmode=verify-full&sslrootcert=" + ca.toString().replace('\\', '/');
    }

    @Bean
    @Order(-1)
    public CommandLineRunner protegerTabelas(DataSource dataSource,
            @Value("${spring.jpa.properties.hibernate.default_schema:smartstock}") String schema) {
        return args -> {
            try (Connection c = dataSource.getConnection()) {
                if (!"PostgreSQL".equals(c.getMetaData().getDatabaseProductName())) {
                    return; // perfil h2 (banco em memória): nada a fazer
                }
                List<String> tabelas = new ArrayList<>();
                try (ResultSet rs = c.getMetaData().getTables(null, schema, "%", new String[] { "TABLE" })) {
                    while (rs.next()) {
                        tabelas.add(rs.getString("TABLE_NAME"));
                    }
                }
                try (Statement st = c.createStatement()) {
                    for (String t : tabelas) {
                        st.execute("alter table " + q(schema) + "." + q(t) + " enable row level security");
                    }
                    for (String papel : List.of("anon", "authenticated")) {
                        if (papelExiste(c, papel)) {
                            st.execute("revoke all on all tables in schema " + q(schema) + " from " + papel);
                            st.execute("revoke all on all sequences in schema " + q(schema) + " from " + papel);
                            st.execute("revoke usage on schema " + q(schema) + " from " + papel);
                        }
                    }
                }
                log.info("Banco: RLS ligado em {} tabela(s) do schema {}; sem acesso para anon/authenticated", tabelas.size(),
                        schema);
            } catch (SQLException e) {
                log.warn("Não foi possível aplicar as proteções do banco (RLS): {}", e.getMessage());
            }
        };
    }

    private static boolean papelExiste(Connection c, String papel) throws SQLException {
        try (var ps = c.prepareStatement("select 1 from pg_roles where rolname = ?")) {
            ps.setString(1, papel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String q(String identificador) {
        return "\"" + identificador.replace("\"", "\"\"") + "\"";
    }
}
