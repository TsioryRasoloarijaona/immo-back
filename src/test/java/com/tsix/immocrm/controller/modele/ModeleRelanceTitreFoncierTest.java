package com.tsix.immocrm.controller.modele;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.services.modele.DemarrageModeles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot P9d, règle R7 — FEATURE-patrimoine-relances-champs.md décisions 2 et 5 : la lettre de 1re relance est un modèle de
 * document de la catégorie {@code RELANCE_TITRE_FONCIER} (« Relance titre foncier »), dotée d'un catalogue de champs et d'un
 * modèle initial actif posé au démarrage comme l'acte de réservation. Le rendu des variables est vérifié par
 * {@code PremieresRelancesTest} (R6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class ModeleRelanceTitreFoncierTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String RELANCE = "RELANCE_TITRE_FONCIER";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext contexte;

    private String categories() throws Exception {
        return mockMvc.perform(get(BASE + "/categories")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void r7_categorieRelanceTitreFoncier_proposeeAvecSonLibelle() throws Exception {
        List<String> libelles = JsonPath.read(categories(), "$[?(@.code == '" + RELANCE + "')].libelle");

        assertThat(libelles).containsExactly("Relance titre foncier");
    }

    @Test
    void r7_catalogueDeChampsDeLaCategorie_200() throws Exception {
        mockMvc.perform(get(BASE + "/categories/{categorie}/champs", RELANCE)).andExpect(status().isOk());
    }

    /** « Une catégorie sans catalogue n'est pas proposée » : toute catégorie listée sert son catalogue. */
    @Test
    void r7_chaqueCategorieProposee_aUnCatalogue() throws Exception {
        List<String> codes = JsonPath.read(categories(), "$[*].code");

        assertThat(codes).isNotEmpty().contains(RELANCE);
        for (String code : codes) {
            mockMvc.perform(get(BASE + "/categories/{categorie}/champs", code)).andExpect(status().isOk());
        }
    }

    @Test
    void r7_modeleInitial_creeAuDemarrage_actif() throws Exception {
        contexte.getAutowireCapableBeanFactory().createBean(DemarrageModeles.class).run(null);

        String json = mockMvc.perform(get(BASE + "/admin").param("categorie", RELANCE)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Boolean> actifs = JsonPath.read(json, "$[*].actif");
        assertThat(actifs).isNotEmpty().contains(Boolean.TRUE);
    }
}
