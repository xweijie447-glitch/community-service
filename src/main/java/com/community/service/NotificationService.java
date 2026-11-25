package com.community.service;

import com.community.domain.ServiceOrder;
import com.community.domain.User;
import com.community.repository.ServiceOrderRepository;
import com.community.repository.StaffProfileRepository;
import com.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 通知内容编排：把订单信息翻译成居民 / 服务人员能读懂的话术，交给邮件、短信通道发送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MessageService messageService;
    private final ServiceOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;

    public void orderCreated(Long orderId) {
        ServiceOrder order = order(orderId);
        User resident = userRepository.findById(order.getResidentId()).orElse(null);
        if (resident == null) {
            return;
        }
        String content = """
                尊敬的 %s，您的便民服务预约已提交成功。
                订单编号：%s
                服务项目：%s
                预约时间：%s
                服务地址：%s
                订单金额：%s 元
                我们将在 %d 分钟内为您派单，请保持电话畅通。
                """.formatted(resident.getDisplayName(), order.getOrderNo(), order.getServiceItemName(),
                order.getAppointmentTime().format(TIME_FMT), order.getAddress(), order.getTotalAmount(), 30);
        sendBoth("ORDER_CREATED", resident, "【社区便民】预约提交成功 " + order.getOrderNo(),
                content, order.getOrderNo());
    }

    public void dispatched(Long orderId) {
        ServiceOrder order = order(orderId);
        User resident = userRepository.findById(order.getResidentId()).orElse(null);
        User staff = order.getStaffId() == null ? null
                : userRepository.findById(order.getStaffId()).orElse(null);

        if (resident != null) {
            String content = """
                    尊敬的 %s，您的预约已受理并完成派单。
                    订单编号：%s
                    服务项目：%s
                    服务人员：%s（联系电话 %s）
                    上门时间：%s
                    服务人员会提前与您联系，请保持电话畅通。
                    """.formatted(resident.getDisplayName(), order.getOrderNo(), order.getServiceItemName(),
                    staff == null ? "待确认" : staff.getDisplayName(),
                    staff == null ? "-" : staff.getPhone(),
                    order.getAppointmentTime().format(TIME_FMT));
            sendBoth("DISPATCHED", resident, "【社区便民】已为您派单 " + order.getOrderNo(),
                    content, order.getOrderNo());
        }
        if (staff != null) {
            String content = """
                    您好 %s，您有新的服务工单待执行。
                    订单编号：%s
                    服务项目：%s
                    上门时间：%s
                    服务地址：%s
                    联系人：%s（%s）
                    请按时上门，服务完成后在工作台点击「完成服务」。
                    """.formatted(staff.getDisplayName(), order.getOrderNo(), order.getServiceItemName(),
                    order.getAppointmentTime().format(TIME_FMT), order.getAddress(),
                    order.getContactName(), order.getContactPhone());
            sendBoth("DISPATCH_STAFF", staff, "【社区便民】新工单派给您 " + order.getOrderNo(),
                    content, order.getOrderNo());
        }
    }

    public void completed(Long orderId) {
        ServiceOrder order = order(orderId);
        User resident = userRepository.findById(order.getResidentId()).orElse(null);
        if (resident == null) {
            return;
        }
        String content = """
                尊敬的 %s，您的服务订单已完成。
                订单编号：%s
                服务项目：%s
                完成时间：%s
                欢迎在「我的订单」中对本次服务作出评价，您的反馈对我们非常重要。
                """.formatted(resident.getDisplayName(), order.getOrderNo(), order.getServiceItemName(),
                LocalDateTime.now().format(TIME_FMT));
        sendBoth("COMPLETED", resident, "【社区便民】服务已完成 " + order.getOrderNo(), content, order.getOrderNo());
    }

    public void cancelled(Long orderId, String reason) {
        ServiceOrder order = order(orderId);
        userRepository.findById(order.getResidentId()).ifPresent(resident -> {
            String content = """
                    尊敬的 %s，您的预约单已取消。
                    订单编号：%s
                    服务项目：%s
                    取消原因：%s
                    如仍需服务，欢迎重新预约。
                    """.formatted(resident.getDisplayName(), order.getOrderNo(), order.getServiceItemName(),
                    reason == null ? "用户取消" : reason);
            sendBoth("CANCELLED", resident, "【社区便民】预约已取消 " + order.getOrderNo(), content,
                    order.getOrderNo());
        });
    }

    /** 次日待服务工单提醒：发给服务人员 + 汇总给管理员 */
    public void dailyRemind(java.util.List<ServiceOrder> orders) {
        if (orders.isEmpty()) {
            return;
        }
        StringBuilder summary = new StringBuilder("明日待服务工单共 " + orders.size() + " 单：\n");
        for (ServiceOrder order : orders) {
            summary.append(order.getOrderNo()).append(" | ")
                    .append(order.getServiceItemName()).append(" | ")
                    .append(order.getAppointmentTime().format(TIME_FMT)).append(" | ")
                    .append(order.getAddress()).append('\n');
        }
        userRepository.findAllByRoleAndEnabledTrueOrderByRealNameAsc(com.community.domain.Role.ADMIN)
                .forEach(admin -> sendBoth("DAILY_REMIND", admin, "【社区便民】明日待服务工单汇总",
                        summary.toString(), null));

        orders.stream()
                .filter(o -> o.getStaffId() != null)
                .collect(java.util.stream.Collectors.groupingBy(ServiceOrder::getStaffId))
                .forEach((staffId, list) -> userRepository.findById(staffId).ifPresent(staff -> {
                    StringBuilder body = new StringBuilder("您好 " + staff.getDisplayName() + "，您明日有 "
                            + list.size() + " 个待服务工单：\n");
                    list.forEach(o -> body.append("- ").append(o.getAppointmentTime().format(TIME_FMT))
                            .append(' ').append(o.getOrderNo()).append(' ')
                            .append(o.getServiceItemName()).append(' ')
                            .append(o.getAddress()).append('\n'));
                    sendBoth("DAILY_REMIND", staff, "【社区便民】您明日的服务工单", body.toString(), null);
                }));
        log.info("次日工单提醒已推送，共 {} 单", orders.size());
    }

    private void sendBoth(String bizType, User user, String subject, String content, String orderNo) {
        Optional.ofNullable(user.getEmail())
                .filter(e -> !e.isBlank())
                .ifPresent(email -> messageService.sendEmail(bizType, email, subject, content, orderNo));
        Optional.ofNullable(user.getPhone())
                .filter(p -> !p.isBlank())
                .ifPresent(phone -> messageService.sendSms(bizType, phone,
                        subject + "｜" + content.replaceAll("\\s+", " ").trim(), orderNo));
    }

    private ServiceOrder order(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("订单不存在：" + orderId));
    }
}
