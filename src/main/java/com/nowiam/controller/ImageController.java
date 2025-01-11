package com.nowiam.controller;

import com.nowiam.model.Result;
import com.nowiam.model.pojo.Image;
import com.nowiam.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/image")
public class ImageController {
    @Autowired
    ImageService imageService;
    @GetMapping("/alert/{id}")
    public Result alertImage(@PathVariable("id") Integer id)
    {
        return imageService.alert(id);
    }

    @GetMapping("/buy/{id}")
    public Result buyImage(@PathVariable("id") Integer id)
    {
        return imageService.buy(id);
    }

    @PostMapping("/admin")
    public Result adminUpdate(@RequestBody Image image)
    {
        return imageService.adminUpdate(image);
    }

    @GetMapping("/sale/{id}")
    public Result sale(@PathVariable("id") Integer id){
        return imageService.sale(id);
    }
    @GetMapping
    public Result show(){
        return imageService.show();
    }
}
