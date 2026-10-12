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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-ecriture.md §5.4 (règles 5 à 8) et §11. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SousArticleTest {

    private static final String PRIX_VENTE = "$.comSalePrice";
    private static final String ARTICLES = "/api/articles/";
    private static final String SOUS_ARTICLES = "/sous-articles";
    private static final String AFFECTER = "/affecter";
    private static final String DESAFFECTER = "/desaffecter";
    private static final String CATEGORIE_ID = "categorieId";
    private static final String TYPE_ID = "typeId";
    private static final String ETAGE_ID = "etageId";
    private static final String DESCRIPTION = "description";
    private static final String CODE = "code";
    private static final String IDS_LIBRES = "$.content[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private ReferentielArticle ref;
    private UUID appartement;

    @BeforeEach
    void setUp() throws Exception {
        admin = fixture.utilisateur("ADM-SOUS");
        ref = fixture.referentielArticle("SOUS");
        // Appartement : 10 000 HT/m² × 60 m², TVA 20 % → prix article TTC 720 000.
        Map<String, Object> corps = new HashMap<>();
        corps.put("trancheId", ref.tranche().getId());
        corps.put(CODE, "SOUS-APP");
        corps.put(DESCRIPTION, "Appartement");
        corps.put(CATEGORIE_ID, ref.categorie().getId());
        corps.put(TYPE_ID, ref.type().getId());
        corps.put("immeubleId", ref.immeuble().getId());
        corps.put(ETAGE_ID, ref.etage().getId());
        corps.put("surfaceProvisoire", 60);
        corps.put("prixM2ProvisoireHT", 10000);
        corps.put("prixM2DefinitifHT", 10000);
        corps.put("tauxTva", 0.2);
        appartement = idDe(poster("/api/articles", corps));
    }

    private ResultActions poster(String url, Map<String, ?> corps) throws Exception {
        return mockMvc.perform(post(url).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private UUID idDe(ResultActions actions) throws Exception {
        String reponse = actions.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        em.clear();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    /** Parking : 2 000 HT/m² × 12 m², TVA 20 % → prix article TTC 28 800. */
    private Map<String, Object> corpsParking(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(CODE, code);
        corps.put(DESCRIPTION, "Parking " + code);
        corps.put(CATEGORIE_ID, ref.categorieSous().getId());
        corps.put(TYPE_ID, ref.typeSous().getId());
        corps.put(ETAGE_ID, ref.etage().getId());
        corps.put("surfaceProvisoire", 12);
        corps.put("prixM2ProvisoireHT", 2000);
        corps.put("prixM2DefinitifHT", 2000);
        corps.put("tauxTva", 0.2);
        return corps;
    }

    private UUID sousArticleRattache(String code) throws Exception {
        return idDe(poster(ARTICLES + appartement + SOUS_ARTICLES, corpsParking(code)));
    }

    /** Sous-article libre : créé rattaché puis désaffecté (seule voie d'écriture d'un sous-article). */
    private UUID sousArticleLibre(String code) throws Exception {
        UUID id = sousArticleRattache(code);
        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + id + DESAFFECTER, Map.of()).andExpect(status().isOk());
        em.flush();
        em.clear();
        return id;
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
    void creation_rattacheeHeriteDuParentEtRecalculeLePrixDeVente() throws Exception {
        UUID parking = sousArticleRattache("SOUS-PRK1");

        StkItem sous = itemRepository.findById(parking).orElseThrow();
        StkItem parent = itemRepository.findById(appartement).orElseThrow();
        assertAll(
                () -> assertThat(sous.getParent().getId()).isEqualTo(appartement),
                () -> assertThat(sous.getIsUnderItem()).isTrue(),
                () -> assertThat(sous.getIsUnderItemOrigin()).isTrue(),
                () -> assertThat(sous.getIsTitled()).isFalse(),
                () -> assertThat(sous.getStkHierarchy().getId()).isEqualTo(ref.tranche().getId()),
                () -> assertThat(sous.getStkVocation().getId()).isEqualTo(parent.getStkVocation().getId()),
                () -> assertThat(sous.getStkBuilding().getId()).isEqualTo(ref.immeuble().getId()),
                () -> assertThat(sous.getComSalePrice()).isEqualByComparingTo("28800"),
                () -> assertThat(parent.getComSalePrice()).isEqualByComparingTo("748800"),
                () -> assertThat(parent.getComSalePriceHT()).isEqualByComparingTo("624000"));
    }

    @Test
    void creation_400_parentSousArticle() throws Exception {
        UUID parking = sousArticleRattache("SOUS-PRK2");
        poster(ARTICLES + parking + SOUS_ARTICLES, corpsParking("SOUS-PRK2B"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creation_409_parentOccupe() throws Exception {
        occuper(appartement, "26/SOUS-OCC0");
        poster(ARTICLES + appartement + SOUS_ARTICLES, corpsParking("SOUS-PRK3"))
                .andExpect(status().isConflict());
    }

    @Test
    void affecter_unLibre_200_etParentRecalcule() throws Exception {
        UUID libre = sousArticleLibre("SOUS-LIB1");

        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + libre + AFFECTER, Map.of("toujoursAffecte", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PRIX_VENTE).value(748800));

        em.flush();
        em.clear();
        StkItem sous = itemRepository.findById(libre).orElseThrow();
        assertAll(
                () -> assertThat(sous.getParent().getId()).isEqualTo(appartement),
                () -> assertThat(sous.getAlwaysAffected()).isTrue());
    }

    @Test
    void affecter_409_dejaRattache() throws Exception {
        UUID rattache = sousArticleRattache("SOUS-RAT1");
        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + rattache + AFFECTER, Map.of())
                .andExpect(status().isConflict());
    }

    @Test
    void affecter_409_autreTranche() throws Exception {
        ReferentielArticle autre = fixture.referentielArticle("SOUS-AUTRE");
        Map<String, Object> corps = new HashMap<>(Map.of("trancheId", autre.tranche().getId(), CODE, "SOUS-AUTRE-APP",
                DESCRIPTION, "Autre", CATEGORIE_ID, autre.categorie().getId(), TYPE_ID, autre.type().getId(),
                "immeubleId", autre.immeuble().getId(), ETAGE_ID, autre.etage().getId()));
        UUID autreArticle = idDe(poster("/api/articles", corps));
        Map<String, Object> parking = corpsParking("SOUS-AUTRE-PRK");
        parking.put(CATEGORIE_ID, autre.categorieSous().getId());
        parking.put(TYPE_ID, autre.typeSous().getId());
        parking.put(ETAGE_ID, autre.etage().getId());
        UUID sousAutre = idDe(poster(ARTICLES + autreArticle + SOUS_ARTICLES, parking));
        poster(ARTICLES + autreArticle + SOUS_ARTICLES + "/" + sousAutre + DESAFFECTER, Map.of())
                .andExpect(status().isOk());

        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + sousAutre + AFFECTER, Map.of())
                .andExpect(status().isConflict());
    }

    @Test
    void affecter_409_sousArticleOccupe() throws Exception {
        UUID libre = sousArticleLibre("SOUS-LIB2");
        occuper(libre, "26/SOUS-OCC1");
        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + libre + AFFECTER, Map.of())
                .andExpect(status().isConflict());
    }

    @Test
    void desaffecter_200_parentNullEtPrixParentRecalcule() throws Exception {
        UUID parking = sousArticleRattache("SOUS-DES1");

        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + parking + DESAFFECTER, Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath(PRIX_VENTE).value(720000));

        em.flush();
        em.clear();
        StkItem sous = itemRepository.findById(parking).orElseThrow();
        assertAll(
                () -> assertThat(sous.getParent()).isNull(),
                () -> assertThat(sous.getAlwaysAffected()).isFalse());
    }

    @Test
    void desaffecter_409_sousArticleOccupe() throws Exception {
        UUID parking = sousArticleRattache("SOUS-DES2");
        occuper(parking, "26/SOUS-OCC2");
        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + parking + DESAFFECTER, Map.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vous ne pouvez pas désaffecter un sous article ayant un dossier de vente !"));
    }

    @Test
    void desaffecter_409_parentOccupe() throws Exception {
        UUID parking = sousArticleRattache("SOUS-DES3");
        occuper(appartement, "26/SOUS-OCC3");
        poster(ARTICLES + appartement + SOUS_ARTICLES + "/" + parking + DESAFFECTER, Map.of())
                .andExpect(status().isConflict());
    }

    @Test
    void sousArticlesLibres_excluentRattachesToujoursAffectesEtOccupes() throws Exception {
        UUID libre = sousArticleLibre("SOUS-L-OK");
        UUID rattache = sousArticleRattache("SOUS-L-RAT");
        UUID occupe = sousArticleLibre("SOUS-L-OCC");
        occuper(occupe, "26/SOUS-OCC4");
        UUID toujours = sousArticleLibre("SOUS-L-TA");
        StkItem item = itemRepository.findById(toujours).orElseThrow();
        item.setAlwaysAffected(true);
        itemRepository.save(item);
        em.flush();

        mockMvc.perform(get("/api/tranches/{id}/sous-articles-libres", ref.tranche().getId())
                        .with(connecte(admin, ROLE_ADMIN)).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS_LIBRES, hasItem(libre.toString())))
                .andExpect(jsonPath(IDS_LIBRES, not(hasItem(rattache.toString()))))
                .andExpect(jsonPath(IDS_LIBRES, not(hasItem(occupe.toString()))))
                .andExpect(jsonPath(IDS_LIBRES, not(hasItem(toujours.toString()))));
    }

    @Test
    void sousArticlesLibres_404_trancheInconnue() throws Exception {
        mockMvc.perform(get("/api/tranches/{id}/sous-articles-libres", UUID.randomUUID()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }
}
