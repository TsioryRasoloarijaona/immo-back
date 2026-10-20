package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Formules du bloc A et phases, servies par l'aperçu {@code POST /calcul} (calcul partagé {@code CalculFaisabilite}) —
 * FEATURE-patrimoine-faisabilite.md §2.2, §2.7 et §8 bis (décision 6). Valeurs de contrôle mesurées sur YESTEST et
 * ASM TERRAIN 1, tolérance 1e-6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudeFaisabiliteCalculTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;

    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
    }

    private CorpsJson fiche() {
        return FixtureFaisabilite.fiche("CAL-" + jeton, societe.getId());
    }

    private String calculer(CorpsJson corps) throws Exception {
        return api.calculer(corps).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal nombre(String json, String champ) {
        Object v = JsonPath.read(json, "$." + champ);
        assertThat(v).as(champ).isNotNull();
        return new BigDecimal(String.valueOf(v));
    }

    private static ThrowingConsumer<String> vaut(String champ, String attendu) {
        return json -> assertThat(nombre(json, champ)).as(champ).isCloseTo(new BigDecimal(attendu), within(TOLERANCE));
    }

    @SafeVarargs
    private static void verifier(String json, ThrowingConsumer<String>... controles) {
        assertAll(Arrays.stream(controles).<Executable>map(c -> () -> c.accept(json)));
    }

    // ------------------------------------------------------------------ valeurs de contrôle (§2.2)

    /** YESTEST : AreaNet 990, emprise 297, plancher couvert 2 970, vendable 2 970, non bâti 703, prix 4 500 000, total 4 725 000. */
    @Test
    void r2_2_controle_yestest() throws Exception {
        String json = calculer(yestest(fiche()));

        verifier(json,
                vaut(SUPERFICIE_NETTE, "990"),
                vaut(EMPRISE_AU_SOL, "297"),
                vaut(PLANCHER_COUVERT, "2970"),
                vaut(PLANCHER_VENDABLE, "2970"),
                vaut(SURFACE_NON_BATIE, "703"),
                vaut(PRIX_TERRAIN, "4500000"),
                vaut(FRAIS_ENREGISTREMENT, "90000"),
                vaut(AUTRES_FRAIS_TERRAIN, "135000"),
                vaut(COUT_TOTAL_TERRAIN, "4725000"));
    }

    /** ASM TERRAIN 1 (saisi par la superficie des équipements, 8 404) : 16 000 ; 2 370,752 ; 11 484,016 ; 22 033,248 ; 22 197 878,4. */
    @Test
    void r2_2_controle_asmTerrain1() throws Exception {
        String json = calculer(fiche().avec(SUPERFICIE, d("24404")).avec(SUPERFICIE_EQUIPEMENTS, d("8404"))
                .avec(COEF_EMPRISE, d("0.148172")).avec(CUF, d("0.717751")).avec(COEF_VENDABLE, d("1"))
                .avec(PRIX_ACHAT_M2, d("800")).avec(TAUX_TVA_TERRAIN, BigDecimal.ZERO)
                .avec(TAUX_FRAIS_ENREGISTREMENT, d("0.06")).avec(TAUX_AUTRES_FRAIS_TERRAIN, d("0.077")));

        verifier(json,
                vaut(SUPERFICIE_NETTE, "16000"),
                vaut(EMPRISE_AU_SOL, "2370.752"),
                vaut(PLANCHER_COUVERT, "11484.016"),
                vaut(PLANCHER_VENDABLE, "11484.016"),
                vaut(SURFACE_NON_BATIE, "22033.248"),
                vaut(PRIX_TERRAIN, "19523200"),
                vaut(FRAIS_ENREGISTREMENT, "1171392"),
                vaut(AUTRES_FRAIS_TERRAIN, "1503286.4"),
                vaut(COUT_TOTAL_TERRAIN, "22197878.4"));
    }

    // ------------------------------------------------------------------ formules une par une (§2.2)

    /** PurchaseUnitCostHT = prix / (1 + TVA terrain) ; PriceAreaHT, frais HT ; TVA = TTC − HT ; TotalCostLand TTC (l.865). */
    @Test
    void r2_2_tvaTerrain_montantsHtEtTtc() throws Exception {
        String json = calculer(fiche().avec(SUPERFICIE, d("1000")).avec(COEF_EQUIPEMENTS, BigDecimal.ZERO)
                .avec(COEF_EMPRISE, d("0.3")).avec(CUF, d("3")).avec(COEF_VENDABLE, d("1"))
                .avec(PRIX_ACHAT_M2, d("1200")).avec(TAUX_TVA_TERRAIN, d("0.2"))
                .avec(TAUX_FRAIS_ENREGISTREMENT, d("0.02")).avec(TAUX_AUTRES_FRAIS_TERRAIN, d("0.03")));

        verifier(json,
                vaut(PRIX_ACHAT_M2_HT, "1000"),
                vaut(PRIX_TERRAIN, "1200000"),
                vaut(PRIX_TERRAIN_HT, "1000000"),
                vaut(FRAIS_ENREGISTREMENT, "24000"),
                vaut(FRAIS_ENREGISTREMENT_HT, "20000"),
                vaut(AUTRES_FRAIS_TERRAIN, "36000"),
                vaut(AUTRES_FRAIS_TERRAIN_HT, "30000"),
                vaut(AUTRES_FRAIS_TERRAIN_TVA, "6000"),
                vaut(COUT_TOTAL_TERRAIN, "1260000"));
    }

    /** CostAreaBuilt = coût m² bâti × AreaCuf ; CostAreaNoBuilt = coût m² non bâti × NoBuiltArea ; autres frais = ratio × Σ. */
    @Test
    void r2_2_coutsDeConstruction() throws Exception {
        String json = calculer(yestest(fiche()).avec(COUT_M2_BATI, d("5000")).avec(COUT_M2_NON_BATI, d("200"))
                .avec(TAUX_AUTRES_FRAIS_EXECUTION, d("0.05")));

        verifier(json,
                vaut(COUT_BATI, "14850000"),
                vaut(COUT_NON_BATI, "140600"),
                vaut(AUTRES_FRAIS_EXECUTION, "749530"));
    }

    /** Divisions par zéro → 0 (OMRAN : {@code != 0m ? … : 0m}) : superficie nulle, coefficient d'équipements nul. */
    @Test
    void r2_2_divisionParZero_coefficientNul() throws Exception {
        String json = calculer(fiche().avec(SUPERFICIE, BigDecimal.ZERO).avec(SUPERFICIE_EQUIPEMENTS, d("10")));

        verifier(json, vaut(COEF_EQUIPEMENTS, "0"));
    }

    @Test
    void r2_7_calcul_renvoieLaListeDesErreursVerif_videSansGrille() throws Exception {
        api.calculer(yestest(fiche()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + ERREURS).isArray())
                .andExpect(jsonPath("$." + ERREURS).isEmpty());
    }

    @Test
    void r2_7_calcul_nEcritRien() throws Exception {
        calculer(yestest(fiche()));

        mockMvc.perform(get(RACINE).with(connecte(patrimoine.administrateur()))
                        .param("search", "CAL-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.code=='CAL-" + jeton + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------ deux sens (§2.2 ; §8 bis décision 6)

    @Test
    void d8bis6_empriseSeule_coefficientRecalcule() throws Exception {
        String json = calculer(yestest(fiche()).sans(COEF_EMPRISE).avec(EMPRISE_AU_SOL, d("297")));

        verifier(json, vaut(COEF_EMPRISE, "0.3"), vaut(EMPRISE_AU_SOL, "297"));
    }

    @Test
    void d8bis6_plancherCouvertSeul_cufRecalcule() throws Exception {
        String json = calculer(yestest(fiche()).sans(CUF).avec(PLANCHER_COUVERT, d("2970")));

        verifier(json, vaut(CUF, "3"), vaut(PLANCHER_COUVERT, "2970"));
    }

    @Test
    void d8bis6_plancherVendableSeul_coefVendableRecalcule() throws Exception {
        String json = calculer(yestest(fiche()).sans(COEF_VENDABLE).avec(PLANCHER_VENDABLE, d("1485")));

        verifier(json, vaut(COEF_VENDABLE, "0.5"), vaut(PLANCHER_VENDABLE, "1485"));
    }

    /** coef équipements = montant / Area (l.691-726). */
    @Test
    void d8bis6_superficieEquipementsSeule_coefficientRecalcule() throws Exception {
        String json = calculer(yestest(fiche()).sans(COEF_EQUIPEMENTS).avec(SUPERFICIE_EQUIPEMENTS, d("10")));

        verifier(json, vaut(COEF_EQUIPEMENTS, "0.01"), vaut(SUPERFICIE_NETTE, "990"));
    }

    @Test
    void d8bis6_prixTerrainSeul_prixM2Recalcule() throws Exception {
        String json = calculer(yestest(fiche()).sans(PRIX_ACHAT_M2).avec(PRIX_TERRAIN, d("4500000")));

        verifier(json, vaut(PRIX_ACHAT_M2, "4500"), vaut(PRIX_TERRAIN, "4500000"));
    }

    /** Les deux membres d'une paire envoyés et discordants : le coefficient gagne, le montant est recalculé. */
    @Test
    void d8bis6_lesDeuxEnvoyes_leCoefficientGagne() throws Exception {
        String json = calculer(yestest(fiche()).avec(EMPRISE_AU_SOL, d("500")).avec(PLANCHER_COUVERT, d("10"))
                .avec(PLANCHER_VENDABLE, d("10")).avec(SUPERFICIE_EQUIPEMENTS, d("50")).avec(PRIX_TERRAIN, d("1")));

        verifier(json,
                vaut(EMPRISE_AU_SOL, "297"),
                vaut(PLANCHER_COUVERT, "2970"),
                vaut(PLANCHER_VENDABLE, "2970"),
                vaut(SUPERFICIE_EQUIPEMENTS, "10"),
                vaut(PRIX_TERRAIN, "4500000"));
    }

    // ------------------------------------------------------------------ phases (§2.2, l.1115-1700, mois)

    private static CorpsJson phases(CorpsJson corps, String debut, int[] durees, int[] delais) {
        return corps.avec(DEBUT_ACHAT_TERRAIN, debut + "T00:00:00")
                .avec(DUREE_ACHAT_TERRAIN, durees[0]).avec(DUREE_PERIODE_VALIDATION, durees[1])
                .avec(DUREE_PERMIS_BATIR, durees[2]).avec(DUREE_ETUDES, durees[3]).avec(DUREE_TRAVAUX, durees[4])
                .avec(DUREE_CONCRETISATION, durees[5]).avec(DUREE_FINALISATION, durees[6])
                .avec(DELAI_ACHAT_VALIDATION, delais[0]).avec(DELAI_VALIDATION_PERMIS, delais[1])
                .avec(DELAI_PERMIS_ETUDES, delais[2]).avec(DELAI_PERMIS_TRAVAUX, delais[3])
                .avec(DELAI_TRAVAUX_CONCRETISATION, delais[4]).avec(DELAI_TRAVAUX_FINALISATION, delais[5]);
    }

    private static void dates(String json, Map<String, String> attendues) {
        assertAll(attendues.entrySet().stream().<Executable>map(e -> () -> assertThat(String.valueOf(JsonPath.<Object>read(json,
                "$." + e.getKey()))).as(e.getKey()).startsWith(e.getValue())));
    }

    /** ASM TERRAIN 1 : début 01/03/2022, durées 1, 8, 5, 5, 18, 18, 1, délais nuls — dates stockées par OMRAN. */
    @Test
    void r2_2_phases_asmTerrain1() throws Exception {
        String json = calculer(phases(fiche(), "2022-03-01", new int[]{1, 8, 5, 5, 18, 18, 1}, new int[]{0, 0, 0, 0, 0, 0}));

        dates(json, Map.ofEntries(
                Map.entry(FIN_ACHAT_TERRAIN, "2022-04-01"),
                Map.entry(DEBUT_PERIODE_VALIDATION, "2022-04-01"),
                Map.entry(FIN_PERIODE_VALIDATION, "2022-12-01"),
                Map.entry(DEBUT_PERMIS_BATIR, "2022-12-01"),
                Map.entry(FIN_PERMIS_BATIR, "2023-05-01"),
                Map.entry(DEBUT_ETUDES, "2022-12-01"),
                Map.entry(FIN_ETUDES, "2023-05-01"),
                Map.entry(DEBUT_TRAVAUX, "2023-05-01"),
                Map.entry(FIN_TRAVAUX, "2024-11-01"),
                Map.entry(DEBUT_CONCRETISATION, "2023-05-01"),
                Map.entry(FIN_CONCRETISATION, "2024-11-01"),
                Map.entry(DEBUT_FINALISATION, "2024-11-01"),
                Map.entry(FIN_FINALISATION, "2024-12-01")));
    }

    /**
     * Délais non nuls, enchaînement des setters OMRAN : validation = fin achat + délai ; permis = fin validation + délai ;
     * études = DÉBUT permis + délai ; travaux = fin permis + délai ; concrétisation = DÉBUT travaux + délai ;
     * finalisation = fin travaux + délai.
     */
    @Test
    void r2_2_phases_delaisEnMois() throws Exception {
        String json = calculer(phases(fiche(), "2026-01-15", new int[]{2, 3, 4, 2, 12, 10, 1}, new int[]{1, 2, 1, 3, 6, 2}));

        dates(json, Map.ofEntries(
                Map.entry(FIN_ACHAT_TERRAIN, "2026-03-15"),
                Map.entry(DEBUT_PERIODE_VALIDATION, "2026-04-15"),
                Map.entry(FIN_PERIODE_VALIDATION, "2026-07-15"),
                Map.entry(DEBUT_PERMIS_BATIR, "2026-09-15"),
                Map.entry(FIN_PERMIS_BATIR, "2027-01-15"),
                Map.entry(DEBUT_ETUDES, "2026-10-15"),
                Map.entry(FIN_ETUDES, "2026-12-15"),
                Map.entry(DEBUT_TRAVAUX, "2027-04-15"),
                Map.entry(FIN_TRAVAUX, "2028-04-15"),
                Map.entry(DEBUT_CONCRETISATION, "2027-10-15"),
                Map.entry(FIN_CONCRETISATION, "2028-08-15"),
                Map.entry(DEBUT_FINALISATION, "2028-06-15"),
                Map.entry(FIN_FINALISATION, "2028-07-15")));
    }

    /** {@code AddMonths} : un 31 janvier + 1 mois donne le dernier jour de février. */
    @Test
    void r2_2_phases_finDeMois_bornee() throws Exception {
        String json = calculer(phases(fiche(), "2026-01-31", new int[]{1, 0, 0, 0, 0, 0, 0}, new int[]{0, 0, 0, 0, 0, 0}));

        dates(json, Map.of(FIN_ACHAT_TERRAIN, "2026-02-28"));
    }

    /** Les phases calculées sont stockées à l'écriture (calcul partagé). */
    @Test
    void r2_2_phases_stockeesALaCreation() throws Exception {
        UUID id = api.etude(phases(fiche(), "2022-03-01", new int[]{1, 8, 5, 5, 18, 18, 1}, new int[]{0, 0, 0, 0, 0, 0}));

        Tuple l = fixture.etude(id);
        assertAll(
                () -> assertThat(String.valueOf(l.get("EndDatePurchaseLand"))).startsWith("2022-04-01"),
                () -> assertThat(String.valueOf(l.get("StartDateStudy"))).startsWith("2022-12-01"),
                () -> assertThat(String.valueOf(l.get("EndDateExpenses"))).startsWith("2024-11-01"),
                () -> assertThat(String.valueOf(l.get("EndDateFinalization"))).startsWith("2024-12-01"),
                () -> assertThat(l.get("DelayExpenses")).isEqualTo(18));
    }
}
