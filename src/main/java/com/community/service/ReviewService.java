package com.community.service;

import com.community.common.BusinessException;
import com.community.common.TxUtils;
import com.community.domain.OrderStatus;
import com.community.domain.Review;
import com.community.domain.ServiceItem;
import com.community.domain.ServiceOrder;
import com.community.domain.StaffProfile;
import com.community.dto.ReviewForm;
import com.community.mq.OrderEventProducer;
import com.community.mq.message.ReviewStatsMessage;
import com.community.repository.ReviewRepository;
import com.community.repository.ServiceItemRepository;
import com.community.repository.ServiceOrderRepository;
import com.community.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务评价：一单一评，统计走 RabbitMQ 异步汇总
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ServiceOrderRepository orderRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ServiceItemRepository itemRepository;
    private final OrderEventProducer producer;

    public Review submit(ReviewForm form, Long residentId) {
        ServiceOrder order = orderRepository.findById(form.getOrderId())
                .orElseThrow(() -> BusinessException.notFound("预约单"));
        if (!order.getResidentId().equals(residentId)) {
            throw BusinessException.denied("只能评价自己的订单");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw BusinessException.stateConflict("服务完成后才能评价");
        }
        if (Boolean.TRUE.equals(order.getReviewed()) || reviewRepository.existsByOrderId(order.getId())) {
            throw new BusinessException("该订单已评价，请勿重复提交");
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setOrderNo(order.getOrderNo());
        review.setResidentId(residentId);
        review.setStaffId(order.getStaffId());
        review.setItemId(order.getServiceItemId());
        review.setScore(form.getScore());
        review.setTags(form.getTags());
        review.setContent(form.getContent());
        Review saved = reviewRepository.save(review);

        order.setReviewed(true);
        orderRepository.save(order);

        TxUtils.afterCommit(() -> producer.sendReviewStats(new ReviewStatsMessage(saved.getId(),
                saved.getStaffId(), saved.getItemId(), saved.getScore(), saved.getOrderNo())));
        log.info("订单 {} 收到评价 {} 分", order.getOrderNo(), form.getScore());
        return saved;
    }

    /** MQ 消费：异步汇总服务人员评分与服务项目评分 */
    public void applyStats(ReviewStatsMessage message) {
        if (reviewRepository.existsById(message.getReviewId()) == false) {
            log.warn("评价统计消息对应的评价不存在：{}", message.getReviewId());
            return;
        }
        if (message.getStaffId() != null) {
            StaffProfile profile = staffProfileRepository.findByUserId(message.getStaffId()).orElse(null);
            if (profile != null) {
                double avg = reviewRepository.avgScoreByStaff(message.getStaffId());
                profile.setAvgScore(Math.round(avg * 10) / 10.0);
                staffProfileRepository.save(profile);
                log.info("服务人员 {} 平均分更新为 {}", profile.getUserId(), profile.getAvgScore());
            }
        }
        if (message.getItemId() != null) {
            ServiceItem item = itemRepository.findById(message.getItemId()).orElse(null);
            if (item != null) {
                item.setReviewCount(item.getReviewCount() + 1);
                item.setScoreTotal(item.getScoreTotal() + message.getScore());
                itemRepository.save(item);
                log.info("服务项目 {} 评分数量更新为 {}", item.getName(), item.getReviewCount());
            }
        }
    }

    public java.util.Optional<Review> ofOrder(Long orderId) {
        return reviewRepository.findByOrderId(orderId);
    }

    public List<Review> ofResident(Long residentId) {
        return reviewRepository.findByResidentIdOrderByCreatedAtDesc(residentId);
    }

    public List<Review> latest() {
        return reviewRepository.findTop20ByOrderByIdDesc();
    }
}
