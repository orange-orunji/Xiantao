package com.xiantao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(proxyTargetClass = true,exposeProxy = true)
@MapperScan("com.xiantao.mapper")
@SpringBootApplication
public class XianTaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(XianTaoApplication.class, args);
    }


}
