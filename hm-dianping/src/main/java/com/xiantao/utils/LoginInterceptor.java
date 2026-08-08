package com.xiantao.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xiantao.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginInterceptor implements HandlerInterceptor {
    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) throws Exception {
        UserHolder.removeUser();
    }

    @Override
    public boolean preHandle(HttpServletRequest request
            , @NonNull HttpServletResponse response
            , @NonNull Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            javax.servlet.http.Cookie[] cookies = request.getCookies();
            if(cookies != null){
                for(javax.servlet.http.Cookie cookie : cookies){
                    if("token".equals(cookie.getName())){
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        if(StrUtil.isBlank(token)){
            return true;
        }

        String key = com.xiantao.utils.RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> user = stringRedisTemplate.opsForHash().entries(key);
        if (user == null || user.isEmpty()) {
            return true;
        }

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(key, com.xiantao.utils.RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }
}
