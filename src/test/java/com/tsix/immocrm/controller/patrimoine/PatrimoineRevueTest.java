package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine.ReferentielArticle;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-patrimoine-ecriture.md §5.4 règle 8 (sous-article rattaché modifié → parent recalculé ; parent occupé →
 * sous-article figé) et §2.6 (unicité du code par tranche, sous-articles compris).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatrimoineRevueTest {

    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String CATEGORIE_ID = "categorieId";
    private static final String TYPE_ID = "typeId";
    private static final String ETAGE_ID = "etageId";
    private static final String SURFACE_PROVISOIRE = "surfaceProvisoire";
    private static final String PRIX_M2_PROVISOIRE = "prixM2ProvisoireHT";
    private static final String PRIX_M2_DEFINITIF = "prixM2DefinitifHT";
    private static final String TAUX_TVA = "tauxTva";
    private static final String CODE_APPARTEMENT = "REV-APP";
    private static final String SOUS_ARTICLES = "/sous-articles";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private ReferentielArticle ref;
    private UUID appartement;

    @BeforeEach
    void setUp() throws Exception {
        admin = fixture.utilisateur("ADM-REV");
        ref = fixture.referentielArticle("REV");
        // Appartement : 10 000 HT/m² × 60 m², TVA 20 % → prix article TTC 720 000 (HT 600 000).
        Map<String, Object> corps = new HashMap<>();
        corps.put("trancheId", ref.tranche().getId());
        corps.put(CODE, CODE_APPARTEMENT);
        corps.put(DESCRIPTION, "Appartement revue");
        corps.put(CATEGORIE_ID, ref.categorie().getId());
        corps.put(TYPE_ID, ref.type().getId());
        corps.put("immeubleId", ref.immeuble().getId());
        corps.put(ETAGE_ID, ref.etage().getId());
        corps.put(SURFACE_PROVISOIRE, 60);
        corps.put(PRIX_M2_PROVISOIRE, 10000);
        corps.put(PRIX_M2_DEFINITIF, 10000);
        corps.put(TAUX_TVA, 0.2);
        appartement = idDe(poster("/api/articles", corps));
    }

    private ResultActions poster(String url, Map<String, ?> corps) throws Exception {
        return mockMvc.perform(post(url).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private ResultActions modifier(UUID id, Map<String, ?> corps) throws Exception {
        return mockMvc.perform(patch("/api/articles/{id}", id).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private UUID idDe(ResultActions actions) throws Exception {
        String reponse = actions.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        em.clear();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    /** Parking : 2 000 HT/m² × 12 m², TVA 20 % → prix article TTC 28 800 (HT 24 000). */
    private Map<String, Object> corpsParking(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(CODE, code);
        corps.put(DESCRIPTION, "Parking " + code);
        corps.put(CATEGORIE_ID, ref.categorieSous().getId());
        corps.put(TYPE_ID, ref.typeSous().getId());
        corps.put(ETAGE_ID, ref.etage().getId());
        corps.put(SURFACE_PROVISOIRE, 12);
        corps.put(PRIX_M2_PROVISOIRE, 2000);
        corps.put(PRIX_M2_DEFINITIF, 2000);
        corps.put(TAUX_TVA, 0.2);
        return corps;
    }

    private UUID parkingRattache(String code) throws Exception {
        return idDe(poster("/api/articles/" + appartement + SOUS_ARTICLES, corpsParking(code)));
    }

    private void occuper(UUID articleId, String codeDossier) {
        ComFolder dossier = fixture.dossier(codeDossier, ref.vente());
        StkItem item = itemRepository.findById(articleId).orElseThrow();
        item.setComFolderId(dossier.getId());
        itemRepository.save(item);
        em.flush();
        em.clear();
    }

    @Test
    void sousArticleRattache_prixModifie_recalculeLePrixDeVenteDuParent() throws Exception {
        UUID parking = parkingRattache("REV-PRK1");

        // Définitif 3 000 HT/m² × 12 m² → 36 000 HT, 43 200 TTC ; le provisoire reste à 28 800.
        modifier(parking, Map.of(PRIX_M2_DEFINITIF, 3000)).andExpect(status().isOk());
        em.flush();
        em.clear();

        StkItem parent = itemRepository.findById(appartement).orElseThrow();
        assertAll(
                () -> assertThat(parent.getComSalePrice()).isEqualByComparingTo("763200"),
                () -> assertThat(parent.getComSalePriceHT()).isEqualByComparingTo("636000"),
                () -> assertThat(parent.getComSalePriceFloor()).isEqualByComparingTo("748800"),
                () -> assertThat(parent.getSalePrice()).isEqualByComparingTo("720000"));
    }

    @Test
    void parentModifie_prixDeVenteGardeSesSousArticles() throws Exception {
        parkingRattache("REV-PRK2");

        // 11 000 HT/m² × 1,2 × 60 m² = 792 000, + 28 800 du parking.
        modifier(appartement, Map.of(PRIX_M2_DEFINITIF, 11000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixArticleDefinitifTTC").value(792000))
                .andExpect(jsonPath("$.comSalePrice").value(820800));
    }

    @Test
    void sousArticleRattache_parentOccupe_409SurLePrix_200SurLaDescription() throws Exception {
        UUID parking = parkingRattache("REV-PRK3");
        occuper(appartement, "26/REV-OCC1");

        modifier(parking, Map.of(PRIX_M2_DEFINITIF, 3000))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.champ").value(PRIX_M2_DEFINITIF))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("26/REV-OCC1")));
        modifier(parking, Map.of(DESCRIPTION, "Parking couvert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupe").value(true));
    }

    @Test
    void sousArticle_codeDUnArticleDeLaTranche_409() throws Exception {
        poster("/api/articles/" + appartement + SOUS_ARTICLES, corpsParking("rev-app"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.champ").value(CODE));
    }
}
