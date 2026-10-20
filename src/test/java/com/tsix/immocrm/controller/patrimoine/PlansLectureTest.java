package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lecture des plans — FEATURE-patrimoine-plans.md §2, « Recalage sur main » (pièges 1-4, ajustements) : méta sans
 * image, image décodée + ETag + 304, tracés joints par étage et tranche, statut et couleur de l'occupant actif,
 * onglet Plan de l'article.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlansLectureTest {

    private static final String PLANS_TRANCHE = "/api/tranches/{id}/plans";
    private static final String IMAGE = "/api/plans/{planId}/image";
    private static final String TRACES = "/api/plans/{planId}/traces";
    private static final String PLANS_ARTICLE = "/api/articles/{id}/plans";
    private static final String D = "m 10.5,20 h 30 v 40 z";
    private static final String CODE_PLAN = "Etage 1";
    private static final String PREMIER = "$[0]";
    private static final String LONGUEUR = "$.length()";
    private static final String ARTICLE_ID = "$[0].articleId";
    private static final String LIBRE = "$[0].libre";
    private static final String COULEUR = "$[0].couleur";
    private static final String DOSSIER_ID = "$[0].dossierId";
    private static final String STATUT_CODE = "$[0].statutCode";
    private static final double EPSILON = 1e-6;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;

    private AdmUser admin;
    private String jeton;
    private CfgTranche tranche;
    private StkFloor etage1;
    private UUID plan;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = plans.tranche("PL-" + jeton, true);
        etage1 = plans.etage("E1-" + jeton);
    }

    /** Plan de l'étage 1, créé au premier appel : après les articles et dossiers du test (parents d'abord). */
    private UUID plan() {
        if (plan == null) {
            plan = plans.plan(tranche.getId(), etage1.getId(), CODE_PLAN, FixturePlans.dataUriPng());
        }
        return plan;
    }

    private StkItem article(String suffixe) {
        return plans.article(tranche, "A" + suffixe + "-" + jeton, false, etage1);
    }

    // ------------------------------------------------------------------ §2 méta sans l'image

    @Test
    void r2_meta_plansDeLaTranche_valeursSansImage() throws Exception {
        UUID attendu = plan();

        String corps = mockMvc.perform(get(PLANS_TRANCHE, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LONGUEUR).value(1))
                .andExpect(jsonPath("$[0].id").value(attendu.toString()))
                .andExpect(jsonPath("$[0].code").value(CODE_PLAN))
                .andExpect(jsonPath("$[0].largeur").value(closeTo(1312.5979, EPSILON), Double.class))
                .andExpect(jsonPath("$[0].hauteur").value(closeTo(928.15833, EPSILON), Double.class))
                .andExpect(jsonPath("$[0].x").value(closeTo(73.34, EPSILON), Double.class))
                .andExpect(jsonPath("$[0].y").value(closeTo(22.7, EPSILON), Double.class))
                .andExpect(jsonPath("$[0].planImage").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(corps).doesNotContain("base64").doesNotContain("data:image");
    }

    @Test
    void r2_meta_planDUneAutreTrancheSurLeMemeEtage_exclu() throws Exception {
        CfgTranche autre = plans.tranche("PL-B-" + jeton, true);
        plans.plan(autre.getId(), etage1.getId(), CODE_PLAN, FixturePlans.dataUriPng());
        UUID attendu = plan();

        mockMvc.perform(get(PLANS_TRANCHE, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LONGUEUR).value(1))
                .andExpect(jsonPath("$[0].id").value(attendu.toString()));
    }

    // ------------------------------------------------------------------ §2 image décodée, ETag, 304

    @Test
    void r2_image_png_binaireDecode_contentTypeEtCache() throws Exception {
        mockMvc.perform(get(IMAGE, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(FixturePlans.png()))
                .andExpect(header().string(HttpHeaders.ETAG, not(emptyOrNullString())))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, not(emptyOrNullString())));
    }

    @Test
    void r2_image_jpeg_contentTypeJpeg() throws Exception {
        UUID planJpeg = plans.plan(tranche.getId(), plans.etage("E2-" + jeton).getId(), "Etage 2",
                FixturePlans.PREFIXE_JPEG + Base64.getEncoder().encodeToString(FixturePlans.jpeg()));

        mockMvc.perform(get(IMAGE, planJpeg).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(FixturePlans.jpeg()));
    }

    @Test
    void r2_image_ifNoneMatchEgalAEtag_304() throws Exception {
        String etag = etag();

        mockMvc.perform(get(IMAGE, plan()).with(connecte(admin)).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void r2_image_etagChange_quandLImageChange() throws Exception {
        String ancien = etag();
        plans.remplacerImage(plan(), FixturePlans.PREFIXE_JPEG + Base64.getEncoder().encodeToString(FixturePlans.jpeg()),
                LocalDateTime.of(2026, 2, 1, 9, 30));

        MockHttpServletResponse reponse = mockMvc.perform(get(IMAGE, plan()).with(connecte(admin))
                        .header(HttpHeaders.IF_NONE_MATCH, ancien))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FixturePlans.jpeg()))
                .andReturn().getResponse();

        assertThat(reponse.getHeader(HttpHeaders.ETAG)).isNotBlank().isNotEqualTo(ancien);
    }

    private String etag() throws Exception {
        String etag = mockMvc.perform(get(IMAGE, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etag).isNotBlank();
        return etag;
    }

    // ------------------------------------------------------------------ §2 tracés : occupant actif, libre

    @Test
    void r2_traces_articleOccupe_statutCouleurEtDossierDuDossierActif() throws Exception {
        StkItem article = article("1");
        ComFolder dossier = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        plans.trace(article.getId(), etage1.getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LONGUEUR).value(1))
                .andExpect(jsonPath(ARTICLE_ID).value(article.getId().toString()))
                .andExpect(jsonPath("$[0].code").value(article.getCode()))
                .andExpect(jsonPath("$[0].d").value(D))
                .andExpect(jsonPath(STATUT_CODE).value(StatutDossier.RESERVATION.getCode()))
                .andExpect(jsonPath(COULEUR).value("Magenta"))
                .andExpect(jsonPath(DOSSIER_ID).value(dossier.getId().toString()))
                .andExpect(jsonPath(LIBRE).value(false))
                .andExpect(jsonPath("$[0].estSousArticle").value(false))
                .andExpect(jsonPath("$[0].client").value(not(emptyOrNullString())));
    }

    @Test
    void r2_traces_articleSansDossier_libre() throws Exception {
        StkItem article = article("2");
        plans.trace(article.getId(), etage1.getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ARTICLE_ID).value(article.getId().toString()))
                .andExpect(jsonPath(LIBRE).value(true))
                .andExpect(jsonPath(DOSSIER_ID).value(nullValue()))
                .andExpect(jsonPath(STATUT_CODE).value(nullValue()))
                .andExpect(jsonPath(COULEUR).value(nullValue()));
    }

    @Test
    void piege4_deuxDossiersDontUnAnnule_couleurDuDossierActif() throws Exception {
        StkItem article = article("3");
        plans.dossier(article, StatutDossier.ANNULATION, "Black", "Annulation");
        ComFolder actif = plans.dossier(article, StatutDossier.CONCRETISATION, "Gold", "Concrétisation");
        plans.trace(article.getId(), etage1.getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DOSSIER_ID).value(actif.getId().toString()))
                .andExpect(jsonPath(STATUT_CODE).value(StatutDossier.CONCRETISATION.getCode()))
                .andExpect(jsonPath(COULEUR).value("Gold"))
                .andExpect(jsonPath(LIBRE).value(false));
    }

    @Test
    void piege4_comFolderIdRenseigneSansDossierActif_libre() throws Exception {
        StkItem article = article("4");
        ComFolder annule = plans.dossier(article, StatutDossier.ANNULATION, "Black", "Annulation");
        plans.comFolderId(article.getId(), annule.getId());
        plans.trace(article.getId(), etage1.getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIBRE).value(true))
                .andExpect(jsonPath(DOSSIER_ID).value(nullValue()))
                .andExpect(jsonPath(COULEUR).value(nullValue()));
    }

    @Test
    void couleur_statutSansColorName_couleurNulle() throws Exception {
        StkItem article = article("5");
        plans.dossier(article, StatutDossier.RESERVATION, null, "Réservation");
        plans.trace(article.getId(), etage1.getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIBRE).value(false))
                .andExpect(jsonPath(STATUT_CODE).value(StatutDossier.RESERVATION.getCode()))
                .andExpect(jsonPath(COULEUR).value(nullValue()));
    }

    @Test
    void piege3_sousArticle_estSousArticleVrai() throws Exception {
        StkItem sous = plans.article(tranche, "SA-" + jeton, true, etage1);
        plans.trace(sous.getId(), etage1.getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ARTICLE_ID).value(sous.getId().toString()))
                .andExpect(jsonPath("$[0].estSousArticle").value(true));
    }

    // ------------------------------------------------------------------ piège 1 : étage ET tranche

    @Test
    void piege1_traceDuMemeEtageDUneAutreTranche_exclu() throws Exception {
        StkItem mien = article("6");
        plans.trace(mien.getId(), etage1.getId(), D);
        CfgTranche autre = plans.tranche("PL-C-" + jeton, true);
        StkItem etranger = plans.article(autre, "AX-" + jeton, false, etage1);
        plans.trace(etranger.getId(), etage1.getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LONGUEUR).value(1))
                .andExpect(jsonPath(ARTICLE_ID).value(mien.getId().toString()));
    }

    @Test
    void piege1_traceDUnAutreEtageDeLaTranche_exclu() throws Exception {
        StkItem mien = article("7");
        plans.trace(mien.getId(), etage1.getId(), D);
        StkItem ailleurs = article("8");
        plans.trace(ailleurs.getId(), plans.etage("E3-" + jeton).getId(), D);

        mockMvc.perform(get(TRACES, plan()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LONGUEUR).value(1))
                .andExpect(jsonPath(ARTICLE_ID).value(mien.getId().toString()));
    }

    // ------------------------------------------------------------------ onglet Plan de l'article (piège 2)

    @Test
    void ongletArticle_planDeLEtageDuTrace_pasDeLEtageDeLArticle() throws Exception {
        StkFloor etage2 = plans.etage("E4-" + jeton);
        StkItem article = plans.article(tranche, "A9-" + jeton, false, etage2);
        plans.trace(article.getId(), etage1.getId(), D);
        plans.plan(tranche.getId(), etage2.getId(), "Etage 4", FixturePlans.dataUriPng());
        UUID attendu = plan();

        mockMvc.perform(get(PLANS_ARTICLE, article.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LONGUEUR).value(1))
                .andExpect(jsonPath(PREMIER + ".planId").value(attendu.toString()))
                .andExpect(jsonPath(PREMIER + ".trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath(PREMIER + ".etageCode").value(etage1.getCode()))
                .andExpect(jsonPath(PREMIER + ".largeur").value(closeTo(1312.5979, EPSILON), Double.class))
                .andExpect(jsonPath(PREMIER + ".hauteur").value(closeTo(928.15833, EPSILON), Double.class))
                .andExpect(jsonPath(PREMIER + ".x").value(closeTo(73.34, EPSILON), Double.class))
                .andExpect(jsonPath(PREMIER + ".y").value(closeTo(22.7, EPSILON), Double.class));
    }

    @Test
    void ongletArticle_articleNonTrace_listeVide() throws Exception {
        StkItem article = article("10");
        plan();

        mockMvc.perform(get(PLANS_ARTICLE, article.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LONGUEUR).value(0));
    }
}
