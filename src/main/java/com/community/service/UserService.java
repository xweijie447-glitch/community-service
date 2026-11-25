package com.community.service;

import com.community.common.BusinessException;
import com.community.domain.Role;
import com.community.domain.User;
import com.community.dto.PasswordForm;
import com.community.dto.ProfileForm;
import com.community.dto.RegisterForm;
import com.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 居民账号与个人信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new BusinessException("该账号已被注册，请更换");
        }
        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRealName(form.getRealName().trim());
        user.setPhone(form.getPhone().trim());
        user.setEmail(form.getEmail());
        user.setAddress(form.getAddress());
        user.setRole(Role.RESIDENT);
        user.setEnabled(true);
        log.info("居民注册成功：{}", user.getUsername());
        return userRepository.save(user);
    }

    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("用户"));
    }

    public User updateProfile(Long userId, ProfileForm form) {
        User user = get(userId);
        user.setRealName(form.getRealName().trim());
        user.setPhone(form.getPhone().trim());
        user.setEmail(form.getEmail());
        user.setAddress(form.getAddress());
        return userRepository.save(user);
    }

    public void changePassword(Long userId, PasswordForm form) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        User user = get(userId);
        if (!passwordEncoder.matches(form.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (form.getOldPassword().equals(form.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
        log.info("用户 {} 修改密码成功", user.getUsername());
    }
}
