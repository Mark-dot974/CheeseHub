package com.mark.community.controller.interceptor;

import com.mark.community.entity.User;
import com.mark.community.service.MessageService;
import com.mark.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if(user != null && modelAndView != null){
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            int topicNoticeUnreadCount = messageService.findTopicNoticeUnreadCount(user.getId(), null);
            int totalCount = letterUnreadCount+topicNoticeUnreadCount;
            modelAndView.addObject("allUnreadCount",totalCount);
        }
    }
}
