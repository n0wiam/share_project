package com.nowiam.delay;

import com.nowiam.config.DelayMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DelayService {

    @RabbitListener(queues = DelayMqConfig.DELAY_TARGET_QUEUE)
    public void handler(String message){
        System.out.println("收到消息："+message+",结束时间："+System.currentTimeMillis());
    }
}
