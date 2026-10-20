package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CATEGORIES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CATEGORIE_COMPTABLE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_DOSSIER_VENTE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_PRM_FOLDER_SALE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.NATURE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.OPERATEUR;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SEUILS_VERSEMENT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TYPES_PRESENTATION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.VOCATIONS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue P6 (FEATURE-patrimoine-referentiels.md §8, §8 bis) : chemins de refus des écritures non couverts ailleurs. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminRevueTest {

    private static final BigDecimal RATIO = new BigDecimal("0.3");
    private static final String SEUILS_DU_STATUT = "SELECT COUNT(*) FROM CfgTrancheStatus WHERE StrStatusId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    private CfgTranche tranche(String code, boolean entiteActive) {
        return patrimoine.tranche(code, patrimoine.projet("PRJ-" + code, patrimoine.entite("EJ-" + code, entiteActive)));
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, CorpsJson corps) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    // ------------------------------------------------------------------ R4 sur la modification

    @Test
    void presentation_patchVersUneTrancheInactive_404_trancheConservee() throws Exception {
        CfgTranche active = tranche("RVA-" + jeton, true);
        CfgTranche inactive = tranche("RVI-" + jeton, false);
        UUID id = fixture.presentation(active.getId(), "PRV-" + jeton);

        ecrire(patch(LIGNE, TYPES_PRESENTATION, id), corps().avec(TRANCHE_ID, inactive.getId()))
                .andExpect(status().isNotFound());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkPresentationType WHERE PKey = :id AND StkHierarchyId = '"
                + active.getId() + "'", id)).isEqualTo(1);
    }

    @Test
    void seuil_dUneTrancheInactive_patchEtSuppression404() throws Exception {
        fixture.parametrageSeuils();
        UUID statut = fixture.statut(ENTITE_DOSSIER_VENTE, "TST-RV-" + jeton);
        UUID cache = fixture.seuilDeTranche(tranche("RVI-" + jeton, false).getId(), statut, 1, RATIO);

        ecrire(patch(LIGNE, SEUILS_VERSEMENT, cache), corps().avec(OPERATEUR, 2)).andExpect(status().isNotFound());
        mockMvc.perform(delete(LIGNE, SEUILS_VERSEMENT, cache).with(connecte(admin))).andExpect(status().isNotFound());

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE PKey = :id AND ThresholdOperator = 1",
                cache)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ unicité (tranche NULL) et statut au PATCH

    @Test
    void seuil_patchSurchargeVersGlobal_alorsQuUnGlobalExiste_409() throws Exception {
        fixture.parametrageSeuils();
        UUID statut = fixture.statut(ENTITE_DOSSIER_VENTE, "TST-RV-" + jeton);
        fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, RATIO);
        UUID surcharge = fixture.seuilDeTranche(tranche("RVA-" + jeton, true).getId(), statut, 1, RATIO);

        ecrire(patch(LIGNE, SEUILS_VERSEMENT, surcharge), corps().avec(TRANCHE_ID, null)).andExpect(status().isConflict());

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE PKey = :id AND CfgTrancheId IS NOT NULL",
                surcharge)).isEqualTo(1);
    }

    @Test
    void seuil_patchVersUnStatutPrmFolderSale_400_statutConserve() throws Exception {
        fixture.parametrageSeuils();
        UUID statut = fixture.statut(ENTITE_DOSSIER_VENTE, "TST-RV-" + jeton);
        UUID statutPrm = fixture.statut(ENTITE_PRM_FOLDER_SALE, "TST-RVP-" + jeton);
        UUID id = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, RATIO);

        ecrire(patch(LIGNE, SEUILS_VERSEMENT, id), corps().avec(STATUT_ID, statutPrm)).andExpect(status().isBadRequest());

        assertThat(fixture.compter(SEUILS_DU_STATUT, statutPrm)).isZero();
    }

    // ------------------------------------------------------------------ PATCH jamais à moitié appliqué

    @Test
    void categorie_patchDescriptionValideEtCategorieComptableInconnue_400_descriptionConservee() throws Exception {
        UUID nature = fixture.nature("NRV-" + jeton);
        UUID comptable = fixture.categorieComptable("SRV-" + jeton);
        String json = ecrire(post(TABLE, CATEGORIES), corps().avec(NATURE_ID, nature).avec(CODE, "CRV-" + jeton)
                .avec(DESCRIPTION, "Avant").avec(CATEGORIE_COMPTABLE_ID, comptable))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        ecrire(patch(LIGNE, CATEGORIES, id), corps().avec(DESCRIPTION, "Apres")
                .avec(CATEGORIE_COMPTABLE_ID, UUID.randomUUID())).andExpect(status().isBadRequest());

        assertThat(fixture.lignes("StkItemCategory", id, "CRV-" + jeton, "Avant")).isEqualTo(1);
    }

    @Test
    void presentation_patchCodeEtContenanceInvalide_400_codeEtContenancesConserves() throws Exception {
        UUID tranche = tranche("RVA-" + jeton, true).getId();
        UUID id = fixture.presentation(tranche, "PRV-" + jeton);
        fixture.contenanceDePresentation(id, fixture.contenance("CRV-" + jeton));

        mockMvc.perform(patch(LIGNE, TYPES_PRESENTATION, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"PRV2-%s\",\"contenances\":[{\"nombre\":1}]}".formatted(jeton)))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkPresentationType WHERE PKey = :id AND Code = 'PRV-"
                + jeton + "'", id)).isEqualTo(1);
        assertThat(fixture.compter("SELECT COUNT(*) FROM StkPresentationTypeContenance WHERE StkPresentationTypeId = :id",
                id)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ codes protégés

    @Test
    void vocationVente_recodeeEnMinuscules_409() throws Exception {
        UUID vente = patrimoine.vocationVente().getId();
        fixture.recoder("StkVocation", vente, "VENTE");

        ecrire(patch(LIGNE, VOCATIONS, vente), corps().avec(CODE, "vente")).andExpect(status().isConflict());
    }
}
