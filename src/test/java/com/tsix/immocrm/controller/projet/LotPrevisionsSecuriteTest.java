package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_BASE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_BASE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.ESTIMATION_BASE_HT;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.FIN_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.MODIFIABLE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.POINTS_BASE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.inchange;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.point;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotFiche.DROIT_ESTIMATION;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.DROIT_PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-lot-previsions.md §4.3 (ordre 404 → 403 → 400) et R1 / R2 : droit {@code ProjectPrevision.toolNew} pour écrire
 * et lire la saisie, {@code modifiable} du graphe ; champ « de base » modifié sans {@code ManualEstimationAdministrator}
 * → 403, identique → accepté. Lot de fixture : 01/01/2026 + 90 j sur les deux onglets, estimation de base 1000 / 200 / 1200.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsSecuriteTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String START_DATE = "StartDate";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;

    private PrevisionsLotSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private UUID lot;
    private int comptes;

    @BeforeEach
    void setUp() {
        appels = new PrevisionsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        projet = parite.projet("PVS-" + jeton, parite.entiteJuridique("PVE-" + jeton));
        statut = parite.statut(ENTREPRENEURS, "PVT-" + jeton);
        lot = lot(projet, "PVL");
    }

    private UUID lot(UUID projetDuLot, String code) {
        UUID id = parite.lot(projetDuLot, code + "-" + jeton, ENTREPRENEURS, statut);
        prev.planning(id, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(id, "1000", "200", "1200", "0.2");
        prev.retenues(id, "0", "0", "0");
        return id;
    }

    /** Compte non Administrateur, exempté de la clause de statut, habilité sur le projet, portant exactement ces droits. */
    private RequestPostProcessor avec(String... droits) {
        comptes++;
        return connecte(parite.avecDroits("lot.pv" + comptes + "." + jeton, List.of(droits), projet), COMMERCIAL);
    }

    private String debutStocke() {
        return String.valueOf(prev.lot(lot).get(START_DATE));
    }

    // ------------------------------------------------------------------ ordre : 404 d'abord

    @Test
    void ordre_lotInconnu_put_404_memeMessageQueLaFiche() throws Exception {
        RequestPostProcessor qui = avec(DROIT_PREVISIONS, DROIT_ESTIMATION);
        UUID inconnu = UUID.randomUUID();
        Reponse fiche = new AppelsProjet(mockMvc).lire(qui, AppelsProjet.LOTS + "/" + inconnu);

        Reponse r = appels.enregistrer(qui, inconnu, inchange());

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(detail(r)).isNotBlank().isEqualTo(detail(fiche));
    }

    // Lot d'un projet non habilité, compte sans aucun droit : 404, ni le droit ni le lot révélés.
    @Test
    void ordre_lotHorsPerimetre_compteSansDroit_put_404AvantLe403() throws Exception {
        UUID cache = lot(parite.projet("PVX-" + jeton, parite.entiteJuridique("PVY-" + jeton)), "PVH");
        RequestPostProcessor sansDroit = avec();
        Reponse fiche = new AppelsProjet(mockMvc).lire(sansDroit, AppelsProjet.LOTS + "/" + cache);

        Reponse r = appels.enregistrer(sansDroit, cache, inchange());

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(detail(r)).isNotBlank().isEqualTo(detail(fiche));
    }

    @Test
    void ordre_lotHorsPerimetre_compteSansDroit_saisie_404AvantLe403() throws Exception {
        UUID cache = lot(parite.projet("PVZ-" + jeton, parite.entiteJuridique("PVW-" + jeton)), "PVI");
        RequestPostProcessor sansDroit = avec();
        Reponse fiche = new AppelsProjet(mockMvc).lire(sansDroit, AppelsProjet.LOTS + "/" + cache);

        Reponse r = appels.saisie(sansDroit, cache);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(detail(r)).isNotBlank().isEqualTo(detail(fiche));
    }

    // ------------------------------------------------------------------ R1 : ProjectPrevision.toolNew

    @Test
    void r1_sansDroitPrevisions_put_403_rienEcrit() throws Exception {
        Reponse r = appels.enregistrer(avec(DROIT_ESTIMATION), lot, inchange().avec(DEBUT, LocalDate.of(2026, 2, 1)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(debutStocke()).isEqualTo(DEBUT_2026.toString());
        });
    }

    @Test
    void r1_sansDroitPrevisions_saisie_403() throws Exception {
        Reponse r = appels.saisie(avec(DROIT_ESTIMATION), lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r1_sansDroitPrevisions_graphe_200_nonModifiable() throws Exception {
        Reponse r = appels.graphe(avec(DROIT_ESTIMATION), lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.valeur(MODIFIABLE)).isEqualTo(false);
    }

    @Test
    void r1_avecDroitPrevisions_graphe_modifiable() throws Exception {
        Reponse r = appels.graphe(avec(DROIT_PREVISIONS), lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.valeur(MODIFIABLE)).isEqualTo(true);
    }

    @Test
    void r1_avecDroitPrevisions_saisie_200_modifiable() throws Exception {
        Reponse r = appels.saisie(avec(DROIT_PREVISIONS), lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.valeur(MODIFIABLE)).isEqualTo(true);
    }

    // ------------------------------------------------------------------ R2 : estimation de base

    private static CorpsJson baseModifiee(String champ) {
        CorpsJson corps = inchange();
        return switch (champ) {
            case DEBUT_BASE -> corps.avec(DEBUT_BASE, DEBUT_2026.plusDays(1));
            case DUREE_BASE -> corps.avec(DUREE_BASE, DUREE_90 - 1);
            case ESTIMATION_BASE_HT -> corps.avec(ESTIMATION_BASE_HT, new BigDecimal("1500"));
            default -> corps.avec(POINTS_BASE, List.of(point(DEBUT_2026, "0"), point(FIN_2026, "1")));
        };
    }

    @ParameterizedTest(name = "{0} modifié")
    @ValueSource(strings = {DEBUT_BASE, DUREE_BASE, ESTIMATION_BASE_HT, POINTS_BASE})
    void r2_baseModifieeSansDroitEstimation_403_rienEcrit(String champ) throws Exception {
        Reponse r = appels.enregistrer(avec(DROIT_PREVISIONS), lot, baseModifiee(champ).avec(DEBUT, LocalDate.of(2026, 2, 1)));

        Map<String, Object> enBase = prev.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(String.valueOf(enBase.get(START_DATE))).as("prévisions non écrites").isEqualTo(DEBUT_2026.toString());
            s.assertThat(String.valueOf(enBase.get("StartDateManual"))).isEqualTo(DEBUT_2026.toString());
            s.assertThat(prev.pointsBase(lot)).isEmpty();
        });
    }

    @Test
    void r2_baseIdentiqueSansDroitEstimation_200_previsionsEcrites() throws Exception {
        CorpsJson corps = inchange().avec(DEBUT, LocalDate.of(2026, 2, 1)).avec(DUREE, 60)
                .avec(ESTIMATION_BASE_HT, new BigDecimal("1000.000000"));

        Reponse r = appels.enregistrer(avec(DROIT_PREVISIONS), lot, corps);

        Map<String, Object> enBase = prev.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(String.valueOf(enBase.get(START_DATE))).isEqualTo("2026-02-01");
            s.assertThat(enBase).containsEntry("Delay", 60);
        });
    }

    @Test
    void r2_pointsBaseIdentiquesAuxStockes_sansDroitEstimation_200() throws Exception {
        prev.pointBase(lot, DEBUT_2026, "0", "0", "0", "0");
        prev.pointBase(lot, FIN_2026, "1", "1", "1", "1200");
        CorpsJson corps = inchange().avec(POINTS_BASE, List.of(point(DEBUT_2026, "0"), point(FIN_2026, "1")));

        Reponse r = appels.enregistrer(avec(DROIT_PREVISIONS), lot, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void r2_saisie_sansDroitEstimation_estimationDeBaseNonModifiable() throws Exception {
        Reponse r = appels.saisie(avec(DROIT_PREVISIONS), lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.valeur("$.estimationDeBaseModifiable")).isEqualTo(false);
    }

    @Test
    void r2_saisie_avecDroitEstimation_estimationDeBaseModifiable() throws Exception {
        Reponse r = appels.saisie(avec(DROIT_PREVISIONS, DROIT_ESTIMATION), lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.valeur("$.estimationDeBaseModifiable")).isEqualTo(true);
    }

    @Test
    void r2_baseModifieeAvecDroitEstimation_200_baseEcrite() throws Exception {
        Reponse r = appels.enregistrer(avec(DROIT_PREVISIONS, DROIT_ESTIMATION), lot, baseModifiee(DEBUT_BASE));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(String.valueOf(prev.lot(lot).get("StartDateManual"))).isEqualTo(DEBUT_2026.plusDays(1).toString());
    }

    // ------------------------------------------------------------------ ordre : 403 avant 400

    @Test
    void ordre_sansDroitPrevisions_corpsInvalide_403AvantLe400() throws Exception {
        Reponse r = appels.enregistrer(avec(DROIT_ESTIMATION), lot, inchange().avec(DUREE, -5));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void ordre_baseModifieeSansDroitEstimation_corpsInvalide_403AvantLe400() throws Exception {
        Reponse r = appels.enregistrer(avec(DROIT_PREVISIONS), lot, baseModifiee(ESTIMATION_BASE_HT).avec(DUREE, -5));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }
}
