package com.xiantao.config;

import io.lettuce.core.ReadFrom;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    /**
     * Redis 密码从配置/环境变量注入（spring.redis.password -> ${REDIS_PASSWORD}），禁止硬编码
     */
    @Value("${spring.redis.password}")
    private String redisPassword;

    /**
     * 配置Redisson
     * @return
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
//        改为虚拟机配置docker部署到虚拟机上
        config.useSingleServer().setAddress("redis://192.168.161.128:7000")
        .setPassword(redisPassword);
        return Redisson.create(config);
    }

    /**
     * 配置Redis 哨兵客户端
     * @return
     */
    //=========================================================
//    @Bean
//    public LettuceClientConfigurationBuilderCustomizer clientConfigurationBuilderCustomizer(){
//        return clientConfigurationBuilder -> clientConfigurationBuilder.readFrom(ReadFrom.REPLICA_PREFERRED);
//    }
//    ==========================================================================================
}
