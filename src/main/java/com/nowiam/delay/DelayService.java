package com.nowiam.delay;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nowiam.config.DelayMqConfig;
import com.nowiam.mapper.MessageMapper;
import com.nowiam.mapper.NoteMapper;
import com.nowiam.model.enums.NoteStatus;
import com.nowiam.model.pojo.Message;
import com.nowiam.model.pojo.Note;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Date;

@Component
public class DelayService {
    @Autowired
    Gson gson;
    @Autowired
    NoteMapper noteMapper;
    @Autowired
    MessageMapper messageMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @RabbitListener(queues = DelayMqConfig.DELAY_TARGET_QUEUE)
    public void delaySubmit(String message){
        //解析消息体
        Type typeToken=new TypeToken<Message<Note>>(){}.getType();
        Message<Note> res = gson.fromJson(message, typeToken);
        Note note=res.getContent();
        //处理过的消息放行
        if(messageMapper.check(res.getMessageId())>0) return ;
        //System.out.println(note.getContent()+":"+note.getId());
        note.setCreateTime(new Date());
        note.setStatus(NoteStatus.PUBLIC);
        noteMapper.updateById(note);
        messageMapper.insert(res.getMessageId());

        clearCache("NOTE_LIST:"+note.getAuthor()+":"+NoteStatus.UNSUBMIT);
        clearCache("NOTE_LIST:"+note.getAuthor()+":"+NoteStatus.PUBLIC);
    }

    private void clearCache(String key)
    {
        stringRedisTemplate.opsForValue().getOperations().delete(key);
    }
}
