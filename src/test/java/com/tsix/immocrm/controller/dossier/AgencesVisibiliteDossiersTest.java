package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets.LigneHistorique;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.LectureExport;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité des dossiers et règlements pour un compte agence — FEATURE-agences-vente.md §4 V3, R-V4, R-V8, §8.
 * Projet P affecté à l'agence A : un dossier de A (vendeur vA), un de B (vB), un du siège (vS, sans agence).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesVisibiliteDossiersTest {

    private static final String AGENCE = FixtureAgences.ROLE_AGENCE;
    private static final String DOSSIERS = "/api/dossiers";
    private static final String DOSSIER = "/api/dossiers/{id}";
    private static final String ENCAISSEMENTS = "/api/encaissements";
    private static final String LOT_PDF = "/api/dossiers/documents/fiche-reservation";
    private static final String FICHE_PDF = "/documents/fiche-reservation";
    private static final String SEARCH = "search";
    private static final String TOTAL = "$.totalElements";
    private static final String IDS = "$.content[*].id";
    private static final String ANNULE = "Annulé";
    private static final String SIZE = "size";
    private static final String TAILLE = "200";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private ComDocumentRepository documents;
    @Autowired private StlSettlementRepository reglements;
    @Autowired private StlSettlementLineRepository lignesReglement;

    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche trancheP;
    private UUID agenceA;
    private UUID vendeurA;
    private UUID vendeurB;
    private UUID vendeurSiege;
    private Dossier dossierA;
    private Dossier dossierB;
    private Dossier dossierSiege;
    private RequestPostProcessor compteA;

    /** Id et code gardés : {@code commercialDuDossier} vide le contexte de persistance. */
    private record Dossier(UUID id, String code) {
    }

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        siege = connecte(patrimoine.administrateur());
        trancheP = plans.tranche("AGP-" + jeton, true);

        agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        vendeurA = agences.vendeur("VA-" + jeton);
        vendeurB = agences.vendeur("VB-" + jeton);
        vendeurSiege = agences.vendeur("VS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        dossierA = dossierVif(article(trancheP, "LA"), vendeurA);
        dossierB = dossierVif(article(trancheP, "LB"), vendeurB);
        dossierSiege = dossierVif(article(trancheP, "LS"), vendeurSiege);

        compteA = compteAgence("CA", vendeurA, true);
    }

    // ------------------------------------------------------------------ fixtures

    private StkItem article(CfgTranche tranche, String prefixe) {
        return plans.article(tranche, prefixe + "-" + jeton, false, null);
    }

    /** Dossier en réservation porté par l'article ({@code StkItem.ComFolderId}). */
    private Dossier dossierVif(StkItem article, UUID vendeur) {
        Dossier dossier = dossier(article, StatutDossier.RESERVATION, "Réservation", vendeur);
        plans.comFolderId(article.getId(), dossier.id());
        return dossier;
    }

    /** Dossier sans {@code StkItem.ComFolderId} : pour les annulés, à créer avant tout dossier vif (trigger). */
    private Dossier dossier(StkItem article, StatutDossier statut, String libelle, UUID vendeur) {
        ComFolder cree = plans.dossier(article, statut, "Magenta", libelle);
        Dossier dossier = new Dossier(cree.getId(), cree.getCode());
        agences.commercialDuDossier(dossier.id(), vendeur);
        return dossier;
    }

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

    private UUID echeance(UUID dossierId) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(em.getReference(ComFolder.class, dossierId));
        echeance.setAmount(new BigDecimal("1000"));
        return documents.saveAndFlush(echeance).getId();
    }

    /** Encaissement ({@code IsCredit} = 1, filtre de la liste) avec une ligne par échéance fournie. */
    private UUID reglement(String prefixe, UUID... echeances) {
        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode(prefixe + "-" + jeton);
        reglement.setDocDate(LocalDateTime.of(2026, 3, 2, 10, 0));
        reglement.setAmount(new BigDecimal("1000").multiply(BigDecimal.valueOf(echeances.length)));
        reglement.setStatus("IN_WORK");
        reglement.setIsCredit(true);
        reglements.save(reglement);
        for (UUID echeance : echeances) {
            StlSettlementLine ligne = new StlSettlementLine();
            ligne.setId(UUID.randomUUID());
            ligne.setStlSettlement(reglement);
            ligne.setComDocument(em.getReference(ComDocument.class, echeance));
            ligne.setAmount(new BigDecimal("1000"));
            lignesReglement.save(ligne);
        }
        lignesReglement.flush();
        return reglement.getId();
    }

    private ComProspection prospection(String prefixe, StkItem article, UUID vendeur) {
        CfgTier prospect = vente.tier("PR" + prefixe + "-" + jeton);
        ComProspection p = onglets.prospection("PRO" + prefixe + "-" + jeton, article, trancheP, prospect,
                FixtureArticleOnglets.PROSPECTION_VENTE);
        if (vendeur != null) {
            p.setCfgCommercial(em.find(CfgTier.class, vendeur));
            em.flush();
        }
        return p;
    }

    // ------------------------------------------------------------------ lecture

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> ids(MockHttpServletRequestBuilder requete) throws Exception {
        List<String> ids = JsonPath.read(corps(requete), IDS);
        return ids.stream().map(id -> id.toLowerCase(Locale.ROOT)).toList();
    }

    private MockHttpServletRequestBuilder liste(RequestPostProcessor qui) {
        return get(DOSSIERS).param(SEARCH, jeton).param(SIZE, TAILLE).with(qui);
    }

    private long nombreDansLesGroupes(RequestPostProcessor qui) throws Exception {
        List<Number> nombres = JsonPath.read(corps(get(DOSSIERS + "/groupes").param("groupBy", "statut")
                .param(SEARCH, jeton).with(qui)), "$.content[*].nombre");
        return nombres.stream().mapToLong(Number::longValue).sum();
    }

    private List<String> codesExport(RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse reponse = LectureExport.executer(mockMvc, get(DOSSIERS + "/export").param(SEARCH, jeton)
                .with(qui)).getResponse();
        assertThat(reponse.getStatus()).isEqualTo(200);
        return LectureExport.codesDesLignes(LectureExport.lignesXlsx(reponse.getContentAsByteArray()),
                List.of(dossierA.code(), dossierB.code(), dossierSiege.code()));
    }

    private String id(UUID id) {
        return id.toString().toLowerCase(Locale.ROOT);
    }

    private MockHttpServletRequestBuilder lot(RequestPostProcessor qui, UUID... dossierIds) {
        String liste = String.join(",", Arrays.stream(dossierIds).map(d -> "\"" + d + "\"").toList());
        return post(LOT_PDF).with(qui).contentType(MediaType.APPLICATION_JSON).content("{\"dossierIds\":[" + liste + "]}");
    }

    // ------------------------------------------------------------------ V3 : liste, groupes, totaux, export

    @Test
    void v3_liste_agenceA_dossierDeSonAgence_sansCeuxDeBNiDuSiege() throws Exception {
        assertThat(ids(liste(compteA)))
                .contains(id(dossierA.id()))
                .doesNotContain(id(dossierB.id()), id(dossierSiege.id()));
    }

    @Test
    void v3_liste_temoinSiege_voitLesTroisDossiers() throws Exception {
        assertThat(ids(liste(siege))).contains(id(dossierA.id()), id(dossierB.id()), id(dossierSiege.id()));
    }

    @Test
    void v3_groupes_agenceA_unSeulDossier_siegeTrois() throws Exception {
        assertThat(nombreDansLesGroupes(compteA)).isEqualTo(1);
        assertThat(nombreDansLesGroupes(siege)).isEqualTo(3);
    }

    @Test
    void v3_totaux_agenceA_unSeulDossier() throws Exception {
        mockMvc.perform(get(DOSSIERS + "/totaux").param(SEARCH, jeton).with(compteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(1));
    }

    @Test
    void v3_totaux_temoinSiege_troisDossiers() throws Exception {
        mockMvc.perform(get(DOSSIERS + "/totaux").param(SEARCH, jeton).with(siege))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(3));
    }

    @Test
    void v3_export_agenceA_sansDossiersDeBNiDuSiege() throws Exception {
        assertThat(codesExport(compteA))
                .contains(dossierA.code())
                .doesNotContain(dossierB.code(), dossierSiege.code());
    }

    @Test
    void v3_export_temoinSiege_contientLesTroisDossiers() throws Exception {
        assertThat(codesExport(siege)).contains(dossierA.code(), dossierB.code(), dossierSiege.code());
    }

    // ------------------------------------------------------------------ V3 / §8 : fiche et sous-routes → 404

    @ParameterizedTest(name = "GET dossier{0}")
    @ValueSource(strings = {"", "/echeancier", "/historique", "/pieces-jointes", "/montants", "/payeurs", FICHE_PDF})
    void v3_ficheEtSousRoutes_agenceA_dossierDeBEtDuSiege_404(String sousRoute) throws Exception {
        mockMvc.perform(get(DOSSIER + sousRoute, dossierB.id()).with(compteA)).andExpect(status().isNotFound());
        mockMvc.perform(get(DOSSIER + sousRoute, dossierSiege.id()).with(compteA)).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "GET dossier{0}")
    @ValueSource(strings = {"", "/echeancier", "/historique", "/pieces-jointes", "/montants", "/payeurs"})
    void v3_ficheEtSousRoutes_temoinSiege_dossierDeB_200(String sousRoute) throws Exception {
        mockMvc.perform(get(DOSSIER + sousRoute, dossierB.id()).with(siege)).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "GET dossier{0}")
    @ValueSource(strings = {"", "/echeancier", "/historique"})
    void v3_ficheEtSousRoutes_agenceA_dossierDeSonAgence_200(String sousRoute) throws Exception {
        mockMvc.perform(get(DOSSIER + sousRoute, dossierA.id()).with(compteA)).andExpect(status().isOk());
    }

    /** Témoin du PDF : le siège n'a pas de 404 (le rendu sur données de fixture n'est pas l'objet du test). */
    @Test
    void v3_fichePdfUnitaire_temoinSiege_pas404() throws Exception {
        int statut = mockMvc.perform(get(DOSSIER + FICHE_PDF, dossierB.id()).with(siege)).andReturn().getResponse()
                .getStatus();
        assertThat(statut).isNotEqualTo(404);
    }

    @Test
    void v3_documentsEnLot_agenceA_dossierDeB_404() throws Exception {
        mockMvc.perform(lot(compteA, dossierB.id())).andExpect(status().isNotFound());
    }

    @Test
    void v3_documentsEnLot_agenceA_dossierDuSiegeMeleAuSien_404() throws Exception {
        mockMvc.perform(lot(compteA, dossierA.id(), dossierSiege.id())).andExpect(status().isNotFound());
    }

    @Test
    void v3_documentsEnLot_temoinSiege_dossierDeB_pas404() throws Exception {
        int statut = mockMvc.perform(lot(siege, dossierB.id())).andReturn().getResponse().getStatus();
        assertThat(statut).isNotEqualTo(404);
    }

    // ------------------------------------------------------------------ V3 / R-V8 : règlements

    @Test
    void rV8_reglementsDeLEcheance_agenceA_echeanceDeSonDossier_visible() throws Exception {
        UUID echeanceA = echeance(dossierA.id());
        UUID reglementA = reglement("RGA", echeanceA);

        assertThat(corps(get(DOSSIER + "/echeancier/{echeanceId}/reglements", dossierA.id(), echeanceA).with(compteA)))
                .containsIgnoringCase(id(reglementA));
    }

    @Test
    void rV8_reglementsDeLEcheance_agenceA_echeanceDuDossierDeB_404() throws Exception {
        UUID echeanceB = echeance(dossierB.id());
        reglement("RGB", echeanceB);

        mockMvc.perform(get(DOSSIER + "/echeancier/{echeanceId}/reglements", dossierB.id(), echeanceB).with(compteA))
                .andExpect(status().isNotFound());
    }

    @Test
    void rV8_reglementsDeLEcheance_temoinSiege_echeanceDuDossierDeB_visible() throws Exception {
        UUID echeanceB = echeance(dossierB.id());
        UUID reglementB = reglement("RGB", echeanceB);

        assertThat(corps(get(DOSSIER + "/echeancier/{echeanceId}/reglements", dossierB.id(), echeanceB).with(siege)))
                .containsIgnoringCase(id(reglementB));
    }

    /** Les routes encaissements peuvent être fermées en A1d (V9) : 403 accepté, sinon le contenu est vérifié. */
    @Test
    void rV8_encaissements_agenceA_reglementDeBAbsent_reglementsDeAEtMixteVisibles() throws Exception {
        UUID echeanceA = echeance(dossierA.id());
        UUID echeanceB = echeance(dossierB.id());
        UUID reglementA = reglement("RGA", echeanceA);
        UUID reglementB = reglement("RGB", echeanceB);
        UUID reglementMixte = reglement("RGM", echeanceA, echeanceB);

        MockHttpServletResponse reponse = mockMvc.perform(get(ENCAISSEMENTS).param("reference", jeton)
                .param(SIZE, TAILLE).with(compteA)).andReturn().getResponse();

        assertThat(reponse.getStatus()).isIn(200, 403);
        if (reponse.getStatus() == 200) {
            List<String> ids = JsonPath.<List<String>>read(reponse.getContentAsString(StandardCharsets.UTF_8), IDS)
                    .stream().map(i -> i.toLowerCase(Locale.ROOT)).toList();
            assertThat(ids).contains(id(reglementA), id(reglementMixte)).doesNotContain(id(reglementB));
        }
    }

    @Test
    void rV8_encaissement_agenceA_reglementDeB_404ou403() throws Exception {
        UUID reglementB = reglement("RGB", echeance(dossierB.id()));

        int statut = mockMvc.perform(get(ENCAISSEMENTS + "/{id}", reglementB).with(compteA)).andReturn().getResponse()
                .getStatus();
        assertThat(statut).isIn(403, 404);
    }

    @Test
    void rV8_encaissements_temoinSiege_voitLeReglementDeB() throws Exception {
        UUID reglementB = reglement("RGB", echeance(dossierB.id()));

        assertThat(ids(get(ENCAISSEMENTS).param("reference", jeton).param(SIZE, TAILLE).with(siege)))
                .contains(id(reglementB));
        mockMvc.perform(get(ENCAISSEMENTS + "/{id}", reglementB).with(siege)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ R-V4 : dossier sans projet

    @Test
    void rV4_dossierDeSonVendeurSansProjet_agenceA_404() throws Exception {
        Dossier sansProjet = dossierVif(article(agences.trancheSansProjet("AGX-" + jeton), "LX"), vendeurA);

        mockMvc.perform(get(DOSSIER, sansProjet.id()).with(compteA)).andExpect(status().isNotFound());
        assertThat(ids(liste(compteA))).doesNotContain(id(sansProjet.id()));
    }

    @Test
    void rV4_temoinSiege_dossierSansProjet_200() throws Exception {
        Dossier sansProjet = dossierVif(article(agences.trancheSansProjet("AGY-" + jeton), "LY"), vendeurA);

        mockMvc.perform(get(DOSSIER, sansProjet.id()).with(siege)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ R-V8 : onglets de la fiche lot

    @Test
    void rV8_ongletDossiersDuLot_agenceA_annuleDeBInvisible() throws Exception {
        StkItem lotLibre = article(trancheP, "LT");
        Dossier annuleA = dossier(lotLibre, StatutDossier.ANNULATION, ANNULE,vendeurA);
        Dossier annuleB = dossier(lotLibre, StatutDossier.ANNULATION, ANNULE,vendeurB);

        String agence = corps(get("/api/articles/{id}/dossiers", lotLibre.getId()).with(compteA));
        String temoin = corps(get("/api/articles/{id}/dossiers", lotLibre.getId()).with(siege));

        assertThat(agence).containsIgnoringCase(id(annuleA.id())).doesNotContainIgnoringCase(id(annuleB.id()));
        assertThat(temoin).containsIgnoringCase(id(annuleA.id())).containsIgnoringCase(id(annuleB.id()));
    }

    @Test
    void rV8_ongletHistoriqueDuLot_agenceA_ligneDuDossierDeBInvisible() throws Exception {
        StkItem lotLibre = article(trancheP, "LH");
        Dossier annuleA = dossier(lotLibre, StatutDossier.ANNULATION, ANNULE,vendeurA);
        Dossier annuleB = dossier(lotLibre, StatutDossier.ANNULATION, ANNULE,vendeurB);
        String ligneA = "HIST-A-" + jeton;
        String ligneB = "HIST-B-" + jeton;
        onglets.historique(LigneHistorique.de(LocalDateTime.of(2026, 1, 10, 9, 0), ligneA, lotLibre.getId())
                .dossier(annuleA.id()));
        onglets.historique(LigneHistorique.de(LocalDateTime.of(2026, 1, 11, 9, 0), ligneB, lotLibre.getId())
                .dossier(annuleB.id()));

        String agence = corps(get("/api/articles/{id}/historique", lotLibre.getId()).with(compteA));
        String temoin = corps(get("/api/articles/{id}/historique", lotLibre.getId()).with(siege));

        assertThat(agence).contains(ligneA).doesNotContain(ligneB);
        assertThat(temoin).contains(ligneA, ligneB);
    }

    @Test
    void rV8_ongletProspectionsDuLot_agenceA_prospectionsDeBEtSansCommercialInvisibles() throws Exception {
        StkItem lotLibre = article(trancheP, "LP");
        ComProspection deA = prospection("A", lotLibre, vendeurA);
        ComProspection deB = prospection("B", lotLibre, vendeurB);
        ComProspection sansCommercial = prospection("N", lotLibre, null);

        String agence = corps(get("/api/articles/{id}/prospections", lotLibre.getId()).with(compteA));
        String temoin = corps(get("/api/articles/{id}/prospections", lotLibre.getId()).with(siege));

        assertThat(agence).containsIgnoringCase(id(deA.getId()))
                .doesNotContainIgnoringCase(id(deB.getId()))
                .doesNotContainIgnoringCase(id(sansCommercial.getId()));
        assertThat(temoin).containsIgnoringCase(id(deB.getId())).containsIgnoringCase(id(sansCommercial.getId()));
    }

    // ------------------------------------------------------------------ §8 : comptes incomplets, Agence + Administrateur

    @Test
    void s8_compteAgenceSansProjet_listeDesDossiersVide() throws Exception {
        UUID vendeur = agences.vendeur("VNP-" + jeton);
        agences.lier(agenceA, vendeur);
        RequestPostProcessor sansProjet = compteAgence("CNP", vendeur, false);

        mockMvc.perform(liste(sansProjet)).andExpect(status().isOk()).andExpect(jsonPath(TOTAL).value(0));
    }

    @Test
    void s8_compteAgenceSansVendeur_listeDesDossiersVide() throws Exception {
        RequestPostProcessor sansVendeur = compteAgence("CSV", null, true);

        mockMvc.perform(liste(sansVendeur)).andExpect(status().isOk()).andExpect(jsonPath(TOTAL).value(0));
    }

    @Test
    void s8_agenceEtAdministrateur_traiteCommeAgence_neVoitPasLeDossierDeB() throws Exception {
        UUID vendeur = agences.vendeur("VAD-" + jeton);
        agences.lier(agenceA, vendeur);
        RequestPostProcessor agenceAdmin = compteAgence("CAD", vendeur, true, "Administrateur");

        assertThat(ids(liste(agenceAdmin)))
                .contains(id(dossierA.id()))
                .doesNotContain(id(dossierB.id()), id(dossierSiege.id()));
        mockMvc.perform(get(DOSSIER, dossierB.id()).with(agenceAdmin)).andExpect(status().isNotFound());
    }
}
