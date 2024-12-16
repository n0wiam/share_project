package com.nowiam.annotation;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented

public @interface AutoCache {
    String value();//缓存前缀
    int ops() default -1;//是否需要状态区分，填入区分参数的下标

}
