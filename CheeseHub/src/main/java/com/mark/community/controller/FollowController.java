package com.mark.community.controller;

import com.mark.community.entity.Event;
import com.mark.community.entity.Page;
import com.mark.community.entity.User;
import com.mark.community.event.EventProducer;
import com.mark.community.service.FollowService;
import com.mark.community.service.UserService;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController {
    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path="/follow" , method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType , int entityId){
        User user = hostHolder.getUser();
        followService.follow(user.getId(),entityType,entityId);

        // 触发关注事件，发送系统通知
        Event event = new Event()
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setUserId(hostHolder.getUser().getId())
                .setTopic(CommunityConstant.TOPIC_FOLLOW)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0,"已关注！");
    }

    @RequestMapping(path = "unfollow" , method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType , int entityId){
        User user = hostHolder.getUser();
        followService.unfollow(user.getId(),entityType,entityId);
        return CommunityUtil.getJSONString(0,"已取消关注！");
    }

    // 获取userId对应用户关注的用户列表
    @RequestMapping(path="/followees/{userId}",method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId , Page page , Model model)
    {
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user",user);

        // 设置分页信息
        page.setLimit(5);
        page.setPath("/followees/"+userId);
        page.setRows((int) followService.findFolloweeCount(userId, CommunityConstant.ENTITY_TYPE_USER));

        // 填充每条followee的信息
        List<Map<String,Object>> userList = followService.findFollowee(userId,page.getOffset(), page.getLimit());
        if(userList != null){
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        return "/site/followee";
    }

    @RequestMapping(path="/followers/{userId}",method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId , Page page , Model model)
    {
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user",user);

        // 设置分页信息
        page.setLimit(5);
        page.setPath("/followers/"+userId);
        page.setRows((int) followService.findFollowerCount(CommunityConstant.ENTITY_TYPE_USER,userId));

        // 填充每条followee的信息
        List<Map<String,Object>> userList = followService.findFollowers(userId, page.getOffset(),page.getLimit());
        if(userList != null){
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        return "/site/follower";
    }

    // 查询当前用户是否关注了id对应的用户
    private boolean hasFollowed(int id) {
        if(hostHolder.getUser() == null){
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(),CommunityConstant.ENTITY_TYPE_USER,id);
    }


}
