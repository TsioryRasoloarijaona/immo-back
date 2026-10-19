package com.tsix.immocrm.config;

import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.event.spi.PostInsertEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FIX-rapatriement-synchrone.md. La suite n'a pas de source distincte : contexte minimal, {@link RemonteeLecture}
 * simulée, transaction simulée par les synchronisations de Spring.
 */
class RemonteeSynchroneTest {

    /** {@code @EnableAsync} présent comme dans l'application : un {@code @Async} oublié se verrait ici. */
    @Configuration
    @EnableAsync
    static class AvecAsync {
    }

    private AnnotationConfigApplicationContext contexte;
    private RemonteeLecture remontee;

    @BeforeEach
    void demarrer() {
        remontee = mock(RemonteeLecture.class);
        contexte = new AnnotationConfigApplicationContext();
        contexte.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("app.source-de-verite.host", "source.test")));
        contexte.register(AvecAsync.class);
        contexte.registerBean(RemonteeLecture.class, () -> remontee);
        contexte.register(RemonteeLectureListener.class);
        contexte.refresh();
    }

    @AfterEach
    void arreter() {
        contexte.close();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        CollecteurLignesEcrites.reinitialiser();
    }

    @Test
    void ecouteur_recopieDansLeFilAppelant_avantRetourDeLaPublication() {
        AtomicReference<Thread> fil = new AtomicReference<>();
        doAnswer(i -> {
            fil.set(Thread.currentThread());
            return null;
        }).when(remontee).remonter(any());

        contexte.publishEvent(new EcrituresValidees(List.of(new LigneEcrite("ComFolder", UUID.randomUUID()))));

        assertThat(fil.get()).as("recopie faite, et dans le fil de la requête").isSameAs(Thread.currentThread());
    }

    @Test
    void ecouteur_echecDeRecopie_neFaitPasEchouerLaRequete() {
        doThrow(new IllegalStateException("copie injoignable")).when(remontee).remonter(any());

        assertThatCode(() -> contexte.publishEvent(
                new EcrituresValidees(List.of(new LigneEcrite("ComFolder", UUID.randomUUID())))))
                .doesNotThrowAnyException();
    }

    /** De l'écouteur Hibernate à la recopie : ligne RDV + action « Visite », comme ProspectionService. */
    @Test
    void afterCommit_recopieSynchrone_admEventAvantComActionMessage() {
        List<List<LigneEcrite>> recopies = new ArrayList<>();
        AtomicReference<Thread> fil = new AtomicReference<>();
        doAnswer(i -> {
            fil.set(Thread.currentThread());
            recopies.add(List.copyOf(i.getArgument(0)));
            return null;
        }).when(remontee).remonter(any());

        UUID message = UUID.randomUUID();
        UUID rdv = UUID.randomUUID();
        CollecteurLignesEcrites collecteur = new CollecteurLignesEcrites(contexte);

        TransactionSynchronizationManager.initSynchronization();
        collecteur.onPostInsert(insertion(new ComActionMessage(), message));
        collecteur.onPostInsert(insertion(new AdmEvent(), rdv));
        List<TransactionSynchronization> synchronisations = TransactionSynchronizationManager.getSynchronizations();
        synchronisations.forEach(TransactionSynchronization::afterCommit);

        assertThat(fil.get()).isSameAs(Thread.currentThread());
        assertThat(recopies).containsExactly(List.of(
                new LigneEcrite("AdmEvent", rdv), new LigneEcrite("ComActionMessage", message)));
    }

    /** Une erreur SQL sur une ligne (contrainte…) n'est pas une coupure : les lignes suivantes sont tentées. */
    @Test
    void erreurSqlSurUneLigne_lesSuivantesSontTentees() throws Exception {
        Connection connexion = mock(Connection.class);
        PreparedStatement requete = mock(PreparedStatement.class);
        ResultSet vide = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(connexion.prepareStatement(anyString()))
                .thenThrow(new SQLException("violation de contrainte", "23000", 547))
                .thenReturn(requete);
        when(requete.executeQuery()).thenReturn(vide);
        when(vide.getMetaData()).thenReturn(meta);
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(connexion);
        DataSource copie = mock(DataSource.class);
        when(copie.getConnection()).thenReturn(connexion);

        new RemonteeLecture(new JdbcTemplate(copie), source).remonter(List.of(
                new LigneEcrite("ComFolder", UUID.randomUUID()),
                new LigneEcrite("ComFolder", UUID.randomUUID()),
                new LigneEcrite("ComFolder", UUID.randomUUID())));

        verify(source, times(3)).getConnection();
    }

    @Test
    void ordre_admEventAvantComActionMessage() {
        assertThat(CollecteurLignesEcrites.rang("AdmEvent"))
                .isLessThan(CollecteurLignesEcrites.rang("ComActionMessage"));
        assertThat(CollecteurLignesEcrites.rang("TableInconnue"))
                .isEqualTo(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size());
    }

    /** FEATURE-patrimoine-ecriture.md §2.1 / §13.2 : l'habilitation créée avec un programme ou une tranche remonte après eux. */
    @Test
    void ordre_admUserHabilitationConnueEtApresCfgTranche() {
        int rang = CollecteurLignesEcrites.rang("AdmUserHabilitation");

        assertThat(rang)
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(CollecteurLignesEcrites.rang("CfgTranche"));
    }

    @Test
    void poolRemontee_distinctDuPoolEcriture_taillePropre() {
        SourceDeVeriteProperties p = new SourceDeVeriteProperties();
        p.setHost("197.0.2.10");
        p.setNom("yesRems");
        p.setTaillePool(5);
        p.setTaillePoolRemontee(3);
        DataSourceConfig config = new DataSourceConfig();

        try (HikariDataSource remonteeDs = (HikariDataSource) config.remontee(p);
             HikariDataSource ecritureDs = (HikariDataSource) config.ecriture(p)) {
            assertThat(remonteeDs).isNotSameAs(ecritureDs);
            assertThat(remonteeDs.getPoolName()).isEqualTo("hikari-remontee");
            assertThat(remonteeDs.getMaximumPoolSize()).isEqualTo(3);
            assertThat(remonteeDs.getJdbcUrl()).isEqualTo(ecritureDs.getJdbcUrl());
            assertThat(remonteeDs.getInitializationFailTimeout()).isEqualTo(-1);
            assertThat(ecritureDs.getMaximumPoolSize()).isEqualTo(5);
        }
    }

    @Test
    void poolRemontee_defautDeux() {
        assertThat(new SourceDeVeriteProperties().getTaillePoolRemontee()).isEqualTo(2);
    }

    private static PostInsertEvent insertion(Object entite, UUID id) {
        PostInsertEvent evenement = mock(PostInsertEvent.class);
        when(evenement.getEntity()).thenReturn(entite);
        when(evenement.getId()).thenReturn(id);
        return evenement;
    }
}
