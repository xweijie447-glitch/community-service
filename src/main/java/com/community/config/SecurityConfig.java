package com.community.config;

import com.community.security.AuthSuccessHandler;
import com.community.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * 安全配置：居民 / 服务人员 / 管理员三级权限。
 * <p>URL 级别拦截在此声明，方法级别由 @PreAuthorize 兜底，页面级别由 Thymeleaf sec:authorize 控制。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthSuccessHandler authSuccessHandler;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 前后端同源、页面有 AJAX 提交，使用 Cookie 存 CSRF 并关闭 BREACH 掩码以便 JS 读取
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/services", "/services/**",
                                "/login", "/register", "/error", "/403", "/404",
                                "/css/**", "/js/**", "/img/**", "/favicon.ico",
                                "/actuator/health").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasRole("STAFF")
                        .requestMatchers("/api/staff/**").hasRole("STAFF")
                        .requestMatchers("/resident/**", "/orders/**").hasAnyRole("RESIDENT", "ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll())
                .rememberMe(remember -> remember
                        .key("community-service-remember-key")
                        .tokenValiditySeconds(7 * 24 * 3600)
                        .userDetailsService(userDetailsService))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // 仅保留页面/接口需要的路径，静态资源与登录页不做校验
                        .ignoringRequestMatchers("/css/**", "/js/**", "/img/**"))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(opts -> {
                        }))
                .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?expired"));

        return http.build();
    }
}
