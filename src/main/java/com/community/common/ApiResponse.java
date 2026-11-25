package com.community.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 统一响应格式：{ code, message, data, timestamp }
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(int code, String message, T data, String timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data,
                LocalDateTime.now().toString());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(ResultCode code) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), null, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> fail(ResultCode code, String message) {
        return new ApiResponse<>(code.getCode(), message, null, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now().toString());
    }

    public boolean isSuccess() {
        return code == ResultCode.SUCCESS.getCode();
    }
}
