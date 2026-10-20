package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
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
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_DOSSIER_VENTE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_PRM_FOLDER_SALE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.OPERATEUR;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RATIO;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.REGLEMENT_AU_COMPTANT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SEUILS_VERSEMENT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TYPE_REGLEMENT_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seuils de versement {@code CfgTrancheStatus} — FEATURE-patrimoine-referentiels.md §2 (tranche facultative, NULL =
 * global ; opérateur 0/1/2 ; ratio) et §8.2 (limités à {@code ComFolder} ; unicité (tranche, entité, statut) → 409 :
 * le contrôle de versement lit {@code findFirst()}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeuilsVersementTest {

    private static final BigDecimal TRENTE_POUR_CENT = new BigDecimal("0.300000");
    private static final String COMPTE = "SELECT COUNT(*) FROM CfgTrancheStatus WHERE StrStatusId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;

    private AdmUser admin;
    private String jeton;
    private UUID statut;
    private UUID autreStatut;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
        fixture.parametrageSeuils();
        statut = fixture.statut(ENTITE_DOSSIER_VENTE, "TST-SEUIL-" + jeton);
        autreStatut = fixture.statut(ENTITE_DOSSIER_VENTE, "TST-SEUIL2-" + jeton);
        tranche = tranche("T-" + jeton);
    }

    private CfgTranche tranche(String code) {
        return patrimoine.tranche(code, patrimoine.projet("PRJ-" + code, patrimoine.entite("EJ-" + code, true)));
    }

    private static CorpsJson corpsSeuil(UUID statutId, int operateur) {
        return corps().avec(STATUT_ID, statutId).avec(TYPE_REGLEMENT_ID, REGLEMENT_AU_COMPTANT)
                .avec(OPERATEUR, operateur).avec(RATIO, TRENTE_POUR_CENT);
    }

    private ResultActions poster(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, SEUILS_VERSEMENT).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(LIGNE, SEUILS_VERSEMENT, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creer(CorpsJson corps) throws Exception {
        String json = poster(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    // ------------------------------------------------------------------ création

    @Test
    void s2_seuilGlobal_201_trancheNulle_entiteDossierDeVente() throws Exception {
        UUID id = creer(corpsSeuil(statut, 1));

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE PKey = :id AND CfgTrancheId IS NULL"
                + " AND StrFolderEntityId = '" + ENTITE_DOSSIER_VENTE + "' AND StrStatusId = '" + statut
                + "' AND ComFolderStatusPaymentTypeId = '" + REGLEMENT_AU_COMPTANT
                + "' AND ThresholdOperator = 1 AND ThresholdRatio = 0.3", id)).isEqualTo(1);
    }

    @Test
    void s2_seuilDeTranche_201_trancheEcrite() throws Exception {
        UUID id = creer(corpsSeuil(statut, 2).avec(TRANCHE_ID, tranche.getId()));

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE PKey = :id AND CfgTrancheId = '"
                + tranche.getId() + "' AND StrFolderEntityId = '" + ENTITE_DOSSIER_VENTE + "'", id)).isEqualTo(1);
    }

    @ParameterizedTest(name = "opérateur {0}")
    @ValueSource(ints = {0, 1, 2})
    void s2_operateur_0_1_2_acceptes_ecrits(int operateur) throws Exception {
        UUID id = creer(corpsSeuil(statut, operateur));

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE PKey = :id AND ThresholdOperator = "
                + operateur, id)).isEqualTo(1);
    }

    @Test
    void s2_operateur_3_400() throws Exception {
        poster(corpsSeuil(statut, 3)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "sans {0}")
    @ValueSource(strings = {STATUT_ID, TYPE_REGLEMENT_ID, OPERATEUR, RATIO})
    void s2_requis_400(String champ) throws Exception {
        poster(corpsSeuil(statut, 1).sans(champ)).andExpect(status().isBadRequest());
    }

    /** §2 « StrStatusId O (statuts de l'entité) » et §8.2 : un statut d'une autre entité de workflow est refusé. */
    @Test
    void r8_2_statutDUneAutreEntite_400() throws Exception {
        UUID statutPrm = fixture.statut(ENTITE_PRM_FOLDER_SALE, "TST-PRM-" + jeton);

        poster(corpsSeuil(statutPrm, 1)).andExpect(status().isBadRequest());

        assertThat(fixture.compter(COMPTE, statutPrm)).isZero();
    }

    // ------------------------------------------------------------------ unicité (tranche, entité, statut)

    @Test
    void r8_2_unicite_409_doublonGlobal() throws Exception {
        fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, TRENTE_POUR_CENT);

        poster(corpsSeuil(statut, 2)).andExpect(status().isConflict());

        assertThat(fixture.compter(COMPTE, statut)).isEqualTo(1);
    }

    @Test
    void r8_2_unicite_409_doublonDeTranche() throws Exception {
        fixture.seuilDeTranche(tranche.getId(), statut, 1, TRENTE_POUR_CENT);

        poster(corpsSeuil(statut, 2).avec(TRANCHE_ID, tranche.getId())).andExpect(status().isConflict());

        assertThat(fixture.compter(COMPTE, statut)).isEqualTo(1);
    }

    @Test
    void r8_2_unicite_201_memeStatut_globalPuisSurchargeDeTranche() throws Exception {
        fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, TRENTE_POUR_CENT);

        poster(corpsSeuil(statut, 1).avec(TRANCHE_ID, tranche.getId())).andExpect(status().isCreated());
    }

    @Test
    void r8_2_unicite_201_memeStatut_deuxTranches() throws Exception {
        fixture.seuilDeTranche(tranche.getId(), statut, 1, TRENTE_POUR_CENT);

        poster(corpsSeuil(statut, 1).avec(TRANCHE_ID, tranche("T2-" + jeton).getId())).andExpect(status().isCreated());
    }

    @Test
    void r8_2_unicite_globalNonBloqueParUneLignePrmFolderSale() throws Exception {
        fixture.seuilGlobal(ENTITE_PRM_FOLDER_SALE, statut, 1, TRENTE_POUR_CENT);

        poster(corpsSeuil(statut, 1)).andExpect(status().isCreated());
    }

    @Test
    void r8_2_patch_409_versLeStatutDUnAutreSeuil() throws Exception {
        fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, TRENTE_POUR_CENT);
        UUID id = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, autreStatut, 1, TRENTE_POUR_CENT);

        modifier(id, corps().avec(STATUT_ID, statut)).andExpect(status().isConflict());

        assertThat(fixture.compter(COMPTE, autreStatut)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ lecture, modification, suppression

    @Test
    void s2_patch_200_ratioEtOperateurModifies() throws Exception {
        UUID id = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, TRENTE_POUR_CENT);

        modifier(id, corps().avec(OPERATEUR, 2).avec(RATIO, new BigDecimal("0.5"))).andExpect(status().isOk());

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE PKey = :id AND ThresholdOperator = 2"
                + " AND ThresholdRatio = 0.5", id)).isEqualTo(1);
    }

    @Test
    void s2_liste_contientSeuilGlobalEtSeuilDeTranche() throws Exception {
        UUID global = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, TRENTE_POUR_CENT);
        UUID deTranche = fixture.seuilDeTranche(tranche.getId(), statut, 1, TRENTE_POUR_CENT);

        mockMvc.perform(get(TABLE, SEUILS_VERSEMENT).with(connecte(admin)).param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + global + "')]").exists())
                .andExpect(jsonPath("$..[?(@.id=='" + deTranche + "')]").exists());
    }

    @Test
    void s2_suppression_204() throws Exception {
        UUID id = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, TRENTE_POUR_CENT);

        mockMvc.perform(delete(LIGNE, SEUILS_VERSEMENT, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(fixture.lignes("CfgTrancheStatus", id)).isZero();
    }

    // ------------------------------------------------------------------ §8.2 : lignes PrmFolderSale hors écran

    @Test
    void r8_2_lignePrmFolderSale_absenteDeLaListe() throws Exception {
        UUID prm = fixture.seuilGlobal(ENTITE_PRM_FOLDER_SALE, statut, 1, TRENTE_POUR_CENT);
        UUID vente = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, autreStatut, 1, TRENTE_POUR_CENT);

        mockMvc.perform(get(TABLE, SEUILS_VERSEMENT).with(connecte(admin)).param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + vente + "')]").exists())
                .andExpect(jsonPath("$..[?(@.id=='" + prm + "')]").doesNotExist());
    }

    @Test
    void r8_2_lignePrmFolderSale_fiche404() throws Exception {
        UUID prm = fixture.seuilGlobal(ENTITE_PRM_FOLDER_SALE, statut, 1, TRENTE_POUR_CENT);
        UUID vente = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, autreStatut, 1, TRENTE_POUR_CENT);
        mockMvc.perform(get(LIGNE, SEUILS_VERSEMENT, vente).with(connecte(admin))).andExpect(status().isOk());

        mockMvc.perform(get(LIGNE, SEUILS_VERSEMENT, prm).with(connecte(admin))).andExpect(status().isNotFound());
    }

    @Test
    void r8_2_lignePrmFolderSale_modification404_inchangee() throws Exception {
        UUID prm = fixture.seuilGlobal(ENTITE_PRM_FOLDER_SALE, statut, 1, TRENTE_POUR_CENT);
        UUID vente = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, autreStatut, 1, TRENTE_POUR_CENT);
        modifier(vente, corps().avec(OPERATEUR, 2)).andExpect(status().isOk());

        modifier(prm, corps().avec(OPERATEUR, 2)).andExpect(status().isNotFound());

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE PKey = :id AND ThresholdOperator = 1",
                prm)).isEqualTo(1);
    }

    @Test
    void r8_2_lignePrmFolderSale_suppression404_conservee() throws Exception {
        UUID prm = fixture.seuilGlobal(ENTITE_PRM_FOLDER_SALE, statut, 1, TRENTE_POUR_CENT);
        UUID vente = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, autreStatut, 1, TRENTE_POUR_CENT);
        mockMvc.perform(delete(LIGNE, SEUILS_VERSEMENT, vente).with(connecte(admin))).andExpect(status().isNoContent());

        mockMvc.perform(delete(LIGNE, SEUILS_VERSEMENT, prm).with(connecte(admin))).andExpect(status().isNotFound());

        assertThat(fixture.lignes("CfgTrancheStatus", prm)).isEqualTo(1);
    }
}
