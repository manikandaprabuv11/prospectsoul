package com.vyoog.prospectsoul_backend.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "importProcessingExecutor")
    public Executor importProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Tuned for large registry files. Each running import claims
        // one core thread; workers spawned within a batch (see
        // ImportProcessingService) run inside the same pool up to max.
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("import-proc-");
        executor.initialize();
        return executor;
    }
}
