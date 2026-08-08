package com.xiantao.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class IRedisLock implements ILock{

    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final String ID_KEY_PREFIX = UUID.randomUUID().toString(true)+"-";
    private final StringRedisTemplate stringRedisTemplate;
    private final String name;
    private static DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public IRedisLock( String name,StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY_PREFIX + name, ID_KEY_PREFIX+Thread.currentThread().getId()+"", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(b);
    }

    @Override
    public void unlock() {
        String currentId = ID_KEY_PREFIX + Thread.currentThread().getId();
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_KEY_PREFIX+name),
                Collections.singletonList(currentId)
                );
    }
//    @Override
//    public void unlock() {
//        String currentId = ID_KEY_PREFIX + Thread.currentThread().getId();
//        String id = stringRedisTemplate.opsForValue().get(LOCK_KEY_PREFIX + name);
//        if (currentId.equals(id)) {
//            stringRedisTemplate.delete(ID_KEY_PREFIX + name);
//        }
//    }
}
