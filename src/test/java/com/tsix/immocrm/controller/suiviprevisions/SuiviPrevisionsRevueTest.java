package com.tsix.immocrm.controller.suiviprevisions;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureSuiviPrevisions;
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

import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.ANNEE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.DETAIL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.FRAIS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MOIS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MONTANTS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.QUANTITES;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.RACINE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.REEL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.SCHEMA;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.lignes;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.mois;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.requete;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.section;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.somme;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.valeur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue du lot Pa (FEATURE-tdb-vente-previsions.md §4.3) : le détail d'un mois reste égal aux cellules de l'écran pour
 * chaque filtre de frais, lit toutes les années pour « regle » mais ne montre que le mois demandé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviPrevisionsRevueTest {

    private static final String AN = "2025";
    private static final String DOSSIERS = "dossiers";
    private static final String MOUVEMENTS = "mouvements";
    private static final String ECHEANCES = "echeances";
    private static final String MONTANT = "montant";
    private static final String REGLE = "regle";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureSuiviPrevisions sp;
    @Autowired private FixtureRapports rapports;

    private SuiviPrevisionsSupport appels;
    private Jeu jeu;
    private CfgTranche t1;
    private RequestPostProcessor direction;

    @BeforeEach
    void setUp() {
        appels = new SuiviPrevisionsSupport(mockMvc);
        jeu = fixture.jeu("SPR");
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        AdmUser dg = rapports.compte("dg." + jeu.prefixe(), rapports.role("Direction Générale", false));
        direction = connecte(dg);
    }

    private static LocalDate j(int an, int mois, int jour) {
        return LocalDate.of(an, mois, jour);
    }

    /** Mars 2025 : autofinancement 2 000 réglé 1 000, frais 70 réglés 50 ; mars 2024 : une vente encaissée 400. */
    private void jeuDeMars() {
        ComFolder d = sp.vente(jeu, t1, j(2025, 3, 5), "GAMMA" + jeu.prefixe());
        ComDocument auto = sp.echeance(jeu, d, Genre.AUTOFINANCEMENT, "2000", j(2025, 3, 10));
        sp.regler(jeu, auto, "1000", IN_WORK, j(2025, 3, 10));
        ComDocument frais = sp.echeance(jeu, d, Genre.FRAIS, "70", j(2025, 3, 12));
        sp.regler(jeu, frais, "50", IN_WORK, j(2025, 3, 12));
        ComFolder ancien = sp.vente(jeu, t1, j(2024, 3, 7), null);
        ComDocument avance = sp.echeance(jeu, ancien, Genre.AVANCE, "400", j(2024, 3, 7));
        sp.regler(jeu, avance, "400", IN_WORK, j(2024, 3, 8));
        fixture.relire();
    }

    private List<Map<String, Object>> sections(String url, String... parametres) throws Exception {
        return appels.sections(requete(url, direction, List.of(t1), parametres));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SANS", "AVEC", "SEULS"})
    void detail_chaqueFiltreDeFrais_sommesEgalesAuxCellulesDeMars(String frais) throws Exception {
        jeuDeMars();

        Map<String, Object> montants = section(sections(RACINE, ANNEE, AN, FRAIS, frais), MONTANTS);
        List<Map<String, Object>> detail = sections(DETAIL, ANNEE, AN, FRAIS, frais, MOIS, "3");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(somme(lignes(section(detail, MOUVEMENTS)), MONTANT)).as("mouvements")
                .isEqualByComparingTo(valeur(mois(montants, 3), REEL));
        soft.assertThat(somme(lignes(section(detail, ECHEANCES)), MONTANT)).as("échéances")
                .isEqualByComparingTo(valeur(mois(montants, 3), SCHEMA));
        soft.assertAll();
    }

    @Test
    void detail_lectureToutesAnnees_neMontrePasMars2024DansMars2025() throws Exception {
        jeuDeMars();

        List<Map<String, Object>> detail = sections(DETAIL, ANNEE, AN, MOIS, "3");
        Map<String, Object> quantites = section(sections(RACINE, ANNEE, AN), QUANTITES);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignes(section(detail, DOSSIERS))).as("dossiers").hasSize(1);
        soft.assertThat(valeur(mois(quantites, 3), REEL)).as("cellule").isEqualByComparingTo("1");
        soft.assertThat(somme(lignes(section(detail, MOUVEMENTS)), MONTANT)).as("mouvements")
                .isEqualByComparingTo("1000");
        soft.assertThat(somme(lignes(section(detail, ECHEANCES)), MONTANT)).as("échéances")
                .isEqualByComparingTo("2000");
        soft.assertAll();
    }

    // « regle » d'une échéance : lignes retenues en encaissement toutes dates, signées (décaissement négatif)
    @Test
    void detail_regleEcheance_reglementDUneAutreAnneeEtDecaissement_comptesSignes() throws Exception {
        ComFolder d = sp.vente(jeu, t1, j(2025, 3, 5), null);
        ComDocument auto = sp.echeance(jeu, d, Genre.AUTOFINANCEMENT, "2000", j(2025, 3, 10));
        jeu.regler(auto, new BigDecimal("800"), true, IN_WORK, null, j(2026, 1, 15));
        jeu.regler(auto, new BigDecimal("100"), false, IN_WORK, null, j(2025, 6, 1));
        fixture.relire();

        Map<String, Object> echeances = section(sections(DETAIL, ANNEE, AN, MOIS, "3"), ECHEANCES);

        assertThat(lignes(echeances)).singleElement().satisfies(l -> {
            assertThat(valeur(l, REGLE)).isEqualByComparingTo("700");
            assertThat(valeur(l, "reste")).isEqualByComparingTo("1300");
        });
    }

    // ECHEANCES_DUES n'a aucun filtre d'article (§3.1) : un dossier sur sous-article est au schéma et au détail
    @Test
    void detail_dossierSurSousArticle_auSchemaEtAuDetail_sommesEgales() throws Exception {
        ComFolder d = jeu.dossierSousArticle(t1, jeu.statuts().concretisation());
        jeu.dater(d, j(2025, 3, 5), null, null, null);
        sp.echeance(jeu, d, Genre.CREDIT, "5000", j(2025, 3, 15));
        fixture.relire();

        Map<String, Object> montants = section(sections(RACINE, ANNEE, AN), MONTANTS);
        List<Map<String, Object>> detail = sections(DETAIL, ANNEE, AN, MOIS, "3");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(montants, 3), SCHEMA)).as("cellule").isEqualByComparingTo("5000");
        soft.assertThat(somme(lignes(section(detail, ECHEANCES)), MONTANT)).as("détail")
                .isEqualByComparingTo("5000");
        soft.assertThat(lignes(section(detail, DOSSIERS))).as("dossiers").hasSize(1);
        soft.assertAll();
    }

    // Choix documenté : « regle » d'une échéance de frais compte ses lignes ENCAISSEMENTS_FRAIS
    @Test
    void detail_avecFrais_echeanceDeFrais_regleDesFraisEncaisses() throws Exception {
        jeuDeMars();

        Map<String, Object> echeances = section(sections(DETAIL, ANNEE, AN, FRAIS, "SEULS", MOIS, "3"), ECHEANCES);

        assertThat(lignes(echeances)).singleElement().satisfies(l -> {
            assertThat(valeur(l, MONTANT)).isEqualByComparingTo("70");
            assertThat(valeur(l, REGLE)).isEqualByComparingTo("50");
        });
    }
}
