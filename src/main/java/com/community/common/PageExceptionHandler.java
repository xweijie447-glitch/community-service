package com.community.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * 页面（Thymeleaf）统一异常处理：渲染友好的错误页，而不是白页 500
 */
@Slf4j
@ControllerAdvice(basePackages = "com.community.controller.page")
public class PageExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("页面业务异常 uri={} code={} msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        ModelAndView mv = new ModelAndView("error/business");
        mv.addObject("code", e.getCode());
        mv.addObject("message", e.getMessage());
        mv.addObject("uri", request.getRequestURI());
        return mv;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ModelAndView handleMissingParam(MissingServletRequestParameterException e) {
        ModelAndView mv = new ModelAndView("error/business");
        mv.addObject("code", ResultCode.PARAM_ERROR.getCode());
        mv.addObject("message", "缺少必要参数：" + e.getParameterName());
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleOther(Exception e, HttpServletRequest request) {
        log.error("页面未捕获异常 uri={}", request.getRequestURI(), e);
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("message", "系统处理出现异常，请稍后重试或联系物业管理员");
        return mv;
    }
}
