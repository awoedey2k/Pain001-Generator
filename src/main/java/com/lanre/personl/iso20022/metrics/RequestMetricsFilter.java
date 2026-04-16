package com.lanre.personl.iso20022.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

@Component
public class RequestMetricsFilter extends OncePerRequestFilter {

    private final Iso20022MetricsService metricsService;

    public RequestMetricsFilter(Iso20022MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            metricsService.incrementRequest(
                    request.getMethod(),
                    resolvePath(request),
                    response.getStatus()
            );
        }
    }

    private String resolvePath(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchingPattern instanceof String pattern && !pattern.isBlank()) {
            return pattern;
        }
        String path = request.getRequestURI();
        return (path == null || path.isBlank()) ? "UNKNOWN" : path;
    }
}
