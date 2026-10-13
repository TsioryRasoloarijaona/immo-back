package com.tsix.immocrm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Tâches planifiées des notifications (ping SSE, résumé) dans tous les modes, pas seulement en deux bases ; les
 * autres {@code @Scheduled} (synchro, trace) restent conditionnés à la source de vérité. Exécuteur propre.
 */
@Configuration
@EnableScheduling
public class NotificationsConfig {

    @Bean("notificationExecutor")
    public TaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-");
        executor.initialize();
        return executor;
    }
}
