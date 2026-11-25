package com.community.common;

/**
 * 业务异常：由全局异常处理器统一转换为响应
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BusinessException of(String message, Object... args) {
        return new BusinessException(String.format(message, args));
    }

    public static BusinessException notFound(String what) {
        return new BusinessException(ResultCode.NOT_FOUND, what + "不存在或已被删除");
    }

    public static BusinessException denied(String message) {
        return new BusinessException(ResultCode.FORBIDDEN, message);
    }

    public static BusinessException stateConflict(String message) {
        return new BusinessException(ResultCode.STATE_CONFLICT, message);
    }
}
