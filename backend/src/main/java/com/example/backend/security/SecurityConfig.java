package com.example.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
            AppSecurityProperties securityProperties,
            AdminAuthorityFixFilter adminAuthorityFixFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .accessDeniedHandler((request, response, ex) ->
                                writeJsonError403(request, response, ex, objectMapper)))
                .addFilterBefore(adminAuthorityFixFilter, AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(securityProperties)))
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期", objectMapper)))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppSecurityProperties properties, PasswordEncoder passwordEncoder,
            com.example.backend.repository.CustomerUserRepository customerUserRepository) {
        return new com.example.backend.security.CompositeUserDetailsService(
                properties, passwordEncoder, customerUserRepository);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public JwtEncoder jwtEncoder(AppSecurityProperties properties) {
        SecretKey secretKey = jwtSecretKey(properties);
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(AppSecurityProperties properties) {
        SecretKey secretKey = jwtSecretKey(properties);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter(AppSecurityProperties securityProperties) {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        String adminUsername = securityProperties.getAdminUsername() != null
                ? securityProperties.getAdminUsername().trim() : "";

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String subject = jwt.getSubject();
            boolean isAdminBySubject = subject != null && !adminUsername.isEmpty()
                    && subject.trim().equalsIgnoreCase(adminUsername);
            boolean isAdminByRoles = false;
            try {
                Object rolesClaim = jwt.getClaim("roles");
                if (rolesClaim instanceof Collection<?> c) {
                    isAdminByRoles = c.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(String.valueOf(r)));
                } else if (rolesClaim instanceof String s) {
                    isAdminByRoles = "ADMIN".equalsIgnoreCase(s.trim());
                }
            } catch (Exception ignored) { }
            if (isAdminBySubject || isAdminByRoles) {
                return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            return new ArrayList<>(grantedAuthoritiesConverter.convert(jwt));
        });
        return jwtAuthenticationConverter;
    }

    private SecretKey jwtSecretKey(AppSecurityProperties properties) {
        return new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private void writeJsonError(HttpServletResponse response, int status, String message, ObjectMapper objectMapper) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        try {
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (IOException ignored) {
            // Ignore write failures for error response body.
        }
    }

    /**
     * 403 由此处统一返回。拦截链：AuthorizationFilter 根据 authorizeHttpRequests 判断无 ROLE_ADMIN 后
     * 抛出 AccessDeniedException，由 ExceptionTranslationFilter 调用本 handler。
     */
    private void writeJsonError403(jakarta.servlet.http.HttpServletRequest request,
            HttpServletResponse response, Exception ex, ObjectMapper objectMapper) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Denied-By", "AuthorizationFilter");
        response.setHeader("X-Required-Role", "ROLE_ADMIN");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "无权限访问该资源");
        body.put("detail", "由 AuthorizationFilter 拦截：/api/admin/** 需要 ROLE_ADMIN");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (IOException ignored) {
        }
    }
}
