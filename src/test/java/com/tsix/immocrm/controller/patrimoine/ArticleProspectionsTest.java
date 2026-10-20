package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureArticleOnglets.PRE_PROSPECTION;
import static com.tsix.immocrm.fixtures.FixtureArticleOnglets.PROSPECTION_VENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Prospections de l'article — FEATURE-patrimoine-article-onglets.md §1, §7, §8.4 :
 * {@code GET /api/articles/{id}/prospections} = {@code ComSaleProspectionView WHERE StkItemId = id} recalculé sur
 * {@code ComProspection}, pré-prospections exclues, périmètre commercial de {@code /api/prospections}, R4 de l'article.
 * Les assertions portent sur les identifiants créés ici (forme de page ou de liste non imposée par le spec).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleProspectionsTest {

    private static final String ROUTE = "/api/articles/{id}/prospections";
    private static final String COMMERCIAL = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente ventes;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;

    private String jeton;
    private AdmUser admin;
    private CfgTranche tranche;
    private StkItem article;
    private CfgTier prospect;

    @BeforeEach
    void setUp() {
        jeton = "P2BPR" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        patrimoine.referentielArticle();
        tranche = ventes.tranche("TR-" + jeton, true);
        article = onglets.article("ART-" + jeton, tranche);
        prospect = ventes.tier("PROS-" + jeton);
    }

    private String onglet(UUID articleId, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(ROUTE, articleId).with(qui))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().toLowerCase(Locale.ROOT);
    }

    private static String id(ComProspection p) {
        return p.getId().toString().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ §1 / §7 égalité avec la vue OMRAN

    @Test
    void r1_egaliteAvecLaVueOmran_memesProspectionsQueComSaleProspectionView() throws Exception {
        ComProspection p1 = onglets.prospection("PSN1-" + jeton, article, tranche, prospect, PROSPECTION_VENTE);
        ComProspection p2 = onglets.prospection("PSN2-" + jeton, article, tranche, ventes.tier("PROS2-" + jeton),
                PROSPECTION_VENTE);

        List<String> vue = onglets.idsDeLaVueOmran(article.getId());
        String corps = onglet(article.getId(), connecte(admin));

        assertThat(vue).containsExactlyInAnyOrder(id(p1), id(p2));
        assertThat(corps).contains(vue);
    }

    @Test
    void r1_prospectionDUnAutreArticle_absente() throws Exception {
        StkItem autre = onglets.article("ART2-" + jeton, tranche);
        ComProspection mienne = onglets.prospection("PSN-" + jeton, article, tranche, prospect, PROSPECTION_VENTE);
        ComProspection etrangere = onglets.prospection("PSNX-" + jeton, autre, tranche, prospect, PROSPECTION_VENTE);

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).contains(id(mienne)).doesNotContain(id(etrangere));
    }

    /** §8.4 : pré-prospections exclues, comme la vue ({@code StrEntityId <> ComPreProspection}). */
    @Test
    void r1_preProspection_exclue() throws Exception {
        ComProspection vente = onglets.prospection("PSN-" + jeton, article, tranche, prospect, PROSPECTION_VENTE);
        ComProspection pre = onglets.prospection("PRE-" + jeton, article, tranche, prospect, PRE_PROSPECTION);

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).contains(id(vente)).doesNotContain(id(pre));
    }

    // ------------------------------------------------------------------ §1 périmètre commercial

    @Test
    void r1_perimetre_commercialRestreint_neVoitPasLaProspectionDUnProjetHorsPerimetre() throws Exception {
        CfgTranche autreTranche = ventes.tranche("TRB-" + jeton, true);
        ComProspection dansPerimetre = onglets.prospection("PSNA-" + jeton, article, tranche, prospect,
                PROSPECTION_VENTE);
        ComProspection horsPerimetre = onglets.prospection("PSNB-" + jeton, article, autreTranche, prospect,
                PROSPECTION_VENTE);
        AdmUser compte = ventes.compte("COM-" + jeton, COMMERCIAL);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(tranche.getCfgProject());
        habilitationRepository.save(habilitation);

        String corps = onglet(article.getId(), FixtureDossierVente.connecte(compte, COMMERCIAL));

        assertThat(corps).contains(id(dansPerimetre)).doesNotContain(id(horsPerimetre));
    }

    // ------------------------------------------------------------------ R4 / 404

    @Test
    void r1_articleInconnu_404() throws Exception {
        mockMvc.perform(get(ROUTE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r1_r4_articleDUneEntiteInactive_404() throws Exception {
        StkItem masque = onglets.article("ARTI-" + jeton, ventes.tranche("TRI-" + jeton, false));

        mockMvc.perform(get(ROUTE, masque.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
