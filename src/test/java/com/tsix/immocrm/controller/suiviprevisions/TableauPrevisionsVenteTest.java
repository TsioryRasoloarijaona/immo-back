package com.tsix.immocrm.controller.suiviprevisions;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureSuiviPrevisions;
import com.tsix.immocrm.services.rapport.vente.flux.AnomaliesFlux;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.SourcePrevu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.ANNEE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MONTANTS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.QUANTITES;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.RACINE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.REEL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.colonne;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.lignes;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.mois;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.nomsDesColonnes;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.requete;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.section;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.totaux;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.valeur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Gabarit de 12 mois {@code TableauPrevisionsVente} (FEATURE-tdb-vente-previsions.md §3.4, §9.2 R-P6, §9.1 R-P5), lu par
 * la section {@code quantites} de l'écran. Tranche T, 2025 : concrétisations nettes 4, 4, 8, 5, 3, 1, 3, 2, 1, 2, 0, 0
 * (Σ 33), le 10 de chaque mois. Prévu : {@link SourcePrevu} remplacé (par défaut « non saisi », comme {@code PrevuNonSaisi}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TableauPrevisionsVenteTest {

    private static final int AN = 2025;
    private static final int[] PAR_MOIS = {4, 4, 8, 5, 3, 1, 3, 2, 1, 2, 0, 0};
    private static final List<String> LIBELLES = List.of("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet",
            "Août", "Septembre", "Octobre", "Novembre", "Décembre");
    private static final String PREVU = "prevu";
    private static final String BASE = "base";
    private static final String ANCIEN_PREVU = "ancienPrevu";
    private static final String CUMUL_PREVU = "cumulPrevu";
    private static final String CUMUL_REEL = "cumulReel";
    private static final String POURCENTAGE_REALISE = "pourcentageRealise";
    private static final String POURCENTAGE_CUMULE = "pourcentageCumule";
    private static final String RENTABILITE = "rentabilite";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureSuiviPrevisions sp;
    @Autowired private FixtureRapports rapports;
    @MockitoBean private SourcePrevu sourcePrevu;

    private SuiviPrevisionsSupport appels;
    private CfgTranche t;
    private RequestPostProcessor direction;

    @BeforeEach
    void setUp() {
        when(sourcePrevu.prevu(any())).thenReturn(FluxMensuel.vide(new AnomaliesFlux(0, null, List.of(), true)));
        appels = new SuiviPrevisionsSupport(mockMvc);
        Jeu jeu = fixture.jeu("TPV");
        t = jeu.tranche("T", jeu.projet("P", true));
        for (int m = 1; m <= 12; m++) {
            for (int n = 0; n < PAR_MOIS[m - 1]; n++) {
                sp.vente(jeu, t, LocalDate.of(AN, m, 10), null);
            }
        }
        AdmUser dg = rapports.compte("dg." + jeu.prefixe(), rapports.role("Direction Générale", false));
        direction = connecte(dg);
        fixture.relire();
    }

    private static MesureFlux mesure(String nom) {
        try {
            return MesureFlux.valueOf(nom);
        } catch (IllegalArgumentException absente) {
            throw new IllegalStateException("contrat absent : MesureFlux." + nom, absente);
        }
    }

    /** Prévu saisi de test : une valeur par (mesure, mois) sur T en 2025. */
    private void prevu(Map<String, Map<Integer, String>> valeurs) {
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        valeurs.forEach((nom, parMois) -> parMois.forEach((m, v) ->
                cumul.ajouter(mesure(nom), new CleFlux(t.getId(), AN, m), new BigDecimal(v))));
        when(sourcePrevu.prevu(any())).thenReturn(cumul.flux(AnomaliesFlux.AUCUNE));
    }

    private Map<String, Object> quantites() throws Exception {
        return section(appels.sections(requete(RACINE, direction, List.of(t), ANNEE, String.valueOf(AN))), QUANTITES);
    }

    // ------------------------------------------------------------------ R-P6 gabarit, prévu nul

    @Test
    void rP6_douzeLignes_libellesDesMoisEnFrancais_reelDuFlux() throws Exception {
        Map<String, Object> q = quantites();

        assertThat(lignes(q)).hasSize(12).extracting(l -> l.get("mois")).containsExactlyElementsOf(LIBELLES);
        SoftAssertions soft = new SoftAssertions();
        for (int m = 1; m <= 12; m++) {
            soft.assertThat(valeur(mois(q, m), REEL)).as("mois %d", m)
                    .isEqualByComparingTo(String.valueOf(PAR_MOIS[m - 1]));
        }
        soft.assertAll();
    }

    @Test
    void rP6_colonnesDuGabarit_typesEtTotaux() throws Exception {
        Map<String, Object> q = quantites();

        assertThat(nomsDesColonnes(q)).containsExactlyInAnyOrder("mois", BASE, ANCIEN_PREVU, PREVU, REEL, CUMUL_PREVU,
                CUMUL_REEL, POURCENTAGE_REALISE, POURCENTAGE_CUMULE, RENTABILITE);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonne(q, "mois")).containsEntry("type", "TEXTE");
        for (String c : List.of(BASE, ANCIEN_PREVU, PREVU, REEL)) {
            soft.assertThat(colonne(q, c)).as(c).containsEntry("type", "NOMBRE").containsEntry("fonctionTotal", "SOMME");
        }
        for (String c : List.of(CUMUL_PREVU, CUMUL_REEL)) {
            soft.assertThat(colonne(q, c)).as(c).containsEntry("type", "NOMBRE").containsEntry("fonctionTotal", "AUCUNE");
        }
        for (String c : List.of(POURCENTAGE_REALISE, POURCENTAGE_CUMULE, RENTABILITE)) {
            soft.assertThat(colonne(q, c)).as(c).containsEntry("type", "POURCENTAGE")
                    .containsEntry("fonctionTotal", "AUCUNE");
        }
        soft.assertAll();
    }

    @Test
    void rP6_cumulReelDeMars16_rentabiliteDeMars24virgule24_totalReel33() throws Exception {
        Map<String, Object> q = quantites();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(q, 3), CUMUL_REEL)).isEqualByComparingTo("16");
        soft.assertThat(valeur(mois(q, 12), CUMUL_REEL)).isEqualByComparingTo("33");
        soft.assertThat(valeur(mois(q, 3), RENTABILITE)).isEqualByComparingTo("24.24");
        soft.assertThat(valeur(mois(q, 11), RENTABILITE)).isEqualByComparingTo("0");
        soft.assertThat(valeur(totaux(q), REEL)).isEqualByComparingTo("33");
        soft.assertAll();
    }

    @Test
    void rP6_prevuNul_pourcentagesRealiseEtCumuleNullPartout_basePrevuA0() throws Exception {
        Map<String, Object> q = quantites();

        assertThat(lignes(q)).hasSize(12).allSatisfy(l -> {
            assertThat(l.get(POURCENTAGE_REALISE)).isNull();
            assertThat(l.get(POURCENTAGE_CUMULE)).isNull();
            assertThat(valeur(l, PREVU)).isEqualByComparingTo("0");
            assertThat(valeur(l, BASE)).isEqualByComparingTo("0");
            assertThat(valeur(l, ANCIEN_PREVU)).isEqualByComparingTo("0");
        });
    }

    @Test
    void rP6_pasDeTotalSurCumulsEtPourcentages() throws Exception {
        Map<String, Object> totaux = totaux(quantites());

        SoftAssertions soft = new SoftAssertions();
        for (String c : List.of(CUMUL_PREVU, CUMUL_REEL, POURCENTAGE_REALISE, POURCENTAGE_CUMULE, RENTABILITE)) {
            soft.assertThat(totaux.get(c)).as(c).isNull();
        }
        soft.assertAll();
    }

    // §3.4 : rentabilité null si Σ réel de l'année = 0 (aucun encaissement sur T)
    @Test
    void rP6_reelAnnuelNul_rentabiliteNullPartout() throws Exception {
        Map<String, Object> montants = section(appels.sections(requete(RACINE, direction, List.of(t), ANNEE,
                String.valueOf(AN))), MONTANTS);

        assertThat(lignes(montants)).hasSize(12).allSatisfy(l -> assertThat(l.get(RENTABILITE)).isNull());
    }

    // ------------------------------------------------------------------ R-P6 / R-P5 prévu de test

    @Test
    void rP6_prevuDeTest10EnMars_pourcentageRealiseDeMars80_cumuleDeMars160_nullAvant() throws Exception {
        prevu(Map.of("PREVU_CONCRETISATION_QUANTITE", Map.of(3, "10")));

        Map<String, Object> q = quantites();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(q, 3), POURCENTAGE_REALISE)).isEqualByComparingTo("80.00");
        soft.assertThat(valeur(mois(q, 3), POURCENTAGE_CUMULE)).as("16 ÷ 10").isEqualByComparingTo("160.00");
        soft.assertThat(valeur(mois(q, 12), POURCENTAGE_CUMULE)).as("33 ÷ 10").isEqualByComparingTo("330.00");
        soft.assertThat(mois(q, 2).get(POURCENTAGE_CUMULE)).as("cumul prévu de février nul").isNull();
        soft.assertThat(mois(q, 4).get(POURCENTAGE_REALISE)).as("prévu d'avril nul").isNull();
        soft.assertThat(valeur(mois(q, 3), CUMUL_PREVU)).isEqualByComparingTo("10");
        soft.assertAll();
    }

    @Test
    void rP5_sourcePrevuDeTest_basesAncienPrevuEtPrevuRefletes_quantitesEtMontants() throws Exception {
        prevu(Map.of("BASE_CONCRETISATION_QUANTITE", Map.of(3, "3"), "ANCIEN_PREVU_CONCRETISATION_QUANTITE", Map.of(3, "5"),
                "PREVU_CONCRETISATION_QUANTITE", Map.of(3, "10"), "BASE_ENCAISSEMENT", Map.of(5, "100"),
                "ANCIEN_PREVU_ENCAISSEMENT", Map.of(5, "200"), "PREVU_ENCAISSEMENT", Map.of(5, "300")));

        List<Map<String, Object>> sections = appels.sections(requete(RACINE, direction, List.of(t), ANNEE,
                String.valueOf(AN)));

        Map<String, Object> q = section(sections, QUANTITES);
        Map<String, Object> mt = section(sections, MONTANTS);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(q, 3), BASE)).isEqualByComparingTo("3");
        soft.assertThat(valeur(mois(q, 3), ANCIEN_PREVU)).isEqualByComparingTo("5");
        soft.assertThat(valeur(mois(q, 3), PREVU)).isEqualByComparingTo("10");
        soft.assertThat(valeur(mois(mt, 5), BASE)).isEqualByComparingTo("100");
        soft.assertThat(valeur(mois(mt, 5), ANCIEN_PREVU)).isEqualByComparingTo("200");
        soft.assertThat(valeur(mois(mt, 5), PREVU)).isEqualByComparingTo("300");
        soft.assertAll();
    }

    @Test
    void rP5_prevuSaisi_pasDAvertissementPrevuNonSaisi() throws Exception {
        prevu(Map.of("PREVU_CONCRETISATION_QUANTITE", Map.of(3, "10")));

        String corps = appels.corps(requete(RACINE, direction, List.of(t), ANNEE, String.valueOf(AN)));

        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        assertThat(avertissements).filteredOn(a -> a.startsWith("Prévu non saisi")).isEmpty();
    }
}
