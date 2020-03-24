package com.hong.limit;

import com.hong.limit.core.RedisRateLimiter;
import com.hong.limit.service.IRedisService;
import com.hong.limit.service.LimitService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MainTest {

    @Autowired
    private IRedisService redisService;

    @Autowired
    private RedisRateLimiter redisRateLimiter;

   @Autowired
   private LimitService limitService;

    @Test
    public void testRedis() {
        redisService.set("user", "123", 10);
        System.out.println(redisService.getStr("user"));
        Map<String,Object> map = new HashMap<>();
        map.put("name","wanghong");
        map.put("age","30");
        redisService.hMSet("user:123",map);
    }

    @Test
    public void initLimiter(){
        String apps = "rate-limiter";
        String key = RedisRateLimiter.RATE_LIMITER_KEY_PREFIX + "order:add";
        System.out.println(limitService.init(key,apps));

        List<String> fields = new ArrayList<>();
        fields.add("rate");
        fields.add("app");
        List list = redisService.hMGet(key,fields);
        System.out.println(list);
    }

    @Test
    public void testLimiter() throws Exception{
        String key = "order:add";
        String context = "rate-limiter";
        BenchmarkCallback task = () -> {
            System.out.println(Thread.currentThread().getName() + "获取令牌:" + redisRateLimiter.acquire(context,key));
        };
        Benchmark benchmark = new Benchmark(10,task);
        benchmark.test();
    }

}
