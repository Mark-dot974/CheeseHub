package com.mark.community.service;

import com.mark.community.dao.MessageMapper;
import com.mark.community.entity.Message;
import com.mark.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    public int readMessage(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }

    // 查找系统发给当前登录用户userId的topic会话中最新的一条消息
    public Message findLatestNotice(int userId,String topic){
        return messageMapper.selectLatestNotice(userId,topic);
    }

    // 查找指定topic的总共的通知数量
    public int findTopicNoticeCount(int userId,String topic){
        return messageMapper.selectTopicNoticeCount(userId,topic);
    }

    // 查找指定topic未读消息数量 or 总共未读系统通知数量（topic为null时）
    public int findTopicNoticeUnreadCount(int userId , String topic){
        return messageMapper.selectTopicNoticeUnreadCount(userId,topic);
    }

    // 查找某个topic下所有的通知（详情）
    public List<Message> findTopicNotices(int userId,String topic,int offset,int limit){
        return messageMapper.selectTopicNotices(userId,topic,offset,limit);
    }
}
