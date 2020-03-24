package com.hong.limit.domain;

import java.util.Date;

public class RateLimiterInfo {

    private Integer id;
    private String name;
    private String app;
    private Integer maxPermits;
    private Integer rate;
    private Date createdAt;
    private Date updatedAt;

    public RateLimiterInfo(Integer id, String name, String apps, Integer maxPermits, Integer rate, Date createdAt, Date updatedAt) {
        this.id = id;
        this.name = name;
        this.app = apps;
        this.maxPermits = maxPermits;
        this.rate = rate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public RateLimiterInfo() {
        super();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Integer getMaxPermits() {
        return maxPermits;
    }

    public void setMaxPermits(Integer maxPermits) {
        this.maxPermits = maxPermits;
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
