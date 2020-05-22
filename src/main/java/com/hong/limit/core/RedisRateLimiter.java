package com.hong.limit.core;

import com.google.common.collect.ImmutableList;
import com.hong.limit.config.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * @author wanghong
 * @date 2020/03/23 22:47
 * 基于Redis的分布式限流器
 **/
@Component
public class RedisRateLimiter {

    public static final String RATE_LIMITER_KEY_PREFIX = "rate_limiter:";
    public static final String RATE_LIMITER_INIT_METHOD = "init";
    public static final String RATE_LIMITER_DELETE_METHOD = "delete";
    public static final String RATE_LIMITER_ACQUIRE_METHOD = "acquire";

    private Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisScript<Long> rateLimiterLua;

    /**
     * 获取令牌，访问redis异常算做成功
     * 默认的permits为1
     *
     * @param context
     * @param key
     * @return
     */
    public boolean acquire(String context, String key) {
        Token token = acquireToken(context, key);
        return token.isPass() || token.isAccessRedisFail();
    }

    /**
     * 获取{@link Token}
     * 默认的permits为1
     *
     * @param context
     * @param key
     * @return
     */
    private Token acquireToken(String context, String key) {
        return acquireToken(context, key, 1);
    }

    /**
     * 获取{@link Token}
     *
     * @param context
     * @param key
     * @param permits
     * @return
     */
    private Token acquireToken(String context, String key, Integer permits) {
        Token token;
        try {
            /**
             * 这里细节需要注意：
             * 因为 Redis的限制(Lua中有写操作时不能使用带随机性质的读操作)，
             * 如果在Lua脚本中 调用redis.pcall('TIME')后，再HMSET，其他redis命令就执行不了,
             * 因此不能在Redis Lua中使用TIME获取时间戳，只好从应用获取然后传入。
             * 而我们的设计中依赖 时间戳去计算令牌，而在分布式环境下，无法保证
             * 各个实例服务器上的时间完全一致，如果实例服务器有较大时差(哪怕几十上百毫秒时差)，
             * 也会导致令牌计算有误，限流就会有问题。
             *
             * 实际执行中发现的问题：
             * 第一次执行 stringRedisTemplate.execute() 特别耗时，平均时间超过 1s，后面就平均几毫秒了
             */
            long begin = System.currentTimeMillis();
            Long currMillSecond = stringRedisTemplate.execute(new RedisCallback<Long>() {
                @Override
                public Long doInRedis(RedisConnection connection) throws DataAccessException {
                    return connection.time();
                }
            });
           // Long currMillSecond = System.currentTimeMillis();
            long end = System.currentTimeMillis();
            System.out.println("connection.time()耗时：" + (end - begin) + "ms");

            Long acquire = stringRedisTemplate.execute(rateLimiterLua, ImmutableList.of(RATE_LIMITER_KEY_PREFIX + key), RATE_LIMITER_ACQUIRE_METHOD, permits.toString(), currMillSecond.toString(), context);
            System.out.println("lua耗时：" + (System.currentTimeMillis() - end) + "ms");

            if (acquire == 1) {
                token = Token.PASS;
            } else if (acquire == -1) {
                token = Token.FUSING;
            } else {
                logger.error("no rate limit config for context={}", context);
                token = Token.NO_CONFIG;
            }
        } catch (Throwable e) {
            logger.error("get rate limit token from redis error,key=" + key, e);
            token = Token.ACCESS_REDIS_FAIL;
        }
        return token;
    }

}
