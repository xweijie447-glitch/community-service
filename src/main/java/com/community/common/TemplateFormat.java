package com.community.common;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Thymeleaf 模板格式化工具（模板中以 @fmt.xxx(...) 调用），
 * 避免引入额外的模板扩展依赖。
 */
@Component("fmt")
public class TemplateFormat {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DT_SHORT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public String dt(LocalDateTime value) {
        return value == null ? "-" : value.format(DT);
    }

    public String dtShort(LocalDateTime value) {
        return value == null ? "-" : value.format(DT_SHORT);
    }

    public String date(LocalDate value) {
        return value == null ? "-" : value.format(DATE);
    }

    public String time(LocalTime value) {
        return value == null ? "-" : value.format(TIME);
    }

    /** datetime-local 输入框需要的格式 */
    public String isoMinute(LocalDateTime value) {
        return value == null ? "" : value.format(ISO_MINUTE);
    }

    public String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 相对时间描述，例如 3 分钟前 */
    public String ago(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        long minutes = java.time.Duration.between(value, LocalDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " 小时前";
        }
        return hours / 24 + " 天前";
    }
}
