package me.choir_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttemptService;

    public LoginRateLimitFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isLoginRequest(request) && loginAttemptService.isBlocked(clientIp(request))) {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Too many failed login attempts. Please try again later.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && "/api/login".equals(request.getRequestURI());
    }

    static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
