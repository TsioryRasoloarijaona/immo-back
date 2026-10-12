package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.domain.RepartitionActionnaires;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderLineRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

/** FEATURE-entites-juridiques.md §2 : groupes d'actionnaires (Σ 100 %, doublon, suppression refusée si utilisé). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupesActionnairesTest {

    private static final String LISTE = "/api/groupes-actionnaires";
    private static final String FICHE = "/api/groupes-actionnaires/{id}";
    private static final String CHAMP = "$.champ";
    private static final String DETAIL = "$.detail";
    private static final String LIGNES = "lignes";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private CfgGroupShareholderRepository groupeRepository;
    @Autowired private CfgGroupShareholderLineRepository ligneRepository;
    @Autowired private CfgCompanyRepository companyRepository;

    private AdmUser admin;
    private CfgShareholder a;
    private CfgShareholder b;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-P5-GRP");
        a = entites.actionnaire("TST-P5-GA");
        b = entites.actionnaire("TST-P5-GB");
    }

    private static Map<String, Object> ligne(CfgShareholder actionnaire, String pourcentage) {
        return Map.of("actionnaireId", actionnaire.getId(), "pourcentage", pourcentage);
    }

    private ResultActions creer(String code, List<Map<String, Object>> lignes) throws Exception {
        return mockMvc.perform(post(LISTE).with(connecte(admin, ROLE_ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("code", code, "description", "Groupe " + code, LIGNES, lignes))));
    }

    @Test
    void creer_201_lignesTrieesEtCentPourCent() throws Exception {
        creer("TST-P5-G1", List.of(ligne(b, "0.4"), ligne(a, "0.6")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lignes.length()").value(2))
                .andExpect(jsonPath("$.lignes[0].actionnaireCode").value("TST-P5-GA"))
                .andExpect(jsonPath("$.lignes[0].pourcentage").value(0.6));

        mockMvc.perform(get(LISTE).param("search", "TST-P5 -G1").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'TST-P5-G1')]").exists());
    }

    @Test
    void creer_400_sommeDifferenteDe100() throws Exception {
        creer("TST-P5-G2", List.of(ligne(a, "0.6"), ligne(b, "0.3")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(LIGNES))
                .andExpect(jsonPath(DETAIL).value(RepartitionActionnaires.CENT_POUR_CENT));

        assertThat(groupeRepository.findFirstByCodeAndIdNot("TST-P5-G2", UUID.randomUUID())).isEmpty();
    }

    @Test
    void creer_400_groupeSansLigne() throws Exception {
        creer("TST-P5-G3", List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(RepartitionActionnaires.CENT_POUR_CENT));
    }

    @Test
    void creer_400_actionnaireEnDouble() throws Exception {
        creer("TST-P5-G4", List.of(ligne(a, "0.5"), ligne(a, "0.5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(RepartitionActionnaires.DOUBLON));
    }

    @Test
    void creer_400_actionnaireInconnu() throws Exception {
        creer("TST-P5-G5", List.of(Map.of("actionnaireId", UUID.randomUUID(), "pourcentage", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(LIGNES));
    }

    @Test
    void creer_409_codeDejaPris() throws Exception {
        entites.groupe("TST-P5-GDBL");

        creer("TST-P5-GDBL", List.of(ligne(a, "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(CHAMP).value("code"));
    }

    @Test
    void creer_403_nonAdministrateur() throws Exception {
        mockMvc.perform(post(LISTE).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "TST-P5-G403", "description", "Refusé"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void modifier_lignesRemplacees() throws Exception {
        CfgGroupShareholder groupe = entites.groupe("TST-P5-GMOD");
        entites.ligneGroupe(groupe, a, "1");

        mockMvc.perform(patch(FICHE, groupe.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(LIGNES, List.of(ligne(a, "0.25"), ligne(b, "0.75"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TST-P5-GMOD"))
                .andExpect(jsonPath("$.lignes.length()").value(2));

        assertThat(ligneRepository.duGroupe(groupe.getId())).hasSize(2);
    }

    @Test
    void modifier_400_sommeFausse_lignesInchangees() throws Exception {
        CfgGroupShareholder groupe = entites.groupe("TST-P5-GKO");
        entites.ligneGroupe(groupe, a, "1");

        mockMvc.perform(patch(FICHE, groupe.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(LIGNES, List.of(ligne(b, "0.5"))))))
                .andExpect(status().isBadRequest());

        assertThat(ligneRepository.duGroupe(groupe.getId())).singleElement()
                .extracting(l -> l.getCfgShareholder().getId()).isEqualTo(a.getId());
    }

    @Test
    void fiche_404_idInconnu() throws Exception {
        mockMvc.perform(get(FICHE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimer_204_avecSesLignes() throws Exception {
        CfgGroupShareholder groupe = entites.groupe("TST-P5-GSUP");
        entites.ligneGroupe(groupe, a, "1");

        mockMvc.perform(delete(FICHE, groupe.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNoContent());

        assertThat(ligneRepository.duGroupe(groupe.getId())).isEmpty();
        assertThat(groupeRepository.findById(groupe.getId())).isEmpty();
    }

    @Test
    void supprimer_409_groupeUtiliseParUneEntite_entiteIntacte() throws Exception {
        CfgGroupShareholder groupe = entites.groupe("TST-P5-GUSE");
        entites.ligneGroupe(groupe, a, "1");
        CfgCompany entite = fixture.entite("TST-P5-ENT-GRP", true);
        entite.setCfgGroupShareholder(groupe);
        companyRepository.saveAndFlush(entite);

        mockMvc.perform(delete(FICHE, groupe.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString("TST-P5-ENT-GRP")));

        assertThat(companyRepository.findById(entite.getId())).isPresent();
        assertThat(groupeRepository.findById(groupe.getId())).isPresent();
    }
}
