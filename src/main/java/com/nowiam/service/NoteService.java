package com.nowiam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nowiam.model.Result;
import com.nowiam.model.dto.NoteDto;
import com.nowiam.model.pojo.Note;

public interface NoteService extends IService<Note> {
    Result submit(NoteDto noteDto);

    Result deleteById(Integer id);
}
