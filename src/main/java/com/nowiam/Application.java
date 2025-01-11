package com.nowiam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nowiam.mapper")
public class Application {
    public static void main( String[] args ){
        SpringApplication.run(Application.class,args);
    }
}
