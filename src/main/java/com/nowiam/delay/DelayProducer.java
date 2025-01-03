package com.nowiam.delay;

import com.google.gson.Gson;
import com.nowiam.model.pojo.Mes;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class DelayProducer {
    @Autowired
    Gson gson;
    @Autowired
    DefaultMQProducer defaultMQProducer;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    public SendResult delaySend(String topic, Mes mes, Long delayTime) throws MQClientException, MQBrokerException, RemotingException, InterruptedException {
        //defaultMQProducer.start();
        //构造消息
        mes.setMessageId(mesId());
        String json=gson.toJson(mes);
        Message message=new Message(topic,json.getBytes(StandardCharsets.UTF_8));
        //设置延迟时间
        message.setDelayTimeSec(delayTime);

        SendResult send = defaultMQProducer.send(message);
        //defaultMQProducer.shutdown();
        return send;
    }

    private String mesId(){
        //获得唯一id
        Long BEGIN_TIME=1704067200L;
        Long current_time= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long time_stamp=current_time-BEGIN_TIME;
        Long inc=stringRedisTemplate.opsForValue().increment("inc:unique_id");
        if(inc==null) inc=0L;
        long id=(time_stamp<<32)|(inc);
        return Long.toString(id);
    }
}
