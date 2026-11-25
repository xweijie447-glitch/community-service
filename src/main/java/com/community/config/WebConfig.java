package com.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源与简单视图映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/img/**").addResourceLocations("classpath:/static/img/")
                .setCachePeriod(86400);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // /403 由 AuthController 处理（需同时支持 GET 与 POST，避免拒绝访问时 forward 产生 405）
        registry.addViewController("/404").setViewName("error/404");
    }
}
