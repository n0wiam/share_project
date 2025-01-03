package com.nowiam.delay;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nowiam.mapper.MessageMapper;
import com.nowiam.mapper.NoteConMapper;
import com.nowiam.mapper.NoteMapper;
import com.nowiam.model.enums.NoteStatus;
import com.nowiam.model.pojo.Mes;
import com.nowiam.model.pojo.Note;
import com.nowiam.model.pojo.NoteCon;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Date;

@Component
@RocketMQMessageListener(consumerGroup ="ShareConsumerGroup",topic = "DelaySubmit")
public class DelayService implements RocketMQListener<String> {
    @Autowired
    NoteMapper noteMapper;
    @Autowired
    NoteConMapper noteConMapper;
    @Autowired
    MessageMapper messageMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;
    @Override
    public void onMessage(String mes) {
        Type typeToken=new TypeToken<Mes<Note>>(){}.getType();
        System.out.println("延迟提交："+mes);
        Mes<Note> res = gson.fromJson(mes, typeToken);
        Note note=res.getContent();
        //处理过的消息放行
        if(messageMapper.check(res.getMessageId())>0) return ;
        //System.out.println(note.getContent()+":"+note.getId());
        note.setCreateTime(new Date());
        note.setStatus(NoteStatus.PUBLIC);
        if(noteMapper.selectById(note.getId())!=null)
        {
            noteMapper.updateById(note);
            messageMapper.insert(res.getMessageId());
            NoteCon noteCon=new NoteCon();
            noteCon.setNoteId(note.getId());
            noteCon.setUserId(note.getAuthor());
            noteConMapper.insert(noteCon);
        }

        clearCache("NOTE_LIST:"+note.getAuthor());
        clearCache("NOTE_LIST:"+note.getAuthor());
    }
    private void clearCache(String key)
    {
        stringRedisTemplate.opsForValue().getOperations().delete(key);
    }
}
