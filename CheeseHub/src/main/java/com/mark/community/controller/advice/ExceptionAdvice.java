package com.mark.community.controller.advice;

import com.mark.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class)   // 此注解标明该类是一个异常处理类，只要是”controller”抛出的异常都由该类处理
public class ExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler(Exception.class)   // 该注解标明所有Exception的异常都会由该方法执行。也可以定义多个方法针对特定异常进行处理
    public void handleException(Exception e, HttpServletRequest request , HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: "+e.getMessage());
        for(StackTraceElement element:e.getStackTrace()){
            logger.error(element.toString());
        }
        // 根据请求类型进行处理 （ajax请求还是普通页面请求？）
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
