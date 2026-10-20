package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.ChampsAViderJson.avecChampsAVider;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fiche, CRUD, Bloquer / Débloquer, suppression et grilles des études de faisabilité — FEATURE-patrimoine-faisabilite.md
 * §2.1, §2.2 (écriture), §2.3, §2.4, §2.7 et §8 bis (décisions 2, 3, 6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudeFaisabiliteControllerTest {

    private static final String LIGNES_ETUDE = "SELECT COUNT(*) FROM StkFSBaseUnitStkFeasibilityStudy WHERE FeasibilityStudyId = :id";
    private static final String TRANCHES_ETUDE = "SELECT COUNT(*) FROM StkFeasibilityStudyCfgTranche WHERE FeasibilityStudyId = :id";
    private static final String VENTILATIONS_ETUDE =
            "SELECT COUNT(*) FROM StkFeasibilityStudyCfgTrancheStkBaseUnit WHERE FeasibilityStudyId = :id";
    private static final String ETUDE = "SELECT COUNT(*) FROM StkFeasibilityStudy WHERE PKey = :id";
    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTrancheOnglets onglets;
    @Autowired private FixtureFaisabilite fixture;

    private AdmUser admin;
    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        admin = patrimoine.administrateur();
        api = new ApiFaisabilite(mockMvc, admin);
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
    }

    private CorpsJson fiche(String suffixe) {
        return FixtureFaisabilite.fiche(suffixe + "-" + jeton, societe.getId());
    }

    private UUID etudeYestest(String suffixe) throws Exception {
        return api.etude(yestest(fiche(suffixe)));
    }

    private CfgTranche trancheDeLaSociete(String code) {
        return patrimoine.tranche(code + "-" + jeton, patrimoine.projet("PRJ-" + code + "-" + jeton, societe));
    }

    /** Étude avec une unité (sans fonction) de 100 ventilée sur une tranche de poids 1 : grilles des deux tables. */
    private UUID etudeAvecGrilles(String suffixe) throws Exception {
        UUID id = etudeYestest(suffixe);
        UUID unite = api.unite("U-" + suffixe + "-" + jeton, false, null);
        api.unites(id, lignes(ligneUniteCorps(1, unite, d("100")))).andExpect(status().isOk());
        UUID ligne = fixture.ligneUnite(id, unite);
        api.tranches(id, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("TR1").getId(), d("1")),
                ventilation(ligne, d("100"))))).andExpect(status().isOk());
        return id;
    }

    // ------------------------------------------------------------------ §2.1 création : refus

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CODE, DESCRIPTION, DATE_DOCUMENT, SOCIETE_ID})
    void r2_1_creation_400_champObligatoireAbsent(String champ) throws Exception {
        api.creer(fiche("OBL").sans(champ)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_1_creation_400_societeInactive() throws Exception {
        CfgCompany inactive = patrimoine.entite("INA-" + jeton, false);

        api.creer(FixtureFaisabilite.fiche("INA-" + jeton, inactive.getId())).andExpect(status().isBadRequest());
    }

    @Test
    void r2_1_creation_409_codeDejaPris() throws Exception {
        api.etude(fiche("DBL"));

        api.creer(fiche("DBL")).andExpect(status().isConflict());
    }

    @Test
    void r2_1_modification_409_codeDUneAutreEtude() throws Exception {
        api.etude(fiche("PRIS"));
        UUID id = api.etude(fiche("LIBRE"));

        api.modifier(id, corps().avec(CODE, "PRIS-" + jeton).json()).andExpect(status().isConflict());

        assertThat(fixture.etude(id).get("Code")).isEqualTo("LIBRE-" + jeton);
    }

    @Test
    void r2_7_fiche_404_idInconnu() throws Exception {
        api.etude(fiche("F404"));

        mockMvc.perform(get(FICHE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    @Test
    void r2_7_modification_404_idInconnu() throws Exception {
        api.modifier(UUID.randomUUID(), corps().avec(DESCRIPTION, "X").json()).andExpect(status().isNotFound());
    }

    @Test
    void r2_7_suppression_404_idInconnu() throws Exception {
        mockMvc.perform(delete(FICHE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §2.1 création : ce qui est écrit

    @Test
    void r2_1_creation_statutInitialEnEtude_entiteConstante_trace() throws Exception {
        UUID id = api.etude(fiche("CRE"));

        Tuple ligne = fixture.etude(id);
        assertAll(
                () -> assertThat(guid(ligne.get("StkFeasibilityStudyStatusId"))).isEqualTo(guid(STATUT_EN_ETUDE)),
                () -> assertThat(guid(ligne.get("StrEntityId"))).isEqualTo(guid(ENTITE_ETUDE)),
                () -> assertThat(guid(ligne.get("CfgCompanyId"))).isEqualTo(guid(societe.getId())),
                () -> assertThat(String.valueOf(ligne.get("DocDate"))).startsWith("2026-10-01"),
                () -> assertThat(ligne.get("Description")).isEqualTo("Etude CRE-" + jeton),
                () -> assertThat(ligne.get("IsLocked")).isNotEqualTo(Boolean.TRUE),
                () -> assertThat(ligne.get("UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(ligne.get("UserCreateDateTime")).isNotNull());
    }

    @Test
    void r2_1_fiche_200_statutEnEtude() throws Exception {
        UUID id = api.etude(fiche("FIC"));

        mockMvc.perform(get(FICHE, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("FIC-" + jeton))
                .andExpect(jsonPath("$." + STATUT_ID).value(equalToIgnoringCase(STATUT_EN_ETUDE.toString())));
    }

    /** Le statut est en lecture seule : un {@code statutId} envoyé à la création est ignoré. */
    @Test
    void r2_1_statutLectureSeule_statutIdDuCorpsIgnore() throws Exception {
        UUID id = api.etude(fiche("STA").avec(STATUT_ID, UUID.randomUUID()));

        assertThat(guid(fixture.etude(id).get("StkFeasibilityStudyStatusId"))).isEqualTo(guid(STATUT_EN_ETUDE));
    }

    @Test
    void r2_1_valeursParDefaut_tvaDefaut020_tvaInterets010() throws Exception {
        UUID id = api.etude(fiche("DEF"));

        Tuple ligne = fixture.etude(id);
        assertAll(
                () -> assertThat(decimal(ligne.get("DefaultVat"))).isEqualByComparingTo("0.20"),
                () -> assertThat(decimal(ligne.get("InterestVatRatio"))).isEqualByComparingTo("0.10"));
    }

    /** §8 bis décision 3 : une ligne d'historique au statut initial, au nom de l'appelant. */
    @Test
    void d8bis3_creation_historiqueInitialEcrit() throws Exception {
        UUID id = api.etude(fiche("HIS"));

        List<Tuple> historique = fixture.historique(id);
        assertThat(historique).singleElement().satisfies(h -> assertAll(
                () -> assertThat(guid(h.get("statut"))).isEqualTo(guid(STATUT_EN_ETUDE)),
                () -> assertThat(guid(h.get("utilisateur"))).isEqualTo(guid(admin.getId())),
                () -> assertThat(h.get("DocDate")).isNotNull()));
    }

    // ------------------------------------------------------------------ §2.2 calcul partagé appelé à l'écriture

    @Test
    void r2_2_creation_blocACalculeEtStocke_valeursYestest() throws Exception {
        UUID id = etudeYestest("BLA");

        Tuple l = fixture.etude(id);
        assertAll(
                () -> assertThat(decimal(l.get("AreaNet"))).isEqualByComparingTo("990"),
                () -> assertThat(decimal(l.get("EquipementsAmount"))).isEqualByComparingTo("10"),
                () -> assertThat(decimal(l.get("OccupationGroundArea"))).isEqualByComparingTo("297"),
                () -> assertThat(decimal(l.get("TotalCoveredArea"))).isEqualByComparingTo("2970"),
                () -> assertThat(decimal(l.get("SalableArea"))).isEqualByComparingTo("2970"),
                () -> assertThat(decimal(l.get("NoBuiltArea"))).isEqualByComparingTo("703"),
                () -> assertThat(decimal(l.get("PriceArea"))).isEqualByComparingTo("4500000"),
                () -> assertThat(decimal(l.get("RegistryCosts"))).isEqualByComparingTo("90000"),
                () -> assertThat(decimal(l.get("OtherCostsLand"))).isEqualByComparingTo("135000"),
                () -> assertThat(decimal(l.get("TotalCostLand"))).isEqualByComparingTo("4725000"));
    }

    /** §2.1 piège de nommage : OtherCostsExecutionHT porte les autres frais de TERRAIN HT (135 000 sur YESTEST). */
    @Test
    void r2_1_otherCostsExecutionHT_porteLesAutresFraisDeTerrainHt() throws Exception {
        UUID id = etudeYestest("PIE");

        assertThat(decimal(fixture.etude(id).get("OtherCostsExecutionHT"))).isEqualByComparingTo("135000");
    }

    /** §8 bis décision 6 : saisie par le montant, le coefficient est recalculé et stocké. */
    @Test
    void d8bis6_saisieParMontant_coefficientStocke() throws Exception {
        UUID id = api.etude(fiche("MNT").avec(SUPERFICIE, d("1000")).avec(SUPERFICIE_EQUIPEMENTS, d("10"))
                .avec(EMPRISE_AU_SOL, d("297")).avec(CUF, d("3")).avec(COEF_VENDABLE, d("1")));

        Tuple l = fixture.etude(id);
        assertAll(
                () -> assertThat(decimal(l.get("EquipementsCoef"))).isEqualByComparingTo("0.01"),
                () -> assertThat(decimal(l.get("OccupationSolRatio"))).isEqualByComparingTo("0.3"));
    }

    /** §8 bis décision 6 : le coefficient est la saisie stockée ; changer la superficie recalcule les montants. */
    @Test
    void d8bis6_modificationSuperficie_coefficientsConserves_montantsRecalcules() throws Exception {
        UUID id = api.etude(fiche("SUP").avec(SUPERFICIE, d("1000")).avec(SUPERFICIE_EQUIPEMENTS, d("10"))
                .avec(EMPRISE_AU_SOL, d("297")).avec(CUF, d("3")).avec(COEF_VENDABLE, d("1")));

        api.modifier(id, corps().avec(SUPERFICIE, d("2000")).json()).andExpect(status().isOk());

        Tuple l = fixture.etude(id);
        assertAll(
                () -> assertThat(decimal(l.get("EquipementsCoef"))).isEqualByComparingTo("0.01"),
                () -> assertThat(decimal(l.get("EquipementsAmount"))).isEqualByComparingTo("20"),
                () -> assertThat(decimal(l.get("AreaNet"))).isEqualByComparingTo("1980"),
                () -> assertThat(decimal(l.get("OccupationSolRatio"))).isEqualByComparingTo("0.3"),
                () -> assertThat(decimal(l.get("OccupationGroundArea"))).isEqualByComparingTo("594"));
    }

    @Test
    void r2_7_modification_champsAVider_terrainRetire() throws Exception {
        UUID terrain = onglets.terrain("TER-" + jeton);
        UUID id = api.etude(fiche("VID").avec(TERRAIN_ID, terrain));

        api.modifier(id, avecChampsAVider(corps(), TERRAIN_ID)).andExpect(status().isOk());

        assertThat(fixture.etude(id).get("StkItemId")).isNull();
    }

    // ------------------------------------------------------------------ §2.3 bilan en lecture seule ; §8 bis décision 2

    @Test
    void r2_3_bilanLectureSeule_valeurStockeeConserveeALaModification() throws Exception {
        UUID id = etudeYestest("BIL");
        fixture.chiffreAffairesStocke(id, d("123456"));

        api.modifier(id, corps().avec(DESCRIPTION, "Modifiee " + jeton).json()).andExpect(status().isOk());

        assertThat(decimal(fixture.etude(id).get("Turnover"))).isEqualByComparingTo("123456");
    }

    @Test
    void d8bis2_modificationAvecPrevisions_permise_signaleARecalculer() throws Exception {
        UUID id = etudeYestest("PRV");
        fixture.prevision(id);

        api.modifier(id, corps().avec(DESCRIPTION, "Modifiee " + jeton).json())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + PREVISIONS_A_RECALCULER).value(true));
    }

    @Test
    void d8bis2_modificationSansPrevision_rienARecalculer() throws Exception {
        UUID id = etudeYestest("SPR");

        api.modifier(id, corps().avec(DESCRIPTION, "Modifiee " + jeton).json())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + PREVISIONS_A_RECALCULER).value(false));
    }

    // ------------------------------------------------------------------ §8 bis décision 3 : suppression

    @Test
    void d8bis3_suppression_204_etudeLignesTranchesEtVentilationsSupprimees() throws Exception {
        UUID id = etudeAvecGrilles("DEL");

        mockMvc.perform(delete(FICHE, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(List.of(fixture.compter(ETUDE, id), fixture.compter(LIGNES_ETUDE, id), fixture.compter(TRANCHES_ETUDE, id),
                fixture.compter(VENTILATIONS_ETUDE, id))).containsOnly(0);
    }

    @Test
    void d8bis3_suppression_409_etudeAvecPrevision_conservee() throws Exception {
        UUID id = etudeYestest("DPR");
        fixture.prevision(id);

        mockMvc.perform(delete(FICHE, id).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.compter(ETUDE, id)).isEqualTo(1);
    }

    @Test
    void d8bis3_suppression_409_etudeAvecFinancement_conservee() throws Exception {
        UUID id = etudeYestest("DFI");
        fixture.financement(id);

        mockMvc.perform(delete(FICHE, id).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.compter(ETUDE, id)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ §2.7 Bloquer / Débloquer

    @Test
    void r2_7_bloquer_isLockedVrai_debloquer_isLockedFaux() throws Exception {
        UUID id = etudeYestest("BLQ");

        api.bloquer(id).andExpect(status().is2xxSuccessful());
        Object bloquee = fixture.etude(id).get("IsLocked");
        api.debloquer(id).andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(bloquee).isEqualTo(Boolean.TRUE),
                () -> assertThat(fixture.etude(id).get("IsLocked")).isEqualTo(Boolean.FALSE));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"modifier", "unites", "tranches", "supprimer", "bloquer"})
    void r2_7_etudeBloquee_touteEcriture_409_etudeBloquee(String ecriture) throws Exception {
        UUID id = etudeYestest("LCK");
        api.bloquer(id).andExpect(status().is2xxSuccessful());

        ecrire(ecriture, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MSG_BLOQUEE)));

        assertAll(
                () -> assertThat(fixture.compter(ETUDE, id)).isEqualTo(1),
                () -> assertThat(fixture.etude(id).get("Description")).isEqualTo("Etude LCK-" + jeton));
    }

    private ResultActions ecrire(String ecriture, UUID id) throws Exception {
        return switch (ecriture) {
            case "modifier" -> api.modifier(id, corps().avec(DESCRIPTION, "Interdit").json());
            case "unites" -> api.unites(id, lignes(new String[0]));
            case "tranches" -> api.tranches(id, lignes(new String[0]));
            case "supprimer" -> mockMvc.perform(delete(FICHE, id).with(connecte(admin)));
            default -> api.bloquer(id);
        };
    }

    @Test
    void r2_7_debloquee_modification_200() throws Exception {
        UUID id = etudeYestest("DBQ");
        api.bloquer(id).andExpect(status().is2xxSuccessful());
        api.debloquer(id).andExpect(status().is2xxSuccessful());

        api.modifier(id, corps().avec(DESCRIPTION, "Apres deblocage").json()).andExpect(status().isOk());

        assertThat(fixture.etude(id).get("Description")).isEqualTo("Apres deblocage");
    }

    /** §2.5 : Bloquer = enregistrer, donc passe par Verif (unité devenue « vendable » après la saisie de la grille). */
    @Test
    void r2_5_bloquer_executeVerif_400_etudeNonBloquee() throws Exception {
        UUID id = etudeYestest("BVF");
        UUID vendable = api.fonction("FV-" + jeton, true, false, false, false, false);
        UUID unite = api.unite("UV-" + jeton, true, null);
        api.unites(id, lignes(ligneUniteCorps(1, unite, d("1")))).andExpect(status().isOk());
        api.modifierUnite(unite, corps().avec(FONCTION_SPECIFIQUE_ID, vendable)).andExpect(status().isOk());

        api.bloquer(id)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(MSG_VENDABLES));

        assertThat(fixture.etude(id).get("IsLocked")).isNotEqualTo(Boolean.TRUE);
    }

    // ------------------------------------------------------------------ §2.7 liste et choix

    @Test
    void r2_7_liste_search_contientLEtude() throws Exception {
        UUID id = api.etude(fiche("LST"));

        mockMvc.perform(get(RACINE).with(connecte(admin)).param("search", "LST-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void r2_7_liste_filtreSociete_exclutLesAutresSocietes() throws Exception {
        UUID id = api.etude(fiche("SOC"));
        CfgCompany autre = patrimoine.entite("AUT-" + jeton, true);

        mockMvc.perform(get(RACINE).with(connecte(admin)).param(SOCIETE_ID, autre.getId().toString())
                        .param("search", "SOC-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]").doesNotExist());
    }

    @Test
    void r2_7_liste_filtreStatut_contientLEtudeEnEtude() throws Exception {
        UUID id = api.etude(fiche("STU"));

        mockMvc.perform(get(RACINE).with(connecte(admin)).param(STATUT_ID, STATUT_EN_ETUDE.toString())
                        .param("search", "STU-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void r2_1_choixTerrains_articlesDeNatureTER_seulement() throws Exception {
        UUID terrain = onglets.terrain("TER-" + jeton);
        UUID bati = onglets.article("BAT-" + jeton, null, patrimoine.natureBat(), patrimoine.vocationVente());

        mockMvc.perform(get(CHOIX_TERRAINS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + terrain + "')]").exists())
                .andExpect(jsonPath("$..[?(@.id=='" + bati + "')]").doesNotExist());
    }

    @Test
    void r2_4_choixTranches_tranchesDeLaSociete_seulement() throws Exception {
        CfgTranche aLaSociete = trancheDeLaSociete("CH1");
        CfgCompany autre = patrimoine.entite("AUT-" + jeton, true);
        CfgTranche ailleurs = patrimoine.tranche("CH2-" + jeton, patrimoine.projet("PRJ-CH2-" + jeton, autre));

        mockMvc.perform(get(CHOIX_TRANCHES).with(connecte(admin)).param(SOCIETE_ID, societe.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + aLaSociete.getId() + "')]").exists())
                .andExpect(jsonPath("$..[?(@.id=='" + ailleurs.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------ §2.4 grilles

    @Test
    void r2_4_unites_put_ecritLesLignes() throws Exception {
        UUID id = etudeYestest("GUN");
        UUID unite = api.unite("UG-" + jeton, false, null);

        api.unites(id, lignes(ligneUniteCorps(3, unite, d("42.5")))).andExpect(status().isOk());

        assertThat(fixture.compter(LIGNES_ETUDE + " AND StkFSBaseUnitId = '" + unite + "' AND Sorting = 3 AND Quantity = 42.5",
                id)).isEqualTo(1);
    }

    @Test
    void r2_4_tranches_put_ecritTrancheEtVentilations() throws Exception {
        UUID id = etudeAvecGrilles("GTR");

        assertAll(
                () -> assertThat(fixture.compter(TRANCHES_ETUDE + " AND Code = N'Tranche 1' AND Sorting = 1 AND Weight = 1", id))
                        .isEqualTo(1),
                () -> assertThat(fixture.compter(VENTILATIONS_ETUDE + " AND Quantity = 100", id)).isEqualTo(1));
    }

    /** §2.4 : retirer une ligne d'unité efface ses ventilations par tranche ({@code BeforeUpdate} l.3284-3287). */
    @Test
    void r2_4_retraitDUneUnite_effaceSesVentilations_garderLesAutres() throws Exception {
        UUID id = etudeYestest("RET");
        UUID gardee = api.unite("UK-" + jeton, false, null);
        UUID retiree = api.unite("UR-" + jeton, false, null);
        api.unites(id, lignes(ligneUniteCorps(1, gardee, d("100")), ligneUniteCorps(2, retiree, d("50"))))
                .andExpect(status().isOk());
        UUID ligneGardee = fixture.ligneUnite(id, gardee);
        UUID ligneRetiree = fixture.ligneUnite(id, retiree);
        api.tranches(id, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("TRR").getId(), d("1")),
                ventilation(ligneGardee, d("100")), ventilation(ligneRetiree, d("50"))))).andExpect(status().isOk());

        api.unites(id, lignes(ligneUniteCorps(1, gardee, d("100")).avec(ID, ligneGardee))).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.compter(VENTILATIONS_ETUDE + " AND StkBaseUnitId = '" + ligneRetiree + "'", id)).isZero(),
                () -> assertThat(fixture.compter(VENTILATIONS_ETUDE + " AND StkBaseUnitId = '" + ligneGardee + "'", id))
                        .isEqualTo(1),
                () -> assertThat(fixture.compter(LIGNES_ETUDE, id)).isEqualTo(1));
    }

    @Test
    void r2_4_tranche_400_trancheDUneAutreSociete() throws Exception {
        UUID id = etudeYestest("TAS");
        CfgCompany autre = patrimoine.entite("AUT-" + jeton, true);
        CfgTranche ailleurs = patrimoine.tranche("TAS-" + jeton, patrimoine.projet("PRJ-TAS-" + jeton, autre));

        api.tranches(id, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, ailleurs.getId(), d("1")))))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter(TRANCHES_ETUDE, id)).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CODE, SORTING})
    void r2_4_tranche_400_champObligatoireAbsent(String champ) throws Exception {
        UUID id = etudeYestest("TOB");

        api.tranches(id, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("TOB").getId(), d("1"))
                        .sans(champ))))
                .andExpect(status().isBadRequest());
    }

    /** §2.4 : la ventilation pointe une ligne d'unité de CETTE étude. */
    @Test
    void r2_4_ventilation_400_ligneDUniteDUneAutreEtude() throws Exception {
        UUID id = etudeYestest("VA1");
        UUID autreEtude = etudeYestest("VA2");
        UUID unite = api.unite("UA-" + jeton, false, null);
        api.unites(autreEtude, lignes(ligneUniteCorps(1, unite, d("100")))).andExpect(status().isOk());
        UUID ligneAilleurs = fixture.ligneUnite(autreEtude, unite);

        api.tranches(id, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("TVA").getId(), d("1")),
                        ventilation(ligneAilleurs, d("100")))))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter(VENTILATIONS_ETUDE, id)).isZero();
    }

    @Test
    void r2_4_tranche_poidsEtQuantiteDecimaux_ecrits() throws Exception {
        UUID id = etudeYestest("TDC");
        UUID unite = api.unite("UD-" + jeton, false, null);
        api.unites(id, lignes(ligneUniteCorps(1, unite, d("10")))).andExpect(status().isOk());
        UUID ligne = fixture.ligneUnite(id, unite);
        UUID t1 = trancheDeLaSociete("TD1").getId();
        UUID t2 = trancheDeLaSociete("TD2").getId();

        api.tranches(id, lignes(
                        tranche(ligneTrancheCorps("Tranche 1", 1, t1, new BigDecimal("0.25")), ventilation(ligne, d("2.5"))),
                        tranche(ligneTrancheCorps("Tranche 2", 2, t2, new BigDecimal("0.75")), ventilation(ligne, d("7.5")))))
                .andExpect(status().isOk());

        assertThat(fixture.compter(TRANCHES_ETUDE + " AND Weight IN (0.25, 0.75)", id)).isEqualTo(2);
    }
}
