package com.hong.limit.domain;

import lombok.Data;

import java.util.Date;

@Data
public class RateLimiterInfo {

    private Integer id;
    private String name;
    private String app;
    private Integer maxPermits;
    private Integer rate;
    private Date createdAt;
    private Date updatedAt;

}
