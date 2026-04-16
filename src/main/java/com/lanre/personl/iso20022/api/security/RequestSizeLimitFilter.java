package com.lanre.personl.iso20022.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final ApiHardeningProperties properties;

    public RequestSizeLimitFilter(ApiHardeningProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.isEnabled()
                || !request.getRequestURI().startsWith("/api/v1/")
                || HttpMethod.GET.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        long maxBytes = resolveLimit(request.getContentType());
        if (maxBytes <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        long declaredContentLength = request.getContentLengthLong();
        if (declaredContentLength > maxBytes) {
            reject(response, maxBytes);
            return;
        }

        try {
            filterChain.doFilter(new LimitedRequestWrapper(request, maxBytes), response);
        } catch (PayloadTooLargeException ex) {
            reject(response, maxBytes);
        }
    }

    private long resolveLimit(String contentType) {
        if (contentType == null) {
            return properties.getRequestLimits().getTextBytes();
        }

        String normalized = contentType.toLowerCase();
        if (normalized.contains(MediaType.APPLICATION_XML_VALUE) || normalized.contains(MediaType.TEXT_XML_VALUE)) {
            return properties.getRequestLimits().getXmlBytes();
        }
        if (normalized.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return properties.getRequestLimits().getJsonBytes();
        }
        return properties.getRequestLimits().getTextBytes();
    }

    private void reject(HttpServletResponse response, long maxBytes) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write("Request body exceeds configured limit of " + maxBytes + " bytes.");
    }

    private static final class LimitedRequestWrapper extends HttpServletRequestWrapper {
        private final long maxBytes;

        private LimitedRequestWrapper(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CountingServletInputStream(super.getInputStream(), maxBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static final class CountingServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long maxBytes;
        private long bytesRead;

        private CountingServletInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                increment(1);
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = delegate.read(b, off, len);
            if (count > 0) {
                increment(count);
            }
            return count;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        private void increment(int count) throws PayloadTooLargeException {
            bytesRead += count;
            if (bytesRead > maxBytes) {
                throw new PayloadTooLargeException();
            }
        }
    }

    private static final class PayloadTooLargeException extends IOException {
    }
}
