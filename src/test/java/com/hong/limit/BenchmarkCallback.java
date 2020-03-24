package com.hong.limit;

import java.util.concurrent.ExecutionException;

/**
 * @author wanghong
 * @date 2019/07/25 15:55
 **/
@FunctionalInterface
public interface BenchmarkCallback {

    void task() throws ExecutionException, InterruptedException;

}
