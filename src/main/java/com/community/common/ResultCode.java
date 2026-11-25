package com.community.common;

/**
 * 统一响应码
 */
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已失效"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    RATE_LIMITED(429, "请求过于频繁，请稍后再试"),
    BUSINESS_ERROR(1000, "业务处理失败"),
    DUPLICATE_SUBMIT(1001, "请勿重复提交"),
    STATE_CONFLICT(1002, "订单状态已变更，请刷新后重试"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
