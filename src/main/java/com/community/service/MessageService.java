package com.community.service;

import com.community.domain.MessageLog;
import com.community.repository.MessageLogRepository;
import com.community.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 邮件 / 短信通道。
 * <p>开发环境为「模拟发送」：内容落库 + 写日志，不真正投递，方便演示与排查；
 * 生产环境（app.message.simulated=false）走真实 SMTP。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final AppProperties appProperties;
    private final MessageLogRepository messageLogRepository;
    private final Optional<JavaMailSender> mailSender;

    @Async("notifyExecutor")
    public void sendEmail(String bizType, String to, String subject, String content, String orderNo) {
        String status = "SIMULATED";
        if (!appProperties.getMessage().isSimulated()) {
            try {
                mailSender.ifPresent(sender -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(appProperties.getMessage().getSender());
                    message.setTo(to);
                    message.setSubject(subject);
                    message.setText(content);
                    sender.send(message);
                });
                status = "SENT";
            } catch (Exception e) {
                status = "FAILED";
                log.error("邮件发送失败 to={} subject={}", to, subject, e);
            }
        }
        log.info("[邮件/{}] to={} subject={} status={} orderNo={}", bizType, to, subject, status, orderNo);
        save(bizType, "EMAIL", to, subject, content, status, orderNo);
    }

    @Async("notifyExecutor")
    public void sendSms(String bizType, String phone, String content, String orderNo) {
        // 短信通道模拟：不依赖第三方 SDK，落库即可，接线时替换为真实短信服务
        log.info("[短信/{}] to={} content={} orderNo={}", bizType, phone, content, orderNo);
        save(bizType, "SMS", phone, null, content, "SIMULATED", orderNo);
    }

    private void save(String bizType, String channel, String recipient, String subject,
                      String content, String status, String orderNo) {
        MessageLog entity = new MessageLog();
        entity.setBizType(bizType);
        entity.setChannel(channel);
        entity.setRecipient(recipient == null ? "-" : recipient);
        entity.setSubject(subject);
        entity.setContent(content);
        entity.setStatus(status);
        entity.setOrderNo(orderNo);
        messageLogRepository.save(entity);
    }
}
