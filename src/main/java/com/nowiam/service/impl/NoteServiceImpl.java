package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.cj.util.StringUtils;
import com.nowiam.delay.DelayProducer;
import com.nowiam.mapper.FriendMapper;
import com.nowiam.mapper.NoteMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.Result;
import com.nowiam.model.dto.NoteDto;
import com.nowiam.model.enums.NoteStatus;
import com.nowiam.model.pojo.Mes;
import com.nowiam.model.pojo.Note;
import com.nowiam.service.NoteService;
import com.nowiam.util.ThreadLocalUtil;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    //@Autowired
    //RabbitTemplate rabbitTemplate;
    @Autowired
    RocketMQTemplate rocketMQTemplate;
    @Autowired
    FriendMapper friendMapper;
    @Autowired
    Gson gson;
    @Autowired
    DelayProducer delayProducer;

    private static final String DELAY_SUBMIT="DelaySubmit";
    @Override
    public Result submit(NoteDto noteDto) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        Note note=new Note();
        BeanUtils.copyProperties(noteDto,note);
        //清缓存
        Integer userId=ThreadLocalUtil.getUser().getId();
        clearCache("NOTE_LIST:"+userId+":"+note.getStatus());


        if(StringUtils.isEmptyOrWhitespaceOnly(note.getContent())||StringUtils.isEmptyOrWhitespaceOnly(note.getType()))
        {
            return new Result<>().error(400,"事项类型或内容为空");
        }

        note.setAuthor(ThreadLocalUtil.getUser().getId());

        if(note.getStatus().equals(NoteStatus.UNSUBMIT))
        {
            noteMapper.insert(note);
            Mes<Note> mes=new Mes<>();
            mes.setContent(note);
            SendResult sendResult = delayProducer.delaySend(DELAY_SUBMIT,mes, 5L);
            return new Result<>().ok(sendResult.getSendStatus());
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
        if(note!=null){
            clearCache("NOTE_LIST:"+userId+":"+note.getStatus());
            noteMapper.deleteById(id);
        }
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
        String str = stringRedisTemplate.opsForValue().get("NOTE_LIST:" + userId + ":" + status);
        if(str!=null) {
            List<Note> cache = gson.fromJson(str, new TypeToken<List<Note>>(){}.getType());
            return new Result<>().ok(cache);
        }

        //获取缓存失败,开始查询
        List<Note> list = noteMapper.myList(userId,status);
        return new Result<>().ok(list);
    }

    @Override
    public Result shareList() {
        Integer userId=ThreadLocalUtil.getUser().getId();
        List<Integer> friends=friendMapper.myFriend(userId);
//        List<Note> shareNotes=new ArrayList<>();
//        for(Integer i:friends)
//        {
//            shareNotes.addAll(noteMapper.selectList(Wrappers.<Note>lambdaQuery().eq(Note::getAuthor,i).eq(Note::getStatus,NoteStatus.PUBLIC)));
//        }
        String str = stringRedisTemplate.opsForValue().get("SHARE_LIST:" + userId);
        if(str!=null) {
            List<Note> cache = gson.fromJson(str, new TypeToken<List<Note>>(){}.getType());
            return new Result<>().ok(cache);
        }
        List<Note> shareNotes=noteMapper.shareList(friends);
        return new Result<>().ok(shareNotes);
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

    private void clearCache(String key)
    {
        stringRedisTemplate.opsForValue().getOperations().delete(key);
    }
}
