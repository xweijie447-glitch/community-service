package com.community.service;

import com.community.common.BusinessException;
import com.community.domain.OrderStatus;
import com.community.domain.Role;
import com.community.domain.StaffProfile;
import com.community.domain.StaffStatus;
import com.community.domain.User;
import com.community.dto.StaffForm;
import com.community.repository.ServiceOrderRepository;
import com.community.repository.StaffProfileRepository;
import com.community.repository.UserRepository;
import com.community.vo.StaffView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务人员管理：档案、技能标签、接单状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private static final List<OrderStatus> ACTIVE_STATUS = List.of(OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE);

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ServiceOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public List<StaffView> listAll() {
        return staffProfileRepository.findAllByOrderByStatusAscAvgScoreDesc().stream()
                .map(this::toView)
                .toList();
    }

    public StaffView getByUserId(Long userId) {
        StaffProfile profile = staffProfileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("服务人员档案"));
        return toView(profile);
    }

    public List<StaffView> availableStaff() {
        return staffProfileRepository.findAll().stream()
                .map(this::toView)
                .filter(v -> v.getProfile().getStatus() != StaffStatus.OFF && v.getRemaining() > 0)
                .toList();
    }

    public StaffView toView(StaffProfile profile) {
        User user = userRepository.findById(profile.getUserId()).orElse(null);
        long active = orderRepository.countByStaffIdAndStatusIn(profile.getUserId(), ACTIVE_STATUS);
        return new StaffView(profile, user, active);
    }

    public StaffProfile saveStaff(StaffForm form) {
        User user;
        if (form.getUserId() == null) {
            if (userRepository.existsByUsername(form.getUsername())) {
                throw new BusinessException("登录账号已存在：" + form.getUsername());
            }
            if (form.getPassword() == null || form.getPassword().isBlank()) {
                throw new BusinessException("新增服务人员必须设置初始密码");
            }
            user = new User();
            user.setUsername(form.getUsername().trim());
            user.setPassword(passwordEncoder.encode(form.getPassword()));
            user.setRole(Role.STAFF);
        } else {
            user = userRepository.findById(form.getUserId())
                    .orElseThrow(() -> BusinessException.notFound("服务人员账号"));
            if (form.getPassword() != null && !form.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(form.getPassword()));
            }
        }
        user.setRealName(form.getRealName().trim());
        user.setPhone(form.getPhone().trim());
        user.setEmail(form.getEmail());
        user.setEnabled(true);
        user = userRepository.save(user);

        StaffProfile profile = staffProfileRepository.findByUserId(user.getId()).orElseGet(StaffProfile::new);
        profile.setUserId(user.getId());
        profile.setSkills(form.getSkills().trim());
        profile.setWorkStart(form.getWorkStart());
        profile.setWorkEnd(form.getWorkEnd());
        profile.setMaxDailyOrders(form.getMaxDailyOrders());
        profile.setServiceYears(form.getServiceYears());
        profile.setIntro(form.getIntro());
        profile.setStatus(form.getStatus() == null ? StaffStatus.FREE : form.getStatus());
        log.info("保存服务人员 {} 技能={}", user.getDisplayName(), profile.getSkills());
        return staffProfileRepository.save(profile);
    }

    /** 服务人员自己切换接单状态 */
    public void updateStatus(Long userId, StaffStatus status) {
        StaffProfile profile = staffProfileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("服务人员档案"));
        profile.setStatus(status);
        staffProfileRepository.save(profile);
        log.info("服务人员 {} 接单状态改为 {}", userId, status.getLabel());
    }

    /** 服务人员维护技能与接单配置 */
    public void updateSelf(Long userId, String skills, Integer maxDailyOrders, String intro) {
        StaffProfile profile = staffProfileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("服务人员档案"));
        if (skills != null && !skills.isBlank()) {
            profile.setSkills(skills.trim());
        }
        if (maxDailyOrders != null && maxDailyOrders > 0) {
            profile.setMaxDailyOrders(maxDailyOrders);
        }
        profile.setIntro(intro);
        staffProfileRepository.save(profile);
    }

    public void toggleEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("服务人员账号"));
        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        userRepository.save(user);
    }

    public StaffForm toForm(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("服务人员账号"));
        StaffProfile profile = staffProfileRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("服务人员档案"));
        StaffForm form = new StaffForm();
        form.setUserId(user.getId());
        form.setUsername(user.getUsername());
        form.setRealName(user.getRealName());
        form.setPhone(user.getPhone());
        form.setEmail(user.getEmail());
        form.setSkills(profile.getSkills());
        form.setWorkStart(profile.getWorkStart());
        form.setWorkEnd(profile.getWorkEnd());
        form.setMaxDailyOrders(profile.getMaxDailyOrders());
        form.setServiceYears(profile.getServiceYears());
        form.setIntro(profile.getIntro());
        form.setStatus(profile.getStatus());
        return form;
    }
}
