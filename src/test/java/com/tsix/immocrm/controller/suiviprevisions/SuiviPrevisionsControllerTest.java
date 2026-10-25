package com.tsix.immocrm.controller.suiviprevisions;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureSuiviPrevisions;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import com.tsix.immocrm.security.CookieJwt;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.servlet.http.Cookie;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.ANNEE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.DETAIL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.EXPORT;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.FRAIS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MOIS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MONTANTS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.PARAMETRES;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.QUANTITES;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.RACINE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.REEL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.SCHEMA;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.lignes;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.mois;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.requete;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.section;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.somme;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.valeur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.POINTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écran « Suivi des prévisions » (FEATURE-tdb-vente-previsions.md §4, §9.2 R-P7…R-P10, §9.4 R-V5, R-V6). Année 2025 :
 * <pre>
 *   T1 : d1 concrétisé le 05/03 (client ALPHA, prix 900 000) — autofinancement 2 000 au 10/03, réglé 1 000 (IN_WORK)
 *        + 500 CANCELED ; frais 70 au 12/03, réglés 50 ; autofinancement 777 au 02/04, réglé 777
 *   T2 : d2 concrétisé le 20/03 (client BETA) — avance 300 au 20/03, réglée 300 (POINTED) le 21/03
 * </pre>
 * Compte « direction » (Direction Générale, sans restriction) ; tranches toujours passées en {@code trancheId}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviPrevisionsControllerTest {

    private static final String AN = "2025";
    private static final String DOSSIERS = "dossiers";
    private static final String MOUVEMENTS = "mouvements";
    private static final String ECHEANCES = "echeances";
    private static final String MONTANT = "montant";
    private static final String SANS = "SANS";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureSuiviPrevisions sp;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private MontantsParNatureDossier montantsParNature;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;
    @Autowired private Clock clock;

    private SuiviPrevisionsSupport appels;
    private Jeu jeu;
    private CfgTranche t1;
    private CfgTranche t2;
    private ComFolder d1;
    private ComFolder d2;
    private RequestPostProcessor direction;

    @BeforeEach
    void setUp() {
        appels = new SuiviPrevisionsSupport(mockMvc);
        jeu = fixture.jeu("SPC");
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);
        AdmUser dg = rapports.compte("dg." + jeu.prefixe(), rapports.role("Direction Générale", false));
        direction = connecte(dg);
    }

    private static LocalDate j(int mois, int jour) {
        return LocalDate.of(2025, mois, jour);
    }

    private void jeuDeMars() {
        d1 = sp.vente(jeu, t1, j(3, 5), "ALPHA" + jeu.prefixe());
        jeu.prix(d1, new BigDecimal("900000"), null);
        ComDocument auto = sp.echeance(jeu, d1, Genre.AUTOFINANCEMENT, "2000", j(3, 10));
        sp.regler(jeu, auto, "1000", IN_WORK, j(3, 10));
        sp.regler(jeu, auto, "500", CANCELED, j(3, 10));
        ComDocument frais = sp.echeance(jeu, d1, Genre.FRAIS, "70", j(3, 12));
        sp.regler(jeu, frais, "50", IN_WORK, j(3, 12));
        ComDocument avril = sp.echeance(jeu, d1, Genre.AUTOFINANCEMENT, "777", j(4, 2));
        sp.regler(jeu, avril, "777", IN_WORK, j(4, 2));
        d2 = sp.vente(jeu, t2, j(3, 20), "BETA" + jeu.prefixe());
        ComDocument avance = sp.echeance(jeu, d2, Genre.AVANCE, "300", j(3, 20));
        sp.regler(jeu, avance, "300", POINTED, j(3, 21));
        fixture.relire();
    }

    private List<Map<String, Object>> ecran(List<CfgTranche> tranches, String... parametres) throws Exception {
        return appels.sections(requete(RACINE, direction, tranches, parametres));
    }

    private List<Map<String, Object>> detail(List<CfgTranche> tranches, String... parametres) throws Exception {
        return appels.sections(requete(DETAIL, direction, tranches, parametres));
    }

    // ------------------------------------------------------------------ §4.1 / §4.2 résultat

    @Test
    void ecran_resultat_codeLibelleModule_deuxSections_quantitesPuisMontants() throws Exception {
        jeuDeMars();

        String corps = appels.corps(requete(RACINE, direction, List.of(t1), ANNEE, AN));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat((String) JsonPath.read(corps, "$.code")).isEqualTo("SuiviPrevisionsVente");
        soft.assertThat((String) JsonPath.read(corps, "$.libelle")).isEqualTo("Suivi des prévisions de vente");
        soft.assertThat((String) JsonPath.read(corps, "$.module")).isEqualTo("vente");
        soft.assertThat((List<String>) JsonPath.read(corps, "$.sections[*].id")).containsExactly(QUANTITES, MONTANTS);
        soft.assertThat((List<String>) JsonPath.read(corps, "$.sections[*].titre"))
                .containsExactly("Concrétisations (quantité)", "Encaissements (montant) (*)");
        soft.assertAll();
    }

    // Q1 : prévu nul ⇒ avertissement « Prévu non saisi » (constante de LignesMensuelles.avertissements)
    @Test
    void ecran_prevuNonSaisi_avertissement() throws Exception {
        jeuDeMars();

        String corps = appels.corps(requete(RACINE, direction, List.of(t1), ANNEE, AN));

        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        assertThat(avertissements).anyMatch(a -> a.startsWith("Prévu non saisi"));
    }

    @Test
    void ecran_quantitesReel_concretisationsNettesDuMois() throws Exception {
        jeuDeMars();

        Map<String, Object> quantites = section(ecran(List.of(t1, t2), ANNEE, AN), QUANTITES);

        assertThat(valeur(mois(quantites, 3), REEL)).isEqualByComparingTo("2");
        assertThat(somme(lignes(quantites), REEL)).isEqualByComparingTo("2");
    }

    // §4.2 : dix graphiques, cinq par section, sur la colonne « mois »
    @Test
    void ecran_cinqGraphiquesParSection_typesEtSeriesDuSpec() throws Exception {
        jeuDeMars();

        List<Map<String, Object>> sections = ecran(List.of(t1), ANNEE, AN);

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            List<Map<String, Object>> g = SuiviPrevisionsSupport.graphiques(s);
            soft.assertThat(g).as("%s", s.get("id")).extracting(x -> x.get("type"), x -> x.get("colonnesValeurs"))
                    .containsExactly(
                            tuple("COLONNES", List.of("prevu", REEL)),
                            tuple("LIGNES", List.of("cumulPrevu", "cumulReel")),
                            tuple("COLONNES", List.of("pourcentageRealise")),
                            tuple("LIGNES", List.of("pourcentageCumule")),
                            tuple("COLONNES", List.of("rentabilite")));
            soft.assertThat(g).as("%s catégorie", s.get("id")).isNotEmpty()
                    .allSatisfy(x -> assertThat(x.get("colonneCategorie")).isEqualTo(MOIS));
        }
        soft.assertAll();
    }

    @Test
    void ecran_montants_colonneSchemaDeFinancement_montantAvecTotal() throws Exception {
        jeuDeMars();

        Map<String, Object> montants = section(ecran(List.of(t1), ANNEE, AN), MONTANTS);

        Map<String, Object> schema = SuiviPrevisionsSupport.colonne(montants, SCHEMA);
        assertThat(schema).containsEntry("type", "MONTANT").containsEntry("fonctionTotal", "SOMME")
                .containsEntry("libelle", "Schéma de financement");
    }

    // ------------------------------------------------------------------ R-P7 filtre frais

    @ParameterizedTest
    @CsvSource({"SANS,1000,2000", "AVEC,1050,2070", "SEULS,50,70"})
    void rP7_filtreFrais_reelEtSchemaDeFinancementDeMars(String frais, String reel, String schema) throws Exception {
        jeuDeMars();

        Map<String, Object> montants = section(ecran(List.of(t1), ANNEE, AN, FRAIS, frais), MONTANTS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(montants, 3), REEL)).as("réel").isEqualByComparingTo(reel);
        soft.assertThat(valeur(mois(montants, 3), SCHEMA)).as("schéma").isEqualByComparingTo(schema);
        soft.assertAll();
    }

    @Test
    void rP7_fraisAbsent_defautSans_appliqueEtSansFrais() throws Exception {
        jeuDeMars();

        String corps = appels.corps(requete(RACINE, direction, List.of(t1), ANNEE, AN));

        List<String> applique = JsonPath.read(corps, "$.parametres.frais");
        assertThat(applique).containsExactly(SANS);
        List<Number> reels = JsonPath.read(corps, "$.sections[?(@.id == 'montants')].lignes[2].reel");
        assertThat(reels).singleElement().satisfies(v -> assertThat(new BigDecimal(v.toString()))
                .isEqualByComparingTo("1000"));
    }

    @Test
    void rP7_fraisHorsOptions_400() throws Exception {
        assertThat(appels.statut(requete(RACINE, direction, List.of(t1), ANNEE, AN, FRAIS, "AUTRE"))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ §4.1 400 du socle

    @ParameterizedTest
    @CsvSource({"annee,1899", "annee,2201", "inconnu,1", "trancheId,pas-un-uuid"})
    void ecran_parametreInvalide_400(String nom, String valeurInvalide) throws Exception {
        assertThat(appels.statut(get(RACINE).with(direction).param(nom, valeurInvalide))).isEqualTo(400);
    }

    @Test
    void parametres_annee_tranches_frais_defautsDuSpec() throws Exception {
        String corps = appels.corps(get(PARAMETRES).with(direction));

        String annee = String.valueOf(Year.now(clock).getValue());
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat((List<String>) JsonPath.read(corps, "$[*].nom")).contains(ANNEE, TRANCHE_ID, FRAIS);
        soft.assertThat((List<String>) JsonPath.read(corps, "$[?(@.nom == 'annee')].type")).containsExactly("ANNEE");
        soft.assertThat((List<String>) JsonPath.read(corps, "$[?(@.nom == 'annee')].valeurParDefaut"))
                .containsExactly(annee);
        soft.assertThat((List<String>) JsonPath.read(corps, "$[?(@.nom == 'trancheId')].type"))
                .containsExactly("TRANCHES");
        soft.assertThat((List<Boolean>) JsonPath.read(corps, "$[?(@.nom == 'trancheId')].multiple"))
                .containsExactly(true);
        soft.assertThat((List<String>) JsonPath.read(corps, "$[?(@.nom == 'frais')].type")).containsExactly("CHOIX");
        soft.assertThat((List<String>) JsonPath.read(corps, "$[?(@.nom == 'frais')].valeurParDefaut"))
                .containsExactly(SANS);
        soft.assertThat((List<String>) JsonPath.read(corps, "$[?(@.nom == 'frais')].options[*].valeur"))
                .containsExactly(SANS, "AVEC", "SEULS");
        soft.assertThat((List<String>) JsonPath.read(corps, "$[?(@.nom == 'frais')].options[*].libelle"))
                .containsExactly("Sans frais", "Avec frais", "Seulement les frais");
        soft.assertAll();
    }

    @Test
    void ecran_anneeAbsente_anneeDeLHorloge() throws Exception {
        String corps = appels.corps(requete(RACINE, direction, List.of(t1)));

        List<String> annee = JsonPath.read(corps, "$.parametres.annee");
        assertThat(annee).containsExactly(String.valueOf(Year.now(clock).getValue()));
    }

    // ------------------------------------------------------------------ R-P8 concrétisations nettes

    @Test
    void rP8_dossierConcretiseEnNovembre2023AnnuleEnMars2024_ecran2023NovembreA0() throws Exception {
        ComFolder annule = jeu.dossier(t1, jeu.statuts().annulation());
        jeu.dater(annule, LocalDate.of(2023, 11, 15), null, null, LocalDate.of(2024, 3, 10));
        fixture.relire();

        Map<String, Object> quantites = section(ecran(List.of(t1), ANNEE, "2023"), QUANTITES);

        assertThat(valeur(mois(quantites, 11), REEL)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ R-P9 détail d'un mois

    @Test
    void rP9_detail_codeEtTroisSections() throws Exception {
        jeuDeMars();

        String corps = appels.corps(requete(DETAIL, direction, List.of(t1, t2), ANNEE, AN, MOIS, "3"));

        assertThat((String) JsonPath.read(corps, "$.code")).isEqualTo("SuiviPrevisionsVenteMois");
        assertThat((List<String>) JsonPath.read(corps, "$.sections[*].id"))
                .containsExactly(DOSSIERS, MOUVEMENTS, ECHEANCES);
    }

    @Test
    void rP9_detail_dossiers_compteEgalQuantitesReelDuMois_lesDeuxDossiers() throws Exception {
        jeuDeMars();

        Map<String, Object> dossiersMars = section(detail(List.of(t1, t2), ANNEE, AN, MOIS, "3"), DOSSIERS);
        Map<String, Object> quantites = section(ecran(List.of(t1, t2), ANNEE, AN), QUANTITES);

        assertThat(lignes(dossiersMars)).extracting(l -> l.get("dossier"))
                .containsExactlyInAnyOrder(d1.getCode(), d2.getCode());
        assertThat(BigDecimal.valueOf(lignes(dossiersMars).size()))
                .isEqualByComparingTo(valeur(mois(quantites, 3), REEL));
    }

    @Test
    void rP9_detail_mouvements_sommeEgalMontantsReelDuMois_horsAvrilEtCanceled() throws Exception {
        jeuDeMars();

        Map<String, Object> mouvements = section(detail(List.of(t1, t2), ANNEE, AN, MOIS, "3"), MOUVEMENTS);
        Map<String, Object> montants = section(ecran(List.of(t1, t2), ANNEE, AN), MONTANTS);

        assertThat(somme(lignes(mouvements), MONTANT)).isEqualByComparingTo(valeur(mois(montants, 3), REEL))
                .isEqualByComparingTo("1300");
    }

    @Test
    void rP9_detail_echeances_sommeEgalSchemaDeFinancementDuMois() throws Exception {
        jeuDeMars();

        Map<String, Object> echeances = section(detail(List.of(t1, t2), ANNEE, AN, MOIS, "3"), ECHEANCES);
        Map<String, Object> montants = section(ecran(List.of(t1, t2), ANNEE, AN), MONTANTS);

        assertThat(somme(lignes(echeances), MONTANT)).isEqualByComparingTo(valeur(mois(montants, 3), SCHEMA))
                .isEqualByComparingTo("2300");
    }

    @Test
    void rP9_detail_echeances_regleEgalLignesComptees_resteEgalMontantMoinsRegle() throws Exception {
        jeuDeMars();

        Map<String, Object> echeances = section(detail(List.of(t1), ANNEE, AN, MOIS, "3"), ECHEANCES);

        assertThat(lignes(echeances)).singleElement().satisfies(l -> {
            assertThat(valeur(l, MONTANT)).isEqualByComparingTo("2000");
            assertThat(valeur(l, "regle")).as("1 000 compté, 500 CANCELED exclu").isEqualByComparingTo("1000");
            assertThat(valeur(l, "reste")).isEqualByComparingTo("1000");
        });
    }

    @Test
    void rP9_detail_dossiers_regleEgalReglesDeMontantsParNatureDossier() throws Exception {
        jeuDeMars();
        Map<UUID, Map<ColonneDossier, BigDecimal>> montants = montantsParNature.pour(List.of(d1, d2));

        Map<String, Object> dossiersMars = section(detail(List.of(t1, t2), ANNEE, AN, MOIS, "3"), DOSSIERS);

        SoftAssertions soft = new SoftAssertions();
        for (ComFolder d : List.of(d1, d2)) {
            Map<ColonneDossier, BigDecimal> m = montants.get(d.getId());
            BigDecimal attendu = m.get(ColonneDossier.AVANCE_REGLEE).add(m.get(ColonneDossier.AUTOFINANCEMENT_REGLE))
                    .add(m.get(ColonneDossier.CREDIT_REGLE));
            soft.assertThat(attendu).as("précondition %s", d.getCode()).isPositive();
            soft.assertThat(lignes(dossiersMars)).filteredOn(l -> d.getCode().equals(l.get("dossier")))
                    .singleElement().satisfies(l -> assertThat(valeur(l, "regle")).isEqualByComparingTo(attendu));
        }
        soft.assertAll();
    }

    @Test
    void rP9_detail_dossiers_colonnesDuSpec_prixVenteEtClient() throws Exception {
        jeuDeMars();

        Map<String, Object> dossiersMars = section(detail(List.of(t1), ANNEE, AN, MOIS, "3"), DOSSIERS);

        assertThat(SuiviPrevisionsSupport.nomsDesColonnes(dossiersMars)).containsExactlyInAnyOrder("projet", "tranche",
                "article", "dossier", "dateConcretisation", "prixVente", "regle", "client");
        assertThat(lignes(dossiersMars)).singleElement().satisfies(l -> {
            assertThat(valeur(l, "prixVente")).isEqualByComparingTo("900000");
            assertThat(String.valueOf(l.get("client"))).contains("ALPHA" + jeu.prefixe());
            assertThat(String.valueOf(l.get("dateConcretisation"))).startsWith("2025-03-05");
        });
    }

    @Test
    void rP9_detail_mouvements_triParDateDecroissante() throws Exception {
        jeuDeMars();

        Map<String, Object> mouvements = section(detail(List.of(t1, t2), ANNEE, AN, MOIS, "3"), MOUVEMENTS);

        List<String> dates = lignes(mouvements).stream().map(l -> String.valueOf(l.get("dateMouvement"))).toList();
        List<String> triees = new ArrayList<>(dates);
        triees.sort(Comparator.reverseOrder());
        assertThat(dates).hasSize(2).containsExactlyElementsOf(triees);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "13"})
    void rP9_detail_moisHorsBornes_400(String moisInvalide) throws Exception {
        assertThat(appels.statut(requete(DETAIL, direction, List.of(t1), ANNEE, AN, MOIS, moisInvalide)))
                .isEqualTo(400);
    }

    @Test
    void rP9_detail_moisAbsent_400() throws Exception {
        assertThat(appels.statut(requete(DETAIL, direction, List.of(t1), ANNEE, AN))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R-P10 export

    @Test
    void rP10_export_deuxFeuilles_nomsNettoyes_ligneTotal() throws Exception {
        jeuDeMars();

        MockHttpServletResponse reponse = appels.exporter(requete(EXPORT, direction, List.of(t1), ANNEE, AN,
                "format", "xlsx")).getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            assertThat(classeur.getNumberOfSheets()).isEqualTo(2);
            assertThat(classeur.getSheetName(0)).isEqualTo("Concrétisations (quantité)");
            assertThat(classeur.getSheetName(1)).startsWith("Encaissements (montant)").doesNotContain("*")
                    .hasSizeLessThanOrEqualTo(31);
            for (Sheet feuille : classeur) {
                List<String> premieres = new ArrayList<>();
                for (Row ligne : feuille) {
                    premieres.add(ligne.getCell(0) == null ? "" : ligne.getCell(0).toString());
                }
                assertThat(premieres).as(feuille.getSheetName()).contains("Total");
            }
        }
    }

    @Test
    void rP10_export_formatPdf_400() throws Exception {
        assertThat(appels.exporter(requete(EXPORT, direction, List.of(t1), ANNEE, AN, "format", "pdf"))
                .getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void rP10_export_formatAbsent_xlsxParDefaut() throws Exception {
        MockHttpServletResponse reponse = appels.exporter(requete(EXPORT, direction, List.of(t1), ANNEE, AN))
                .getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        assertThat(reponse.getHeader("Content-Disposition")).contains("SuiviPrevisionsVente_").contains(".xlsx");
    }

    // ------------------------------------------------------------------ R-V5 clé d'API, 401

    @Test
    void rV5_cleDApiAvecToutesLesPortees_403HorsPortee_surLesQuatreRoutes() throws Exception {
        AdmUser compte = dossiers.compte("SPC-" + jeu.prefixe(), "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("previsions-" + jeu.prefixe());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        for (String url : List.of(RACINE, PARAMETRES, DETAIL, EXPORT)) {
            mockMvc.perform(get(url).param(TRANCHE_ID, t1.getId().toString()).param(MOIS, "3")
                            .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        }
    }

    @Test
    void ecran_sansSession_401() throws Exception {
        assertThat(appels.statut(get(RACINE))).isEqualTo(401);
    }

    // ------------------------------------------------------------------ R-V6 écran ouvert (vrai cookie)

    @Test
    void rV6_commercialRestreintAuxRapports_ouvreLEcranEn200_parLeVraiCookie() throws Exception {
        AdmUser commercial = rapports.compte("com." + jeu.prefixe(), rapports.role(RolesEquipe.COMMERCIAL, true));
        sp.motDePasse(commercial);
        rapports.relire();
        Cookie cookie = mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(commercial.getCode(),
                                FixtureSuiviPrevisions.MOT_DE_PASSE)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie(CookieJwt.NOM);
        assertThat(cookie).isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(RACINE).cookie(cookie).param(ANNEE, AN))).as("écran").isEqualTo(200);
        soft.assertThat(appels.statut(get(PARAMETRES).cookie(cookie))).as("paramètres").isEqualTo(200);
        soft.assertThat(appels.statut(get(DETAIL).cookie(cookie).param(ANNEE, AN).param(MOIS, "3")))
                .as("détail").isEqualTo(200);
        soft.assertAll();
    }
}
