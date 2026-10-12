package com.tsix.immocrm.controller.entite;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgShareholderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-entites-juridiques.md §2 : actionnaires, CRUD complet. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionnairesTest {

    private static final String LISTE = "/api/actionnaires";
    private static final String FICHE = "/api/actionnaires/{id}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private CfgShareholderRepository repository;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-P5-ACT");
    }

    @Test
    void creer_201_puisFicheEtRechercheSansEspaces() throws Exception {
        String corps = json(Map.of("code", "TST-P5-A1", "description", "Jean Dupont", "notes", "Fondateur"));

        String reponse = mockMvc.perform(post(LISTE).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TST-P5-A1"))
                .andExpect(jsonPath("$.creePar").value("Compte ADM-P5-ACT"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(reponse, "$.id");

        assertThat(repository.findById(UUID.fromString(id))).get()
                .extracting(CfgShareholder::getDescription, CfgShareholder::getNotes)
                .containsExactly("Jean Dupont", "Fondateur");
        mockMvc.perform(get(LISTE).param("search", "Jean Dup ont").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'TST-P5-A1')]").exists());
    }

    @Test
    void creer_400_codeManquant() throws Exception {
        mockMvc.perform(post(LISTE).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("description", "Sans code"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value("code"));
    }

    @Test
    void creer_409_codeDejaPris() throws Exception {
        entites.actionnaire("TST-P5-DOUBLE");

        mockMvc.perform(post(LISTE).with(connecte(admin, ROLE_ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "tst-p5-double", "description", "Autre"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.champ").value("code"));
    }

    @Test
    void creer_403_nonAdministrateur() throws Exception {
        mockMvc.perform(post(LISTE).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "TST-P5-403", "description", "Refusé"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void modifier_200_champsAbsentsInchanges() throws Exception {
        CfgShareholder actionnaire = entites.actionnaire("TST-P5-MOD");

        mockMvc.perform(patch(FICHE, actionnaire.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("notes", "Nouvelle note"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TST-P5-MOD"))
                .andExpect(jsonPath("$.notes").value("Nouvelle note"));
    }

    @Test
    void fiche_404_idInconnu() throws Exception {
        mockMvc.perform(get(FICHE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimer_204_actionnaireLibre() throws Exception {
        CfgShareholder actionnaire = entites.actionnaire("TST-P5-SUP");

        mockMvc.perform(delete(FICHE, actionnaire.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNoContent());

        assertThat(repository.findById(actionnaire.getId())).isEmpty();
    }

    @Test
    void supprimer_409_citeParUneRepartitionDEntite() throws Exception {
        CfgShareholder actionnaire = entites.actionnaire("TST-P5-REP");
        CfgCompany entite = fixture.entite("TST-P5-ENT-REP", true);
        entites.repartition(entite, actionnaire, 10);

        mockMvc.perform(delete(FICHE, actionnaire.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("répartition")));

        assertThat(repository.findById(actionnaire.getId())).isPresent();
    }

    @Test
    void supprimer_409_citeParUneLigneDeGroupe() throws Exception {
        CfgShareholder actionnaire = entites.actionnaire("TST-P5-GRP");
        CfgGroupShareholder groupe = entites.groupe("TST-P5-GRP-A");
        entites.ligneGroupe(groupe, actionnaire, "1");

        mockMvc.perform(delete(FICHE, actionnaire.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("groupe")));
    }
}
