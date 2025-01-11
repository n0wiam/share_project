package com.nowiam.service;

import com.nowiam.model.Result;
import com.nowiam.model.pojo.Image;

public interface ImageService {
    public Result alert(Integer id);

    Result buy(Integer id);

    Result adminUpdate(Image image);

    Result sale(Integer id);

    Result show();
}
