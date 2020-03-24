package com.hong.limit.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hong.limit.core.RedisRateLimiter;
import com.hong.limit.domain.RateLimiterInfo;
import com.hong.limit.form.RateLimiterForm;
import com.hong.limit.vo.RateLimiterVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @Author wanghong
 * @Date 2020/3/24 16:00
 * @Version V1.0
 **/
@Slf4j
@Service
public class RateLimiterService implements InitializingBean {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisScript<Integer> rateLimiterLua;


    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);


    private String getKey(String key) {
        return RedisRateLimiter.RATE_LIMITER_KEY_PREFIX + key;
    }

    public List<RateLimiterVo> getRateLimiters(String context) {
        // 这里模拟从数据库取数据
        List<RateLimiterInfo> rateLimiterInfoList = new ArrayList<>();

        List<Object> rateLimiterListFromRedis = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
            for (RateLimiterInfo rateLimiterInfo : rateLimiterInfoList) {
                stringRedisConn.hGetAll(getKey(rateLimiterInfo.getName()));
            }
            return null;
        });


        List<RateLimiterVo> rateLimiterRespList = Lists.newArrayListWithCapacity(rateLimiterInfoList.size());

        for (int i = 0; i < rateLimiterListFromRedis.size(); i++) {
            Object object = rateLimiterListFromRedis.get(i);
            RateLimiterInfo rateLimiterInfo = rateLimiterInfoList.get(i);
            HashMap<String, String> rateLimiterMap = (HashMap<String, String>) object;
            rateLimiterRespList.add(RateLimiterVo.builder()
                    .name(rateLimiterInfo.getName())
                    .apps(rateLimiterMap.get("app"))
                    .maxPermits(Integer.parseInt(rateLimiterMap.get("max_permits")))
                    .currPermits(Integer.parseInt(rateLimiterMap.get("curr_permits")))
                    .rate(Integer.parseInt(rateLimiterMap.get("rate")))
                    .lastPermitTimestamp(rateLimiterMap.get("last_mill_second"))
                    .build());
        }
        return rateLimiterRespList;
    }


    public void saveOrUpdateRateLimiter(RateLimiterForm form) {
        // 这里模拟从数据库取数据
        RateLimiterInfo rateLimiterInfo = new RateLimiterInfo();
        String apps = form.getContext();
        if (rateLimiterInfo != null) {
            Set<String> contexts = Sets.newHashSet(rateLimiterInfo.getApp().split(","));
            if (!contexts.contains(form.getContext())) {
                contexts.add(form.getContext());
            }
            apps = StringUtils.join(contexts, ",");
        }

        //rateLimiterInfoMapper.saveOrUpdate(form.getName(), apps, form.getMaxPermits(), form.getRate());
        stringRedisTemplate.execute(rateLimiterLua,
                ImmutableList.of(getKey(form.getName())),
                RedisRateLimiter.RATE_LIMITER_INIT_METHOD, form.getMaxPermits() + "", form.getRate() + "", apps);
    }


    public void deleteRateLimiter(String context, String name) {
        // 这里模拟从数据库取数据
        RateLimiterInfo rateLimiterInfo = new RateLimiterInfo();
        if (rateLimiterInfo != null) {
            Set<String> contexts = Sets.newHashSet(rateLimiterInfo.getApp().split(","));
            if (contexts.contains(context)) {
                contexts.remove(context);
            }
            if (contexts.isEmpty()) {
              //  rateLimiterInfoMapper.deleteByName(name);
            } else {
              //  rateLimiterInfoMapper.saveOrUpdate(name, StringUtils.join(contexts, ","), rateLimiterInfo.getMaxPermits(), rateLimiterInfo.getRate());
            }
            stringRedisTemplate.execute(rateLimiterLua,
                    ImmutableList.of(getKey(name)),
                    RedisRateLimiter.RATE_LIMITER_INIT_METHOD, rateLimiterInfo.getMaxPermits().toString(), rateLimiterInfo.getRate().toString(), StringUtils.join(contexts, ","));
        }
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("diff db and redis job start.....");
                   // List<RateLimiterInfo> rateLimiterInfoList = rateLimiterInfoMapper.selectAll();
                    List<RateLimiterInfo> rateLimiterInfoList = new ArrayList<>();
                    for (RateLimiterInfo rateLimiterInfo : rateLimiterInfoList) {
                        stringRedisTemplate.execute(rateLimiterLua,
                                ImmutableList.of(getKey(rateLimiterInfo.getName())),
                                RedisRateLimiter.RATE_LIMITER_INIT_METHOD, rateLimiterInfo.getMaxPermits().toString(), rateLimiterInfo.getRate().toString(), rateLimiterInfo.getApp());
                    }
                    log.info("diff db and redis job end.....");
                } catch (Exception e) {
                    log.error("diff db and redis error.....", e);
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }
}

