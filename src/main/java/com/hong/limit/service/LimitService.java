package com.hong.limit.service;

import com.google.common.collect.ImmutableList;
import com.hong.limit.core.RedisRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * @Description:
 * @Author wanghong
 * @Date 2020/3/24 13:12
 * @Version V1.0
 **/
@Component
public class LimitService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisScript<Long> rateLimiterLua;

    public long init (String key,String context){
        return stringRedisTemplate.execute(rateLimiterLua, ImmutableList.of(key), RedisRateLimiter.RATE_LIMITER_INIT_METHOD, "100", "100",context);
    }
}
