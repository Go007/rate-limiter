package com.hong.limit;

import com.hong.limit.service.IRedisService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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

    @Test
    public void testRedis(){
        redisService.set("user","123",10);
        System.out.println(redisService.getStr("user"));
    }

}
