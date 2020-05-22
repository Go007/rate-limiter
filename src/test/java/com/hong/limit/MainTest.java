package com.hong.limit;

import com.hong.limit.core.RedisRateLimiter;
import com.hong.limit.service.IRedisService;
import com.hong.limit.service.LimitService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author wanghong
 * @Date 2020/3/23 17:29
 * @Version V1.0
 **/
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MainTest {

    @Autowired
    private IRedisService redisService;

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Autowired
    private LimitService limitService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedis() {
        redisService.set("user", "123", 10);
        System.out.println(redisService.getStr("user"));
        Map<String, Object> map = new HashMap<>();
        map.put("name", "wanghong");
        map.put("age", "30");
        redisService.hMSet("user:123", map);
    }

    @Test
    public void initLimiter() {
        String apps = "order";
        String key = RedisRateLimiter.RATE_LIMITER_KEY_PREFIX + "order:add";
        System.out.println(limitService.init(key, apps, "2", "2"));

        List<String> fields = new ArrayList<>();
        fields.add("rate");
        fields.add("app");
        List list = redisService.hMGet(key, fields);
        System.out.println(list);
    }

    @Test
    public void testLimiter() throws Exception {
        // 预热下 stringRedisTemplate.execute()
        stringRedisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return null;
            }
        });
        String key = "order:add";
        String context = "order";
        BenchmarkCallback task = () -> {
            boolean acquire = redisRateLimiter.acquire(context, key);
            if (acquire) {
                log.info(Thread.currentThread().getName() + "获取令牌:" + acquire);
            }
            Thread.sleep(500);
        };
        Benchmark benchmark = new Benchmark(10, task);
        benchmark.test();
    }

    @Test
    public void testLimiter2() {
        // 预热下 stringRedisTemplate.execute()
        stringRedisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return null;
            }
        });

        String key = "order:add";
        String context = "order";
        long begin;
        for (int i = 0; i < 10; i++) {
            begin = System.currentTimeMillis();
            boolean acquire = redisRateLimiter.acquire(context, key);
            log.info("获取令牌：{},消耗时间：{}ms", acquire, System.currentTimeMillis() - begin);
            if (acquire) {
                try {
                    // 模拟业务耗时
                    Thread.sleep(500);
                    log.info("执行业务");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
