package com.mark.community.controller.interceptor;

import com.mark.community.entity.LoginTicket;
import com.mark.community.entity.User;
import com.mark.community.service.UserService;
import com.mark.community.util.CookieUtil;
import com.mark.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

// 该拦截器用于在用户登录后将维持用户登录状态，后面的方法可以从中获得user信息
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;

    @Autowired
    HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取凭证
        String ticket = CookieUtil.getValue(request,"ticket");
        if(ticket!=null){
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // ticket状态为登录状态且没有过期
            if(loginTicket!=null &&loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())){
                // 根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有用户
                hostHolder.setUser(user);

                // 用户登录后获取到用户的权限，并且存入SecurityContext，以便于Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user,user.getPassword(),userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if(user!=null && modelAndView != null){
            modelAndView.addObject("loginUser",user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
