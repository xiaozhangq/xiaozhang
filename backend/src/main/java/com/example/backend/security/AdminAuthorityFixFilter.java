package com.example.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 在 JWT 认证之后运行：若当前请求为 /api/admin/** 且 JWT 的 subject 为配置的 admin 用户名，
 * 则强制将权限设为 ROLE_ADMIN，避免因 JWT 中 roles 解析问题导致 403。
 */
@Component
public class AdminAuthorityFixFilter extends OncePerRequestFilter {

    private final AppSecurityProperties securityProperties;

    public AdminAuthorityFixFilter(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/admin/") && !path.contains("/api/admin/auth/login")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String subject = jwt != null ? jwt.getSubject() : null;
                String adminUsername = securityProperties.getAdminUsername();
                if (subject != null && adminUsername != null
                        && subject.trim().equalsIgnoreCase(adminUsername.trim())) {
                    JwtAuthenticationToken withAdmin = new JwtAuthenticationToken(
                            jwt,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                            jwtAuth.getName());
                    SecurityContextHolder.getContext().setAuthentication(withAdmin);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
