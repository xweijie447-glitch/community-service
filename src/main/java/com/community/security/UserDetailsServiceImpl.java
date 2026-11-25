package com.community.security;

import com.community.domain.User;
import com.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 基于数据库的账号认证
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("账号不存在：" + username));
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(),
                user.getRealName(), user.getRole(), Boolean.TRUE.equals(user.getEnabled()));
    }
}
