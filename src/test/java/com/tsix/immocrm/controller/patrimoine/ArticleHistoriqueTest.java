package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets.LigneHistorique;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Historique de l'article — FEATURE-patrimoine-article-onglets.md §3, §7, §8.3, §8.4 :
 * {@code GET /api/articles/{id}/historique} = {@code StrFolderStatusHistory} de l'article ({@code StkItemId}) ou d'un
 * transfert produit légalisé dont l'article est l'ancien ou le nouveau ; {@code origine} VENTE / LOCATION / SYNDIC ;
 * tri par date décroissante ; auteur {@code AdmUserId}, « Système » pour une ligne de location / syndic sans compte ;
 * nom du client ; périmètre par le dossier. Lignes repérées par leur description (unique par test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleHistoriqueTest {

    private static final String ROUTE = "/api/articles/{id}/historique";
    private static final String COMMERCIAL = "Commercial";
    private static final LocalDateTime JANVIER = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final LocalDateTime MARS = LocalDateTime.of(2026, 3, 10, 9, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente ventes;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;

    private String jeton;
    private AdmUser admin;
    private CfgTranche tranche;
    private StkItem article;

    @BeforeEach
    void setUp() {
        jeton = "P2BHI" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        patrimoine.referentielArticle();
        tranche = ventes.tranche("TR-" + jeton, true);
        article = onglets.article("ART-" + jeton, tranche);
    }

    private String onglet(UUID articleId, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(ROUTE, articleId).with(qui))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<String> origines(String corps) {
        return JsonPath.read(corps, "$..origine");
    }

    private String libelle(String suffixe) {
        return "HIST-" + suffixe + "-" + jeton;
    }

    // ------------------------------------------------------------------ §3 lignes de l'article et origine

    @Test
    void r3_ligneDeStatutDUnDossierDeVente_presente_origineVente() throws Exception {
        ComFolder dossier = ventes.dossier("DOS-" + jeton, tranche, StatutDossier.RESERVATION);
        onglets.historique(LigneHistorique.de(MARS, libelle("VENTE"), article.getId()).dossier(dossier.getId()));

        String corps = onglet(article.getId(), connecte(admin));

        assertAll(
                () -> assertThat(corps).contains(libelle("VENTE"), dossier.getCode()),
                () -> assertThat(origines(corps)).containsExactly("VENTE"));
    }

    @Test
    void r3_ligneDUnAutreArticle_absente() throws Exception {
        StkItem autre = onglets.article("ART2-" + jeton, tranche);
        onglets.historique(LigneHistorique.de(MARS, libelle("MIENNE"), article.getId()));
        onglets.historique(LigneHistorique.de(MARS, libelle("AUTRE"), autre.getId()));

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).contains(libelle("MIENNE")).doesNotContain(libelle("AUTRE"));
    }

    /** §8.3 : {@code RntFolderId} non nul sans dossier de vente ⇒ LOCATION ; sans compte ⇒ « Système ». */
    @Test
    void r3_ligneDeLocation_origineLocation_auteurSysteme() throws Exception {
        UUID location = onglets.location("LOC-" + jeton, article.getId(),
                ventes.statut(StatutDossier.RESERVATION).getId(), ventes.tier("LOC-" + jeton).getId());
        onglets.historique(LigneHistorique.de(MARS, libelle("LOC"), article.getId()).location(location));

        String corps = onglet(article.getId(), connecte(admin));

        assertAll(
                () -> assertThat(corps).contains(libelle("LOC"), "Système"),
                () -> assertThat(origines(corps)).containsExactly("LOCATION"));
    }

    @Test
    void r3_ligneDeSyndic_origineSyndic() throws Exception {
        UUID syndic = onglets.syndicSansArticle("SYN-" + jeton);
        onglets.historique(LigneHistorique.de(MARS, libelle("SYN"), article.getId()).syndic(syndic));

        String corps = onglet(article.getId(), connecte(admin));

        assertAll(
                () -> assertThat(corps).contains(libelle("SYN")),
                () -> assertThat(origines(corps)).containsExactly("SYNDIC"));
    }

    // ------------------------------------------------------------------ §8.3 transfert produit

    /** Ligne portée par le nouvel article d'un transfert légalisé : visible aussi sur l'ancien article. */
    @Test
    void r3_transfertProduitLegalise_ligneVisibleSurLAncienArticle() throws Exception {
        StkItem nouvel = onglets.article("ARTN-" + jeton, tranche);
        UUID transfert = onglets.transfertProduit("TRP-" + jeton, article.getId(), nouvel.getId(), true);
        onglets.historique(LigneHistorique.de(MARS, libelle("TRANSFERT"), nouvel.getId()).transfertProduit(transfert));

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).contains(libelle("TRANSFERT"));
    }

    @Test
    void r3_transfertProduitNonLegalise_ligneAbsenteDeLAncienArticle() throws Exception {
        StkItem nouvel = onglets.article("ARTN-" + jeton, tranche);
        UUID transfert = onglets.transfertProduit("TRP-" + jeton, article.getId(), nouvel.getId(), false);
        onglets.historique(LigneHistorique.de(MARS, libelle("TRANSFERT"), nouvel.getId()).transfertProduit(transfert));
        onglets.historique(LigneHistorique.de(MARS, libelle("MIENNE"), article.getId()));

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).contains(libelle("MIENNE")).doesNotContain(libelle("TRANSFERT"));
    }

    // ------------------------------------------------------------------ §8.4 tri, auteur, client

    /** §8.4 : tri décroissant par {@code DocDate} (cohérence avec l'historique du tiers). */
    @Test
    void r3_triParDateDecroissante() throws Exception {
        onglets.historique(LigneHistorique.de(JANVIER, libelle("ANCIENNE"), article.getId()));
        onglets.historique(LigneHistorique.de(MARS, libelle("RECENTE"), article.getId()));

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps.indexOf(libelle("RECENTE")))
                .isNotNegative()
                .isLessThan(corps.indexOf(libelle("ANCIENNE")));
    }

    @Test
    void r3_auteur_compteAdmUserDeLaLigne() throws Exception {
        ComFolder dossier = ventes.dossier("DOS-" + jeton, tranche, StatutDossier.RESERVATION);
        AdmUser auteur = ventes.compte("AUT-" + jeton);
        onglets.historique(LigneHistorique.de(MARS, libelle("AUTEUR"), article.getId()).dossier(dossier.getId())
                .compte(auteur.getId()));

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).contains(libelle("AUTEUR"), auteur.getDescription());
    }

    @Test
    void r3_client_nomDuTiersDeLaLigne() throws Exception {
        ComFolder dossier = ventes.dossier("DOS-" + jeton, tranche, StatutDossier.RESERVATION);
        CfgTier client = ventes.tier("CLIH-" + jeton);
        onglets.historique(LigneHistorique.de(MARS, libelle("CLIENT"), article.getId()).dossier(dossier.getId())
                .client(client.getId()));

        String corps = onglet(article.getId(), connecte(admin));

        assertThat(corps).contains(libelle("CLIENT"), client.getLastName());
    }

    // ------------------------------------------------------------------ §8.7 périmètre par le dossier

    @Test
    void r3_perimetre_ligneDUnDossierHorsPerimetre_masquee() throws Exception {
        ComFolder dansPerimetre = ventes.dossier("DOSA-" + jeton, tranche, StatutDossier.RESERVATION);
        ComFolder horsPerimetre = ventes.dossier("DOSB-" + jeton, ventes.tranche("TRB-" + jeton, true),
                StatutDossier.RESERVATION);
        onglets.historique(LigneHistorique.de(MARS, libelle("DANS"), article.getId()).dossier(dansPerimetre.getId()));
        onglets.historique(LigneHistorique.de(MARS, libelle("HORS"), article.getId()).dossier(horsPerimetre.getId()));
        AdmUser compte = ventes.compte("COM-" + jeton, COMMERCIAL);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(tranche.getCfgProject());
        habilitationRepository.save(habilitation);

        String corps = onglet(article.getId(), FixtureDossierVente.connecte(compte, COMMERCIAL));

        assertThat(corps).contains(libelle("DANS")).doesNotContain(libelle("HORS"));
    }

    // ------------------------------------------------------------------ R4 / 404

    @Test
    void r3_articleInconnu_404() throws Exception {
        mockMvc.perform(get(ROUTE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r3_r4_articleDUneEntiteInactive_404() throws Exception {
        StkItem masque = onglets.article("ARTI-" + jeton, ventes.tranche("TRI-" + jeton, false));

        mockMvc.perform(get(ROUTE, masque.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
