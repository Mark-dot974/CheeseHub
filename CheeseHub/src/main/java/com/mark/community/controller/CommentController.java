package com.mark.community.controller;

import com.mark.community.annotation.LoginRequired;
import com.mark.community.entity.Comment;
import com.mark.community.entity.DiscussPost;
import com.mark.community.entity.Event;
import com.mark.community.event.EventProducer;
import com.mark.community.service.CommentService;
import com.mark.community.service.DiscussPostService;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.HostHolder;
import com.mark.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    @LoginRequired
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        // 触发评论事件(producer会生产事件放到消息队列中，一旦消息队列中有事件，consumer会自动从消息队列中取出事件，构造消息，存到数据库中)
        Event event = new Event()
                .setTopic(CommunityConstant.TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                // entityType可能是评论或回复，由前端传递
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                // 详情页面跳转页面需要
                .setData("postId",discussPostId);
        // 当评论是评论or回复时，查询EntityUserId调用的Service不同
        if(comment.getEntityType() == CommunityConstant.ENTITY_TYPE_POST)
        {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }else if(comment.getEntityType() == CommunityConstant.ENTITY_TYPE_COMMENT){
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        // 判断触发当前事件的是否是当前用户
        if(hostHolder.getUser().getId() != event.getEntityUserId()){
            eventProducer.fireEvent(event);
        }

        // 因为评论时（回复不需要）会更改帖子的评论数量，所以也要触发更改es服务器中的数据
        if(comment.getEntityType() == CommunityConstant.ENTITY_TYPE_POST){
            // 触发事件，更新es服务器
            event = new Event()
                    .setTopic(CommunityConstant.TOPIC_UPDATE_ENTITY)
                    .setUserId(comment.getUserId())
                    .setEntityType(CommunityConstant.ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
            // 将帖子id放入缓存中，待计算帖子分数。
            String postScoreKey = RedisKeyUtil.getPostScoreKey();
            // 使用set集合存储需要计算分数的帖子id，因为对于同一帖子，即使被多次触发，在一段时间内，只需要计算最后一次的分数即可
            redisTemplate.opsForSet().add(postScoreKey,discussPostId);
        }
        return "redirect:/discuss/detail/" + discussPostId;
    }
}
