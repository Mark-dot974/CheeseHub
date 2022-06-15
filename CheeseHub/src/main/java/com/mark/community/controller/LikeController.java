package com.mark.community.controller;

import com.mark.community.annotation.LoginRequired;
import com.mark.community.entity.Event;
import com.mark.community.entity.User;
import com.mark.community.event.EventProducer;
import com.mark.community.service.LikeService;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.HostHolder;
import com.mark.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController {
    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 点赞
     * @param entityType        点赞对象的类型
     * @param entityId          点赞对象的Id
     * @param entityUserId      点赞对象发布者的Id
     * @return
     */
    @RequestMapping(path = "/like" , method = RequestMethod.POST)
    @ResponseBody
//    @LoginRequired
    public String like(int entityType , int entityId , int entityUserId,int postId){
        User user = hostHolder.getUser();
        // 点赞
        likeService.like(user.getId(),entityType,entityId,entityUserId);
        // 数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        // 返回的结果
        Map<String,Object> map = new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);

        // 只有是点赞操作时，触发点赞事件，发送系统通知
        if(likeStatus == 1){
            Event event = new Event()
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityId(entityId)
                    .setEntityType(entityType)
                    .setTopic(CommunityConstant.TOPIC_LIKE)
                    .setData("postId",postId)
                    .setEntityUserId(entityUserId);
            // 判断触发当前事件的是否是当前用户
            if(hostHolder.getUser().getId() != event.getEntityUserId()){
                eventProducer.fireEvent(event);
            }

        }

        if(entityType == CommunityConstant.ENTITY_TYPE_POST){
            // 将帖子id放入缓存中，待计算帖子分数。
            String postScoreKey = RedisKeyUtil.getPostScoreKey();
            // 使用set集合存储需要计算分数的帖子id，因为对于同一帖子，即使被多次触发，在一段时间内，只需要计算最后一次的分数即可
            redisTemplate.opsForSet().add(postScoreKey,postId);
        }
        return CommunityUtil.getJSONString(0,null,map);
    }
}
