package com.tsix.immocrm.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@code @EnableAsync} et {@code mailExecutor}. Ce bean fait reculer l'{@code applicationTaskExecutor} autoconfiguré :
 * un futur endpoint asynchrone (Callable, SseEmitter…) devra avoir son propre exécuteur, pas partager celui-ci.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

    /** Petit pool : envois ponctuels, pas un flux. Fils nommés {@code mail-} pour lire une trace SMTP sans la pile. */
    @Bean("mailExecutor")
    public TaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        // À l'arrêt, laisser partir ce qui est en vol : un mot de passe temporaire perdu, personne ne le connaît.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
