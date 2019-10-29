package com.example.demo.spring.boot.config.util;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * This class defines the configuration for Spring's asynchronous execution
 * 
 * @author KunjPrasad
 *
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    // A thread pool task executor to serve asynchronous request
    @Bean
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // the setting below is made to enable testing behavior when a large work load is giveb
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("ThreadPoolTaskExecutor");
        executor.initialize();
        return executor;
    }
}
