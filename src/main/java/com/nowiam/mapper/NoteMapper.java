package com.nowiam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nowiam.annotation.AutoCaches;
import com.nowiam.model.pojo.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    @AutoCaches(value = "NOTE_LIST",ops = 1)
    public List<Note> myList(@Param("userId") Integer userId,@Param("status") Integer status);
}
