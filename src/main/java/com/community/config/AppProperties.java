package com.community.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务配置绑定（见 application.yml 的 app 节点）
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Order order = new Order();
    private RateLimit rateLimit = new RateLimit();
    private Message message = new Message();
    private Schedule schedule = new Schedule();
    private Dispatch dispatch = new Dispatch();

    @Getter
    @Setter
    public static class Order {
        /** 未派单超时自动取消（分钟） */
        private int timeoutMinutes = 30;
        /** 已完成订单保留天数 */
        private int keepDays = 7;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private int bookingLimit = 5;
        private int windowSeconds = 60;
    }

    @Getter
    @Setter
    public static class Message {
        private boolean simulated = true;
        private String sender = "community-service@example.com";
    }

    @Getter
    @Setter
    public static class Schedule {
        private boolean enabled = true;
        private String cleanupCron = "0 30 2 * * ?";
        private String dailyRemindCron = "0 0 8 * * ?";
    }

    @Getter
    @Setter
    public static class Dispatch {
        private int skillWeight = 50;
        private int ratingWeight = 30;
        private int loadWeight = 20;
    }
}
