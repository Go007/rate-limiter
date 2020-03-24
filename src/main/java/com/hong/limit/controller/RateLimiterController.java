package com.hong.limit.controller;

import com.hong.limit.form.RateLimiterForm;
import com.hong.limit.service.RateLimiterService;
import com.hong.limit.vo.RateLimiterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description:  Redis 令牌桶限流 元数据配置管理
 * @Author wanghong
 * @Date 2020/3/24 15:59
 * @Version V1.0
 **/
@RestController
public class RateLimiterController {

    @Autowired
    private RateLimiterService rateLimiterService;

    @RequestMapping(value = "/rate-limiters")
    public List<RateLimiterVo> getRateLimiters(@RequestParam String context) {
        return rateLimiterService.getRateLimiters(context);
    }

    @RequestMapping(value = "/rate-limiters", method = RequestMethod.POST)
    public void saveOrUpdateRateLimiter(@RequestBody RateLimiterForm form) {
        rateLimiterService.saveOrUpdateRateLimiter(form);
    }


    @RequestMapping(value = "/rate-limiters/{context}/{name}", method = RequestMethod.DELETE)
    public void deleteRateLimiter(@PathVariable String context, @PathVariable String name) {
        rateLimiterService.deleteRateLimiter(context, name);
    }

}
