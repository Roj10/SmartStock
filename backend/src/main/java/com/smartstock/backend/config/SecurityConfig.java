package com.smartstock.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.smartstock.backend.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * "Portaria" do sistema: decide quem pode entrar em cada endereço da API.
 *
 * <p>Como funciona, em linguagem simples:
 * <ul>
 *   <li>Toda chamada ao backend passa por aqui antes de chegar ao código que faz o trabalho.</li>
 *   <li>O login ({@code /api/auth/**}) é aberto: é onde a pessoa prova quem é (senha + código do app).</li>
 *   <li>Todo o resto exige o "crachá" digital (token JWT) que o login entrega. Sem ele a resposta é 401.</li>
 *   <li>Com o crachá, cada aba ainda confere a permissão da conta (anotações {@code @PreAuthorize}
 *       nos controllers). Sem permissão a resposta é 403.</li>
 *   <li>Também são enviados cabeçalhos que pedem ao navegador para não abrir o sistema dentro de
 *       outros sites e não vazar endereços (proteções contra golpes comuns na web).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /** Endereços (origens) que podem chamar a API pelo navegador. Padrão: o frontend em desenvolvimento. */
    @Value("${app.cors.origens}")
    private List<String> origensPermitidas;

    @Value("${spring.h2.console.enabled:false}")
    private boolean consoleH2;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Regras só para a documentação da API (Swagger): a página {@code /swagger-ui.html} e o arquivo
     * {@code /openapi.yaml}. Ficam abertas para leitura (não mostram dados do sistema, só como usar a API);
     * para testar as rotas pela página continua sendo preciso fazer login e colar o token em "Authorize".
     * {@code @Order(1)}: é avaliada antes da regra geral abaixo.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain documentacaoApi(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs", "/openapi.yaml")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> {
                    headers.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER));
                    headers.frameOptions(f -> f.deny());
                    // a página do Swagger precisa carregar os próprios scripts, estilos e imagens
                    headers.contentSecurityPolicy(c -> c.policyDirectives(
                            "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                    + "script-src 'self'; frame-ancestors 'none'"));
                });
        return http.build();
    }

    /** Regra geral de todas as outras rotas (a API do sistema). */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sem cookies: o login vai no cabeçalho Authorization
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**").permitAll();
                    auth.requestMatchers("/arquivos/pastilhas/**").permitAll();
                    // a página de erro precisa ser acessível; sem isso todo erro inesperado virava "403"
                    auth.requestMatchers("/error").permitAll();
                    if (consoleH2) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> erroJson(res, HttpServletResponse.SC_UNAUTHORIZED,
                                "Sessão expirada ou inválida. Entre de novo."))
                        .accessDeniedHandler((req, res, e) -> erroJson(res, HttpServletResponse.SC_FORBIDDEN,
                                "Você não tem permissão para esta ação.")))
                .headers(headers -> {
                    headers.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER));
                    headers.permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()"));
                    // o console do H2 (só no perfil h2) abre em frames da própria origem
                    if (consoleH2) {
                        headers.frameOptions(f -> f.sameOrigin());
                    } else {
                        headers.frameOptions(f -> f.deny());
                        headers.contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
                    }
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void erroJson(HttpServletResponse res, int status, String mensagem) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"message\":\"" + mensagem + "\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origensPermitidas);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
