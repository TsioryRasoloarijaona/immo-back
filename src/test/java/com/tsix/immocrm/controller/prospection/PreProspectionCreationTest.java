package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/pre-prospections} — voir {@code instructions/FEATURE-creation-pre-prospection.md}.
 *
 * <p>Chaque test tire un nom unique : le contrôle de doublon porte sur toute la table, et la base
 * de test est partagée entre les classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class PreProspectionCreationTest {

    private static final String URL = "/api/pre-prospections";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    private static String nomUnique() {
        return "Lead " + UUID.randomUUID();
    }

    private ResultActions creer(String json) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String idCree(ResultActions resultat) throws Exception {
        return JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), "$.preProspection.id");
    }

    private CfgTranche newTranche() {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-PPC-" + UUID.randomUUID());
        return trancheRepository.save(tranche);
    }

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code + "-" + UUID.randomUUID());
        tier.setFirstName("Com");
        tier.setLastName("Mercial");
        return tierRepository.save(tier);
    }

    @Test
    void creer_ecritUnePreProspectionAuSensDOmran() throws Exception {
        String nom = nomUnique();
        CfgTranche tranche = newTranche();

        String id = idCree(creer("""
                {"nom":"  %s  ","telephone":"0612345678","email":"lead@test.local",
                 "notes":"Vu sur Facebook","trancheId":"%s"}""".formatted(nom, tranche.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preProspection.code").value(matchesPattern("\\d{2}/\\d{5}")))
                .andExpect(jsonPath("$.preProspection.tempDescription").value(nom))
                .andExpect(jsonPath("$.preProspection.tempTel").value("0612345678"))
                .andExpect(jsonPath("$.preProspection.tempEmail").value("lead@test.local"))
                .andExpect(jsonPath("$.preProspection.typeProspectionCode").value("ComPreProspection"))
                .andExpect(jsonPath("$.preProspection.prospectId").doesNotExist())
                .andExpect(jsonPath("$.preProspection.commercialId").doesNotExist())
                .andExpect(jsonPath("$.preProspection.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.doublons", hasSize(0))));

        ComProspection ligne = prospectionRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(ligne.getStrEntityId()).isEqualTo(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        assertThat(ligne.getIsClosed()).isFalse();
        assertThat(ligne.getNotes()).isEqualTo("Vu sur Facebook");
        assertThat(ligne.getDocDate()).isNotNull();
        assertThat(ligne.getUserCreateDateTime()).isNotNull();

        mockMvc.perform(get(URL).param("search", nom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").exists());
        mockMvc.perform(get("/api/prospections").param("search", nom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").doesNotExist());
    }

    @Test
    void creer_sansNom_400() throws Exception {
        creer("""
                {"telephone":"0612345678"}""").andExpect(status().isBadRequest());
        creer("""
                {"nom":"   "}""").andExpect(status().isBadRequest());
    }

    @Test
    void creer_nomTropLong_400() throws Exception {
        creer("""
                {"nom":"%s"}""".formatted("x".repeat(256))).andExpect(status().isBadRequest());
    }

    @Test
    void creer_trancheInconnue_400_etArticleSansTranche_400() throws Exception {
        creer("""
                {"nom":"%s","trancheId":"%s"}""".formatted(nomUnique(), UUID.randomUUID()))
                .andExpect(status().isBadRequest());
        creer("""
                {"nom":"%s","articleId":"%s"}""".formatted(nomUnique(), UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Un doublon est signalé, jamais refusé — et reconnu malgré un format de téléphone et une
     * casse différents, là où l'égalité stricte d'OMRAN échouerait.
     */
    @Test
    void creer_doublon_estCreeEtSignale() throws Exception {
        String nom = nomUnique();
        String premier = idCree(creer("""
                {"nom":"%s","telephone":"0612345678","email":"lead@test.local"}""".formatted(nom))
                .andExpect(status().isCreated()));

        creer("""
                {"nom":"%s","telephone":"+212 6 12 34 56 78","email":"LEAD@test.local"}"""
                .formatted(nom.toUpperCase()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preProspection.id").value(org.hamcrest.Matchers.not(premier)))
                .andExpect(jsonPath("$.doublons", hasSize(1)))
                .andExpect(jsonPath("$.doublons[0].id").value(premier));
    }

    @Test
    void creer_contactDifferent_pasDeDoublon() throws Exception {
        String nom = nomUnique();
        creer("""
                {"nom":"%s","telephone":"0612345678"}""".formatted(nom)).andExpect(status().isCreated());

        creer("""
                {"nom":"%s","telephone":"0699999999"}""".formatted(nom))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.doublons", hasSize(0)));
    }

    /**
     * Le parcours complet, et la bascule de type relevée dans la trace OMRAN : lier un prospect
     * ne suffit pas à quitter la liste des pré-prospections, la transformation si.
     */
    @Test
    void parcoursComplet_transformerBasculeLeType() throws Exception {
        String nom = nomUnique();
        CfgTranche tranche = newTranche();
        String id = idCree(creer("""
                {"nom":"%s","trancheId":"%s"}""".formatted(nom, tranche.getId()))
                .andExpect(status().isCreated()));

        mockMvc.perform(post(URL + "/{id}/creer-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prenom":"Nouveau","nom":"Prospect"}"""))
                .andExpect(status().isCreated());
        mockMvc.perform(get(URL).param("search", nom))
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").exists());

        CfgTier commercial = newTier("COMM-PPC");
        mockMvc.perform(post("/api/prospections/{id}/transformer", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"commercialId":"%s"}""".formatted(commercial.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeProspectionCode").value("ComProspection"));

        mockMvc.perform(get(URL).param("search", nom))
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").doesNotExist());
        mockMvc.perform(get("/api/prospections").param("search", nom))
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").exists());
    }
}
