package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mysql.cj.util.StringUtils;
import com.nowiam.config.DelayMqConfig;
import com.nowiam.mapper.NoteMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.Result;
import com.nowiam.model.dto.NoteDto;
import com.nowiam.model.enums.NoteStatus;
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
import java.util.Date;

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
            //TODO:交给延时队列等待上传
            rabbitTemplate.convertAndSend(
                    DelayMqConfig.DELAY_EXCHANGE,
                    DelayMqConfig.DELAY_IN_KEY,
                    "hello,delay"
            );
            return new Result<>().ok("ok");
        }
        else if(note.getStatus().equals(NoteStatus.SUBMIT)||note.getStatus().equals(NoteStatus.PUBLIC))
        {

            note.setCreateTime(new Date());
            noteMapper.insert(note);
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
}
