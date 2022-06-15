package com.mark.community.config;

import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 不拦截resources目录下的内容
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 以下配置的controller请求，要具有下面配置的authority才可以进行访问。其他的所有请求都可以直接访问
        // 并且取消了防止csrf攻击
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/user/my-post",
                        "/discuss/publish",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like/**",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        CommunityConstant.AUTHORITY_USER,
                        CommunityConstant.AUTHORITY_ADMIN,
                        CommunityConstant.AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        CommunityConstant.AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete"
                )
                .hasAnyAuthority(
                        CommunityConstant.AUTHORITY_ADMIN
                )
                .anyRequest().permitAll()
                .and().csrf().disable();

        // 验证失败时处理
        http.exceptionHandling()
                // 没有登录，根据请求类型进行处理（ajax？html）
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/json;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                // 权限不足
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });
        // Security底层默认会拦截/logout请求，进行退出处理
        // 如果是让springSecurity执行自己的退出逻辑，最终会直接返回，不会执行我们的代码
        // 故此处写一个不存在的路径给security处理，如此就会继续往后执行
        http.logout().logoutUrl("/securityLogout");
    }
}
