package com.nowiam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nowiam.model.Result;
import com.nowiam.model.dto.NoteDto;
import com.nowiam.model.pojo.Note;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;

public interface NoteService extends IService<Note> {
    Result submit(NoteDto noteDto) throws MQBrokerException, RemotingException, InterruptedException, MQClientException;

    Result deleteById(Integer id);

    Result mylist();

    Result shareList();
    Result subscirbe(Integer id);

    Result unsubscirbe(Integer id);

    Result sublist();
}
