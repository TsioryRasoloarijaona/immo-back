package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/etat-des-ventes} (+ {@code /groupes}, {@code /totaux-colonnes}, catalogue de colonnes) —
 * FEATURE-etat-des-ventes.md §2, §3.2, §6. Jeu commun d'une tranche propre au test :
 * <pre>
 *   A1 APP libre          A2 APP RESERVATION (mars 2025)   A3 APP CONCRETISATION (mai 2025)
 *   A4 APP dossier annulé (→ Libre)   A5 APP dossier sans statut   P1 PAR parking autonome (libre)
 *   A2-PK sous-article rattaché à A2 (exclu)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesListeTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String URL_SYNTHESE = "/api/etat-des-ventes/synthese";
    private static final String URL_GROUPES = "/api/etat-des-ventes/groupes";
    private static final String URL_TOTAUX = "/api/etat-des-ventes/totaux-colonnes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String CATEGORIE_ID = "categorieId";
    private static final String STATUT = "statut";
    private static final String NATURE = "nature";
    private static final String SEARCH = "search";
    private static final String COLONNES = "colonnes";
    private static final String TOTAL_ELEMENTS = "$.totalElements";
    private static final String LIBRE = "LIBRE";
    private static final String RESERVATION = "RESERVATION";
    private static final String SANS_STATUT = "SANS_STATUT";
    private static final int NOMBRE_RACINES = 6;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;
    @Autowired
    private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItemCategory appartement;
    private StkItemCategory parking;
    private StkItemType typeParking;
    private StkItem libre;
    private StkItem reserve;
    private StkItem annule;
    private StkItem sansStatut;
    private StkItem parkingAutonome;
    private StkItem sousArticle;
    private ComFolder dossierReserve;
    private CfgTier client;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVL");
        projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        appartement = fixture.categorie("APP-" + prefixe);
        parking = fixture.categorie("PAR-" + prefixe);
        StkItemType typeAppartement = fixture.type("T3-" + prefixe);
        typeParking = fixture.type("PK-" + prefixe);
        client = fixture.client("ZCLI" + prefixe);

        libre = fixture.article(prefixe + "-A1", tranche, appartement, typeAppartement);
        reserve = fixture.article(prefixe + "-A2", tranche, appartement, typeAppartement);
        dossierReserve = fixture.vendre(reserve, "DOS-" + prefixe + "-A2", fixture.statut(StatutDossier.RESERVATION),
                client, LocalDateTime.of(2025, 3, 15, 10, 0));
        fixture.vendre(fixture.article(prefixe + "-A3", tranche, appartement, typeAppartement),
                "DOS-" + prefixe + "-A3", fixture.statut(StatutDossier.CONCRETISATION), null,
                LocalDateTime.of(2025, 5, 15, 10, 0));
        annule = fixture.article(prefixe + "-A4", tranche, appartement, typeAppartement);
        fixture.vendre(annule, "DOS-" + prefixe + "-A4", fixture.statutSortie("ANN-" + prefixe, true), null, null);
        sansStatut = fixture.article(prefixe + "-A5", tranche, appartement, typeAppartement);
        fixture.vendre(sansStatut, "DOS-" + prefixe + "-A5", null, null, null);
        parkingAutonome = fixture.parkingAutonome(prefixe + "-P1", tranche, parking, typeParking);
        sousArticle = fixture.sousArticle(prefixe + "-A2-PK", reserve, parking, typeParking);
        fixture.relire();
    }

    private MockHttpServletRequestBuilder liste() {
        return get(URL).param(TRANCHE_ID, tranche.getId().toString());
    }

    private static String article(StkItem a) {
        return "$.content[?(@.articleId=='" + a.getId() + "')]";
    }

    private long totalElements(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, TOTAL_ELEMENTS)).longValue();
    }

    // ------------------------------------------------------------------ R1 — une ligne = un article racine

    @Test
    void r1_uneLigneParArticleRacine_sousArticleRattacheExclu() throws Exception {
        mockMvc.perform(liste().param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(NOMBRE_RACINES))
                .andExpect(jsonPath(article(parkingAutonome)).exists())
                .andExpect(jsonPath(article(sousArticle)).doesNotExist());
    }

    // ------------------------------------------------------------------ R2 — statut de la ligne

    @Test
    void r2_ligneLibre_statutLibre_prixCatalogue_resteNull() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "articleId,articleCode,statutCode,prixVenteTTC,resteARegler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(article(libre) + ".statutCode").value(LIBRE))
                .andExpect(jsonPath(article(libre) + ".prixVenteTTC").value(FixtureEtatVentes.PRIX_CATALOGUE.doubleValue()))
                .andExpect(jsonPath(article(libre) + ".resteARegler").value(Matchers.contains(Matchers.nullValue())));
    }

    @Test
    void r2_ligneReservee_statutEtDossier() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "articleId,statutCode,dossierId,dossierCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(article(reserve) + ".statutCode").value(RESERVATION))
                .andExpect(jsonPath(article(reserve) + ".dossierId").value(dossierReserve.getId().toString()))
                .andExpect(jsonPath(article(reserve) + ".dossierCode").value(dossierReserve.getCode()));
    }

    @Test
    void r2_dossierAnnule_ligneLibre() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "articleId,statutCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(article(annule) + ".statutCode").value(LIBRE));
    }

    // ------------------------------------------------------------------ §3.2 — filtres

    @Test
    void filtreStatutLibre_rendLibresEtAnnule() throws Exception {
        mockMvc.perform(liste().param(STATUT, LIBRE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(3))
                .andExpect(jsonPath(article(libre)).exists())
                .andExpect(jsonPath(article(annule)).exists())
                .andExpect(jsonPath(article(parkingAutonome)).exists());
    }

    @Test
    void filtreStatutSansStatut_rendLeDossierSansStatut() throws Exception {
        mockMvc.perform(liste().param(STATUT, SANS_STATUT))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(sansStatut)).exists());
    }

    @Test
    void filtreStatut_multiple() throws Exception {
        mockMvc.perform(liste().param(STATUT, RESERVATION, "CONCRETISATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(2));
    }

    @Test
    void filtreNature_articlesOuSousArticles() throws Exception {
        assertThat(new long[] {
                totalElements(liste()),
                totalElements(liste().param(NATURE, "TOUS")),
                totalElements(liste().param(NATURE, "ARTICLES")),
                totalElements(liste().param(NATURE, "SOUS_ARTICLES"))})
                .containsExactly(NOMBRE_RACINES, NOMBRE_RACINES, NOMBRE_RACINES - 1L, 1L);
    }

    @Test
    void filtreNature_sousArticles_rendLeParkingAutonomeSeul() throws Exception {
        mockMvc.perform(liste().param(NATURE, "SOUS_ARTICLES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(article(parkingAutonome)).exists())
                .andExpect(jsonPath(article(sousArticle)).doesNotExist());
    }

    @Test
    void filtreCategorie() throws Exception {
        mockMvc.perform(liste().param(CATEGORIE_ID, parking.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(parkingAutonome)).exists());
    }

    @Test
    void filtreType() throws Exception {
        mockMvc.perform(liste().param("typeId", typeParking.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(parkingAutonome)).exists());
    }

    @Test
    void filtreProjet() throws Exception {
        mockMvc.perform(get(URL).param("projetId", projet.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(NOMBRE_RACINES));
    }

    @Test
    void search_codeArticle() throws Exception {
        mockMvc.perform(liste().param(SEARCH, libre.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(libre)).exists());
    }

    @Test
    void search_codeDossier() throws Exception {
        mockMvc.perform(liste().param(SEARCH, dossierReserve.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(reserve)).exists());
    }

    @Test
    void search_client() throws Exception {
        mockMvc.perform(liste().param(SEARCH, client.getLastName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(reserve)).exists());
    }

    @Test
    void search_categorie() throws Exception {
        mockMvc.perform(liste().param(SEARCH, parking.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(parkingAutonome)).exists());
    }

    @Test
    void champDateReservation_periode() throws Exception {
        mockMvc.perform(liste().param("champDate", "RESERVATION").param("du", "2025-03-01").param("au", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1))
                .andExpect(jsonPath(article(reserve)).exists());
    }

    // ------------------------------------------------------------------ pagination, tri

    @Test
    void pagination() throws Exception {
        mockMvc.perform(liste().param("size", "4").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4))
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(NOMBRE_RACINES))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(liste().param("size", "4").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(NOMBRE_RACINES - 4));
    }

    @Test
    void tri_codeArticle() throws Exception {
        mockMvc.perform(liste().param("sort", "articleCode,asc").param(COLONNES, "articleId,articleCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].articleCode").value(libre.getCode()));

        mockMvc.perform(liste().param("sort", "articleCode,desc").param(COLONNES, "articleId,articleCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].articleCode").value(parkingAutonome.getCode()));
    }

    // ------------------------------------------------------------------ groupes, totaux, colonnes

    @Test
    void groupes_parCategorie_effectifsEtSommeEgaleAuTotal() throws Exception {
        String corps = mockMvc.perform(get(URL_GROUPES).param(TRANCHE_ID, tranche.getId().toString())
                        .param("groupBy", "categorieCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + appartement.getCode() + "')].nombre").value(5))
                .andExpect(jsonPath("$.content[?(@.cle=='" + parking.getCode() + "')].nombre").value(1))
                .andReturn().getResponse().getContentAsString();

        List<Integer> effectifs = JsonPath.read(corps, "$.content[*].nombre");
        assertThat(effectifs).isNotEmpty();
        assertThat(effectifs.stream().mapToLong(Integer::longValue).sum()).isEqualTo(totalElements(liste()));
    }

    @Test
    void groupes_colonneInconnue_rend400() throws Exception {
        mockMvc.perform(get(URL_GROUPES).param("groupBy", "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    /** Totaux sur tout le filtre, pas sur la page. */
    @Test
    void totaux_surToutLeFiltre_pasSurLaPage() throws Exception {
        double surfaceAttendue = FixtureEtatVentes.SURFACE.doubleValue() * NOMBRE_RACINES;
        mockMvc.perform(get(URL_TOTAUX).param(TRANCHE_ID, tranche.getId().toString())
                        .param("size", "2").param("agregats", "surface"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(NOMBRE_RACINES))
                .andExpect(jsonPath("$.totaux.surface").value(surfaceAttendue));
    }

    @Test
    void colonneInconnue_rend400() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogueDesColonnes_groupablesSurCategorieStatutTrancheType() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/etat-des-ventes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='articleCode')]").exists())
                .andExpect(jsonPath("$[?(@.nom=='categorieCode')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='statutCode')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='trancheCode')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='typeCode')].groupable").value(true));
    }

    // ------------------------------------------------------------------ R4 — visibilité

    @Test
    void r4_commercialHorsPerimetre_listeVide() throws Exception {
        AdmUser commercial = fixtureDossier.compte("edv.com." + prefixe);
        fixture.affecter(commercial, fixture.projet("PRJB-" + prefixe, true));
        fixture.relire();

        mockMvc.perform(liste().with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(0));
    }

    @Test
    void r4_entiteInactive_listeVide() throws Exception {
        CfgTranche trancheInactive = fixture.tranche("TRI-" + prefixe, fixture.projet("PRJI-" + prefixe, false), true);
        fixture.article(prefixe + "-I1", trancheInactive, appartement, typeParking);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, trancheInactive.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(0));
    }

    // ------------------------------------------------------------------ R5 — somme de la liste = cellule

    @Test
    void r5_listeFiltree_egaleCelluleDeLaSynthese() throws Exception {
        String synthese = mockMvc.perform(get(URL_SYNTHESE).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String ligne = "$.lignes[?(@.trancheId=='" + tranche.getId() + "' && @.categorieId=='"
                + appartement.getId() + "')]";

        for (String code : List.of(LIBRE, RESERVATION, SANS_STATUT)) {
            List<Number> cellule = JsonPath.read(synthese, ligne + ".parStatut." + code);
            long liste = totalElements(liste().param(CATEGORIE_ID, appartement.getId().toString()).param(STATUT, code));
            assertThat(cellule).as(code).singleElement()
                    .satisfies(n -> assertThat(n.longValue()).isEqualTo(liste).isPositive());
        }
        List<Number> total = JsonPath.read(synthese, ligne + ".total");
        long listeCategorie = totalElements(liste().param(CATEGORIE_ID, appartement.getId().toString()));
        assertThat(total).singleElement().satisfies(n -> assertThat(n.longValue()).isEqualTo(listeCategorie));
    }
}
