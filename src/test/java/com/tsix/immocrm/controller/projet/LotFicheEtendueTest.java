package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E1, R1, R2 — FEATURE-lot-fiche-parite.md : la fiche {@code GET /api/projet/lots/{id}} gagne en-tête (devise, catégorie,
 * estimation de base, délai), 4 dates, options, résiliation, autres. Valeurs posées en base par la fixture, relues sur le fil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotFicheEtendueTest {

    private static final LocalDate DEBUT_BASE = LocalDate.of(2026, 2, 2);
    private static final LocalDate FIN_BASE = LocalDate.of(2026, 8, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureProjetConfig config;

    private LotFicheSupport appels;
    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        projet = parite.projet("FEP-" + jeton, parite.entiteJuridique("FEE-" + jeton));
        lot = parite.lot(projet, "FEL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "FES-" + jeton));
        admin = connecte(parite.administrateur("lots.fe." + jeton, projet), "Administrateur");
    }

    private Reponse lire() throws Exception {
        Reponse r = appels.fiche(admin, lot);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static BigDecimal n(Reponse r, String champ) {
        return nombre(r.valeur("$." + champ));
    }

    // ------------------------------------------------------------------ en-tête

    @Test
    void e1_entete_devise_categorie_estimationDeBase_delaiPrevisionnel() throws Exception {
        UUID devise = fiche.devise("DEV-" + jeton);
        UUID categorie = config.categorie("FEC-" + jeton, false);
        fiche.entete(lot, devise, categorie, DEBUT_BASE, 180, FIN_BASE, 120);

        Reponse r = lire();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.deviseId")).isEqualTo(devise.toString());
            s.assertThat(r.valeur("$.deviseCode")).isEqualTo("DEV-" + jeton);
            s.assertThat(r.valeur("$.categorieId")).isEqualTo(categorie.toString());
            s.assertThat(r.valeur("$.categorieDescription")).isEqualTo("FEC-" + jeton);
            s.assertThat(r.valeur("$.dateDebutBase")).isEqualTo("2026-02-02");
            s.assertThat(n(r, "delaiBaseJours")).isEqualByComparingTo("180");
            s.assertThat(r.valeur("$.dateFinBase")).isEqualTo("2026-08-01");
            s.assertThat(n(r, "delaiPrevisionnelJours")).isEqualByComparingTo("120");
        });
    }

    // ------------------------------------------------------------------ Dates (R2)

    @Test
    void e1_r2_quatreDatesNouvelles_enLocalDate() throws Exception {
        fiche.datesNouvelles(lot, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 20), LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 12, 15));

        Reponse r = lire();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.dateReportAppelOffres")).isEqualTo("2026-01-05");
            s.assertThat(r.valeur("$.dateRapportDepouillement")).isEqualTo("2026-01-20");
            s.assertThat(r.valeur("$.dateAchevementTravaux")).isEqualTo("2026-11-30");
            s.assertThat(r.valeur("$.dateRecollement")).isEqualTo("2026-12-15");
        });
    }

    // ------------------------------------------------------------------ Options (R1)

    @Test
    void e1_r1_options_ratiosEnPourcentage0a100() throws Exception {
        UUID type = fiche.typePaiement("FET-" + jeton);
        fiche.options(lot, new BigDecimal("0.05"), new BigDecimal("0.3"), 42, type);

        Reponse r = lire();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(n(r, "variationMassePct")).as("MassChangeRatio 0,05").isEqualByComparingTo("5");
            s.assertThat(n(r, "tauxRemboursementApproPct")).as("StockPileRatio 0,3").isEqualByComparingTo("30");
            s.assertThat(n(r, "totalPoints")).isEqualByComparingTo("42");
            s.assertThat(r.valeur("$.typePaiementId")).isEqualTo(type.toString());
            s.assertThat(r.valeur("$.typePaiementLibelle")).isEqualTo("FET-" + jeton);
        });
    }

    @Test
    void e1_options_casesACocher() throws Exception {
        fiche.options(lot, BigDecimal.ZERO, BigDecimal.ZERO, 0, null);

        Reponse r = lire();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.remiseSurTravauxSupp")).isEqualTo(Boolean.TRUE);
            s.assertThat(r.valeur("$.retenueSurTravauxSupp")).isEqualTo(Boolean.TRUE);
            s.assertThat(r.valeur("$.saisieSurQuantites")).isEqualTo(Boolean.TRUE);
            s.assertThat(r.valeur("$.avancementParLigne")).isEqualTo(Boolean.TRUE);
        });
    }

    // ------------------------------------------------------------------ Résiliation, Autres

    @Test
    void e1_resiliation_et_autres() throws Exception {
        fiche.resiliationEtAutres(lot, LocalDate.of(2026, 6, 30), "Cause " + jeton, "Description longue " + jeton,
                "Notes " + jeton);

        Reponse r = lire();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.dateResiliation")).isEqualTo("2026-06-30");
            s.assertThat(r.valeur("$.causeResiliation")).isEqualTo("Cause " + jeton);
            s.assertThat(r.valeur("$.descriptionDetaillee")).isEqualTo("Description longue " + jeton);
            s.assertThat(r.valeur("$.notes")).isEqualTo("Notes " + jeton);
        });
    }

    // ------------------------------------------------------------------ V1 conservée

    @Test
    void e1_lotInvisible_404() throws Exception {
        UUID autre = parite.projet("FEX-" + jeton, null);
        UUID invisible = parite.lot(autre, "FEX-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "FEXS-" + jeton));

        assertThat(appels.fiche(admin, invisible).statut()).isEqualTo(404);
    }
}
