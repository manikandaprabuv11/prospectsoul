package com.vyoog.prospectsoul_backend.verification.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Wiring for the verification module: its configuration properties, the
 * scheduler the worker's poll runs on, and the executor the after-commit kick
 * uses.
 *
 * <p>{@code @EnableScheduling} lives here rather than in {@code config/}
 * because this module is the first and only consumer of Spring scheduling; if
 * a second feature needs it, it moves up to an application-wide config class.
 */
@Configuration
@EnableConfigurationProperties({VerificationProperties.class, TwilioProperties.class})
@EnableScheduling
public class VerificationConfig {

    /**
     * A small pool: the worker's own pass is sequential, and this executor
     * exists only so the after-commit kick does not run on the request thread
     * that is already returning 202 to the browser.
     */
    @Bean(name = "verificationExecutor")
    public Executor verificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("verify-worker-");
        executor.initialize();
        return executor;
    }
}
