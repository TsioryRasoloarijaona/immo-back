package com.tsix.immocrm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Revue FIX-rapatriement-synchrone : la recopie tourne désormais dans le fil de la requête, donc chaque attente du pool
 * {@code remontee} s'ajoute à la latence de la réponse HTTP.
 */
class RemonteeLatenceTest {

    @AfterEach
    void nettoyer() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * Pool {@code remontee} indisponible (épuisé ou coupure) : chaque {@code getConnection} coûte
     * {@code timeout-connexion-ms} (5 s). Une écriture de N lignes attend aujourd'hui N × 5 s avant de répondre,
     * alors que la première tentative a déjà prouvé que la source est injoignable.
     */
    @Test
    void sourceInjoignable_uneSeuleAttenteDeConnexionParRecopie() throws Exception {
        DataSource remontee = mock(DataSource.class);
        when(remontee.getConnection()).thenThrow(
                new SQLTransientConnectionException("hikari-remontee - Connection is not available, request timed out after 5000ms"));
        DataSource lecture = mock(DataSource.class);
        RemonteeLecture recopie = new RemonteeLecture(new JdbcTemplate(lecture), remontee);

        List<LigneEcrite> lignes = IntStream.range(0, 8)
                .mapToObj(i -> new LigneEcrite("ComFolder", UUID.randomUUID()))
                .toList();
        recopie.remonter(lignes);

        verify(lecture, never()).getConnection();
        verify(remontee, atMost(1)).getConnection();
    }

    /**
     * Constat (vert) : en {@code afterCommit}, la synchronisation est encore active, donc {@code DataSourceUtils} lie la
     * connexion {@code remontee} au fil — elle est tenue jusqu'à {@code afterCompletion}, pour toute la recopie, et non
     * rendue après chaque relecture comme le suppose la spec (« ne tient que remontee puis lecture »).
     */
    @Test
    void enAfterCommit_connexionRemonteeTenueJusquaAfterCompletion() throws Exception {
        DataSource remontee = mock(DataSource.class);
        Connection connexion = connexionVide();
        when(remontee.getConnection()).thenReturn(connexion);
        RemonteeLecture recopie = new RemonteeLecture(new JdbcTemplate(mock(DataSource.class)), remontee);

        TransactionSynchronizationManager.initSynchronization();
        recopie.remonter(List.of(new LigneEcrite("ComFolder", UUID.randomUUID()),
                new LigneEcrite("ComFolder", UUID.randomUUID()),
                new LigneEcrite("ComFolder", UUID.randomUUID())));

        verify(remontee, times(1)).getConnection();
        verify(connexion, never()).close();

        List<TransactionSynchronization> synchronisations = TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();
        synchronisations.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(connexion).close();
    }

    /** Lignes absentes de la source : la recopie n'écrit rien dans la copie, sauf les DELETE (non simulés ici). */
    private static Connection connexionVide() throws Exception {
        Connection connexion = mock(Connection.class);
        PreparedStatement requete = mock(PreparedStatement.class);
        ResultSet resultat = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(connexion.prepareStatement(anyString())).thenReturn(requete);
        when(requete.executeQuery()).thenReturn(resultat);
        when(resultat.next()).thenReturn(false);
        when(resultat.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(0);
        return connexion;
    }
}
