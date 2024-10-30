package com.nowiam.config;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DelayMqConfig {

    //延迟队列交换机
    public static final String DELAY_EXCHANGE="DELAY_EXCHANGE";
    //延迟消息输入队列
    public static final String DELAY_SOURCE_QUEUE="DELAY_SOURCE_QUEUE";
    //延迟消息输出队列
    public static final String DELAY_TARGET_QUEUE="DELAY_TARGET_QUEUE";
    //延迟输入键
    public static final String DELAY_IN_KEY="DELAY_IN_KEY";
    //延迟输出键
    public static final String DELAY_OUT_KEY="DELAY_OUT_KEY";

    private static final Integer ONE_DAY=86400000;
    private static final Integer test=30000;

    //延迟交换机
    @Bean
    public Exchange delayExchange(){
        return ExchangeBuilder.topicExchange(DELAY_EXCHANGE).durable(true).build();
    }

    //设置延迟输入队列，无人监听自动过期
    @Bean
    public Queue delaySourceQueue(){
        Map<String,Object> map=new HashMap<>();
        map.put("x-dead-letter-exchange",DELAY_EXCHANGE);
        map.put("x-dead-letter-routing-key",DELAY_OUT_KEY);
        //map.put("x-message-ttl",ONE_DAY);
        map.put("x-message-ttl",test);
        return new Queue(DELAY_SOURCE_QUEUE,true,false,false,map);
    }

    //延迟输出队列，监听此队列获得延迟消息
    @Bean
    public Queue delayTargetQueue(){
        return new Queue(DELAY_TARGET_QUEUE,true);
    }

    @Bean
    public Binding sourceQueueBinding(){
        return BindingBuilder.bind(delaySourceQueue()).to(delayExchange()).with(DELAY_IN_KEY).noargs();
    }

    @Bean
    public Binding targetQueueBinding(){
        return BindingBuilder.bind(delayTargetQueue()).to(delayExchange()).with(DELAY_OUT_KEY).noargs();
    }
}
