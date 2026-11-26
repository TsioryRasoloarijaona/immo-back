package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.LectureExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité des lots pour un compte agence — FEATURE-agences-vente.md §4 V1, R-V1, R-V3, R-V4, R-V5, §3 R-INS2 à R-INS4,
 * §8. Projet P affecté à l'agence A : un lot libre, un lot vendu par A, un par B, un par le siège, un sous-article libre et
 * un sous-article porté par le dossier de B ; projet Q non affecté (un lot libre).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesVisibiliteLotsTest {

    private static final String AGENCE = FixtureAgences.ROLE_AGENCE;
    private static final String ARTICLES = "/api/articles";
    private static final String ARTICLE = "/api/articles/{id}";
    private static final String SOUS_ARTICLES = "/api/sous-articles";
    private static final String TRANCHE_ID = "trancheId";
    private static final String TOTAL = "$.totalElements";
    private static final String LIBRE = "libre";
    private static final String LOT_A = "lotA";
    private static final String LOT_B = "lotB";
    private static final String LOT_SIEGE = "lotSiege";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche trancheP;
    private CfgTranche trancheQ;
    private UUID agenceA;
    private UUID lienA;
    private final Map<String, StkItem> lots = new HashMap<>();
    private StkItem libreQ;
    private StkItem sousLibre;
    private StkItem sousB;
    private RequestPostProcessor compteA;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        siege = connecte(patrimoine.administrateur());
        trancheP = plans.tranche("AGP-" + jeton, true);
        trancheQ = plans.tranche("AGQ-" + jeton, true);

        agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        UUID vendeurA = agences.vendeur("VA-" + jeton);
        UUID vendeurB = agences.vendeur("VB-" + jeton);
        UUID vendeurSiege = agences.vendeur("VS-" + jeton);
        lienA = agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        lots.put(LIBRE, article(trancheP, "LIB", false));
        lots.put(LOT_A, vendu(article(trancheP, "LA", false), vendeurA));
        ComFolder dossierB = dossier(article(trancheP, "LB", false), vendeurB);
        lots.put(LOT_B, dossierB.getStkItem());
        lots.put(LOT_SIEGE, vendu(article(trancheP, "LS", false), vendeurSiege));
        libreQ = article(trancheQ, "LQ", false);
        sousLibre = article(trancheP, "SL", true);
        sousB = article(trancheP, "SB", true);
        agences.parent(sousB.getId(), dossierB.getStkItem().getId());
        plans.comFolderId(sousB.getId(), dossierB.getId());
        agences.origineSousArticle(sousLibre.getId());
        agences.origineSousArticle(sousB.getId());

        compteA = compteAgence("CA", vendeurA, true);
    }

    private StkItem article(CfgTranche tranche, String prefixe, boolean sousArticle) {
        return plans.article(tranche, prefixe + "-" + jeton, sousArticle, null);
    }

    private StkItem vendu(StkItem article, UUID vendeur) {
        return dossier(article, vendeur).getStkItem();
    }

    private ComFolder dossier(StkItem article, UUID vendeur) {
        ComFolder dossier = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        agences.commercialDuDossier(dossier.getId(), vendeur);
        plans.comFolderId(article.getId(), dossier.getId());
        return dossier;
    }

    /** Compte {@code Agence} (+ {@code autresRoles}) du vendeur, affecté au projet P si {@code affecte}. */
    private RequestPostProcessor compteAgence(String prefixe, UUID vendeur, boolean affecte, String... autresRoles) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeur, autresRoles);
        if (affecte) {
            plans.habiliter(compte, trancheP.getCfgProject());
        }
        String[] roles = new String[autresRoles.length + 1];
        roles[0] = AGENCE;
        System.arraycopy(autresRoles, 0, roles, 1, autresRoles.length);
        return FixtureDossierVente.connecte(compte, roles);
    }

    private String code(String lot) {
        return lots.get(lot).getCode();
    }

    private List<String> codes(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return LectureExport.codes(corps);
    }

    private List<String> codesExport(String url, RequestPostProcessor qui, List<String> candidats) throws Exception {
        var reponse = LectureExport.executer(mockMvc, get(url).param(TRANCHE_ID, trancheP.getId().toString()).with(qui))
                .getResponse();
        assertThat(reponse.getStatus()).isEqualTo(200);
        return LectureExport.codesDesLignes(LectureExport.lignesXlsx(reponse.getContentAsByteArray()), candidats);
    }

    private MockHttpServletRequestBuilder listeP(RequestPostProcessor qui) {
        return get(ARTICLES).param(TRANCHE_ID, trancheP.getId().toString()).param("size", "200").with(qui);
    }

    private List<String> tousLesCodesDeP() {
        return List.of(code(LIBRE), code(LOT_A), code(LOT_B), code(LOT_SIEGE));
    }

    // ------------------------------------------------------------------ V1 : liste, recherche

    @Test
    void v1_liste_agenceA_libreEtLotDeSonAgence_sansLotsDeBNiDuSiege() throws Exception {
        assertThat(codes(listeP(compteA)))
                .contains(code(LIBRE), code(LOT_A))
                .doesNotContain(code(LOT_B), code(LOT_SIEGE));
    }

    @Test
    void v1_liste_temoinSiege_voitLesLotsVendusEtLeProjetQ() throws Exception {
        List<String> codes = codes(get(ARTICLES).param(TRANCHE_ID, trancheP.getId().toString(), trancheQ.getId().toString())
                .param("size", "200").with(siege));

        assertThat(codes).contains(code(LOT_B), code(LOT_SIEGE), libreQ.getCode());
    }

    @Test
    void v1_liste_agenceA_projetNonAffecte_lotLibreAbsent() throws Exception {
        assertThat(codes(get(ARTICLES).param(TRANCHE_ID, trancheQ.getId().toString()).with(compteA)))
                .doesNotContain(libreQ.getCode());
    }

    @Test
    void v1_recherche_agenceA_neRendQueLibreEtLotDeSonAgence() throws Exception {
        assertThat(codes(get(ARTICLES).param("search", jeton).param("size", "200").with(compteA)))
                .contains(code(LIBRE), code(LOT_A))
                .doesNotContain(code(LOT_B), code(LOT_SIEGE), libreQ.getCode());
    }

    @Test
    void v1_articlesDeLaTranche_agenceA_sansLotsDeBNiDuSiege() throws Exception {
        assertThat(codes(articlesDeTranche(compteA, null))).contains(code(LIBRE));
        assertThat(codes(articlesDeTranche(compteA, "OCCUPE")))
                .contains(code(LOT_A))
                .doesNotContain(code(LOT_B), code(LOT_SIEGE));
    }

    @Test
    void v1_articlesDeLaTranche_temoinSiege_voitLeLotDeB() throws Exception {
        assertThat(codes(articlesDeTranche(siege, "OCCUPE"))).contains(code(LOT_B), code(LOT_SIEGE));
    }

    /** Sans {@code statut} la route ne rend que les lots disponibles ; {@code OCCUPE} rend ceux qui portent un dossier. */
    private MockHttpServletRequestBuilder articlesDeTranche(RequestPostProcessor qui, String statut) {
        MockHttpServletRequestBuilder requete = get("/api/tranches/{id}/articles", trancheP.getId()).param("size", "200");
        return (statut == null ? requete : requete.param("statut", statut)).with(qui);
    }

    // ------------------------------------------------------------------ V1 : fiche (hors périmètre → 404)

    @ParameterizedTest(name = "GET fiche {0}")
    @ValueSource(strings = {LOT_B, LOT_SIEGE})
    void v1_fiche_agenceA_lotNiLibreNiDeSonAgence_404(String lot) throws Exception {
        mockMvc.perform(get(ARTICLE, lots.get(lot).getId()).with(compteA)).andExpect(status().isNotFound());
    }

    @Test
    void v1_fiche_agenceA_lotLibreDuProjetNonAffecte_404() throws Exception {
        mockMvc.perform(get(ARTICLE, libreQ.getId()).with(compteA)).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "GET fiche {0}")
    @ValueSource(strings = {LIBRE, LOT_A})
    void v1_fiche_agenceA_lotLibreOuDeSonAgence_200(String lot) throws Exception {
        mockMvc.perform(get(ARTICLE, lots.get(lot).getId()).with(compteA)).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "GET fiche {0}")
    @ValueSource(strings = {LOT_B, LOT_SIEGE})
    void v1_fiche_temoinSiege_200(String lot) throws Exception {
        mockMvc.perform(get(ARTICLE, lots.get(lot).getId()).with(siege)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ V1 : sous-articles

    @Test
    void v1_sousArticlesDuLotDeB_agenceA_404() throws Exception {
        mockMvc.perform(get(ARTICLE + "/sous-articles", lots.get(LOT_B).getId()).with(compteA))
                .andExpect(status().isNotFound());
    }

    @Test
    void v1_sousArticlesDuLotDeB_temoinSiege_rendLeSousArticle() throws Exception {
        assertThat(codes(get(ARTICLE + "/sous-articles", lots.get(LOT_B).getId()).param("statut", "OCCUPE").with(siege)))
                .contains(sousB.getCode());
    }

    @Test
    void v1_listeSousArticles_agenceA_sousArticleLibreSeulement() throws Exception {
        assertThat(codes(get(SOUS_ARTICLES).param(TRANCHE_ID, trancheP.getId().toString()).param("size", "200")
                .with(compteA)))
                .contains(sousLibre.getCode())
                .doesNotContain(sousB.getCode());
    }

    @Test
    void v1_listeSousArticles_temoinSiege_voitLeSousArticleDeB() throws Exception {
        assertThat(codes(get(SOUS_ARTICLES).param(TRANCHE_ID, trancheP.getId().toString()).param("size", "200")
                .with(siege)))
                .contains(sousLibre.getCode(), sousB.getCode());
    }

    // ------------------------------------------------------------------ V1 : export

    @Test
    void v1_exportArticles_agenceA_sansLotsDeBNiDuSiege() throws Exception {
        assertThat(codesExport(ARTICLES + "/export", compteA, tousLesCodesDeP()))
                .contains(code(LIBRE), code(LOT_A))
                .doesNotContain(code(LOT_B), code(LOT_SIEGE));
    }

    @Test
    void v1_exportArticles_temoinSiege_contientLeLotDeB() throws Exception {
        assertThat(codesExport(ARTICLES + "/export", siege, tousLesCodesDeP())).contains(code(LOT_B), code(LOT_SIEGE));
    }

    @Test
    void v1_exportSousArticles_agenceA_sansLeSousArticleDeB() throws Exception {
        List<String> candidats = List.of(sousLibre.getCode(), sousB.getCode());

        assertThat(codesExport(SOUS_ARTICLES + "/export", compteA, candidats))
                .contains(sousLibre.getCode())
                .doesNotContain(sousB.getCode());
    }

    @Test
    void v1_exportSousArticles_temoinSiege_contientLeSousArticleDeB() throws Exception {
        List<String> candidats = List.of(sousLibre.getCode(), sousB.getCode());

        assertThat(codesExport(SOUS_ARTICLES + "/export", siege, candidats)).contains(sousB.getCode());
    }

    // ------------------------------------------------------------------ R-V1 / R-V5 / R-INS3 : compte incomplet → rien

    @Test
    void rV1_rV5_compteAgenceSansProjet_listeDesArticlesVide() throws Exception {
        UUID vendeur = agences.vendeur("VNP-" + jeton);
        agences.lier(agenceA, vendeur);
        RequestPostProcessor sansProjet = compteAgence("CNP", vendeur, false);

        mockMvc.perform(get(ARTICLES).param("search", jeton).with(sansProjet))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(0));
    }

    @Test
    void rIns3_compteAgenceSansVendeur_listeVide() throws Exception {
        RequestPostProcessor sansVendeur = compteAgence("CSV", null, true);

        mockMvc.perform(listeP(sansVendeur)).andExpect(status().isOk()).andExpect(jsonPath(TOTAL).value(0));
    }

    @Test
    void rIns3_compteAgenceVendeurSansAgence_listeVide() throws Exception {
        RequestPostProcessor vendeurSansAgence = compteAgence("CVSA", agences.vendeur("VSA-" + jeton), true);

        mockMvc.perform(listeP(vendeurSansAgence)).andExpect(status().isOk()).andExpect(jsonPath(TOTAL).value(0));
    }

    // ------------------------------------------------------------------ R-INS2 / R-V3 : Agence l'emporte

    @Test
    void rIns2_rV3_agenceEtAdministrateur_traiteCommeAgence() throws Exception {
        UUID vendeur = agences.vendeur("VAD-" + jeton);
        agences.lier(agenceA, vendeur);
        RequestPostProcessor agenceAdmin = compteAgence("CAD", vendeur, true, "Administrateur");

        assertThat(codes(listeP(agenceAdmin)))
                .contains(code(LIBRE), code(LOT_A))
                .doesNotContain(code(LOT_B), code(LOT_SIEGE));
    }

    // ------------------------------------------------------------------ R-INS4 : lien retiré → accès retiré

    @Test
    void rIns4_lienDuVendeurRetire_auProchainAppel_plusRien() throws Exception {
        assertThat(codes(listeP(compteA))).contains(code(LOT_A));

        mockMvc.perform(delete("/api/administration/tiers/{id}/liens/{lienId}", agenceA, lienA).with(siege))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(listeP(compteA)).andExpect(status().isOk()).andExpect(jsonPath(TOTAL).value(0));
    }

    // ------------------------------------------------------------------ R-V4 : lot sans projet

    @Test
    void rV4_lotDUneTrancheSansProjet_invisiblePourUneAgence_404() throws Exception {
        StkItem sansProjet = article(agences.trancheSansProjet("AGX-" + jeton), "LX", false);

        mockMvc.perform(get(ARTICLE, sansProjet.getId()).with(compteA)).andExpect(status().isNotFound());
    }

    @Test
    void rV4_temoinSiege_lotDUneTrancheSansProjet_200() throws Exception {
        StkItem sansProjet = article(agences.trancheSansProjet("AGY-" + jeton), "LY", false);

        mockMvc.perform(get(ARTICLE, sansProjet.getId()).with(siege)).andExpect(status().isOk());
    }
}
