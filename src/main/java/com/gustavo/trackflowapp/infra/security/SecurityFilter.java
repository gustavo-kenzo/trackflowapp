package com.gustavo.trackflowapp.infra.security;

import com.gustavo.trackflowapp.infra.token.TokenService;
import com.gustavo.trackflowapp.modules.user.UserRepository;
import com.gustavo.trackflowapp.shared.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = Optional
                .ofNullable(request.getHeader("Authorization"))
                .map(headerAuth -> headerAuth.replace("Bearer ", ""))
                .orElse(null);

        if (token != null) {
            var userId = tokenService.verifyToken(token);
            var user = userRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("User not found"));
            var authenticate = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticate);
        }

        filterChain.doFilter(request, response);
    }
}
