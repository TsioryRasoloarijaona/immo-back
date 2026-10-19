package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-entites-juridiques.md : défauts relevés hors des tests d'acceptation verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RevueEntitesJuridiquesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private StkItemRepository itemRepository;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "REV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("EJ-" + jeton, true);
    }

    // ------------------------------------------------------------------ PUT répartition : corps mal formé → 400, pas 500

    /** §7.11.9 : un actionnaireId illisible dans le corps est une erreur de saisie (400), pas une erreur interne. */
    @Test
    void put_repartition_actionnaireIdIllisible_400() throws Exception {
        mockMvc.perform(put("/api/entites-juridiques/{id}/repartition", entite.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"actionnaireId\":\"pas-un-uuid\",\"nombreParts\":10}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_repartition_ligneNulle_400() throws Exception {
        mockMvc.perform(put("/api/entites-juridiques/{id}/repartition", entite.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("[null]"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §4 : articles d'une entité désactivée hors des listes

    private StkItem article(String code, CfgTranche tranche, StkItemCategory categorie, StkItemType type,
                            StkItem parent) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemCategory(categorie);
        item.setStkItemType(type);
        item.setStkKind(fixture.natureBat());
        item.setStkVocation(fixture.vocationVente());
        item.setStkHierarchy(tranche);
        item.setIsUnderItem(parent != null);
        item.setAlwaysAffected(false);
        item.setParent(parent);
        return itemRepository.save(item);
    }

    /** Sélecteur d'articles d'une tranche : après désactivation, l'article ne doit plus être proposé (R2/R3/R4). */
    @Test
    void referentielArticles_trancheDUneEntiteDesactivee_articleAbsent() throws Exception {
        fixture.referentielArticle();
        StkKind nature = fixture.natureBat();
        CfgTranche tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, entite));
        StkItem art = article("ART-" + jeton, tranche, fixture.categorie("CAT-" + jeton, nature, false),
                fixture.type("TYP-" + jeton, nature, false), null);

        mockMvc.perform(get("/api/referentiels/articles").param("trancheId", tranche.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(art.getId().toString())));

        entites.actif(entite.getId(), false);

        mockMvc.perform(get("/api/referentiels/articles").param("trancheId", tranche.getId().toString())
                        .with(connecte(admin)))
                .andExpect(content().string(not(containsString(art.getId().toString()))));
    }

    /** Sous-articles d'un article d'une entité désactivée : même règle que GET /api/articles/{id}/sous-articles (404). */
    @Test
    void referentielSousArticles_articleDUneEntiteDesactivee_sousArticleAbsent() throws Exception {
        fixture.referentielArticle();
        StkKind nature = fixture.natureBat();
        CfgTranche tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, entite));
        StkItem parent = article("ART-" + jeton, tranche, fixture.categorie("CAT-" + jeton, nature, false),
                fixture.type("TYP-" + jeton, nature, false), null);
        StkItem sous = article("SA-" + jeton, tranche, fixture.categorie("CATS-" + jeton, nature, true),
                fixture.type("TYPS-" + jeton, nature, true), parent);

        mockMvc.perform(get("/api/referentiels/sous-articles").param("articleId", parent.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(sous.getId().toString())));

        entites.actif(entite.getId(), false);

        mockMvc.perform(get("/api/referentiels/sous-articles").param("articleId", parent.getId().toString())
                        .with(connecte(admin)))
                .andExpect(content().string(not(containsString(sous.getId().toString()))));
    }
}
