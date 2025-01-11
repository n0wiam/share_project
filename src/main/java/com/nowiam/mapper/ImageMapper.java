package com.nowiam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nowiam.model.pojo.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper extends BaseMapper<Image> {

    public boolean alert(@Param("userId")Integer userId, @Param("id") Integer id);

    List<Image> show();

}
