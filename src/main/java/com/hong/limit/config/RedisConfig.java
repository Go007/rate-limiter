package com.hong.limit.config;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * @Author: wanghong
 * @Description: Redis配置类
 * @Date: 2020/2/12 16:17
 **/
@EnableCaching
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        //解决查询缓存转换异常的问题
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        //配置序列化(解决乱码的问题)
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ZERO)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> cacheName);

        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
        return cacheManager;
    }

    /**
     * redisTemplate相关配置
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer serializer = new StringRedisSerializer();
        //使用JSON格式的序列化,保存
        template.setKeySerializer(serializer);
        template.setHashKeySerializer(serializer);
       // template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
       // template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        return template;
    }

    /**
     * redis key生成策略
     * target: 类
     * method: 方法
     * params: 参数
     * @return KeyGenerator
     *
     * 注意: 该方法只是声明了key的生成策略,还未被使用,需在@Cacheable注解中指定keyGenerator
     *      如: @Cacheable(value = "key", keyGenerator = "cacheKeyGenerator")
     */
    @Bean
    public KeyGenerator cacheKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            //先将类的全限定名和方法名拼装在 key 中
            key.append(method.getDeclaringClass().getName()).append(".").append(method.getName()).append(":");
            if (params.length == 0) {
                return key.append("NO_PARAM").toString();
            }
            for (Object obj : params) {
                // 由于参数可能不同, hashCode肯定不一样, 缓存的key也需要不一样
                key.append(JSON.toJSONString(obj).hashCode());
            }

            return key.toString();
        };
    }

    @Bean
    public KeyGenerator simpleCacheKeyGenerator() {
        return (target, method, params) -> "";
    }

    @Bean
    public RedisScript<Long> rateLimiterLua() {
        DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript();
        // defaultRedisScript.setLocation(new ClassPathResource("classpath:rate_limiter.lua")); 这种方式会报错读取不到文件
        defaultRedisScript.setLocation(new ClassPathResource("rate_limiter.lua"));
        /**
         * 这里如果设置   defaultRedisScript.setResultType(Integer.class);会报如下异常：
         * io.lettuce.core.RedisException: java.lang.IllegalStateException
         * 解决办法：
         * 指定org.springframework.data.redis.connection.ReturnType为Long.class，注意这里不能使用Integer.class，因为ReturnType不支持。
         * 只支持List.class, Boolean.class和Long.class
         *
         * https://www.jianshu.com/p/b8f61421003d
         */
        defaultRedisScript.setResultType(Long.class);
        return defaultRedisScript;
    }

}
