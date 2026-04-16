package com.lanre.personl.iso20022.logging;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LoggingContext {

    public static final String REQUEST_ID = "requestId";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String REQUEST_PATH = "requestPath";
    public static final String MSG_ID = "msgId";
    public static final String END_TO_END_ID = "endToEndId";

    private LoggingContext() {
    }

    public static void putRequest(String requestId, String method, String path) {
        put(REQUEST_ID, requestId);
        put(HTTP_METHOD, method);
        put(REQUEST_PATH, path);
    }

    public static void putMsgId(String msgId) {
        put(MSG_ID, msgId);
    }

    public static void putEndToEndId(String endToEndId) {
        put(END_TO_END_ID, endToEndId);
    }

    public static void putIdentifiers(String msgId, String endToEndId) {
        putMsgId(msgId);
        putEndToEndId(endToEndId);
    }

    public static Scope withIdentifiers(String msgId, String endToEndId) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(MSG_ID, sanitize(msgId));
        values.put(END_TO_END_ID, sanitize(endToEndId));
        return withValues(values);
    }

    public static Scope withMsgId(String msgId) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(MSG_ID, sanitize(msgId));
        return withValues(values);
    }

    public static Scope withEndToEndId(String endToEndId) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(END_TO_END_ID, sanitize(endToEndId));
        return withValues(values);
    }

    public static void clearRequestFields() {
        MDC.remove(REQUEST_ID);
        MDC.remove(HTTP_METHOD);
        MDC.remove(REQUEST_PATH);
    }

    private static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value);
    }

    private static Scope withValues(Map<String, String> values) {
        Map<String, String> previous = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            previous.put(key, MDC.get(key));
            put(key, value);
        });
        return () -> previous.forEach(LoggingContext::put);
    }

    private static String sanitize(String value) {
        return value == null ? null : value.trim();
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
