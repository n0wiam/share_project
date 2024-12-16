package com.nowiam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nowiam.annotation.AutoCache;
import com.nowiam.model.pojo.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    @AutoCache(value = "NOTE_LIST",ops = 1)
    public List<Note> myList(@Param("userId") Integer userId,@Param("status") Integer status);

    @AutoCache(value = "SHARE_LIST")
    public List<Note> shareList(@Param("list") List<Integer> list);
}
