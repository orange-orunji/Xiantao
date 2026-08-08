package com.xiantao.config;

import com.xiantao.utils.LoginInterceptor;
import com.xiantao.utils.ReflashTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringRedisTemplate stringRedisTemplate;
    public WebMvcConfig(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //InterceptorRegistry注册器对象
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .excludePathPatterns("/user/login"
                        , "/user/code"
                        , "/goods/**"
                        , "/goods-type/**"
                        , "/note/hot"
                        , "/flash-sale/**"
                        , "/flash-order/seckill/**"
                        , "upload/**"
                        , "/doc.html"
                        , "/webjars/**"
                        , "/v3/api-docs/**"
                        , "/swagger-resources/**"
                        , "/swagger-ui/**"
                ).order(1);

        registry.addInterceptor(new ReflashTokenInterceptor(stringRedisTemplate))
                .excludePathPatterns("/doc.html"
                        , "/webjars/**"
                        , "/v3/api-docs/**"
                        , "/swagger-resources/**"
                        , "/swagger-ui/**"
                ).order(0);
    }
}
