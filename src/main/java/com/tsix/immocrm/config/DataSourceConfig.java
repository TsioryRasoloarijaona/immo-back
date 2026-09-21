package com.tsix.immocrm.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Sépare les lectures des écritures sur deux bases distinctes.
 *
 * <pre>
 *   ÉCRITURES  →  la source de vérité (OMRAN), seule base écrite, seule à frapper les numéros
 *   LECTURES   →  la copie locale du backend, rapide, éventuellement en retard de quelques minutes
 * </pre>
 *
 * <p>Voir {@link SourceDeVeriteProperties} pour le pourquoi : deux bases qui écrivent, ce sont
 * deux compteurs {@code AdmSequence} qui avancent séparément, donc des numéros de dossier
 * attribués deux fois. Une seule base écrite éteint le problème au lieu de le gérer.
 *
 * <h2>Cette configuration ne s'active pas toujours</h2>
 *
 * <p>{@code app.source-de-verite.host} vide — le cas de la suite de tests et de tout poste de
 * développement non configuré — et <b>rien de tout ceci n'existe</b> : l'autoconfiguration
 * Spring Boot reste seule en place, l'application est mono-base, son comportement est celui
 * d'avant cette feature. C'est ce qui permet aux 525 tests de passer sans modification.
 *
 * <p>{@code @ConditionalOnExpression} plutôt que {@code @ConditionalOnProperty} : la propriété
 * est <b>toujours présente</b> (avec une valeur vide par défaut, cf.
 * {@code application.properties}), et {@code @ConditionalOnProperty} considère qu'une propriété
 * présente-mais-vide est présente. Il faut donc tester le contenu, pas l'existence.
 *
 * <h2>Le routage repose sur une convention déjà tenue par le code</h2>
 *
 * <p>La clé de routage est le drapeau {@code readOnly} de la transaction courante. Les services
 * de ce dépôt portent déjà {@code @Transactional(readOnly = true)} en défaut de classe, surchargé
 * par un {@code @Transactional} nu sur les méthodes d'écriture — 48 sur 93. <b>Aucun service n'a
 * donc eu à être modifié.</b> {@code RoutageEcrituresTest} verrouille cette convention pour le
 * code futur : une méthode d'écriture annotée {@code readOnly} écrirait dans la copie, où sa
 * ligne serait écrasée au prochain rapatriement — sans erreur et sans trace.
 */
// @EnableScheduling ici et pas sur l'application : la seule tâche planifiée du dépôt est la
// synchronisation descendante, qui n'existe qu'en deux-bases. En mono-base, aucun scheduler.
@Configuration
@EnableConfigurationProperties(SourceDeVeriteProperties.class)
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
@EnableScheduling
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    /** Les deux destinations possibles d'une transaction. */
    enum Cible { LECTURE, ECRITURE }

    /**
     * <b>La règle de routage, en une ligne</b>, extraite pour être éprouvée sans base ni contexte
     * Spring (cf. {@code RoutageCibleTest}).
     *
     * <p>Hors transaction, {@code isCurrentTransactionReadOnly()} vaut {@code false} et l'on part
     * donc sur la source de vérité : lent, mais jamais faux. L'erreur inverse — servir une
     * écriture depuis la copie — perdrait la ligne au prochain rapatriement, en silence.
     */
    static Cible cibleCourante() {
        Cible cible = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? Cible.LECTURE
                : Cible.ECRITURE;
        if (log.isDebugEnabled()) {
            // Activer avec logging.level.com.tsix.immocrm.config.DataSourceConfig=DEBUG.
            // C'est le seul moyen de voir si une connexion est empruntee trop tot, cas ou le
            // drapeau readOnly n'est pas encore pose et ou tout part sur la source de verite.
            log.debug("Routage -> {} (transaction active: {}, nom: {})", cible,
                    TransactionSynchronizationManager.isActualTransactionActive(),
                    TransactionSynchronizationManager.getCurrentTransactionName());
        }
        return cible;
    }

    /**
     * Déclarer nos propres {@code DataSourceProperties} est obligatoire, pas décoratif :
     * en publiant un bean {@link DataSource}, on fait reculer {@code DataSourceAutoConfiguration}
     * — et celle-ci emporte avec elle le bean {@code DataSourceProperties} qu'elle fournissait.
     */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties proprietesLecture() {
        return new DataSourceProperties();
    }

    /** La copie locale : tout le trafic de consultation. */
    @Bean("lecture")
    public DataSource lecture(@Qualifier("proprietesLecture") DataSourceProperties proprietes) {
        HikariDataSource ds = proprietes.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("hikari-lecture");
        return ds;
    }

    /** La source de vérité : uniquement les écritures, et l'attribution des numéros. */
    @Bean("ecriture")
    public DataSource ecriture(SourceDeVeriteProperties proprietes) {
        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                .url(proprietes.urlJdbc())
                .username(proprietes.getUsername())
                .password(proprietes.getPassword())
                .build();
        ds.setConnectionTimeout(proprietes.getTimeoutConnexionMs());
        ds.setMaximumPoolSize(proprietes.getTaillePool());
        ds.setPoolName("hikari-source-de-verite");
        return ds;
    }

    /**
     * L'aiguillage, vu par JPA comme une source de données ordinaire.
     *
     * <p><b>{@link LazyConnectionDataSourceProxy} n'est pas une optimisation, c'est la condition
     * de fonctionnement.</b> Sans lui, Spring emprunte la connexion <i>avant</i> que la
     * transaction ne pose son drapeau {@code readOnly} ;
     * {@code determineCurrentLookupKey()} lit alors systématiquement {@code false} et
     * <b>tout part sur la source de vérité</b>, lectures comprises. Aucune exception, aucun
     * avertissement : seulement une application lente et un aiguillage qui n'aiguille rien.
     * Le proxy diffère l'emprunt jusqu'à la première instruction, donc après la pose du drapeau.
     *
     * <p>Hors transaction, on part sur la source de vérité : lent, mais jamais faux. L'erreur
     * inverse — servir une écriture depuis la copie — perdrait la ligne en silence.
     */
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("lecture") DataSource lecture,
                                 @Qualifier("ecriture") DataSource ecriture,
                                 SourceDeVeriteProperties proprietes) {

        AbstractRoutingDataSource routage = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return cibleCourante();
            }
        };
        routage.setTargetDataSources(Map.of(Cible.LECTURE, lecture, Cible.ECRITURE, ecriture));
        routage.setDefaultTargetDataSource(ecriture);
        routage.afterPropertiesSet();

        log.warn("Mode deux bases ACTIF : ecritures vers {}, lectures sur la base locale.",
                proprietes.description());

        return new LazyConnectionDataSourceProxy(routage);
    }

    /**
     * Le seul chemin par lequel le backend écrit dans la copie de lecture.
     *
     * <p>Il ne peut pas passer par l'aiguillage : une transaction d'écriture y est routée vers
     * la source de vérité, par construction. {@link RemonteeLecture} a donc besoin d'un accès
     * direct et nommé à la copie.
     *
     * <p>Aucun {@code JdbcTemplate} n'était injecté ailleurs dans le projet au moment d'écrire
     * ceci — vérifié — donc publier ce bean ne détourne aucun point d'injection existant.
     */
    @Bean
    public JdbcTemplate jdbcLecture(@Qualifier("lecture") DataSource lecture) {
        return new JdbcTemplate(lecture);
    }

    /**
     * Fils dédiés au rapatriement, nommés {@code remontee-}, sur le modèle de
     * {@code mailExecutor}. Un pool distinct évite qu'une source de vérité lente ne consomme
     * les fils de l'envoi d'email, et les fils nommés font qu'une trace d'erreur se lit sans
     * remonter la pile.
     *
     * <p>À l'arrêt, on n'attend pas : ce qui n'a pas été rapatrié sera repris par la
     * synchronisation périodique. C'est la différence avec {@code mailExecutor}, qui lui doit
     * finir ses envois — un mot de passe temporaire à moitié parti n'est renvoyable par personne.
     */
    @Bean("remonteeExecutor")
    public TaskExecutor remonteeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("remontee-");
        executor.initialize();
        return executor;
    }
}
