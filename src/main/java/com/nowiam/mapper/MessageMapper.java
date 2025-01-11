package com.nowiam.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageMapper{
    public Long check(@Param("id") String id);
    public Long insert(@Param("id") String id);
}
