package com.hong.limit;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author wanghong
 * @date 2019/07/25 15:46
 * 压测工具类
 **/
public class Benchmark {

    private int CONCURRENCY_LEVEL;

    private CountDownLatch cdl;

    private BenchmarkCallback benchmarkCallback;

    public Benchmark(BenchmarkCallback benchmarkCallback){
        this(100,benchmarkCallback);
    }

    public Benchmark(int CONCURRENCY_LEVEL,BenchmarkCallback benchmarkCallback) {
        this.CONCURRENCY_LEVEL = CONCURRENCY_LEVEL;
        this.cdl = new CountDownLatch(CONCURRENCY_LEVEL);
        this.benchmarkCallback = benchmarkCallback;
    }

    public void test() throws IOException {
        long start = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
            // 多线程模拟客户端查询请求
            Thread thread = new Thread(() -> {
                try {
                     cdl.await();
                     benchmarkCallback.task();
                    System.out.println(Thread.currentThread().getName() + "执行");
                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + "线程执行出现异常:" + e.getMessage());
                }
            },"Thread-Benchmark-" + i);

            thread.start();
            cdl.countDown();
        }

        System.in.read();
        System.out.println("耗时：" + (System.currentTimeMillis() - start) + "ms");
    }

}
