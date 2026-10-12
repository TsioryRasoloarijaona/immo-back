package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.domain.RepartitionActionnaires;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyShareholderRepository;
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
import java.util.stream.Collectors;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-entites-juridiques.md §2 : onglet Répartition (Verif, doublon, chargement depuis un groupe). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartitionTest {

    private static final String REPARTITION = "/api/entites-juridiques/{id}/repartition";
    private static final String DEPUIS_GROUPE = "/api/entites-juridiques/{id}/repartition/depuis-groupe/{groupeId}";
    private static final String DETAIL = "$.detail";
    private static final String CHAMP = "$.champ";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgCompanyShareholderRepository repartitionRepository;

    private AdmUser admin;
    private CfgCompany entite;
    private CfgShareholder a;
    private CfgShareholder b;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-P5-REP");
        entites.donnerRole(admin, ROLE_ADMIN);
        entite = fixture.entite("TST-P5-REPENT", true);
        entite.setTotalSharing(2);
        entite.setSharingNumber(1000);
        entite = companyRepository.saveAndFlush(entite);
        a = entites.actionnaire("TST-P5-RA");
        b = entites.actionnaire("TST-P5-RB");
    }

    private static Map<String, Object> ligne(CfgShareholder actionnaire, int parts) {
        return Map.of("actionnaireId", actionnaire.getId(), "nombreParts", parts);
    }

    private ResultActions remplacer(List<Map<String, Object>> lignes) throws Exception {
        return mockMvc.perform(put(REPARTITION, entite.getId()).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(lignes.stream().map(FixturePatrimoine::json).collect(Collectors.joining(",", "[", "]"))));
    }

    @Test
    void put_200_repartitionCoherente() throws Exception {
        remplacer(List.of(ligne(a, 600), ligne(b, 400)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repartition.length()").value(2));

        assertThat(repartitionRepository.deLEntite(entite.getId()))
                .extracting(l -> l.getCfgShareholder().getCode(), CfgCompanyShareholder::getSharingNumber)
                .containsExactly(tuple("TST-P5-RA", 600), tuple("TST-P5-RB", 400));
    }

    @Test
    void put_400_actionnaireEnDouble() throws Exception {
        remplacer(List.of(ligne(a, 500), ligne(a, 500)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(RepartitionActionnaires.DOUBLON));
    }

    @Test
    void put_400_totalDesPartsDifferent_rienNEstEcrit() throws Exception {
        entites.repartition(entite, a, 1000);
        entite.setTotalSharing(1);
        companyRepository.saveAndFlush(entite);

        mockMvc.perform(put(REPARTITION, entite.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + json(ligne(b, 999)) + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("nombreParts"))
                .andExpect(jsonPath(DETAIL).value(RepartitionActionnaires.NOMBRE_PARTS));

        assertThat(repartitionRepository.deLEntite(entite.getId())).singleElement()
                .extracting(CfgCompanyShareholder::getSharingNumber).isEqualTo(1000);
    }

    @Test
    void put_400_nombreDeLignesDifferentDuNombreDActionnaires() throws Exception {
        mockMvc.perform(put(REPARTITION, entite.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("[" + json(ligne(a, 1000)) + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("nombreActionnaires"))
                .andExpect(jsonPath(DETAIL).value(RepartitionActionnaires.NOMBRE_ACTIONNAIRES));
    }

    @Test
    void put_403_nonAdministrateur() throws Exception {
        mockMvc.perform(put(REPARTITION, entite.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void put_404_entiteInconnue() throws Exception {
        mockMvc.perform(put(REPARTITION, UUID.randomUUID()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_nombrePartsSeul_refuseSiLaRepartitionNeSuitPas() throws Exception {
        entites.repartition(entite, a, 600);
        entites.repartition(entite, b, 400);

        mockMvc.perform(patch("/api/entites-juridiques/{id}", entite.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("nombreParts", 2000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(RepartitionActionnaires.NOMBRE_PARTS));
    }

    @Test
    void depuisGroupe_calculOmran_groupePoseEtNombreActionnairesAligne() throws Exception {
        CfgShareholder c = entites.actionnaire("TST-P5-RC");
        CfgGroupShareholder groupe = entites.groupe("TST-P5-RGRP");
        entites.ligneGroupe(groupe, a, "0.333333");
        entites.ligneGroupe(groupe, b, "0.333333");
        entites.ligneGroupe(groupe, c, "0.333334");
        entites.repartition(entite, a, 1000);

        mockMvc.perform(post(DEPUIS_GROUPE, entite.getId(), groupe.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupeCode").value("TST-P5-RGRP"))
                .andExpect(jsonPath("$.nombreActionnaires").value(3))
                .andExpect(jsonPath("$.repartition.length()").value(3));

        assertThat(repartitionRepository.deLEntite(entite.getId()))
                .extracting(l -> l.getCfgShareholder().getCode(), CfgCompanyShareholder::getSharingNumber)
                .containsExactly(tuple("TST-P5-RA", 330), tuple("TST-P5-RB", 330), tuple("TST-P5-RC", 340));
    }

    @Test
    void depuisGroupe_404_groupeInconnu() throws Exception {
        mockMvc.perform(post(DEPUIS_GROUPE, entite.getId(), UUID.randomUUID()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void depuisGroupe_403_nonAdministrateur() throws Exception {
        CfgGroupShareholder groupe = entites.groupe("TST-P5-R403");

        mockMvc.perform(post(DEPUIS_GROUPE, entite.getId(), groupe.getId()).with(connecte(admin)))
                .andExpect(status().isForbidden());
    }
}
