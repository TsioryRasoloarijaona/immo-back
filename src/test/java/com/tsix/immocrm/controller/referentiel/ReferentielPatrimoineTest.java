package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_PROJET;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Référentiels du formulaire article — FEATURE-patrimoine-ecriture.md §6, §13.5, §11 (ReferentielPatrimoineTest). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReferentielPatrimoineTest {

    private static final String CATEGORIES = "/api/referentiels/categories-bien";
    private static final String TYPES = "/api/referentiels/types-bien";
    private static final String VOCATIONS = "/api/referentiels/vocations";
    private static final String POUR_SOUS_ARTICLE = "pourSousArticle";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private EntityManager entityManager;

    private StkItemCategory categorieArticle;
    private StkItemCategory categorieSous;
    private StkItemCategory categorieTerrainSous;
    private StkItemType typeArticle;
    private StkItemType typeSous;
    private StkItemType typeTerrain;

    @BeforeEach
    void setUp() {
        StkKind bat = fixture.natureBat();
        StkKind terrain = fixture.nature(UUID.randomUUID(), "TER-REF-PAT");
        categorieArticle = fixture.categorie("CAT-REF-PAT-ART", bat, false);
        categorieSous = fixture.categorie("CAT-REF-PAT-SOUS", bat, true);
        categorieTerrainSous = fixture.categorie("CAT-REF-PAT-TER", terrain, true);
        typeArticle = fixture.type("TYP-REF-PAT-ART", bat, false);
        typeSous = fixture.type("TYP-REF-PAT-SOUS", bat, true);
        typeTerrain = fixture.type("TYP-REF-PAT-TER", terrain, false);
    }

    private static String ligne(UUID id) {
        return "$[?(@.id=='" + id + "')]";
    }

    /**
     * Sorting n'est pas mappé avant la feature (§13.5) : insertion native. SQL Server trie les GUID d'abord sur
     * leurs 6 derniers octets : PREMIER_* (Sorting 1) passe après SECOND_* dans l'ordre de la clé primaire, donc
     * seul un tri sur Sorting met PREMIER_* devant.
     */
    private static final UUID PREMIER_IMMEUBLE = UUID.fromString("A1000000-0000-4000-8000-FFFFFFFFFFF1");
    private static final UUID SECOND_IMMEUBLE = UUID.fromString("A2000000-0000-4000-8000-000000000001");
    private static final UUID PREMIER_ETAGE = UUID.fromString("B1000000-0000-4000-8000-FFFFFFFFFFF1");
    private static final UUID SECOND_ETAGE = UUID.fromString("B2000000-0000-4000-8000-000000000001");

    private UUID inserer(String table, UUID id, String code, int tri) {
        String sql = "StkBuilding".equals(table)
                ? "INSERT INTO StkBuilding (PKey, Code, Description, Sorting) VALUES (:id, :code, :code, :tri)"
                : "INSERT INTO StkFloor (PKey, Code, Description, Sorting) VALUES (:id, :code, :code, :tri)";
        entityManager.createNativeQuery(sql)
                .setParameter("id", id).setParameter("code", code).setParameter("tri", tri)
                .executeUpdate();
        return id;
    }

    private List<String> idsDansLOrdre(ResultActions resultat) throws Exception {
        return JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), "$[*].id");
    }

    // ------------------------------------------------------------------ immeubles / étages triés

    @Test
    void r6_immeubles_listeTrieeParSorting() throws Exception {
        UUID second = inserer("StkBuilding", SECOND_IMMEUBLE, "AA-IMM-REF-PAT", 2);
        UUID premier = inserer("StkBuilding", PREMIER_IMMEUBLE, "ZZ-IMM-REF-PAT", 1);

        List<String> ids = idsDansLOrdre(mockMvc.perform(get("/api/referentiels/immeubles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(premier) + ".code").value("ZZ-IMM-REF-PAT")));

        assertThat(ids).contains(premier.toString(), second.toString());
        assertThat(ids.indexOf(premier.toString())).isLessThan(ids.indexOf(second.toString()));
    }

    @Test
    void r13_5_etages_listeTrieeParSorting() throws Exception {
        UUID second = inserer("StkFloor", SECOND_ETAGE, "AA-ETG-REF-PAT", 2);
        UUID premier = inserer("StkFloor", PREMIER_ETAGE, "ZZ-ETG-REF-PAT", 1);

        List<String> ids = idsDansLOrdre(mockMvc.perform(get("/api/referentiels/etages")).andExpect(status().isOk()));

        assertThat(ids).contains(premier.toString(), second.toString());
        assertThat(ids.indexOf(premier.toString())).isLessThan(ids.indexOf(second.toString()));
    }

    // ------------------------------------------------------------------ catégories / types

    @Test
    void r6_categories_pourSousArticleTrue_natureBatEtForUnderItem() throws Exception {
        mockMvc.perform(get(CATEGORIES).param(POUR_SOUS_ARTICLE, "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(categorieSous.getId())).exists())
                .andExpect(jsonPath(ligne(categorieArticle.getId())).doesNotExist())
                .andExpect(jsonPath(ligne(categorieTerrainSous.getId())).doesNotExist());
    }

    @Test
    void r6_categories_pourSousArticleFalse_natureBatSansForUnderItem() throws Exception {
        mockMvc.perform(get(CATEGORIES).param(POUR_SOUS_ARTICLE, "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(categorieArticle.getId())).exists())
                .andExpect(jsonPath(ligne(categorieSous.getId())).doesNotExist())
                .andExpect(jsonPath(ligne(categorieTerrainSous.getId())).doesNotExist());
    }

    @Test
    void r6_categories_sansParametre_inchange() throws Exception {
        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(categorieArticle.getId())).exists())
                .andExpect(jsonPath(ligne(categorieSous.getId())).exists())
                .andExpect(jsonPath(ligne(categorieTerrainSous.getId())).exists());
    }

    @Test
    void r6_types_pourSousArticleTrue() throws Exception {
        mockMvc.perform(get(TYPES).param(POUR_SOUS_ARTICLE, "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(typeSous.getId())).exists())
                .andExpect(jsonPath(ligne(typeArticle.getId())).doesNotExist())
                .andExpect(jsonPath(ligne(typeTerrain.getId())).doesNotExist());
    }

    @Test
    void r6_types_pourSousArticleFalse() throws Exception {
        mockMvc.perform(get(TYPES).param(POUR_SOUS_ARTICLE, "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(typeArticle.getId())).exists())
                .andExpect(jsonPath(ligne(typeSous.getId())).doesNotExist())
                .andExpect(jsonPath(ligne(typeTerrain.getId())).doesNotExist());
    }

    @Test
    void r6_types_sansParametre_inchange() throws Exception {
        mockMvc.perform(get(TYPES))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(typeArticle.getId())).exists())
                .andExpect(jsonPath(ligne(typeSous.getId())).exists())
                .andExpect(jsonPath(ligne(typeTerrain.getId())).exists());
    }

    // ------------------------------------------------------------------ vocations

    @Test
    void r6_vocations_pourArticle_exclutProjet() throws Exception {
        fixture.vocationVente();
        fixture.vocationProjet();

        mockMvc.perform(get(VOCATIONS).param("pourArticle", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(VOCATION_VENTE)).exists())
                .andExpect(jsonPath(ligne(VOCATION_PROJET)).doesNotExist());
    }

    @Test
    void r6_vocations_sansParametre_inchange() throws Exception {
        fixture.vocationVente();
        fixture.vocationProjet();

        mockMvc.perform(get(VOCATIONS))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(VOCATION_VENTE)).exists())
                .andExpect(jsonPath(ligne(VOCATION_PROJET)).exists());
    }
}
