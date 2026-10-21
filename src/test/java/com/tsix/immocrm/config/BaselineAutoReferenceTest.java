package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Baseline d'une table qui se cite elle-même (BuyDocument : facture → sa commande). Avant la correction, la ligne qui
 * citait une commande placée plus loin faisait échouer la table à chaque cycle, au même endroit, sans jamais converger.
 * La « copie » est simulée : une ligne n'entre que si son parent y est déjà, comme sous une clé étrangère.
 */
class BaselineAutoReferenceTest {

    /** Une ligne et le parent qu'elle cite dans la même table (null : aucun). */
    private record Ligne(String id, String parent) {
    }

    private final Set<String> copie = new LinkedHashSet<>();

    private void inserer(Ligne l) {
        if (l.parent() != null && !copie.contains(l.parent())) {
            throw new DataIntegrityViolationException("FK " + l.id() + " -> " + l.parent());
        }
        copie.add(l.id());
    }

    /** Comme un batch JDBC sous SQL Server : chaque instruction passe ou non, l'échec est levé en fin de lot. */
    private void parLot(List<Ligne> lot) {
        DataIntegrityViolationException echec = null;
        for (Ligne l : lot) {
            try {
                inserer(l);
            } catch (DataIntegrityViolationException e) {
                echec = e;
            }
        }
        if (echec != null) {
            throw echec;
        }
    }

    @Test
    void uneLigneQuiCiteUnParentDUnLotSuivantPasseAuTourSuivant() {
        List<Ligne> lignes = List.of(new Ligne("facture", "commande"), new Ligne("a", null), new Ligne("commande", null));

        RemonteeLecture.fusionnerJusquaStabilite(lignes, 2, this::parLot, this::inserer);

        assertThat(copie).containsExactlyInAnyOrder("facture", "a", "commande");
    }

    @Test
    void uneChaineDansLeMemeLotEnOrdreInverseFinitParPasser() {
        List<Ligne> lignes = List.of(new Ligne("c", "b"), new Ligne("b", "a"), new Ligne("a", null));

        RemonteeLecture.fusionnerJusquaStabilite(lignes, 500, this::parLot, this::inserer);

        assertThat(copie).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void uneLigneDontLeParentNExisteNullePartFaitEchouerLaTableApresAvoirCopieLesAutres() {
        List<Ligne> lignes = List.of(new Ligne("orpheline", "absente"), new Ligne("a", null), new Ligne("b", "a"));

        assertThatThrownBy(() -> RemonteeLecture.fusionnerJusquaStabilite(lignes, 2, this::parLot, this::inserer))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("orpheline");
        assertThat(copie).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void uneConnexionPerdueNEstPasRejoueeLigneALigne() {
        List<String> appels = new ArrayList<>();
        Consumer<List<Ligne>> horsLigne = lot -> {
            appels.add("lot");
            throw new CannotGetJdbcConnectionException("copie injoignable");
        };
        Consumer<Ligne> parLigne = l -> appels.add("ligne");
        List<Ligne> lignes = List.of(new Ligne("a", null));

        assertThatThrownBy(() -> RemonteeLecture.fusionnerJusquaStabilite(lignes, 500, horsLigne, parLigne))
                .isInstanceOf(CannotGetJdbcConnectionException.class);
        assertThat(appels).containsExactly("lot");
    }

    @Test
    void unRefusPassagerEstRejoueEtLaTableAboutit() {
        List<Ligne> lignes = List.of(new Ligne("a", null), new Ligne("b", null));
        Set<String> dejaRefusee = new LinkedHashSet<>();
        Consumer<Ligne> verrouUneFois = l -> {
            if (dejaRefusee.add(l.id()) && "b".equals(l.id())) {
                throw new CannotAcquireLockException("verrou");
            }
            inserer(l);
        };

        RemonteeLecture.fusionnerJusquaStabilite(lignes, 500, lot -> {
            throw new CannotAcquireLockException("verrou");
        }, verrouUneFois);

        assertThat(copie).containsExactlyInAnyOrder("a", "b");
    }
}
