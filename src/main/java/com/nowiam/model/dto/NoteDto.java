package com.nowiam.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;

public class NoteDto {
    private Integer status;
    private String content;

    private String type;

    //get&set

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
