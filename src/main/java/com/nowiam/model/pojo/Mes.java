package com.nowiam.model.pojo;

public class Mes<T> {
    private String messageId;
    private T content;
    //get&set

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
}
