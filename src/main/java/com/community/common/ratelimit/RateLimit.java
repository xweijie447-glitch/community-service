package com.community.common.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于 Redis 的接口限流注解。
 * <p>默认按「方法 + 客户端 IP」做固定窗口计数，用于防刷预约、防暴力登录。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 业务标识，默认取方法全名 */
    String key() default "";

    /** 窗口内允许次数，<=0 时使用全局配置 */
    int limit() default 0;

    /** 窗口长度（秒），<=0 时使用全局配置 */
    int window() default 0;

    /** 触发限流后的提示语 */
    String message() default "操作过于频繁，请稍后再试";

    /** 是否按登录用户维度限流（默认按 IP） */
    boolean byUser() default false;
}
