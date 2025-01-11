package com.nowiam.model.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nowiam.mapper.NoteConMapper;
import com.nowiam.mapper.NoteMapper;
import com.nowiam.model.pojo.Note;
import com.nowiam.model.pojo.NoteCon;
import org.springframework.beans.factory.annotation.Autowired;

public class NoteDelTask implements Runnable{
    private Integer id;
    private Integer userId;
    private Note note;
    private NoteMapper noteMapper;
    private NoteConMapper noteConMapper;

    public NoteDelTask(Integer id,Integer userId, NoteMapper noteMapper, NoteConMapper noteConMapper) {
        this.id=id;
        this.userId=userId;
        this.noteMapper = noteMapper;
        this.noteConMapper = noteConMapper;
    }

    @Override
    public void run() {
        //noteMapper.deleteById(Wrappers.<Note>lambdaQuery().eq(Note::getAuthor, userId).eq(Note::getId,id));

        try {
            noteMapper.delById(userId,id);
            noteConMapper.delete(Wrappers.<NoteCon>lambdaQuery().eq(NoteCon::getNoteId,id).eq(NoteCon::getUserId,userId));
        }catch (Exception e)
        {
            System.out.println("删除发生异常: 日程id--"+id+"   用户id---"+userId);
        }

    }
}
