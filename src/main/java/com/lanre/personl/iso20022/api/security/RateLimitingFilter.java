package com.lanre.personl.iso20022.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final ApiHardeningProperties properties;
    private final InMemoryRateLimiter rateLimiter;

    public RateLimitingFilter(ApiHardeningProperties properties, InMemoryRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.isEnabled()
                || !properties.getRateLimit().isEnabled()
                || !request.getRequestURI().startsWith("/api/v1/")
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        ApiHardeningProperties.RateLimit config = properties.getRateLimit();
        String clientKey = resolveClientKey(request, config.isTrustForwardedFor());

        if (!rateLimiter.allow(clientKey, config.getRequestsPerWindow(), config.getWindowSeconds())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(config.getWindowSeconds()));
            response.getWriter().write("Rate limit exceeded. Retry later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request, boolean trustForwardedFor) {
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
