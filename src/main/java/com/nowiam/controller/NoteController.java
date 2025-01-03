package com.nowiam.controller;

import com.nowiam.model.Result;
import com.nowiam.model.dto.NoteDto;
import com.nowiam.service.NoteService;
import org.apache.ibatis.annotations.Param;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/note")
public class NoteController {
    @Autowired
    NoteService noteService;

    @PostMapping("/submit")
    public Result submit(@RequestBody NoteDto noteDto) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        return noteService.submit(noteDto);
    }

    @GetMapping("/delete/{id}")
    public Result deleteById(@PathVariable("id") Integer id){
        return noteService.deleteById(id);
    }

    @GetMapping("/share")
    public Result shareList(){
        return noteService.shareList();
    }

    @GetMapping("/list")
    public Result mylist(){
        return noteService.mylist();
    }
    @GetMapping("/subscribe/{id}")
    public Result subscribe(@PathVariable("id") Integer id){
        return noteService.subscirbe(id);
    }

    @GetMapping("/unsubscribe/{id}")
    public Result unsubscribe(@PathVariable("id") Integer id){
        return noteService.unsubscirbe(id);
    }

    @GetMapping("/sublist")
    public Result sublist(){
        return noteService.sublist();
    }
}
