package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.cj.util.StringUtils;
import com.nowiam.delay.DelayProducer;
import com.nowiam.mapper.FriendMapper;
import com.nowiam.mapper.NoteConMapper;
import com.nowiam.mapper.NoteMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.Result;
import com.nowiam.model.dto.NoteDto;
import com.nowiam.model.enums.NoteStatus;
import com.nowiam.model.pojo.Mes;
import com.nowiam.model.pojo.Note;
import com.nowiam.model.pojo.NoteCon;
import com.nowiam.model.task.NoteDelTask;
import com.nowiam.model.vo.NoteVo;
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
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {
    @Autowired
    NoteMapper noteMapper;
    @Autowired
    NoteConMapper noteConMapper;
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
    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    private static final String DELAY_SUBMIT="DelaySubmit";
    @Override
    public Result submit(NoteDto noteDto) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        Note note=new Note();
        BeanUtils.copyProperties(noteDto,note);
        //清缓存
        Integer userId=ThreadLocalUtil.getUser().getId();
        clearCache("NOTE_LIST:"+userId);


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
            new AsynSubmit(note,noteMapper,noteConMapper).start();
            return new Result<>().ok("上传成功!");
        }
        return new Result<>().error(400,"上传错误");
    }

    @Override
    public Result deleteById(Integer id) {
        Integer userId = ThreadLocalUtil.getUser().getId();
        clearCache("NOTE_LIST:"+userId);
//        Note note = noteMapper.selectOne(Wrappers.<Note>lambdaQuery().eq(Note::getAuthor, userId).eq(Note::getId, id));
//        if(note!=null){
//            clearCache("NOTE_LIST:"+userId);
//            noteMapper.deleteById(id);
//        }
//        noteConMapper.delete(Wrappers.<NoteCon>lambdaQuery().eq(NoteCon::getNoteId,id));
        threadPoolExecutor.execute(new NoteDelTask(id,userId,noteMapper,noteConMapper));
        return new Result<>().ok("删除成功");
    }

    @Override
    public Result mylist() {
        //if(status==null) return new Result<>().error(400,"状态错误");
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
        String str = stringRedisTemplate.opsForValue().get("NOTE_LIST:");
        if(str!=null) {
            List<NoteVo> cache = gson.fromJson(str, new TypeToken<List<NoteVo>>(){}.getType());
            return new Result<>().ok(cache);
        }

        //获取缓存失败,开始查询
        List<NoteVo> list = noteMapper.myList(userId);
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
            List<NoteVo> cache = gson.fromJson(str, new TypeToken<List<NoteVo>>(){}.getType());
            return new Result<>().ok(cache);
        }
        if(friends.isEmpty()) return new Result<>().ok(null);
        List<NoteVo> shareNotes=noteMapper.shareList(friends);
        return new Result<>().ok(shareNotes);
    }

    @Override
    public Result subscirbe(Integer id) {
        Note note=noteMapper.selectById(id);
        if(note==null||note.getStatus()!=NoteStatus.PUBLIC) return new Result<>().error(400,"订阅失败");
        NoteCon noteCon=new NoteCon();
        noteCon.setUserId(ThreadLocalUtil.getUser().getId());
        noteCon.setNoteId(id);
        if(noteConMapper.selectOne(Wrappers.<NoteCon>lambdaQuery().eq(NoteCon::getUserId,noteCon.getUserId()).eq(NoteCon::getNoteId,noteCon.getNoteId()))!=null)
        {
            return new Result<>().ok("已订阅");
        }
        try{
            noteConMapper.insert(noteCon);
        }catch (Exception e)
        {
            return new Result<>().error(400,"订阅失败");
        }
        return new Result<>().ok("订阅成功");
    }

    @Override
    public Result unsubscirbe(Integer id) {
        NoteCon noteCon=new NoteCon();
        noteCon.setUserId(ThreadLocalUtil.getUser().getId());
        noteCon.setNoteId(id);
        noteConMapper.delete(Wrappers.<NoteCon>lambdaQuery().eq(NoteCon::getUserId,noteCon.getUserId()).eq(NoteCon::getNoteId,noteCon.getNoteId()));
        return new Result<>().ok("取消订阅成功");
    }

    @Override
    public Result sublist() {
        List<NoteVo> list=noteMapper.sublist(ThreadLocalUtil.getUser().getId());
        //System.out.println(list);
        return new Result<>().ok(list);
    }


    private static class AsynSubmit extends Thread{
        private Note note;
        private NoteMapper noteMapper;
        private NoteConMapper noteConMapper;
        private AsynSubmit(){}

        public AsynSubmit(Note note,NoteMapper noteMapper,NoteConMapper noteConMapper){
            this.note=note;
            this.noteMapper=noteMapper;
            this.noteConMapper=noteConMapper;
        }
        @Override
        public void run(){
            noteMapper.insert(note);
            NoteCon noteCon=new NoteCon();
            noteCon.setNoteId(note.getId());
            noteCon.setUserId(note.getAuthor());
            noteConMapper.insert(noteCon);
        }
    }

    private void clearCache(String key)
    {
        stringRedisTemplate.opsForValue().getOperations().delete(key);
    }
}
