package com.community.common.ratelimit;

import com.community.common.BusinessException;
import com.community.common.ResultCode;
import com.community.config.AppProperties;
import com.community.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 限流切面：Redis 固定窗口计数。
 * <p>Redis 不可用时「放行」并打告警日志，避免限流组件故障导致业务整体不可用。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate:limit:";

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        AppProperties.RateLimit cfg = appProperties.getRateLimit();
        if (cfg == null || !cfg.isEnabled()) {
            return pjp.proceed();
        }
        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : cfg.getBookingLimit();
        int window = rateLimit.window() > 0 ? rateLimit.window() : cfg.getWindowSeconds();
        String key = buildKey(pjp, rateLimit);

        try {
            Long count = redisTemplate.execute(SCRIPT, List.of(key), String.valueOf(window));
            if (count != null && count > limit) {
                log.warn("触发限流 key={} count={} limit={}", key, count, limit);
                throw new BusinessException(ResultCode.RATE_LIMITED, rateLimit.message());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 故障时降级放行
            log.error("限流组件异常，已降级放行 key={}", key, e);
        }
        return pjp.proceed();
    }

    private String buildKey(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        String biz = rateLimit.key();
        if (biz.isBlank()) {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            biz = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        }
        String identity;
        if (rateLimit.byUser() && SecurityUtils.currentUserId() != null) {
            identity = "u" + SecurityUtils.currentUserId();
        } else {
            identity = "ip" + clientIp();
        }
        return KEY_PREFIX + biz + ":" + identity;
    }

    private String clientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                int idx = value.indexOf(',');
                return idx > 0 ? value.substring(0, idx).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
