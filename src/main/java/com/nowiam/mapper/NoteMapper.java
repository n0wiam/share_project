package com.nowiam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nowiam.annotation.AutoCache;
import com.nowiam.model.pojo.Note;
import com.nowiam.model.vo.NoteVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    @AutoCache(value = "NOTE_LIST")
    public List<NoteVo> myList(@Param("userId") Integer userId);

    @AutoCache(value = "SHARE_LIST")
    public List<NoteVo> shareList(@Param("list") List<Integer> list);

    List<NoteVo> sublist(Integer id);

    void delById(@Param("userId")Integer userId,@Param("id")Integer id);
}
