package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Dossiers de l'article — FEATURE-patrimoine-article-onglets.md §2, §7, §8.4 : {@code GET /api/articles/{id}/dossiers}
 * = vente ({@code ComFolder.StkItemId} ou {@code ComFolderItem.StkItemId}, sortis inclus, périmètre de la liste des
 * dossiers) ∪ location ({@code RntFolder.StkItemId}) ∪ syndic ({@code SynFolder.StkItemId}), champ {@code origine}
 * VENTE / LOCATION / SYNDIC. Forme de la réponse (page ou liste) non imposée : codes cherchés dans le corps,
 * {@code origine} lue en profondeur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleDossiersTest {

    private static final String ROUTE = "/api/articles/{id}/dossiers";
    private static final String ORIGINES = "$..origine";
    private static final String VENTE = "VENTE";
    private static final String COMMERCIAL = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente ventes;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;

    private String jeton;
    private AdmUser admin;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        jeton = "P2BDO" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        patrimoine.referentielArticle();
        tranche = ventes.tranche("TR-" + jeton, true);
    }

    private String onglet(UUID articleId, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(ROUTE, articleId).with(qui))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<String> origines(String corps) {
        return JsonPath.read(corps, ORIGINES);
    }

    // ------------------------------------------------------------------ §2 branche vente

    @Test
    void r2_venteDirecte_dossierDeLArticle_origineVente() throws Exception {
        ComFolder dossier = ventes.dossier("DOS-" + jeton, tranche, StatutDossier.RESERVATION);

        String corps = onglet(dossier.getStkItem().getId(), connecte(admin));

        assertAll(
                () -> assertThat(corps).contains(dossier.getCode()),
                () -> assertThat(origines(corps)).containsExactly(VENTE));
    }

    /** §8.4 : {@code ComFolderItem} retrouve le dossier d'un sous-article (jamais l'article principal en prod). */
    @Test
    void r2_venteViaComFolderItem_dossierDuSousArticle() throws Exception {
        ComFolder dossier = ventes.dossier("DOS-" + jeton, tranche, StatutDossier.RESERVATION);
        StkItem sousArticle = onglets.article("SA-" + jeton, tranche, patrimoine.natureBat(), dossier.getStkItem());
        onglets.ligneDossier(dossier.getId(), sousArticle.getId());

        String corps = onglet(sousArticle.getId(), connecte(admin));

        assertAll(
                () -> assertThat(corps).contains(dossier.getCode()),
                () -> assertThat(origines(corps)).containsExactly(VENTE));
    }

    /** §8.4 : {@code inclureSortis = true}, OMRAN n'exclut aucun statut. */
    @Test
    void r2_dossierAnnule_inclus() throws Exception {
        ComFolder annule = ventes.dossier("DOSANN-" + jeton, tranche, StatutDossier.ANNULATION);

        String corps = onglet(annule.getStkItem().getId(), connecte(admin));

        assertThat(corps).contains(annule.getCode());
    }

    @Test
    void r2_perimetre_dossierDUnProjetHorsPerimetre_absent() throws Exception {
        CfgTranche autreTranche = ventes.tranche("TRB-" + jeton, true);
        ComFolder dansPerimetre = ventes.dossier("DOSA-" + jeton, tranche, StatutDossier.RESERVATION);
        ComFolder horsPerimetre = ventes.dossier("DOSB-" + jeton, autreTranche, StatutDossier.RESERVATION);
        StkItem sousArticle = onglets.article("SA-" + jeton, tranche, patrimoine.natureBat(),
                dansPerimetre.getStkItem());
        onglets.ligneDossier(dansPerimetre.getId(), sousArticle.getId());
        onglets.ligneDossier(horsPerimetre.getId(), sousArticle.getId());
        AdmUser compte = ventes.compte("COM-" + jeton, COMMERCIAL);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(tranche.getCfgProject());
        habilitationRepository.save(habilitation);

        String corps = onglet(sousArticle.getId(), FixtureDossierVente.connecte(compte, COMMERCIAL));

        assertThat(corps).contains(dansPerimetre.getCode()).doesNotContain(horsPerimetre.getCode());
    }

    // ------------------------------------------------------------------ §2 branches location et syndic

    @Test
    void r2_location_dossierRntFolderDeLArticle_origineLocation() throws Exception {
        StkItem article = onglets.article("ART-" + jeton, tranche);
        String code = "LOC-" + jeton;
        onglets.location(code, article.getId(), ventes.statut(StatutDossier.RESERVATION).getId(),
                ventes.tier("LOC-" + jeton).getId());

        String corps = onglet(article.getId(), connecte(admin));

        assertAll(
                () -> assertThat(corps).contains(code),
                () -> assertThat(origines(corps)).containsExactly("LOCATION"));
    }

    @Test
    void r2_syndic_dossierSynFolderDeLArticle_origineSyndic() throws Exception {
        StkItem article = onglets.article("ART-" + jeton, tranche);
        String code = "SYN-" + jeton;
        onglets.syndic(code, article.getId(), ventes.statut(StatutDossier.RESERVATION).getId(),
                ventes.tier("SYN-" + jeton).getId());

        String corps = onglet(article.getId(), connecte(admin));

        assertAll(
                () -> assertThat(corps).contains(code),
                () -> assertThat(origines(corps)).containsExactly("SYNDIC"));
    }

    @Test
    void r2_dossierDUnAutreArticle_absent() throws Exception {
        StkItem article = onglets.article("ART-" + jeton, tranche);
        ComFolder autre = ventes.dossier("DOSX-" + jeton, tranche, StatutDossier.RESERVATION);

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).doesNotContain(autre.getCode());
    }

    // ------------------------------------------------------------------ R4 / 404

    @Test
    void r2_articleInconnu_404() throws Exception {
        mockMvc.perform(get(ROUTE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r2_r4_articleDUneEntiteInactive_404() throws Exception {
        StkItem masque = onglets.article("ARTI-" + jeton, ventes.tranche("TRI-" + jeton, false));

        mockMvc.perform(get(ROUTE, masque.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
