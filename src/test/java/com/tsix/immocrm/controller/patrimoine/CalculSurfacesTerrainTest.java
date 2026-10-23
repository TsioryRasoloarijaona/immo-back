package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.d;
import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.lireDecimal;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Calculs deux sens de {@code CalculSurfacesTerrain} — FEATURE-patrimoine-prospections-terrain.md §3.1 et §5.5, exemples
 * chiffrés du spec (ligne de production en MA, variante hors MA, sens inverse, division par zéro). Le spec ne donne pas la
 * signature de la classe pure : testée par l'aperçu {@code POST …/apercu-calcul}, qui n'écrit rien. Tolérance 1e-6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CalculSurfacesTerrainTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;

    private ApiProspectionsTerrain api;

    @BeforeEach
    void setUp() {
        fixture.configuration();
        fixture.formule("MA");
        api =new ApiProspectionsTerrain(mockMvc, patrimoine.administrateur());
    }

    /** Ligne de production (§1.1) telle que saisie : coefficients donnés, montants absents. */
    private static CorpsJson ligneDeProd() {
        return corps().avec(SUPERFICIE, d("1200")).avec(SUPERFICIE_EQUIPEMENTS, d("100")).avec(COS, d("0.30"))
                .avec(CUF, d("2.5")).avec(COEF_VENDABLE, d("1")).avec(COEF_COMMERCE, d("0"))
                .avec(COEF_BUREAU, d("0")).avec(COEF_PARKING, d("0")).avec(PRIX_M2, d("4000"));
    }

    private String apercu(CorpsJson corps) throws Exception {
        return api.apercu(corps).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static void vaut(String json, String champ, String attendu) {
        assertThat(lireDecimal(json, "$." + champ)).as(champ).isCloseTo(new BigDecimal(attendu), within(TOLERANCE));
    }

    // ------------------------------------------------------------------ MA (CountryFormule = MA)

    /** §3.1 exemple chiffré : net 1 100, emprise 330, plancher 2 750, vendable 2 750, habitat 2 750, coef 0,090909, prix 4,8 M. */
    @Test
    void r3_1_ligneDeProd_MA_toutesLesGrandeurs() throws Exception {
        String json = apercu(ligneDeProd());

        assertAll(
                () -> vaut(json, SUPERFICIE_NETTE, "1100"),
                () -> vaut(json, COEF_EQUIPEMENTS, "0.090909"),
                () -> vaut(json, SUPERFICIE_COS, "330"),
                () -> vaut(json, SUPERFICIE_CUF, "2750"),
                () -> vaut(json, SUPERFICIE_VENDABLE, "2750"),
                () -> vaut(json, SUPERFICIE_HABITAT, "2750"),
                () -> vaut(json, SUPERFICIE_COMMERCE, "0"),
                () -> vaut(json, SUPERFICIE_BUREAU, "0"),
                () -> vaut(json, SUPERFICIE_PARKING, "0"),
                () -> vaut(json, PRIX, "4800000"));
    }

    /** §3.1 : la superficie nette retire les quatre surfaces (voies, à céder, à régulariser, cédée). */
    @Test
    void r3_1_superficieNette_retireLesQuatreSurfaces() throws Exception {
        String json = apercu(ligneDeProd().avec("superficieEquipementACeder", d("50"))
                .avec("superficieARegler", d("30")).avec("superficieACeder", d("20")));

        assertAll(
                () -> vaut(json, SUPERFICIE_NETTE, "1000"),
                () -> vaut(json, COEF_EQUIPEMENTS, "0.1"),
                () -> vaut(json, SUPERFICIE_COS, "300"),
                () -> vaut(json, SUPERFICIE_CUF, "2500"));
    }

    /** §3.1 sens inverse : emprise donnée 440, champModifie = superficieCos → COS = 440 / 1 100 = 0,40. */
    @Test
    void r3_1_sensInverse_empriseDonnee_cosDerive_MA() throws Exception {
        String json = apercu(ligneDeProd().avec(SUPERFICIE_COS, d("440")).avec(CHAMP_MODIFIE, SUPERFICIE_COS));

        assertAll(
                () -> vaut(json, COS, "0.40"),
                () -> vaut(json, SUPERFICIE_COS, "440"));
    }

    /** §3.1 : deux côtés d'une paire envoyés sans champModifie → le coefficient gagne (patron CalculFaisabilite.paire). */
    @Test
    void r3_1_coefficientEtMontantEnvoyes_leCoefficientGagne() throws Exception {
        String json = apercu(ligneDeProd().avec(SUPERFICIE_COS, d("440")));

        assertAll(
                () -> vaut(json, COS, "0.30"),
                () -> vaut(json, SUPERFICIE_COS, "330"));
    }

    /** §3.1 : plancher couvert donné (MA) → CUF = AreaCuf / AreaNet. */
    @Test
    void r3_1_sensInverse_plancherDonne_cufDerive_MA() throws Exception {
        String json = apercu(ligneDeProd().avec(SUPERFICIE_CUF, d("3300")).avec(CHAMP_MODIFIE, SUPERFICIE_CUF));

        vaut(json, CUF, "3");
    }

    /** §3.1 : vendable donné → coefVendable = SalableArea / AreaCuf. */
    @Test
    void r3_1_sensInverse_vendableDonne_coefVendableDerive() throws Exception {
        String json = apercu(ligneDeProd().avec(SUPERFICIE_VENDABLE, d("1375")).avec(CHAMP_MODIFIE, SUPERFICIE_VENDABLE));

        vaut(json, COEF_VENDABLE, "0.5");
    }

    /** §3.1 exemple : commerce 0,10 et bureau 0,05 de 1 200 → 120 et 60 ; habitat 2 750 − 60 − 120 = 2 570 ; parking coef × Area. */
    @Test
    void r3_1_commerceBureauParking_coefFoisSuperficie_habitatEnDecoule() throws Exception {
        String json = apercu(ligneDeProd().avec(COEF_COMMERCE, d("0.10")).avec(COEF_BUREAU, d("0.05"))
                .avec(COEF_PARKING, d("0.2")));

        assertAll(
                () -> vaut(json, SUPERFICIE_COMMERCE, "120"),
                () -> vaut(json, SUPERFICIE_BUREAU, "60"),
                () -> vaut(json, SUPERFICIE_PARKING, "240"),
                () -> vaut(json, SUPERFICIE_HABITAT, "2570"));
    }

    /** §3.1 : montant commerce / bureau / parking donné → coef = montant / Area. */
    @Test
    void r3_1_sensInverse_montantsCommerceBureauParking_coefsDerives() throws Exception {
        String json = apercu(ligneDeProd().sans(COEF_COMMERCE).sans(COEF_BUREAU).sans(COEF_PARKING)
                .avec(SUPERFICIE_COMMERCE, d("240")).avec(SUPERFICIE_BUREAU, d("120"))
                .avec(SUPERFICIE_PARKING, d("600")));

        assertAll(
                () -> vaut(json, COEF_COMMERCE, "0.2"),
                () -> vaut(json, COEF_BUREAU, "0.1"),
                () -> vaut(json, COEF_PARKING, "0.5"));
    }

    /** §3.1 : prix total donné → prix du m² = ProspectionPrice / Area. */
    @Test
    void r3_1_sensInverse_prixDonne_prixM2Derive() throws Exception {
        String json = apercu(ligneDeProd().sans(PRIX_M2).avec(PRIX, d("6000000")).avec(CHAMP_MODIFIE, PRIX));

        vaut(json, PRIX_M2, "5000");
    }

    /** §3.1 : le prix total se recalcule quand la superficie change (setter Area l.1156). */
    @Test
    void r3_1_prixRecalculeQuandLaSuperficieChange() throws Exception {
        String json = apercu(ligneDeProd().avec(SUPERFICIE, d("1500")).avec(CHAMP_MODIFIE, SUPERFICIE));

        vaut(json, PRIX, "6000000");
    }

    // ------------------------------------------------------------------ hors MA

    /** §3.1 hors MA : emprise = Cos × Area = 360 ; plancher couvert = Area × Cos × Cuf = 900. */
    @Test
    void r3_1_horsMA_empriseEtPlancherSurLaSuperficie() throws Exception {
        fixture.formule("FR");

        String json = apercu(ligneDeProd());

        assertAll(
                () -> vaut(json, SUPERFICIE_COS, "360"),
                () -> vaut(json, SUPERFICIE_CUF, "900"));
    }

    /** §3.1 hors MA, sens inverse : plancher donné 1 800 → Cuf = 1 800 / (1 200 × 0,3) = 5. */
    @Test
    void r3_1_horsMA_plancherDonne_cufDerive() throws Exception {
        fixture.formule("FR");

        String json = apercu(ligneDeProd().avec(SUPERFICIE_CUF, d("1800")).avec(CHAMP_MODIFIE, SUPERFICIE_CUF));

        vaut(json, CUF, "5");
    }

    /** §3.1 hors MA, sens inverse : emprise donnée → Cos = AreaCos / Area. */
    @Test
    void r3_1_horsMA_empriseDonnee_cosSurLaSuperficie() throws Exception {
        fixture.formule("FR");

        String json = apercu(ligneDeProd().avec(SUPERFICIE_COS, d("480")).avec(CHAMP_MODIFIE, SUPERFICIE_COS));

        vaut(json, COS, "0.4");
    }

    // ------------------------------------------------------------------ division par zéro → 0

    /** §3 : superficie nette nulle → coef d'équipements 0, COS dérivé 0 (comme OMRAN), sans erreur. */
    @Test
    void r3_divisionParZero_netNul_coefficientsAZero() throws Exception {
        String json = apercu(ligneDeProd().avec(SUPERFICIE_EQUIPEMENTS, d("1200")).avec(SUPERFICIE_COS, d("100"))
                .avec(CHAMP_MODIFIE, SUPERFICIE_COS));

        assertAll(
                () -> vaut(json, SUPERFICIE_NETTE, "0"),
                () -> vaut(json, COEF_EQUIPEMENTS, "0"),
                () -> vaut(json, COS, "0"));
    }

    /** §3 : plancher couvert nul → coefVendable dérivé 0. */
    @Test
    void r3_divisionParZero_plancherNul_coefVendableAZero() throws Exception {
        String json = apercu(ligneDeProd().avec(CUF, d("0")).avec(SUPERFICIE_VENDABLE, d("500"))
                .avec(CHAMP_MODIFIE, SUPERFICIE_VENDABLE));

        vaut(json, COEF_VENDABLE, "0");
    }

    /** §5.5 : l'aperçu n'écrit rien — aucune prospection créée sous le code envoyé. */
    @Test
    void r5_5_apercu_nEcritRien() throws Exception {
        String code = FixtureTerrains.jeton("APC-");

        api.apercu(ligneDeProd().avec(CODE, code)).andExpect(status().isOk());

        assertThat(fixture.prospectionsDeCode(code)).isZero();
    }
}
