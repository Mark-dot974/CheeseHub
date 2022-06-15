package com.mark.community.event;

import com.alibaba.fastjson.JSONObject;
import com.mark.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 触发时间
    public void fireEvent(Event event){
        // 生产发送后，监听相关topic的消费者会收到value
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
