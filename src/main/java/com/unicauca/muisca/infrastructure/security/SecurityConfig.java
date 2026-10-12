package com.unicauca.muisca.infrastructure.security;

import com.unicauca.muisca.domain.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Permisos por rol:
 * <ul>
 * <li>ADMIN: todo, incluida la gestión de usuarios y la publicación de transformaciones.</li>
 * <li>MODEL_ENGINEER: metamodelos, módulos ATL/Acceleo, espacio de trabajo y definición de
 * transformaciones (sin publicarlas).</li>
 * <li>USER: consulta y ejecuta las transformaciones publicadas y gestiona sus ejecuciones, sus
 * modelos y su propia cuenta.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String MODEL_ENGINEER = "MODEL_ENGINEER";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil, UserRepository userRepository)
            throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeJson(response, 401, "Unauthorized",
                                "Falta el token de autenticación o es inválido/expirado. Inicie sesión en "
                                        + "/auth/login y envíe 'Authorization: Bearer <token>'."))
                        .accessDeniedHandler((request, response, e) -> writeJson(response, 403, "Forbidden",
                                "Su rol no tiene permiso para acceder a este recurso.")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/oauth/**", "/error", "/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings/public").permitAll()
                        .requestMatchers("/api/users/**", "/api/settings/**").hasRole(ADMIN)
                        .requestMatchers("/api/account/**", "/api/models/**").authenticated()
                        .requestMatchers("/api/metamodels/**", "/api/atl-modules/**", "/api/acceleo-modules/**",
                                "/api/workspace/**").hasAnyRole(MODEL_ENGINEER, ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/transformations/*/publication").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/transformations/*/executions").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/transformations/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/transformations").hasAnyRole(MODEL_ENGINEER, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/transformations/*").hasAnyRole(MODEL_ENGINEER, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/transformations/*")
                        .hasAnyRole(MODEL_ENGINEER, ADMIN)
                        .requestMatchers("/api/executions/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Orígenes del frontend (propiedad app.cors.allowed-origins / variable CORS_ALLOWED_ORIGINS). */
    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static void writeJson(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                Instant.now(), status, error, message.replace("\"", "\\\"")));
    }
}
