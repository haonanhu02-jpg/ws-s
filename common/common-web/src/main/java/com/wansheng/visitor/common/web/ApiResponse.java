package com.wansheng.visitor.common.web;

public record ApiResponse<T>(boolean success, T data, String traceId) {
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(true, data, traceId);
    }
}

