package org.example.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Configuration
public class ThreadPoolConfig {
    // 自定义分块上传线程池
    @Bean(name = "uploadThreadPool")
    public ExecutorService uploadThreadPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("upload-chunk-thread-%d") // 线程名前缀（便于日志排查）
                .setDaemon(false) // 非守护线程（确保任务执行完）
                .build();

        return new ThreadPoolExecutor(
                20, // 核心线程数（根据CPU核心数调整，如CPU核心数*2）
                50, // 最大线程数
                60L, // 空闲线程存活时间（60秒）
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50), // 任务队列大小（避免任务堆积）
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略（任务满时直接拒绝，避免OOM）
        );
    }
}