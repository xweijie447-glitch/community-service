package com.community.controller.page;

import com.community.repository.MessageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 管理端：邮件 / 短信发送记录（模拟通道留痕）
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMessageController {

    private final MessageLogRepository messageLogRepository;

    @GetMapping("/admin/messages")
    public String messages(@RequestParam(required = false) String channel, Model model) {
        var logs = messageLogRepository.findTop100ByOrderByIdDesc();
        if (channel != null && !channel.isBlank()) {
            logs = logs.stream().filter(m -> channel.equals(m.getChannel())).toList();
        }
        model.addAttribute("logs", logs);
        model.addAttribute("channel", channel == null ? "" : channel);
        model.addAttribute("emailCount", messageLogRepository.countByStatus("SIMULATED"));
        return "admin/messages";
    }
}
