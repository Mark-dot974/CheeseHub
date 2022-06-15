package com.mark.community.controller;

import com.mark.community.annotation.LoginRequired;
import com.mark.community.entity.*;
import com.mark.community.event.EventProducer;
import com.mark.community.service.CommentService;
import com.mark.community.service.DiscussPostService;
import com.mark.community.service.LikeService;
import com.mark.community.service.UserService;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.HostHolder;
import com.mark.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/publish",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String publish(String title,String content){
        // 判断用户是否登录
        User user = hostHolder.getUser();
        if(user == null){
            return CommunityUtil.getJSONString(403,"你还没有登录哦");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setContent(content);
        discussPost.setTitle(title);
        discussPost.setUserId(user.getId());
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        // 在对数据库中的帖子信息进行操作时，要同步更新es服务器中的内容，防止查询到的内容不一致
        Event event = new Event()
                .setTopic(CommunityConstant.TOPIC_UPDATE_ENTITY)
                .setUserId(user.getId())
                .setEntityType(CommunityConstant.ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);

        // 将帖子id放入缓存中，待计算帖子分数。
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        // 使用set集合存储需要计算分数的帖子id，因为对于同一帖子，即使被多次触发，在一段时间内，只需要计算最后一次的分数即可
        redisTemplate.opsForSet().add(postScoreKey,discussPost.getId());

        return CommunityUtil.getJSONString(0,"发布成功！");
    }

    @RequestMapping(path = "/detail/{id}",method = RequestMethod.GET)
    public String detail(Model model, @PathVariable("id")int id, Page page){
        // 帖子
        DiscussPost discussPost = discussPostService.showDetail(id);
        model.addAttribute("post",discussPost);

        // 发布者
        int userId = discussPost.getUserId();
        User user = userService.findUserById(userId);
        model.addAttribute("user",user);

        // 设置帖子的点赞数量
        long likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST, id);
        model.addAttribute("likeCount",likeCount);
        // 设置帖子的点赞状态
        int likeStatus;
        if(hostHolder.getUser() == null)
        {
            likeStatus=0;
        }
        else{
            likeStatus = likeService.findEntityLikeStatus(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_POST, id);
        }
        model.addAttribute("likeStatus",likeStatus);

        // 评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+ id);
        page.setRows(discussPost.getCommentCount());

        // 获取评论列表信息
        // 评论：给帖子的评论
        // 回复：给评论的评论
        // 评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(CommunityConstant.ENTITY_TYPE_POST, discussPost.getId(), page.getOffset(), page.getLimit());
        // 评论VO(View Object)列表
        List<Map<String,Object>> commentVoList = new ArrayList<>();
        if(commentList!=null){
            for (Comment comment : commentList) {
                // 评论VO
                Map<String , Object> commentVo = new HashMap<>();
                commentVo.put("comment",comment);
                // 添加评论的评论者
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                //添加评论的点赞数量
                likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);
                // 未登录时
                if(hostHolder.getUser() == null)
                {
                    likeStatus=0;
                }
                else {
                    likeStatus = likeService.findEntityLikeStatus(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_COMMENT, comment.getId());
                }
                commentVo.put("likeStatus",likeStatus);

                // 获取帖子的回复列表,有多少显示多少
                List<Comment> replyList = commentService.findCommentsByEntity(CommunityConstant.ENTITY_TYPE_COMMENT,comment.getId(),0,Integer.MAX_VALUE);
                // 构造回复的VO列表
                List<Map<String,Object>> replyVoList = new ArrayList<>();
                if(replyVoList!=null){
                    for (Comment reply : replyList) {
                        Map<String,Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply",reply);
                        // 作者，回复者
                        replyVo.put("user",userService.findUserById(reply.getUserId()));
                        // 回复对象
                        // 有两种可能，在帖子的评论下评论（回复），但是这时是不显示回复对象的，所以target是0；
                        // 第二种可能是楼主回复评论者，或评论者回复楼主，这时有target
                        // 被回复者
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target",target);
                        // 设置reply的点赞数量
                        likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_COMMENT,reply.getId());
                        replyVo.put("likeCount",likeCount);
                        if(hostHolder.getUser() == null)
                        {
                            likeStatus=0;
                        }
                        else likeStatus = likeService.findEntityLikeStatus(hostHolder.getUser().getId(),CommunityConstant.ENTITY_TYPE_COMMENT,reply.getId());
                        replyVo.put("likeStatus",likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys",replyVoList);
                // 回复数量
                int replyCount = commentService.findCommentCount(CommunityConstant.ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount",replyCount);
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments",commentVoList);
        return "/site/discuss-detail";
    }

    // 置顶帖子,版主权限
    @RequestMapping(path = "/top" , method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        // 判断是置顶还是取消置顶操作
        int discussPostType = discussPostService.getDiscussPostType(id);
        if(discussPostType!=1){
            discussPostService.updateDiscussPostType(id,1);
        }else{
            discussPostService.updateDiscussPostType(id,0);
        }

        // 触发更新帖子信息事件
        Event event = new Event()
                .setEntityId(id)
                .setEntityType(CommunityConstant.ENTITY_TYPE_POST)
                .setTopic(CommunityConstant.TOPIC_UPDATE_ENTITY);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    // 加精帖子，版主权限
    @RequestMapping(path ="/wonderful" , method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        int discussPostStatus = discussPostService.getDiscussPostStatus(id);
        if(discussPostStatus!=1){
            discussPostService.updateDiscussPostStatus(id,1);
        }else{
            discussPostService.updateDiscussPostStatus(id,0);
        }

        // 触发更新帖子信息事件
        Event event = new Event()
                .setEntityId(id)
                .setEntityType(CommunityConstant.ENTITY_TYPE_POST)
                .setTopic(CommunityConstant.TOPIC_UPDATE_ENTITY);
        eventProducer.fireEvent(event);

        // 将帖子id放入缓存中，待计算帖子分数。
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        // 使用set集合存储需要计算分数的帖子id，因为对于同一帖子，即使被多次触发，在一段时间内，只需要计算最后一次的分数即可
        redisTemplate.opsForSet().add(postScoreKey,id);

        return CommunityUtil.getJSONString(0);
    }

    // 拉黑（删除）帖子，管理员权限
    @RequestMapping(path = "/delete" , method = RequestMethod.POST)
    @ResponseBody
    public String deletePost(int id){
        discussPostService.updateDiscussPostStatus(id,2);

        // 触发删除帖子事件
        Event event = new Event()
                .setEntityId(id)
                .setEntityType(CommunityConstant.ENTITY_TYPE_POST)
                .setTopic(CommunityConstant.TOPIC_DELETE);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }
}
