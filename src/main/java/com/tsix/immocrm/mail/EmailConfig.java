package com.tsix.immocrm.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Active l'envoi d'email et le fil d'exécution qui le porte.
 *
 * <p>{@code @EnableAsync} est introduit ici et n'existait nulle part ailleurs dans le projet :
 * c'est cette feature qui rend l'application asynchrone, et le point d'entrée doit rester
 * lisible depuis le package qui en a besoin.
 *
 * <p><b>Conséquence assumée du bean {@code mailExecutor}</b> : déclarer un {@code Executor}
 * fait reculer l'autoconfiguration Spring Boot qui aurait fourni {@code applicationTaskExecutor}.
 * C'est sans effet ici — aucun contrôleur ne renvoie de {@code Callable},
 * {@code DeferredResult}, {@code SseEmitter} ni {@code StreamingResponseBody}, donc rien
 * d'autre ne consomme cet exécuteur. Si un jour un endpoint devient asynchrone (un export
 * volumineux, typiquement), il faudra lui rendre le sien plutôt que partager celui-ci.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

    /**
     * Petit pool dédié, aux fils nommés {@code mail-}. Le dimensionnement est délibérément
     * modeste : la population réelle est de quinze comptes actifs et les envois sont des
     * événements ponctuels (création de compte, réinitialisation), pas un flux.
     *
     * <p>Les fils nommés sont le vrai apport : une trace d'erreur SMTP doit désigner le mail
     * sans qu'on ait à remonter la pile pour comprendre d'où elle vient.
     */
    @Bean("mailExecutor")
    public TaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        // À l'arrêt, laisser partir ce qui est en vol : un mot de passe temporaire à moitié
        // envoyé n'est renvoyable par personne, l'utilisateur ne le connaît pas encore.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
