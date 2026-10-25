package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportAutres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.Autres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.EnTete;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.lignes;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.ANNEE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.MOIS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.NOMBRE_ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.NOTES;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.POURCENTAGE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.QTE_CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.QTE_FINALISATION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des prévisions de base de la tranche — FEATURE-patrimoine-import-autres.md §0 (I16), §2 (en-tête lu puis
 * superposé), §3.3, décision du manager 25/10 n° 1 ({@code nombreArticles} : colonne, sinon tranche &gt; 0, sinon stock,
 * avertissement) ; import-export §2.3 (« remplace tout » via {@code PrevisionsBaseService.enregistrerPrevisions}, règles
 * {@code Verif}, une ligne globale de bilan dont le pivot est le code de la tranche).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportPrevisionsTest {

    private static final String RESULTAT = "resultat";
    private static final String SOMME_CONCRETISATION =
            "La somme des quantités de concrétisation prévisionnelles doit être égal au nombre d'article en stock !";
    private static final LocalDateTime DEBUT_C = LocalDateTime.of(2027, 1, 1, 0, 0);
    private static final LocalDateTime FIN_C = LocalDateTime.of(2027, 3, 1, 0, 0);
    private static final LocalDateTime DEBUT_F = LocalDateTime.of(2027, 3, 1, 0, 0);
    private static final LocalDateTime FIN_F = LocalDateTime.of(2027, 5, 1, 0, 0);
    private static final String COUNT = "CountItemBasePrevision";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportAutres fixture;
    @Autowired private FixtureImportPatrimoine base;

    private Autres a;

    @BeforeEach
    void setUp() {
        a = fixture.preparer("P9BV");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(a);
    }

    // ------------------------------------------------------------------ outils

    private UUID tranche() {
        return a.i().trancheId();
    }

    /** En-tête de 3 articles, 40 %, 3 000 000, fenêtres 2027, écarts types 2. */
    private void enTeteDeTroisArticles() {
        fixture.enTete(tranche(), new EnTete(3, new BigDecimal("3000000"), new BigDecimal("0.4"), DEBUT_C, FIN_C, DEBUT_F,
                FIN_F, new BigDecimal("2")));
    }

    private static Map<String, Object> mois(int annee, int mois, int concretisation, int finalisation, String notes) {
        return colonnes(ANNEE, annee, MOIS, mois, QTE_CONCRETISATION, concretisation, QTE_FINALISATION, finalisation,
                NOTES, notes);
    }

    /** Trois articles répartis sur trois mois de 2027 (concrétisation 2 + 1 + 0, finalisation 0 + 1 + 2). */
    private static List<Map<String, Object>> troisArticles() {
        return new ArrayList<>(List.of(mois(2027, 1, 2, 0, "Lancement"), mois(2027, 2, 1, 1, null),
                mois(2027, 3, 0, 2, null)));
    }

    private String importer(UUID trancheId, Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(requete(PREVISIONS, trancheId, simulation,
                        fichier("PREVISIONS", lignes), connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String ecrire(List<Map<String, Object>> lignes) throws Exception {
        return importer(tranche(), false, lignes);
    }

    /** Ligne globale du bilan : pivot = code de la tranche. */
    private Map<String, Object> globale(String bilan) {
        return lignes(bilan).stream()
                .filter(l -> a.i().tranche().getCode().equals(l.get("pivot")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("pas de ligne globale (pivot = code de la tranche) : " + bilan));
    }

    // ------------------------------------------------------------------ §3.3 accès et tranche

    @Test
    void r3_3_sansTrancheId_400_messageOmran() throws Exception {
        mockMvc.perform(requete(PREVISIONS, null, true, fichier("PREVISIONS", troisArticles()), connecte(a.i().admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Vous devez sélectionner une Tranche !")));
    }

    @Test
    void r3_3_trancheDUneEntiteInactive_404() throws Exception {
        CfgTranche invisible = base.trancheInvisible(a.i());

        mockMvc.perform(requete(PREVISIONS, invisible.getId(), true, fichier("PREVISIONS", troisArticles()),
                        connecte(a.i().admin())))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ remplace tout, CREATION / MISE_A_JOUR

    @Test
    void r3_3_aucuneLigneAvant_creation_lignesEcrites() throws Exception {
        enTeteDeTroisArticles();

        String bilan = ecrire(troisArticles());

        List<Map<String, Object>> ecrites = fixture.lignesPrevision(tranche());
        assertAll(
                () -> assertThat(globale(bilan)).as(bilan).containsEntry(RESULTAT, CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(ecrites).hasSize(3),
                () -> assertThat(ecrites.get(0)).containsEntry("Annee", 2027).containsEntry("Mois", 1)
                        .containsEntry("Quantity", 2).containsEntry("Notes", "Lancement"));
    }

    @Test
    void r3_3_lignesExistantes_miseAJour_remplaceToutesLesLignes() throws Exception {
        enTeteDeTroisArticles();
        fixture.lignePrevision(tranche(), 2026, 5, 3, 3);

        String bilan = ecrire(troisArticles());

        List<Map<String, Object>> ecrites = fixture.lignesPrevision(tranche());
        assertAll(
                () -> assertThat(globale(bilan)).as(bilan).containsEntry(RESULTAT, MISE_A_JOUR),
                () -> assertThat(ecrites).hasSize(3).noneSatisfy(l -> assertThat(l).containsEntry("Annee", 2026)));
    }

    @Test
    void r3_3_simulationParDefaut_rienEcrit() throws Exception {
        enTeteDeTroisArticles();

        String bilan = importer(tranche(), null, troisArticles());

        assertAll(
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.lignesPrevision(tranche())).isEmpty());
    }

    // ------------------------------------------------------------------ §2 en-tête existant gardé (cellule absente = intact)

    @Test
    void r2_fichierSansColonneDEnTete_enTeteDeLaTrancheIntact() throws Exception {
        enTeteDeTroisArticles();

        ecrire(troisArticles());

        Map<String, Object> e = fixture.enTete(tranche());
        assertAll(
                () -> assertThat(e).containsEntry(COUNT, 3)
                        .containsEntry("BaseMarketingStartDate", "2027-01-01")
                        .containsEntry("BaseEndDateConcretisation", "2027-03-01")
                        .containsEntry("BaseStartDateFinalisation", "2027-03-01")
                        .containsEntry("BaseMarketingEndDate", "2027-05-01"),
                () -> assertThat((BigDecimal) e.get("BaseConcretisationPercent")).isEqualByComparingTo("0.4"),
                () -> assertThat((BigDecimal) e.get("TotalSaleBasePrevision")).isEqualByComparingTo("3000000"),
                () -> assertThat((BigDecimal) e.get("BaseEcarttypeConcretisation")).isEqualByComparingTo("2"),
                () -> assertThat((BigDecimal) e.get("BaseEcarttypeFinalisation")).isEqualByComparingTo("2"));
    }

    @Test
    void r3_3_pourcentageConcretisationEnPourcent_diviseePar100() throws Exception {
        enTeteDeTroisArticles();
        List<Map<String, Object>> lignes = troisArticles();
        lignes.forEach(l -> l.put(POURCENTAGE, 25));

        ecrire(lignes);

        assertThat((BigDecimal) fixture.enTete(tranche()).get("BaseConcretisationPercent")).isEqualByComparingTo("0.25");
    }

    // ------------------------------------------------------------------ décision 1 : source de nombreArticles

    @Test
    void q1_colonneNombreDArticles_prioritaire_surLaTranche() throws Exception {
        fixture.enTete(tranche(), new EnTete(5, new BigDecimal("3000000"), new BigDecimal("0.4"), DEBUT_C, FIN_C, DEBUT_F,
                FIN_F, BigDecimal.ONE));
        List<Map<String, Object>> lignes = troisArticles();
        lignes.forEach(l -> l.put(NOMBRE_ARTICLES, 3));

        String bilan = ecrire(lignes);

        assertAll(
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(fixture.enTete(tranche())).containsEntry(COUNT, 3));
    }

    @Test
    void q1_sansColonne_valeurDeLaTrancheSupA0() throws Exception {
        enTeteDeTroisArticles();

        String bilan = ecrire(troisArticles());

        assertAll(
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(fixture.enTete(tranche())).containsEntry(COUNT, 3));
    }

    @Test
    void q1_sansColonne_trancheA0_stockDeLaTranche_avertissement() throws Exception {
        fixture.enTete(tranche(), new EnTete(0, BigDecimal.ZERO, new BigDecimal("0.4"), DEBUT_C, FIN_C, DEBUT_F, FIN_F,
                BigDecimal.ONE));
        base.principal(a.i(), "ST1-" + a.jeton());
        base.principal(a.i(), "ST2-" + a.jeton());

        String bilan = ecrire(List.of(mois(2027, 1, 2, 0, null), mois(2027, 2, 0, 2, null)));

        List<String> avertissements = JsonPath.read(bilan, "$.avertissements");
        assertAll(
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(fixture.enTete(tranche())).containsEntry(COUNT, 2),
                () -> assertThat(avertissements).isNotEmpty());
    }

    // ------------------------------------------------------------------ I16 règles Verif du service

    @Test
    void i16_sommeDifferenteDuNombreDArticles_refusDuService_rienEcrit() throws Exception {
        enTeteDeTroisArticles();
        fixture.lignePrevision(tranche(), 2026, 5, 3, 3);

        String bilan = ecrire(List.of(mois(2027, 1, 1, 0, null), mois(2027, 2, 0, 1, null)));

        assertAll(
                () -> assertThat(globale(bilan)).as(bilan).containsEntry(RESULTAT, ERREUR),
                () -> assertThat(String.valueOf(globale(bilan).get("message"))).contains(SOMME_CONCRETISATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.lignesPrevision(tranche())).singleElement()
                        .satisfies(l -> assertThat(l).containsEntry("Annee", 2026)));
    }

    @Test
    void i16_moisEnDouble_refusDuService() throws Exception {
        enTeteDeTroisArticles();

        String bilan = ecrire(List.of(mois(2027, 1, 2, 1, null), mois(2027, 1, 1, 2, null)));

        assertAll(
                () -> assertThat(globale(bilan)).as(bilan).containsEntry(RESULTAT, ERREUR),
                () -> assertThat(String.valueOf(globale(bilan).get("message")))
                        .contains("Il ne faut pas que les prévisions contiennent des mois en double !"),
                () -> assertThat(fixture.lignesPrevision(tranche())).isEmpty());
    }

    // ------------------------------------------------------------------ §1.2 erreurs de lecture par ligne Excel

    @Test
    void r1_2_moisIllisible_erreurDeLaLigneExcel_rienEcrit() throws Exception {
        enTeteDeTroisArticles();
        List<Map<String, Object>> lignes = troisArticles();
        lignes.get(1).put(MOIS, "abc");

        String bilan = ecrire(lignes);

        assertAll(
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 3)).contains("abc").contains("illisible"),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.lignesPrevision(tranche())).isEmpty());
    }

    // ------------------------------------------------------------------ modèle

    @Test
    void modele_feuillePrevisions_preRempliAvecLesLignesExistantes() throws Exception {
        enTeteDeTroisArticles();
        fixture.lignePrevision(tranche(), 2027, 3, 3, 3);

        byte[] contenu = mockMvc.perform(get(MODELE, PREVISIONS).param("trancheId", tranche().toString())
                        .with(connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        List<List<String>> donnees = ClasseurDeTest.lire(contenu).get("PREVISIONS");
        assertAll(
                () -> assertThat(donnees).isNotEmpty().hasSize(2),
                () -> assertThat(donnees.get(0)).contains(ANNEE, MOIS, QTE_CONCRETISATION, QTE_FINALISATION, NOTES),
                () -> assertThat(donnees.get(1)).contains("2027", "3"));
    }

    @Test
    void modele_sansTrancheId_400() throws Exception {
        mockMvc.perform(get(MODELE, PREVISIONS).with(connecte(a.i().admin())))
                .andExpect(status().isBadRequest());
    }
}
