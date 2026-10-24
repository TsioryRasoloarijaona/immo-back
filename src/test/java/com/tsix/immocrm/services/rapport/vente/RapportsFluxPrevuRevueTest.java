package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.services.rapport.vente.flux.AnomaliesFlux;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Revue R1a : n° 5 « à ce jour » et n° 3 avec un prévu non vide (stub), spec §4.1, §4.4, R-R. */
class RapportsFluxPrevuRevueTest {

    private static final UUID TRANCHE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"),
            ZoneId.of("Africa/Casablanca"));

    private static ContexteRapport contexte(String annee) {
        Map<String, List<String>> valeurs = annee == null ? Map.of() : Map.of("annee", List.of(annee));
        return new ContexteRapport(UUID.randomUUID(), HORLOGE, valeurs, List.of(TRANCHE), List::of);
    }

    private static FluxVenteMensuel flux(FluxMensuel resultat) {
        FluxVenteMensuel flux = mock(FluxVenteMensuel.class);
        when(flux.calculer(any())).thenReturn(resultat);
        when(flux.remises(anyList())).thenReturn(BigDecimal.ZERO);
        return flux;
    }

    private static ValorisationTranche stockVide() {
        ValorisationTranche valorisation = mock(ValorisationTranche.class);
        when(valorisation.pour(anyList())).thenReturn(new ValorisationTranche.Valorisations(List.of(), List.of()));
        return valorisation;
    }

    private static Map<String, Object> ligne(ResultatRapport r, String section, String cle, String valeur) {
        SectionRapport s = r.sections().stream().filter(x -> x.id().equals(section)).findFirst().orElseThrow();
        return s.lignes().stream().filter(l -> valeur.equals(l.get(cle))).findFirst().orElseThrow();
    }

    @Test
    void n5_aCeJour_anneeCourante_prorataDuMoisCourant_entierPourLesQuantites() {
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        for (int mois = 1; mois <= 10; mois++) {
            cumul.ajouter(MesureFlux.PREVU_CONCRETISATION_QUANTITE, new CleFlux(TRANCHE, 2026, mois), BigDecimal.TEN);
        }
        cumul.ajouter(MesureFlux.PREVU_ENCAISSEMENT, new CleFlux(TRANCHE, 2026, 10), new BigDecimal("31000"));
        cumul.ajouter(MesureFlux.ENCAISSEMENTS, new CleFlux(TRANCHE, 2026, 2), new BigDecimal("11500"));
        ActiviteParMoisRapport rapport = new ActiviteParMoisRapport(flux(cumul.flux(AnomaliesFlux.AUCUNE)), stockVide(),
                HORLOGE);

        ResultatRapport r = rapport.executer(contexte("2026"));

        Map<String, Object> quantite = ligne(r, "a-ce-jour", "mesure", "Concrétisation quantité");
        // 9 mois passés × 10 + 10 × 23 / 31 = 7,42 tronqué à 7
        assertThat((BigDecimal) quantite.get("prevuACeJour")).isEqualByComparingTo("97");
        Map<String, Object> encaissements = ligne(r, "a-ce-jour", "mesure", "Encaissements (*)");
        assertThat((BigDecimal) encaissements.get("prevuACeJour")).isEqualByComparingTo("23000");
        assertThat((BigDecimal) encaissements.get("taux")).isEqualByComparingTo("50");
    }

    @Test
    void n5_aCeJour_autreAnnee_prevuDeLAnnee() {
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        cumul.ajouter(MesureFlux.PREVU_ENCAISSEMENT, new CleFlux(TRANCHE, 2025, 12), new BigDecimal("1000"));
        ActiviteParMoisRapport rapport = new ActiviteParMoisRapport(flux(cumul.flux(AnomaliesFlux.AUCUNE)), stockVide(),
                HORLOGE);

        ResultatRapport r = rapport.executer(contexte("2025"));

        assertThat((BigDecimal) ligne(r, "a-ce-jour", "mesure", "Encaissements (*)").get("prevuACeJour"))
                .isEqualByComparingTo("1000");
    }

    @Test
    void n3_moisAvecSeulementUnPrevu_presentDansSaSectionSeulement() {
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        cumul.ajouter(MesureFlux.PREVU_CA_TTC, new CleFlux(TRANCHE, 2025, 3), new BigDecimal("500"));
        ComparaisonVentesRapport rapport = new ComparaisonVentesRapport(flux(cumul.flux(AnomaliesFlux.AUCUNE)), HORLOGE);

        ResultatRapport r = rapport.executer(contexte(null));

        Map<String, Object> mars = ligne(r, "chiffre-affaires", "mois", "Mars 2025");
        assertThat((BigDecimal) mars.get("prevision")).isEqualByComparingTo("500");
        assertThat((BigDecimal) mars.get("reel")).isEqualByComparingTo("0");
        assertThat(r.sections().stream().filter(s -> s.id().equals("encaissements")).findFirst().orElseThrow()
                .lignes()).isEmpty();
    }
}
