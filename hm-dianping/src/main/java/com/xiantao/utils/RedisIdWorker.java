package com.xiantao.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private final StringRedisTemplate stringRedisTemplate;
    private final static long BEGIN_TIMESTAMP = 1778670042L;
    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long second = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        String date = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
        //2.生成序列号
        Long along = stringRedisTemplate.opsForValue().increment("icr" + keyPrefix + date);
        //3.拼接并返回
        return second<<32 | along;
    }

}
