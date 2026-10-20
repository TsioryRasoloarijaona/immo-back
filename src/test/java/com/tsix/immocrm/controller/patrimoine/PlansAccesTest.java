package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès aux plans — FEATURE-patrimoine-plans.md, « Réponses du manager » n° 1 (périmètre projet sur toutes les routes
 * de plans → 404 hors périmètre ; rôles exemptés ; entité inactive → 404) et §2 (écriture Administrateur).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlansAccesTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String DIRECTION = "Direction Générale";
    private static final String PLANS_TRANCHE = "/api/tranches/{id}/plans";
    private static final String IMPORT = "/api/tranches/{id}/plans/import";
    private static final String D = "M 0,0 h 10 v 10 z";
    private static final String PLANS_DE_LA_TRANCHE = "SELECT COUNT(*) FROM StkHierarchyPlan WHERE StkHierarchyId = :id";
    private static final String TRACES_DE_LA_TRANCHE = """
            SELECT COUNT(*) FROM StkHierarchyPath p JOIN StkItem i ON i.PKey = p.StkItemId
            WHERE i.StkHierarchyId = :id""";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureArticleFiche lecture;

    private String jeton;
    private AdmUser admin;
    private StkFloor etage;
    private Lieu dansPerimetre;
    private Lieu horsPerimetre;
    private Lieu inactif;
    private RequestPostProcessor commercial;
    private RequestPostProcessor direction;

    /** Tranche avec un plan et un article tracé dessus. */
    private record Lieu(CfgTranche tranche, UUID plan, StkItem article) {
    }

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        etage = plans.etage("ACC-" + jeton);
        CfgTranche trancheA = plans.tranche("ACC-A-" + jeton, true);
        StkItem articleA = articleTrace(trancheA);
        plans.dossier(articleA, StatutDossier.RESERVATION, "Magenta", "Réservation");

        AdmUser compteCommercial = vente.compte("ACC-COM-" + jeton, COMMERCIAL);
        plans.habiliter(compteCommercial, trancheA.getCfgProject());
        commercial = FixtureDossierVente.connecte(compteCommercial, COMMERCIAL);

        AdmUser compteDirection = vente.compte("ACC-DIR-" + jeton, DIRECTION);
        plans.habiliter(compteDirection, trancheA.getCfgProject());
        direction = FixtureDossierVente.connecte(compteDirection, DIRECTION);

        CfgTranche trancheB = plans.tranche("ACC-B-" + jeton, true);
        StkItem articleB = articleTrace(trancheB);
        CfgTranche trancheI = plans.tranche("ACC-I-" + jeton, false);
        StkItem articleI = articleTrace(trancheI);

        // Plans en dernier : parents et dossiers d'abord.
        dansPerimetre = new Lieu(trancheA, plan(trancheA), articleA);
        horsPerimetre = new Lieu(trancheB, plan(trancheB), articleB);
        inactif = new Lieu(trancheI, plan(trancheI), articleI);
    }

    private StkItem articleTrace(CfgTranche tranche) {
        StkItem article = plans.article(tranche, "ART-" + tranche.getCode(), false, etage);
        plans.trace(article.getId(), etage.getId(), D);
        return article;
    }

    private UUID plan(CfgTranche tranche) {
        return plans.plan(tranche.getId(), etage.getId(), "Etage", FixturePlans.dataUriPng());
    }

    /** Les quatre routes de lecture, par nom. */
    private static String url(String route, Lieu lieu) {
        return switch (route) {
            case "tranche" -> "/api/tranches/" + lieu.tranche().getId() + "/plans";
            case "image" -> "/api/plans/" + lieu.plan() + "/image";
            case "traces" -> "/api/plans/" + lieu.plan() + "/traces";
            case "article" -> "/api/articles/" + lieu.article().getId() + "/plans";
            default -> throw new IllegalArgumentException(route);
        };
    }

    /** Identifiants jamais écrits en base. */
    private static String urlInconnue(String route) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        return url(route, new Lieu(tranche, UUID.randomUUID(), article));
    }

    private MockMultipartHttpServletRequestBuilder importer(CfgTranche tranche, RequestPostProcessor qui) {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                <image width="100" height="100" x="0" y="0" xlink:href="%s"/>
                <path id="%s" d="%s"/>
                </svg>""".formatted(FixturePlans.dataUriPng(), "ART-" + tranche.getCode(), D);
        MockMultipartHttpServletRequestBuilder requete = multipart(IMPORT, tranche.getId());
        requete.file(new MockMultipartFile("fichier", "plan.svg", "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8)))
                .param("etageId", etage.getId().toString());
        requete.with(qui);
        return requete;
    }

    // ------------------------------------------------------------------ réponse 1 : périmètre projet

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"tranche", "image", "traces", "article"})
    void reponse1_commercial_horsPerimetre_404(String route) throws Exception {
        mockMvc.perform(get(url(route, horsPerimetre)).with(commercial))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"tranche", "image", "traces", "article"})
    void reponse1_commercial_dansPerimetre_200(String route) throws Exception {
        mockMvc.perform(get(url(route, dansPerimetre)).with(commercial))
                .andExpect(status().isOk());
    }

    @Test
    void reponse1_commercial_dansPerimetre_clientExpose() throws Exception {
        mockMvc.perform(get("/api/plans/{planId}/traces", dansPerimetre.plan()).with(commercial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value(dansPerimetre.article().getId().toString()))
                .andExpect(jsonPath("$[0].client").value(not(emptyOrNullString())));
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"tranche", "image", "traces", "article"})
    void reponse1_roleExempte_directionGenerale_voitHorsDeSesProjets_200(String route) throws Exception {
        mockMvc.perform(get(url(route, horsPerimetre)).with(direction))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ entité inactive, id inconnu

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"tranche", "image", "traces", "article"})
    void visibilite_entiteInactive_404_administrateurCompris(String route) throws Exception {
        mockMvc.perform(get(url(route, inactif)).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"tranche", "image", "traces", "article"})
    void idInconnu_404(String route) throws Exception {
        mockMvc.perform(get(urlInconnue(route)).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void visibilite_purge_entiteInactive_404_plansGardes() throws Exception {
        mockMvc.perform(delete(PLANS_TRANCHE, inactif.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());

        assertThat(lecture.compter(PLANS_DE_LA_TRANCHE, inactif.tranche().getId())).isEqualTo(1);
    }

    @Test
    void visibilite_import_entiteInactive_404() throws Exception {
        mockMvc.perform(importer(inactif.tranche(), connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void purge_trancheInconnue_404() throws Exception {
        mockMvc.perform(delete(PLANS_TRANCHE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §2 écriture Administrateur seulement

    @Test
    void r2_purge_commercial_403_plansGardes() throws Exception {
        mockMvc.perform(delete(PLANS_TRANCHE, dansPerimetre.tranche().getId()).with(commercial))
                .andExpect(status().isForbidden());

        assertThat(lecture.compter(PLANS_DE_LA_TRANCHE, dansPerimetre.tranche().getId())).isEqualTo(1);
    }

    @Test
    void r2_purge_directionGenerale_403() throws Exception {
        mockMvc.perform(delete(PLANS_TRANCHE, dansPerimetre.tranche().getId()).with(direction))
                .andExpect(status().isForbidden());

        assertThat(lecture.compter(TRACES_DE_LA_TRANCHE, dansPerimetre.tranche().getId())).isEqualTo(1);
    }

    @Test
    void r2_import_commercial_403() throws Exception {
        mockMvc.perform(importer(dansPerimetre.tranche(), commercial))
                .andExpect(status().isForbidden());
    }

    @Test
    void r2_import_directionGenerale_403() throws Exception {
        mockMvc.perform(importer(dansPerimetre.tranche(), direction))
                .andExpect(status().isForbidden());
    }
}
