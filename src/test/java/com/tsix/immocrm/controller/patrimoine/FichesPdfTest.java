package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9c §3.2 (GEN0011) — FEATURE-patrimoine-import-export.md, règle F1 : {@code GET /api/<fiche>/{id}/fiche.pdf} rend
 * en {@code inline} un PDF portant les champs du détail ; même droit et même visibilité que {@code GET /{id}} (404 sinon,
 * entité inactive comprise).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichesPdfTest {

    private static final String PROJETS = "/api/projets";
    private static final String TRANCHES = "/api/tranches";
    private static final String ARTICLES = "/api/articles";
    private static final String TERRAINS = "/api/terrains";
    private static final String PROSPECTIONS_TERRAIN = "/api/prospections-terrain";
    private static final String ENTITES = "/api/entites-juridiques";
    private static final String DETAIL = "/{id}";
    private static final String FICHE_PDF = "/{id}/fiche.pdf";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureExportPatrimoine articles;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureProspectionsTerrain prospections;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgProject projet;
    private CfgTranche tranche;

    /** Une fiche : sa route, l'id, les champs attendus dans le PDF et l'entité dont dépend sa visibilité (R4). */
    private record Fiche(String route, UUID id, String code, String description, UUID entiteId) {
    }

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = FixtureExportPatrimoine.jeton();
        entite = fixture.entite("ENT-F-" + jeton, true);
        projet = fixture.projet("PRJ-F-" + jeton, entite);
        tranche = fixture.tranche("TR-F-" + jeton, projet);
    }

    // ------------------------------------------------------------------ 200, inline, champs du détail

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJETS, TRANCHES, ARTICLES, TERRAINS, PROSPECTIONS_TERRAIN, ENTITES})
    void fiche_200_pdfInline_codeEtLibelleDuDetail(String route) throws Exception {
        Fiche fiche = fiche(route);
        mockMvc.perform(get(route + DETAIL, fiche.id()).with(connecte(admin))).andExpect(status().isOk());

        MvcResult resultat = executer(mockMvc, get(route + FICHE_PDF, fiche.id()).with(connecte(admin)));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultat.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PDF_VALUE);
        assertThat(resultat.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline");
        byte[] contenu = resultat.getResponse().getContentAsByteArray();
        assertThat(estPdf(contenu)).isTrue();
        assertThat(compact(textePdf(contenu))).contains(fiche.code()).contains(compact(fiche.description()));
    }

    // ------------------------------------------------------------------ F1 : entité inactive → 404, comme le détail

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJETS, TRANCHES, ARTICLES, TERRAINS})
    void f1_fiche_404_entiteInactive_commeLeDetail(String route) throws Exception {
        Fiche fiche = fiche(route);
        assertThat(statutFiche(fiche, connecte(admin))).isEqualTo(200);

        entites.actif(fiche.entiteId(), false);

        mockMvc.perform(get(route + DETAIL, fiche.id()).with(connecte(admin))).andExpect(status().isNotFound());
        assertThat(statutFiche(fiche, connecte(admin))).isEqualTo(404);
    }

    /** L'écran des entités montre aussi les inactives : la fiche PDF suit le détail, quel qu'il soit. */
    @Test
    void entiteInactive_fichePdf_memeStatutQueLeDetail() throws Exception {
        CfgCompany inactive = fixture.entite("ENT-I-" + jeton, false);
        relire();
        int detail = mockMvc.perform(get(ENTITES + DETAIL, inactive.getId()).with(connecte(admin)))
                .andReturn().getResponse().getStatus();

        int pdf = statutFiche(new Fiche(ENTITES, inactive.getId(), inactive.getCode(), inactive.getDescription(), null),
                connecte(admin));

        assertThat(pdf).isEqualTo(detail);
    }

    // ------------------------------------------------------------------ id inconnu → 404

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJETS, TRANCHES, ARTICLES, TERRAINS, PROSPECTIONS_TERRAIN, ENTITES})
    void fiche_idInconnu_404(String route) throws Exception {
        MvcResult resultat = executer(mockMvc, get(route + FICHE_PDF, UUID.randomUUID()).with(connecte(admin)));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(404);
    }

    // Le droit d'accès à la fiche est le même que pour le détail.

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJETS, TRANCHES, ARTICLES, TERRAINS, PROSPECTIONS_TERRAIN, ENTITES})
    void fiche_commercial_memeStatutQueLeDetail(String route) throws Exception {
        Fiche fiche = fiche(route);
        RequestPostProcessor commercial = FixtureExportPatrimoine.commercial(fixture.administrateur());
        relire();
        int detail = mockMvc.perform(get(route + DETAIL, fiche.id()).with(commercial))
                .andReturn().getResponse().getStatus();

        assertThat(statutFiche(fiche, commercial)).isEqualTo(detail);
    }

    // ------------------------------------------------------------------ sections (§3.2)

    @Test
    void ficheArticle_sections_etTableauDesSousArticles() throws Exception {
        FixtureExportPatrimoine.Referentiel r = articles.referentiel(jeton);
        StkItem article = articles.article(r, "ART-F-" + jeton, tranche, null);
        StkItem sous = articles.sousArticle(r, "SR-F-" + jeton, tranche, article);
        relire();

        String texte = textePdfOk(ARTICLES, article.getId());

        assertThat(texte).containsIgnoringCase("Identification").containsIgnoringCase("Superficies")
                .containsIgnoringCase("Foncier").containsIgnoringCase("Autres").containsIgnoringCase("Sous-articles");
        assertThat(compact(texte)).contains(sous.getCode());
    }

    @Test
    void ficheTranche_sections() throws Exception {
        relire();

        String texte = textePdfOk(TRANCHES, tranche.getId());

        assertThat(texte).containsIgnoringCase("Identification").containsIgnoringCase("Délais")
                .containsIgnoringCase("Règles").containsIgnoringCase("Terrains").containsIgnoringCase("Étages");
    }

    // ------------------------------------------------------------------ jeux de données

    private Fiche fiche(String route) {
        Fiche fiche = switch (route) {
            case PROJETS -> new Fiche(route, projet.getId(), projet.getCode(), projet.getDescription(), entite.getId());
            case TRANCHES -> new Fiche(route, tranche.getId(), tranche.getCode(), tranche.getDescription(),
                    entite.getId());
            case ARTICLES -> article();
            case TERRAINS -> terrain();
            case PROSPECTIONS_TERRAIN -> prospectionTerrain();
            case ENTITES -> new Fiche(route, entite.getId(), entite.getCode(), entite.getDescription(), entite.getId());
            default -> throw new IllegalArgumentException(route);
        };
        relire();
        return fiche;
    }

    private Fiche article() {
        StkItem article = articles.article(articles.referentiel(jeton), "ART-F-" + jeton, tranche, null);
        return new Fiche(ARTICLES, article.getId(), article.getCode(), article.getDescription(), entite.getId());
    }

    private Fiche terrain() {
        FixtureTerrains.Contexte c = terrains.contexte("P9F");
        String code = "TER-F-" + jeton;
        return new Fiche(TERRAINS, terrains.terrain(c, code), code, "Terrain " + code, c.entite().getId());
    }

    private Fiche prospectionTerrain() {
        FixtureProspectionsTerrain.Contexte c = prospections.contexte("P9F");
        String code = "PT-F-" + jeton;
        return new Fiche(PROSPECTIONS_TERRAIN, prospections.prospection(c, code), code, "Prospection " + code, null);
    }

    // ------------------------------------------------------------------ outils

    private int statutFiche(Fiche fiche, RequestPostProcessor qui) throws Exception {
        return executer(mockMvc, get(fiche.route() + FICHE_PDF, fiche.id()).with(qui)).getResponse().getStatus();
    }

    private String textePdfOk(String route, UUID id) throws Exception {
        MvcResult resultat = executer(mockMvc, get(route + FICHE_PDF, id).with(connecte(admin)));
        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        return textePdf(resultat.getResponse().getContentAsByteArray());
    }

    private void relire() {
        em.flush();
        em.clear();
    }
}
