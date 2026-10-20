package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code Verif} de l'étude ({@code StkFeasibilityStudy.cs:2408-2494}) — FEATURE-patrimoine-faisabilite.md §2.5 : 400 avec
 * le message OMRAN exact, premier contrôle en échec dans l'ordre (unités de base, poids des tranches, ventilation),
 * comparaisons arrondies à 2 décimales sauf la somme des poids. Étude aux valeurs YESTEST : superficie 1 000, nette 990,
 * emprise 297, plancher couvert 2 970, plancher vendable 2 970.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudeFaisabiliteVerifTest {

    private static final String DETAIL = "$.detail";
    private static final String LIGNES_ETUDE = "SELECT COUNT(*) FROM StkFSBaseUnitStkFeasibilityStudy WHERE FeasibilityStudyId = :id";
    private static final String TRANCHES_ETUDE = "SELECT COUNT(*) FROM StkFeasibilityStudyCfgTranche WHERE FeasibilityStudyId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;

    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;
    private UUID etude;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
        etude = api.etude(yestest(FixtureFaisabilite.fiche("VER-" + jeton, societe.getId())));
    }

    private UUID trancheDeLaSociete(String code) {
        return patrimoine.tranche(code + "-" + jeton, patrimoine.projet("PRJ-" + code + "-" + jeton, societe)).getId();
    }

    /** Unité de surface portant une fonction aux drapeaux vendable, couvert, nette, brute, emprise. */
    private UUID uniteSurface(String code, boolean vendable, boolean couvert, boolean nette, boolean brute, boolean emprise)
            throws Exception {
        return api.unite(code + "-" + jeton, true, api.fonction("F" + code + "-" + jeton, vendable, couvert, nette, brute, emprise));
    }

    private ResultActions unites(UUID unite, String quantite) throws Exception {
        return api.unites(etude, lignes(ligneUniteCorps(1, unite, d(quantite))));
    }

    private static void refus(ResultActions reponse, String message) throws Exception {
        reponse.andExpect(status().isBadRequest()).andExpect(jsonPath(DETAIL).value(message));
    }

    // ------------------------------------------------------------------ 1. VerifBaseUnits, chaque message

    @Test
    void r2_5_1_vendables_400_sommeDifferenteDuPlancherVendable() throws Exception {
        refus(unites(uniteSurface("SV", true, false, false, false, false), "2969"), MSG_VENDABLES);

        assertThat(fixture.compter(LIGNES_ETUDE, etude)).isZero();
    }

    @Test
    void r2_5_1_brut_400_sommeDifferenteDeLaSuperficie() throws Exception {
        refus(unites(uniteSurface("SB", false, false, false, true, false), "999"), MSG_BRUT);
    }

    @Test
    void r2_5_1_nette_400_sommeDifferenteDeLaSuperficieNette() throws Exception {
        refus(unites(uniteSurface("SN", false, false, true, false, false), "1000"), MSG_NETTE);
    }

    @Test
    void r2_5_1_emprise_400_sommeDifferenteDeLEmprise() throws Exception {
        refus(unites(uniteSurface("SE", false, false, false, false, true), "300"), MSG_EMPRISE);
    }

    @Test
    void r2_5_1_couverts_400_sommeDifferenteDuPlancherCouvert() throws Exception {
        refus(unites(uniteSurface("SC", false, true, false, false, false), "2000"), MSG_COUVERTS);
    }

    @Test
    void r2_5_1_sommesEgales_200_lignesEcrites() throws Exception {
        api.unites(etude, lignes(
                        ligneUniteCorps(1, uniteSurface("OV", true, false, false, false, false), d("2970")),
                        ligneUniteCorps(2, uniteSurface("OB", false, false, false, true, false), d("1000")),
                        ligneUniteCorps(3, uniteSurface("ON", false, false, true, false, false), d("990")),
                        ligneUniteCorps(4, uniteSurface("OE", false, false, false, false, true), d("297")),
                        ligneUniteCorps(5, uniteSurface("OC", false, true, false, false, false), d("2970"))))
                .andExpect(status().isOk());

        assertThat(fixture.compter(LIGNES_ETUDE, etude)).isEqualTo(5);
    }

    /** La somme porte sur toutes les lignes concernées. */
    @Test
    void r2_5_1_sommeDePlusieursLignes_200() throws Exception {
        UUID fonction = api.fonction("FSUM-" + jeton, true, false, false, false, false);
        UUID u1 = api.unite("SUM1-" + jeton, true, fonction);
        UUID u2 = api.unite("SUM2-" + jeton, true, fonction);

        api.unites(etude, lignes(ligneUniteCorps(1, u1, d("2000")), ligneUniteCorps(2, u2, d("970"))))
                .andExpect(status().isOk());
    }

    /** {@code Math.Round(x, 2)} : 297,004 et 297 sont égaux. */
    @Test
    void r2_5_1_comparaisonArrondieA2Decimales_200() throws Exception {
        unites(uniteSurface("RND", false, false, false, false, true), "297.004").andExpect(status().isOk());
    }

    @Test
    void r2_5_1_fonctionTotal_ignoree_200() throws Exception {
        UUID unite = api.unite("TOT-" + jeton, true, api.fonctionTotal("FTOT-" + jeton));

        unites(unite, "5").andExpect(status().isOk());
    }

    @Test
    void r2_5_1_uniteNonSurface_ignoree_200() throws Exception {
        UUID unite = api.unite("NSF-" + jeton, false, api.fonction("FNSF-" + jeton, true, false, false, false, false));

        unites(unite, "5").andExpect(status().isOk());
    }

    @Test
    void r2_5_1_uniteSansFonction_ignoree_200() throws Exception {
        unites(api.unite("SSF-" + jeton, true, null), "5").andExpect(status().isOk());
    }

    /** Ordre OMRAN : vendables avant brut, brut avant nette. */
    @Test
    void r2_5_1_ordre_vendablesAvantBrut() throws Exception {
        refus(unites(uniteSurface("OVB", true, false, false, true, false), "5"), MSG_VENDABLES);
    }

    @Test
    void r2_5_1_ordre_brutAvantNette() throws Exception {
        refus(unites(uniteSurface("OBN", false, false, true, true, false), "5"), MSG_BRUT);
    }

    @Test
    void r2_5_1_ordre_netteAvantEmprise() throws Exception {
        refus(unites(uniteSurface("ONE", false, false, true, false, true), "5"), MSG_NETTE);
    }

    @Test
    void r2_5_1_ordre_empriseAvantCouverts() throws Exception {
        refus(unites(uniteSurface("OEC", false, true, false, false, true), "5"), MSG_EMPRISE);
    }

    /** §2.5 : Verif à chaque PATCH — une superficie qui ne correspond plus aux unités « brut » est refusée. */
    @Test
    void r2_5_patch_executeVerif_400_etudeInchangee() throws Exception {
        unites(uniteSurface("PB", false, false, false, true, false), "1000").andExpect(status().isOk());

        refus(api.modifier(etude, corps().avec(SUPERFICIE, d("1200")).json()), MSG_BRUT);

        assertThat(FixturePatrimoine.decimal(fixture.etude(etude).get("Area"))).isEqualByComparingTo("1000");
    }

    // ------------------------------------------------------------------ 2. poids des tranches

    private UUID ligneSimple(String code, String quantite) throws Exception {
        UUID unite = api.unite(code + "-" + jeton, false, null);
        unites(unite, quantite).andExpect(status().isOk());
        return fixture.ligneUnite(etude, unite);
    }

    private String deuxTranches(BigDecimal p1, BigDecimal p2, UUID ligne, String q1, String q2) {
        return lignes(
                tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("T1"), p1), ventilation(ligne, d(q1))),
                tranche(ligneTrancheCorps("Tranche 2", 2, trancheDeLaSociete("T2"), p2), ventilation(ligne, d(q2))));
    }

    @Test
    void r2_5_2_sommeDesPoidsDifferenteDe1_400() throws Exception {
        UUID ligne = ligneSimple("UP", "100");

        refus(api.tranches(etude, deuxTranches(d("0.5"), d("0.4"), ligne, "60", "40")), MSG_TRANCHES);

        assertThat(fixture.compter(TRANCHES_ETUDE, etude)).isZero();
    }

    /** Sans arrondi : 3 × 0,333333 = 0,999999 ≠ 1. */
    @Test
    void r2_5_2_sommeDesPoidsSansArrondi_400() throws Exception {
        UUID ligne = ligneSimple("UT", "3");
        BigDecimal tiers = d("0.333333");

        refus(api.tranches(etude, lignes(
                tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("TA"), tiers), ventilation(ligne, d("1"))),
                tranche(ligneTrancheCorps("Tranche 2", 2, trancheDeLaSociete("TB"), tiers), ventilation(ligne, d("1"))),
                tranche(ligneTrancheCorps("Tranche 3", 3, trancheDeLaSociete("TC"), tiers), ventilation(ligne, d("1"))))),
                MSG_TRANCHES);
    }

    @Test
    void r2_5_2_aucuneTranche_controleSaute_200() throws Exception {
        ligneSimple("UZ", "100");

        api.tranches(etude, lignes(new String[0])).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ 3. ventilation de chaque unité

    @Test
    void r2_5_3_quantiteVentileeDifferente_400_codeDeLUnite() throws Exception {
        UUID ligne = ligneSimple("UVT", "100");

        refus(api.tranches(etude, deuxTranches(d("0.5"), d("0.5"), ligne, "60", "30")), msgVentilation("UVT-" + jeton));
    }

    @Test
    void r2_5_3_quantiteVentileeEgale_200() throws Exception {
        UUID ligne = ligneSimple("UOK", "100");

        api.tranches(etude, deuxTranches(d("0.5"), d("0.5"), ligne, "60", "40")).andExpect(status().isOk());

        assertThat(fixture.compter(TRANCHES_ETUDE, etude)).isEqualTo(2);
    }

    /** {@code Math.Round(x, 2)} des deux côtés : 100,004 ventilé pour 100,001 passe. */
    @Test
    void r2_5_3_comparaisonArrondieA2Decimales_200() throws Exception {
        UUID ligne = ligneSimple("URD", "100.001");

        api.tranches(etude, deuxTranches(d("0.5"), d("0.5"), ligne, "60.004", "40")).andExpect(status().isOk());
    }

    /** Une unité ajoutée alors que des tranches existent, non ventilée : refusée au PUT des unités. */
    @Test
    void r2_5_3_uniteAjouteeNonVentilee_400_auPutDesUnites() throws Exception {
        UUID ventilee = api.unite("UV1-" + jeton, false, null);
        unites(ventilee, "100").andExpect(status().isOk());
        UUID ligne = fixture.ligneUnite(etude, ventilee);
        api.tranches(etude, deuxTranches(d("0.5"), d("0.5"), ligne, "60", "40")).andExpect(status().isOk());
        UUID ajoutee = api.unite("UV2-" + jeton, false, null);

        refus(api.unites(etude, lignes(ligneUniteCorps(1, ventilee, d("100")).avec(ID, ligne),
                        ligneUniteCorps(2, ajoutee, d("10")))),
                msgVentilation("UV2-" + jeton));
    }

    // ------------------------------------------------------------------ ordre global

    @Test
    void r2_5_ordre_poidsAvantVentilation() throws Exception {
        UUID ligne = ligneSimple("UOR", "100");

        refus(api.tranches(etude, deuxTranches(d("0.5"), d("0.4"), ligne, "60", "30")), MSG_TRANCHES);
    }

    /** Unité devenue « vendable » par sa configuration : le contrôle des unités passe avant celui des tranches. */
    @Test
    void r2_5_ordre_unitesAvantTranches() throws Exception {
        UUID unite = api.unite("UUT-" + jeton, true, null);
        unites(unite, "100").andExpect(status().isOk());
        UUID ligne = fixture.ligneUnite(etude, unite);
        api.modifierUnite(unite, corps().avec(FONCTION_SPECIFIQUE_ID,
                api.fonction("FUUT-" + jeton, true, false, false, false, false))).andExpect(status().isOk());

        refus(api.tranches(etude, deuxTranches(d("0.5"), d("0.4"), ligne, "60", "30")), MSG_VENDABLES);
    }
}
