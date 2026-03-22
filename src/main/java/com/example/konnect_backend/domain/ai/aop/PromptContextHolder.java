package com.example.konnect_backend.domain.ai.aop;

public class PromptContextHolder {

    private static final ThreadLocal<PromptContext> CTX = new ThreadLocal<>();

    public static void set(PromptContext ctx) {
        CTX.set(ctx);
    }

    public static PromptContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}