package com.mark.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.mark.community.entity.Message;
import com.mark.community.entity.Page;
import com.mark.community.entity.User;
import com.mark.community.service.MessageService;
import com.mark.community.service.UserService;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.HostHolder;
import com.mark.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController {
    @Autowired
    private MessageService messageService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    // 根据userId查询到所有会话后分页展示
    @RequestMapping(path="/letter/list",method = RequestMethod.GET)
    public String showConversationByPagination(Model model , Page page){
        User user = hostHolder.getUser();

        // 设置分页信息,current已经由SpringMVC自动注入
        page.setLimit(5);
        page.setRows(messageService.findConversationCount(user.getId()));
        page.setPath("/letter/list");

        // 获得会话列表
        List<Message> conversationsList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>> conversations = new ArrayList<>();
        // 填充每行会话 每个会话还包含发送者的信息
        if(conversationsList!=null){
            for (Message message : conversationsList) {
                Map<String,Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount",messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(),message.getConversationId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target",userService.findUserById(targetId));
                conversations.add(map);
            }
        }
        model.addAttribute("conversations",conversations);

        // 查询总的未读消息数量,不传递conversationId就是查询所有对话的未读消息
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        int noticeUnreadCount = messageService.findTopicNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        return "/site/letter";
    }
    @RequestMapping(path = "/letter/detail/{conversationId}" , method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId")String conversationId,Model model,Page page){
        // 设置分页信息
        page.setLimit(5);
        page.setRows(messageService.findLetterCount(conversationId));
        page.setPath("/letter/detail/"+conversationId);
        User user = hostHolder.getUser();

        // 获取私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> letters = new ArrayList<>();
        // 详情页面中的私信发送对象
        int targetId = 0;
        // 填充每条私信的内容（发送者信息。。）
        if(letterList!=null){
            for (Message message : letterList) {
                Map<String,Object> map = new HashMap<>();
                map.put("letter",message);
                targetId = message.getFromId() == user.getId() ? message.getToId() : message.getFromId();
                map.put("fromUser",userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters",letters);
        // 设置私信目标对象（给TA私信按钮发送的对象）
        model.addAttribute("target",userService.findUserById(targetId));

        // 设置已读
        List<Integer> ids = getIds(letterList);
        if(ids!=null && ids.size()!=0){
            messageService.readMessage(ids);
        }
        return "/site/letter-detail";
    }
    public List<Integer> getIds(List<Message> letterList){
        List<Integer> ids = new ArrayList<>();
        for (Message message : letterList) {
            int id = message.getId();
            // 只用更新接收者是自己、状态为未读的消息，减少更新操作的数量，提高效率
            User user = hostHolder.getUser();
            if(message.getToId() == user.getId() && message.getStatus() == 0)
            {
                ids.add(id);
            }
        }
        return ids;
    }
    @RequestMapping(path = "/letter/send" , method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName,String content){
        User target = userService.findUserByName(toName);
        if(target == null){
            return CommunityUtil.getJSONString(1,"目标用户不存在！");
        }
        Message message = new Message();
        message.setCreateTime(new Date());
        // 发送对象是自己，消息状态应该设置为已读
        if(target.getId() == hostHolder.getUser().getId())
        {
            message.setStatus(1);
        }else{
            message.setStatus(0);
        }
        String contentFiltered = sensitiveFilter.filter(HtmlUtils.htmlEscape(content));
        message.setContent(contentFiltered);
        int fromId = hostHolder.getUser().getId();
        message.setFromId(fromId);
        message.setToId(target.getId());
        String conversationId;
        if(message.getFromId() > message.getToId()){
            conversationId = message.getToId() + "_" + message.getFromId();
        }else{
            conversationId = message.getFromId() + "_" + message.getToId();
        }
        message.setConversationId(conversationId);
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path="/notice/list" , method = RequestMethod.GET)
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();

        // 查询评论类通知
        // 1.查询最新的一条评论系统消息
        Message message = messageService.findLatestNotice(user.getId(), CommunityConstant.TOPIC_COMMENT);
        // 2. 填充其他信息，如时间、未读数量、总共消息数量等
        // 如果没有该类的信息，就不展示
        if(message!=null){
            Map<String,Object> messageVO = new HashMap<>();
            messageVO.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content,HashMap.class);
            // user----触发事件者
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));

            int count = messageService.findTopicNoticeCount(user.getId(),CommunityConstant.TOPIC_COMMENT);
            messageVO.put("count",count);

            int unread = messageService.findTopicNoticeUnreadCount(user.getId(),CommunityConstant.TOPIC_COMMENT);
            messageVO.put("unread",unread);
            model.addAttribute("commentNotice",messageVO);
        }

        // 查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), CommunityConstant.TOPIC_LIKE);
        if(message!= null){
            Map<String,Object> messageVO = new HashMap<>();
            messageVO.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));

            int count = messageService.findTopicNoticeCount(user.getId(),CommunityConstant.TOPIC_LIKE);
            messageVO.put("count",count);

            int unread = messageService.findTopicNoticeUnreadCount(user.getId(),CommunityConstant.TOPIC_LIKE);
            messageVO.put("unread",unread);
            model.addAttribute("likeNotice",messageVO);
        }

        // 查询关注类通知
        message = messageService.findLatestNotice(user.getId(), CommunityConstant.TOPIC_FOLLOW);
        if(message!=null){
            Map<String,Object> messageVO = new HashMap<>();
            messageVO.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));

            int count = messageService.findTopicNoticeCount(user.getId(),CommunityConstant.TOPIC_FOLLOW);
            messageVO.put("count",count);

            int unread = messageService.findTopicNoticeUnreadCount(user.getId(),CommunityConstant.TOPIC_FOLLOW);
            messageVO.put("unread",unread);
            model.addAttribute("followNotice",messageVO);
        }

        // 查询未读消息总数(私信的)
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        // 查询系统消息未读总数
        int topicNoticeUnreadCount = messageService.findTopicNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",topicNoticeUnreadCount);

        return "site/notice";
    }

    @RequestMapping(path="/notice/detail/{topic}" , method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic")String topic , Page page, Model model){
        User user = hostHolder.getUser();

        page.setLimit(5);
        page.setRows(messageService.findTopicNoticeCount(user.getId(),topic));
        page.setPath("/notice/detail"+topic);

        List<Message> noticeList = messageService.findTopicNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String,Object>> noticeVoList = new ArrayList<>();
        if(noticeList != null){
            for (Message notice : noticeList) {
                Map<String,Object> map = new HashMap<>();
                // 通知
                map.put("notice",notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                HashMap data = JSONObject.parseObject(content, HashMap.class);
                map.put("user",userService.findUserById((Integer) data.get("userId")));
                map.put("entityType",data.get("entityType"));
                map.put("entityId",data.get("entityId"));
                map.put("postId",data.get("postId"));
                // 通知作者
                map.put("fromUser",userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices",noticeVoList);

        // 设置已读
        List<Integer> ids = getIds(noticeList);
        if(!ids.isEmpty() && ids.size()!=0){
            messageService.readMessage(ids);
        }
        return "/site/notice-detail";
    }
}
