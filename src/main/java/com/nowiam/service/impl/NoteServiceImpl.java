package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.cj.util.StringUtils;
import com.nowiam.config.DelayMqConfig;
import com.nowiam.config.GsonConfig;
import com.nowiam.mapper.NoteMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.Result;
import com.nowiam.model.dto.NoteDto;
import com.nowiam.model.enums.NoteStatus;
import com.nowiam.model.pojo.Message;
import com.nowiam.model.pojo.Note;
import com.nowiam.model.pojo.User;
import com.nowiam.service.NoteService;
import com.nowiam.util.ThreadLocalUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {
    @Autowired
    NoteMapper noteMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    Gson gson;
    @Override
    public Result submit(NoteDto noteDto) {
        Note note=new Note();
        BeanUtils.copyProperties(noteDto,note);

        if(StringUtils.isEmptyOrWhitespaceOnly(note.getContent())||StringUtils.isEmptyOrWhitespaceOnly(note.getType()))
        {
            return new Result<>().error(400,"事项类型或内容为空");
        }

        note.setAuthor(ThreadLocalUtil.getUser().getId());

        if(note.getStatus().equals(NoteStatus.UNSUBMIT))
        {
            noteMapper.insert(note);
            String dto=gson.toJson(getMessage(note));
            //TODO:交给延时队列等待上传
            rabbitTemplate.convertAndSend(
                    DelayMqConfig.DELAY_EXCHANGE,
                    DelayMqConfig.DELAY_IN_KEY,
                    dto
            );
            return new Result<>().ok("ok");
        }
        else if(note.getStatus().equals(NoteStatus.SUBMIT)||note.getStatus().equals(NoteStatus.PUBLIC))
        {

            note.setCreateTime(new Date());
            //noteMapper.insert(note);
            //异步上传
            new AsynSubmit(note,noteMapper).start();

            return new Result<>().ok("上传成功!");
        }
        return new Result<>().error(400,"上传错误");
    }

    @Override
    public Result deleteById(Integer id) {
        Integer userId = ThreadLocalUtil.getUser().getId();
        Note note = noteMapper.selectOne(Wrappers.<Note>lambdaQuery().eq(Note::getAuthor, userId).eq(Note::getId, id));
        if(note!=null) noteMapper.deleteById(id);
        return new Result<>().ok("删除成功");
    }

    @Override
    public Result mylist(Integer status) {
        if(status==null) return new Result<>().error(400,"状态错误");
        Integer userId=ThreadLocalUtil.getUser().getId();
//        //mybatisPlus版
//        List<Note> list = noteMapper.selectList(Wrappers.<Note>lambdaQuery().eq(Note::getAuthor, userId).eq(Note::getStatus, status));
//        //共享文件必发布
//        if(status.equals(NoteStatus.SUBMIT))
//        {
//            List<Note> shareList=noteMapper.selectList(Wrappers.<Note>lambdaQuery().eq(Note::getAuthor, userId).eq(Note::getStatus,NoteStatus.PUBLIC));
//            list.addAll(shareList);
//        }
//        这里需要用到自动缓存故不用mp
        String str = stringRedisTemplate.opsForValue().get("List:" + userId + ":" + status);
        if(str!=null) {
            List<Note> cache = gson.fromJson(str, new TypeToken<List<Note>>(){}.getType());
            return new Result<>().ok(cache);
        }

        //获取缓存失败,开始查询
        List<Note> list = noteMapper.myList(userId,status);
        return new Result<>().ok(list);
    }

    //构建延时消息体
    private Message<Note> getMessage(Note note){
        Message<Note> message=new Message<>();
        message.setContent(note);

        //获得唯一id
        Long BEGIN_TIME=1704067200L;
        Long current_time= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long time_stamp=current_time-BEGIN_TIME;
        Long inc=stringRedisTemplate.opsForValue().increment("inc:unique_id");
        if(inc==null) inc=0L;
        long id=(time_stamp<<32)|(inc);

        message.setMessageId(Long.toString(id));
        return message;
    }

    private static class AsynSubmit extends Thread{
        private Note note;
        private NoteMapper noteMapper;
        private AsynSubmit(){}

        public AsynSubmit(Note note,NoteMapper noteMapper){
            this.note=note;
            this.noteMapper=noteMapper;
        }
        @Override
        public void run(){
            noteMapper.insert(note);
        }
    }
}
