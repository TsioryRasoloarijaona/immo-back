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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/**
 * Écritures vers la source de vérité (OMRAN), lectures sur la copie locale, routées par le drapeau {@code readOnly}
 * de la transaction (FEATURE-source-de-verite-115.md). Inactive si {@code app.source-de-verite.host} est vide (tests) ;
 * {@code @ConditionalOnExpression} car la propriété, vide par défaut, est toujours « présente ».
 */
// @EnableScheduling ici : la seule tâche planifiée (synchro descendante) n'existe qu'en deux bases.
@Configuration
@EnableConfigurationProperties(SourceDeVeriteProperties.class)
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
@EnableScheduling
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    enum Cible { LECTURE, ECRITURE }

    /**
     * Hors transaction, {@code readOnly} vaut faux : source de vérité, lent mais jamais faux. L'inverse (une écriture
     * dans la copie) perdrait la ligne au rapatriement suivant, en silence. Une écriture marquée {@code readOnly} aussi.
     */
    static Cible cibleCourante() {
        Cible cible = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? Cible.LECTURE
                : Cible.ECRITURE;
        if (log.isDebugEnabled()) {
            // Seul moyen de voir une connexion empruntée avant la pose du drapeau readOnly (tout part alors sur la source).
            log.debug("Routage -> {} (transaction active: {}, nom: {})", cible,
                    TransactionSynchronizationManager.isActualTransactionActive(),
                    TransactionSynchronizationManager.getCurrentTransactionName());
        }
        return cible;
    }

    /** Obligatoire : publier un {@link DataSource} fait reculer l'autoconfiguration, qui emporte ce bean avec elle. */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties proprietesLecture() {
        return new DataSourceProperties();
    }

    @Bean("lecture")
    public DataSource lecture(@Qualifier("proprietesLecture") DataSourceProperties proprietes) {
        HikariDataSource ds = proprietes.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("hikari-lecture");
        return ds;
    }

    @Bean("ecriture")
    public DataSource ecriture(SourceDeVeriteProperties proprietes) {
        return poolSource(proprietes, proprietes.getTaillePool(), "hikari-source-de-verite");
    }

    /**
     * Réservé à {@link RemonteeLecture} : la recopie tourne en {@code afterCommit}, la requête tenant encore sa connexion
     * {@code ecriture} ; sur le même pool, N écritures simultanées s'attendraient jusqu'au timeout.
     */
    @Bean("remontee")
    public DataSource remontee(SourceDeVeriteProperties proprietes) {
        return poolSource(proprietes, proprietes.getTaillePoolRemontee(), "hikari-remontee");
    }

    private static HikariDataSource poolSource(SourceDeVeriteProperties proprietes, int taille, String nom) {
        proprietes.verifierReglagesPool();
        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                .url(proprietes.urlJdbc())
                .username(proprietes.getUsername())
                .password(proprietes.getPassword())
                .build();
        ds.setConnectionTimeout(proprietes.getTimeoutConnexionMs());
        ds.setMaximumPoolSize(taille);
        ds.setMinimumIdle(taille);
        ds.setKeepaliveTime(proprietes.getKeepaliveMs());
        ds.setMaxLifetime(proprietes.getDureeVieMaxMs());
        ds.setValidationTimeout(proprietes.getValidationTimeoutMs());
        // Sans -1, un pool jamais démarré renvoie l'erreur brute du pilote (08S01), indiscernable d'une coupure en
        // cours d'opération ; avec -1, Hikari répond SQLTransientConnectionException (503 SOURCE_INJOIGNABLE).
        ds.setInitializationFailTimeout(-1);
        ds.setPoolName(nom);
        return ds;
    }

    /**
     * {@link LazyConnectionDataSourceProxy} est la condition de fonctionnement : sans lui la connexion est empruntée
     * avant la pose du drapeau {@code readOnly}, et tout part sur la source, sans erreur.
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
        DataSource ecritureSuivie = new ConnexionSourceSuivie(ecriture);
        routage.setTargetDataSources(Map.of(Cible.LECTURE, lecture, Cible.ECRITURE, ecritureSuivie));
        routage.setDefaultTargetDataSource(ecritureSuivie);
        routage.afterPropertiesSet();

        if (log.isWarnEnabled()) {
            log.warn("Mode deux bases ACTIF : ecritures vers {}, lectures sur la base locale.",
                    proprietes.description());
        }

        // Défauts déclarés (Hikari : auto-commit ; SQL Server : READ COMMITTED) : sinon le proxy les sonde à chaque
        // getConnection tant qu'il ne les connaît pas, et hors transaction la sonde part vers la source.
        LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy();
        proxy.setTargetDataSource(routage);
        proxy.setDefaultAutoCommit(true);
        proxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        proxy.afterPropertiesSet();
        return proxy;
    }

    /** Seul accès en écriture à la copie : via l'aiguillage, une transaction d'écriture irait à la source. */
    @Bean
    public JdbcTemplate jdbcLecture(@Qualifier("lecture") DataSource lecture) {
        return new JdbcTemplate(lecture);
    }
}
