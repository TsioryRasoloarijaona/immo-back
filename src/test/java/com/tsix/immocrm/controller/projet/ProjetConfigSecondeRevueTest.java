package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.PROJET;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.SLUG_TYPES_LOT;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.STATUTS_LOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seconde revue règle 6 du lot P9a (FEATURE-projet-config.md §8.4, réponse 4) : les 16 tables non couvertes ailleurs. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetConfigSecondeRevueTest {

    private static final String COMPTE_PROJET = "SELECT COUNT(*) FROM PrjProject WHERE PKey = :id";
    private static final String COMPTE_HABILITATIONS = "SELECT COUNT(*) FROM AdmUserHabilitation WHERE PrjProjectId = :id";
    private static final String TYPES_LOT = "/api/referentiels-admin/" + SLUG_TYPES_LOT;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;
    @Autowired private FixtureReferentielsAdmin referentiels;

    private AdmUser admin;
    private String jeton;
    private UUID societe;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        societe = patrimoine.entite("SOC-" + jeton, true).getId();
        fixture.parametrage();
    }

    private RequestPostProcessor enCommercial() {
        return FixtureDossierVente.connecte(admin, "Commercial");
    }

    /** Table.colonne qui référence PrjProject ; clé primaire nommée {@code Pkey} sur deux tables de prod. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"BuyLoan.PKey.PrjProjectId", "BuySelection.PKey.PrjProjectId",
            "GrhWorkedDay.Pkey.PrjProjectId", "PrjProjectCfgTrancheVentilation.PKey.PrjProjectId",
            "BuyFolderPrjProjectVentilation.PKey.PrjProjectId", "BulMeeting.PKey.PrjProjectId",
            "PrjAchivementVersion.PKey.PrjProjectId", "PrjCoefficientCost.PKey.PrjProjectId",
            "PrjGanttTask.Pkey.PrjProjectId", "PrjPrevision.PKey.PrjProjectId", "PrjPrevisionVersion.PKey.PrjProjectId",
            "PrjProjectAchievementPrevision.PKey.PrjProjectId", "PrjProjectAchievementReal.PKey.PrjProjectId",
            "PrjProjectBuildingPrevision.PKey.PrjProjectId", "PrjProjectProgressCriteriaLine.PKey.PrjProjectId",
            "CfgTrancheLevelingDefinition.PKey.CfgTrancheId"})
    void suppressionProjetReference_409_rienSupprime(String reference) throws Exception {
        String[] t = reference.split("\\.");
        UUID projet = fixture.projet("PM2-" + jeton, societe);
        fixture.habilitation(admin.getId(), projet);
        UUID ligne = fixture.reference("INSERT INTO " + t[0] + " (" + t[1] + ", " + t[2]
                + ") SELECT :id, :projet WHERE :code IS NOT NULL", "R-" + jeton, projet);

        mockMvc.perform(delete(PROJET, projet).with(connecte(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Utilisé par")));

        assertThat(new int[]{fixture.compter(COMPTE_PROJET, projet), fixture.compter(COMPTE_HABILITATIONS, projet),
                fixture.compter("SELECT COUNT(*) FROM " + t[0] + " WHERE " + t[1] + " = :id", ligne)})
                .containsExactly(1, 1, 1);
    }

    @Test
    void typesLot_ecrituresRefuseesAuNonAdministrateur_ligneIntacte() throws Exception {
        UUID type = referentiels.typeMarche("TX-" + jeton);
        String corps = "{\"code\":\"TY-" + jeton + "\",\"description\":\"x\"}";

        mockMvc.perform(post(TYPES_LOT).with(enCommercial()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(TYPES_LOT + "/{id}", type).with(enCommercial())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(TYPES_LOT + "/{id}", type).with(enCommercial())).andExpect(status().isForbidden());

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjMarketType WHERE PKey = :id AND Code = N'TX-" + jeton + "'",
                type)).isEqualTo(1);
    }

    @Test
    void statutsLot_aucuneEcriture() throws Exception {
        mockMvc.perform(post(STATUTS_LOT).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put(TYPES_LOT + "/{id}", UUID.randomUUID()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
