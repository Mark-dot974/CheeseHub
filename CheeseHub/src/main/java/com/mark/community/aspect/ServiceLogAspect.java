package com.mark.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
public class ServiceLogAspect {
    private static final Logger logger  = LoggerFactory.getLogger(ServiceLogAspect.class);

    // 方法标明切入点为service包下的所有类中的所有方法
    @Pointcut("execution(* com.mark.community.service.*.*(..))")
    public void pointcut(){
    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint){
        // 用户[1.2.3.4],在[xxx],访问了[com.mark.community.service.xxx()].
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes!=null){
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getRemoteHost();
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS").format(new Date());
            // 获取方法的名字
            String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            logger.info(String.format("用户[%s],在[%s],访问了[%s].", ip, now, target));
        }
    }
}
