package com.unicauca.muisca.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Cola de ejecuciones en segundo plano. Por defecto un solo hilo: los motores ATL/Acceleo cargan
 * metamodelos en memoria y el plan gratuito de hosting tiene 512 MB; las demás esperan en cola.
 */
@Configuration
public class AsyncConfig {

    public static final String EXECUTION_EXECUTOR = "executionExecutor";

    @Bean(name = EXECUTION_EXECUTOR)
    public TaskExecutor executionExecutor(@Value("${app.executions.threads:1}") int threads,
            @Value("${app.executions.queue-capacity:50}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("muisca-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
