package com.voltra.platform.security;

import com.voltra.platform.model.User;
import com.voltra.platform.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService,
                         TokenBlacklistService blacklistService,
                         UserRepository userRepository) {
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty() && !blacklistService.isRevoked(token)) {
                try {
                    Claims claims = jwtService.parseClaims(token);
                    String email = claims.getSubject();
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
                        if (user != null) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
