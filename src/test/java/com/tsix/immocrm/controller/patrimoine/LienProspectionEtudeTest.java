package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureLienProspection;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.COUT_M2_BATI;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.COUT_M2_NON_BATI;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.FICHE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.MSG_BLOQUEE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.RACINE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.TERRAIN_ID;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.yestest;
import static com.tsix.immocrm.fixtures.FixtureLienProspection.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lien étude de faisabilité ↔ prospection terrain — FEATURE-patrimoine-etude-prospection.md §1, §2 (R1-R8, R12), §3.
 * Prospection de la fixture P7b : 1 200 m², COS 0,3, CUF 2,5, coef. vendable 1, prix de prospection 4 000 ; le test pose
 * coef. équipements 0,1, coût non bâti 1 500 et un PurchaseUnitCost leurre (9 999) qui ne doit pas être copié.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LienProspectionEtudeTest {

    private static final String DETAIL = "$.detail";
    private static final String LND_LAND_ID = "LndLandId";
    private static final String STK_ITEM_ID = "StkItemId";
    private static final UUID INCONNU = UUID.fromString("5B6C7D8E-9F0A-4B1C-8D2E-3F4A5B6C7D8E");

    /** Colonnes de l'étude que la copie du §1 écrirait (R2 : intactes quand l'étude a un terrain). */
    private static final List<String> COLONNES_COPIEES = List.of("Area", "OccupationSolRatio", "Cuf", "CoefSaleableArea",
            "SalableArea", "PurchaseUnitCost", "EquipementsCoef", "UnitSalePrice", "UnitCostConstructionBuilt",
            "UnitCostConstructionNoBuilt", "LandDescription", "Code", "Description");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite faisabilite;
    @Autowired private FixtureProspectionsTerrain prospections;
    @Autowired private FixtureTrancheOnglets onglets;
    @Autowired private FixtureLienProspection lien;

    private AdmUser admin;
    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;
    private UUID prospection;
    private String codeProspection;

    @BeforeEach
    void setUp() {
        faisabilite.parametrage();
        admin = patrimoine.administrateur();
        api = new ApiFaisabilite(mockMvc, admin);
        Contexte c = prospections.contexte("LPE");
        jeton = c.jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
        codeProspection = "LPE-" + jeton;
        prospection = prospections.prospection(c, codeProspection);
        prospections.definir(prospection, "EquipementsCoef", new BigDecimal("0.1"));
        prospections.definir(prospection, "UnitCostNonBuiltPrice", new BigDecimal("1500"));
        prospections.definir(prospection, "PurchaseUnitCost", new BigDecimal("9999"));
    }

    // ------------------------------------------------------------------ appui

    private CorpsJson fiche(String suffixe) {
        return yestest(FixtureFaisabilite.fiche(suffixe + "-" + jeton, societe.getId()))
                .avec(COUT_M2_BATI, d("3000")).avec(COUT_M2_NON_BATI, d("2000"))
                .avec("descriptionTerrain", "Terrain initial");
    }

    private UUID etudeSansTerrain(String suffixe) throws Exception {
        return api.etude(fiche(suffixe));
    }

    private UUID etudeAvecTerrain(String suffixe) throws Exception {
        return api.etude(fiche(suffixe).avec(TERRAIN_ID, onglets.terrain("TER-" + suffixe + "-" + jeton)));
    }

    private ResultActions lier(UUID etude, UUID prospectionId) throws Exception {
        return mockMvc.perform(put(LIEN, etude).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(FixtureLienProspection.lien(prospectionId)));
    }

    private ResultActions recopier(UUID etude) throws Exception {
        return mockMvc.perform(post(RECOPIER, etude).with(connecte(admin)));
    }

    private ResultActions delier(UUID etude) throws Exception {
        return mockMvc.perform(delete(LIEN, etude).with(connecte(admin)));
    }

    private ResultActions ecrire(String ecriture, UUID etude) throws Exception {
        return switch (ecriture) {
            case "lier" -> lier(etude, prospection);
            case "recopier" -> recopier(etude);
            default -> delier(etude);
        };
    }

    private Tuple etude(UUID id) {
        return faisabilite.etude(id);
    }

    private List<Object> colonnesCopiees(UUID id) {
        Tuple l = etude(id);
        return COLONNES_COPIEES.stream().map(col -> normaliser(l.get(col))).toList();
    }

    /** Décimaux comparés sans l'échelle (4000.000000 = 4000). */
    private static Object normaliser(Object valeur) {
        return valeur instanceof BigDecimal b ? b.stripTrailingZeros() : valeur;
    }

    /** Copie du §1 sur l'étude, un champ par ligne (source : FeasibilityStudy.cs:LienProspection_Click l.514-550). */
    private void verifierCopie(Tuple l) {
        assertAll(
                // Area ← LndLand.Area
                () -> assertThat(decimal(l.get("Area"))).as("superficie").isEqualByComparingTo("1200"),
                // OccupationSolRatio (coefEmprise) ← LndLand.Cos
                () -> assertThat(decimal(l.get("OccupationSolRatio"))).as("coefEmprise").isEqualByComparingTo("0.3"),
                // Cuf ← LndLand.Cuf
                () -> assertThat(decimal(l.get("Cuf"))).as("cuf").isEqualByComparingTo("2.5"),
                // CoefSaleableArea ← LndLand.CoefSaleableArea (AreaSalableRatio)
                () -> assertThat(decimal(l.get("CoefSaleableArea"))).as("coefVendable").isEqualByComparingTo("1"),
                // PurchaseUnitCost (prixAchatM2) ← LndLand.ProspectionUnitCost, jamais LndLand.PurchaseUnitCost
                () -> assertThat(decimal(l.get("PurchaseUnitCost"))).as("prixAchatM2").isEqualByComparingTo("4000"),
                // EquipementsCoef ← LndLand.EquipementsCoef
                () -> assertThat(decimal(l.get("EquipementsCoef"))).as("coefEquipements").isEqualByComparingTo("0.1"),
                // UnitCostConstructionNoBuilt ← LndLand.UnitCostNonBuiltPrice
                () -> assertThat(decimal(l.get("UnitCostConstructionNoBuilt"))).as("coutConstructionM2NonBati")
                        .isEqualByComparingTo("1500"),
                // LandDescription ← LndLand.Description
                () -> assertThat(l.get("LandDescription")).as("descriptionTerrain").isEqualTo("Prospection " + codeProspection),
                // Code ← LndLand.Code ; Description ← LndLand.Description
                () -> assertThat(l.get("Code")).as("code").isEqualTo(codeProspection),
                () -> assertThat(l.get("Description")).as("description").isEqualTo("Prospection " + codeProspection),
                // Réponse du manager 2 : UnitCostBuiltPrice n'existe pas sur LndLand → coût bâti non copié
                () -> assertThat(decimal(l.get("UnitCostConstructionBuilt"))).as("coutConstructionM2Bati non copié")
                        .isEqualByComparingTo("3000"));
    }

    /** Calcul partagé rejoué sur les entrées copiées : net 1 200 × 0,9, emprise 0,3 × net, plancher 2,5 × net. */
    private void verifierRecalcul(Tuple l) {
        assertAll(
                () -> assertThat(decimal(l.get("EquipementsAmount"))).as("EquipementsAmount").isEqualByComparingTo("120"),
                () -> assertThat(decimal(l.get("AreaNet"))).as("AreaNet").isEqualByComparingTo("1080"),
                () -> assertThat(decimal(l.get("OccupationGroundArea"))).as("OccupationGroundArea").isEqualByComparingTo("324"),
                () -> assertThat(decimal(l.get("TotalCoveredArea"))).as("TotalCoveredArea").isEqualByComparingTo("2700"),
                () -> assertThat(decimal(l.get("SalableArea"))).as("SalableArea").isEqualByComparingTo("2700"),
                () -> assertThat(decimal(l.get("PriceArea"))).as("PriceArea").isEqualByComparingTo("4800000"));
    }

    // ------------------------------------------------------------------ R1 : étude sans terrain

    @Test
    void r1_lier_etudeSansTerrain_200_lndLandIdEcrit_reponsePorteLaProspection() throws Exception {
        UUID id = etudeSansTerrain("R1A");

        lier(id, prospection)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_ID).value(equalToIgnoringCase(prospection.toString())));

        assertThat(guid(etude(id).get(LND_LAND_ID))).isEqualTo(guid(prospection));
    }

    @Test
    void r1_lier_etudeSansTerrain_copieLesChampsDuParagraphe1() throws Exception {
        UUID id = etudeSansTerrain("R1C");

        lier(id, prospection).andExpect(status().isOk());

        verifierCopie(etude(id));
    }

    @Test
    void r1_lier_etudeSansTerrain_surfacesRecalculeesParLeCalculPartage() throws Exception {
        UUID id = etudeSansTerrain("R1R");

        lier(id, prospection).andExpect(status().isOk());

        verifierRecalcul(etude(id));
    }

    // ------------------------------------------------------------------ R2 : étude avec terrain

    @Test
    void r2_lier_etudeAvecTerrain_seulLndLandIdChange_valeursIntactes() throws Exception {
        UUID id = etudeAvecTerrain("R2T");
        List<Object> avant = colonnesCopiees(id);
        Object terrainAvant = guid(etude(id).get(STK_ITEM_ID));

        lier(id, prospection).andExpect(status().isOk());

        Tuple apres = etude(id);
        assertAll(
                () -> assertThat(colonnesCopiees(id)).isEqualTo(avant),
                () -> assertThat(guid(apres.get(STK_ITEM_ID))).isEqualTo(terrainAvant).isNotNull(),
                () -> assertThat(guid(apres.get(LND_LAND_ID))).isEqualTo(guid(prospection)));
    }

    // ------------------------------------------------------------------ R3 : code de la prospection déjà pris

    @Test
    void r3_lier_codeDeLaProspectionPrisParUneAutreEtude_409_rienEcrit() throws Exception {
        api.etude(FixtureFaisabilite.fiche(codeProspection, societe.getId()));
        UUID id = etudeSansTerrain("R3C");
        List<Object> avant = colonnesCopiees(id);

        lier(id, prospection).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(etude(id).get(LND_LAND_ID)).isNull(),
                () -> assertThat(colonnesCopiees(id)).isEqualTo(avant));
    }

    // ------------------------------------------------------------------ R4 : bloquée, hors périmètre

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"lier", "recopier", "delier"})
    void r4_etudeBloquee_ecriture_409_bloquee_rienEcrit(String ecriture) throws Exception {
        UUID id = etudeSansTerrain("R4B");
        lier(id, prospection).andExpect(status().isOk());
        lien.bloquer(id);
        List<Object> avant = colonnesCopiees(id);

        ecrire(ecriture, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MSG_BLOQUEE)));

        assertAll(
                () -> assertThat(guid(etude(id).get(LND_LAND_ID))).isEqualTo(guid(prospection)),
                () -> assertThat(colonnesCopiees(id)).isEqualTo(avant));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"lier", "recopier", "delier"})
    void r4_etudeHorsPerimetre_societeInactive_ecriture_404_rienEcrit(String ecriture) throws Exception {
        UUID id = etudeSansTerrain("R4M");
        lier(id, prospection).andExpect(status().isOk());
        lien.desactiverSociete(societe.getId());

        ecrire(ecriture, id).andExpect(status().isNotFound());

        assertThat(guid(etude(id).get(LND_LAND_ID))).isEqualTo(guid(prospection));
    }

    // ------------------------------------------------------------------ R5 : prospection inconnue, champ absent

    @Test
    void r5_lier_prospectionInconnue_404_rienEcrit() throws Exception {
        UUID id = etudeSansTerrain("R5I");

        lier(id, INCONNU).andExpect(status().isNotFound());

        assertThat(etude(id).get(LND_LAND_ID)).isNull();
    }

    @Test
    void r5_lier_prospectionTerrainIdAbsent_400_champNomme() throws Exception {
        UUID id = etudeSansTerrain("R5A");

        mockMvc.perform(put(LIEN, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(PROSPECTION_TERRAIN_ID)));

        assertThat(etude(id).get(LND_LAND_ID)).isNull();
    }

    @Test
    void r5_lier_etudeInconnue_404() throws Exception {
        lier(INCONNU, prospection).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ R6 : recopier

    @Test
    void r6_recopier_sansLien_409_aucuneProspectionLiee() throws Exception {
        UUID id = etudeSansTerrain("R6S");
        List<Object> avant = colonnesCopiees(id);

        recopier(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MSG_AUCUNE_PROSPECTION)));

        assertThat(colonnesCopiees(id)).isEqualTo(avant);
    }

    @Test
    void r6_recopier_etudeAvecTerrainLiee_copieLesChamps_etDetacheLeTerrain() throws Exception {
        UUID id = etudeAvecTerrain("R6C");
        lier(id, prospection).andExpect(status().isOk());

        recopier(id).andExpect(status().isOk());

        Tuple l = etude(id);
        assertAll(
                () -> verifierCopie(l),
                () -> verifierRecalcul(l),
                () -> assertThat(l.get(STK_ITEM_ID)).as("terrainId").isNull(),
                () -> assertThat(guid(l.get(LND_LAND_ID))).isEqualTo(guid(prospection)));
    }

    // ------------------------------------------------------------------ R7 : délier

    @Test
    void r7_delier_lndLandIdNull_rienDAutre() throws Exception {
        UUID id = etudeSansTerrain("R7D");
        lier(id, prospection).andExpect(status().isOk());
        List<Object> avant = colonnesCopiees(id);

        delier(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_ID).hasJsonPath())
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_ID).value(nullValue()));

        assertAll(
                () -> assertThat(etude(id).get(LND_LAND_ID)).isNull(),
                () -> assertThat(colonnesCopiees(id)).isEqualTo(avant));
    }

    @Test
    void r7_delier_sansLien_200_idempotent() throws Exception {
        UUID id = etudeAvecTerrain("R7I");
        List<Object> avant = colonnesCopiees(id);

        delier(id).andExpect(status().isOk());

        assertAll(
                () -> assertThat(etude(id).get(LND_LAND_ID)).isNull(),
                () -> assertThat(etude(id).get(STK_ITEM_ID)).isNotNull(),
                () -> assertThat(colonnesCopiees(id)).isEqualTo(avant));
    }

    // ------------------------------------------------------------------ R8 : trace de l'écriture

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"lier", "recopier", "delier"})
    void r8_ecriture_traceAuteurEtDateDeModification(String ecriture) throws Exception {
        UUID id = etudeSansTerrain("R8T");
        if (!"lier".equals(ecriture)) {
            lier(id, prospection).andExpect(status().isOk());
        }
        lien.effacerAuteurModification(id);

        ecrire(ecriture, id).andExpect(status().isOk());

        Tuple l = etude(id);
        assertAll(
                () -> assertThat(l.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(l.get("UserUpdateDateTime")).isNotNull());
    }

    // ------------------------------------------------------------------ R12 : fiche, liste, choix

    @Test
    void r12_fiche_exposeProspectionLiee_idEtDescription() throws Exception {
        UUID id = etudeAvecTerrain("R12F");
        lier(id, prospection).andExpect(status().isOk());

        mockMvc.perform(get(FICHE, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_ID).value(equalToIgnoringCase(prospection.toString())))
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_DESCRIPTION).value("Prospection " + codeProspection));
    }

    @Test
    void r12_fiche_sansLien_prospectionNulle() throws Exception {
        UUID id = etudeSansTerrain("R12N");

        mockMvc.perform(get(FICHE, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_ID).hasJsonPath())
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_ID).value(nullValue()))
                .andExpect(jsonPath("$." + PROSPECTION_TERRAIN_DESCRIPTION).hasJsonPath());
    }

    @Test
    void r12_liste_exposeLaDescriptionDeLaProspection() throws Exception {
        UUID id = etudeAvecTerrain("R12L");
        lier(id, prospection).andExpect(status().isOk());

        mockMvc.perform(get(RACINE).with(connecte(admin)).param("search", "R12L-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]." + PROSPECTION_TERRAIN_DESCRIPTION)
                        .value(hasItem("Prospection " + codeProspection)));
    }

    @Test
    void r12_choixProspections_listeIdCodeDescription_archiveesComprises() throws Exception {
        prospections.definir(prospection, "IsInArchive", true);
        String filtre = "$[?(@.id=='" + prospection + "')]";

        mockMvc.perform(get(CHOIX_PROSPECTIONS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre + ".code").value(hasItem(codeProspection)))
                .andExpect(jsonPath(filtre + ".description").value(hasItem("Prospection " + codeProspection)));
    }
}
