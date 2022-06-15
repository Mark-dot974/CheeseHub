package com.mark.community.event;

import com.alibaba.fastjson.JSONObject;
import com.mark.community.entity.DiscussPost;
import com.mark.community.entity.Event;
import com.mark.community.entity.Message;
import com.mark.community.service.DiscussPostService;
import com.mark.community.service.ElasticsearchService;
import com.mark.community.service.MessageService;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.HostHolder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 当事件被触发时，生产者生产出事件。消费者获取到事件，根据事件信息构建消息，将消息存到数据库中。
 */
@Component
public class EventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    // 此处使用hostHolder获得的user为null
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @KafkaListener(topics = {CommunityConstant.TOPIC_COMMENT,CommunityConstant.TOPIC_LIKE,CommunityConstant.TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if(record == null ||record.value() == null){
            logger.error("消息的内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if (event == null){
            logger.error("消息格式错误！");
            return;
        }
        // 发送通知
        Message message = new Message();
        message.setFromId(CommunityConstant.SYSTEM_USER_ID);
        message.setConversationId(event.getTopic());
        message.setToId(event.getEntityUserId());
        message.setCreateTime(new Date());

        // 将构造系统消息需要的信息放到Map中，前端进行拼接
        // 格式：用户xxx点赞了你的xxx....
        Map<String,Object> content = new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());
        if(!event.getData().isEmpty()){
            for (Map.Entry<String, Object> entry: event.getData().entrySet()
            ){
                content.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    // 消费添加事件
    @KafkaListener(topics = {CommunityConstant.TOPIC_UPDATE_ENTITY})
    public void handleAddEntityEvent(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

    // 消费删除帖子事件
    @KafkaListener(topics = {CommunityConstant.TOPIC_DELETE})
    public void handleDeleteEvent(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }
}
























