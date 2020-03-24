package com.hong.limit.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RateLimiterVo {
    private String name;
    private String apps;
    private Integer maxPermits;
    private Integer rate;
    private Integer currPermits;
    private String lastPermitTimestamp;

}
