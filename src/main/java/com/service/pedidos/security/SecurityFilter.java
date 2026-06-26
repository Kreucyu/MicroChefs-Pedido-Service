package com.service.pedidos.security;

import com.service.pedidos.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        System.out.println("Token: " + token);
        if (token != null) {
            var subject = tokenService.validateToken(token);
            if (subject != null && !subject.isEmpty()) {
                String role = tokenService.getRole(token);
                Long clienteId = tokenService.getClienteId(token);

                var authority = new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                UserPrincipal principal = new UserPrincipal(subject, role, clienteId);

                var authentication = new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Token inválido ou expirado\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var header = request.getHeader("Authorization");
        if (header == null) return null;
        return header.replace("Bearer ", "").replace("\"", "");
    }
}
