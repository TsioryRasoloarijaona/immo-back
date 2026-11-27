package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.LectureExport;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vue du siège sur les agences — FEATURE-agences-vente.md §6 « Siège (D11) » R-S1, R-S2, R-S4 et §8 (cas siège).
 * Projet P : un dossier, une prospection et une offre de l'agence A (vendeur vA), de B (vB) et du siège (vS, sans agence).
 * Champs attendus : {@code agence} (libellé de l'agence, « Siège » sinon), {@code agenceId} ; filtre {@code agence} (ids).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesVueSiegeTest {

    private static final UUID ID_PROSPECTION_VENTE = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");
    private static final String SIEGE = "Siège";
    private static final String DOSSIERS = "/api/dossiers";
    private static final String PROSPECTIONS = "/api/prospections";
    private static final String DEVIS = "/api/devis";
    private static final String ETAT_VENTES = "/api/etat-des-ventes";
    private static final String AGENCE = "agence";
    private static final String AGENCE_ID = "agenceId";
    /** Valeur réservée du filtre {@code agence} : dossiers dont le commercial n'est vendeur d'aucune agence. */
    private static final String VALEUR_SIEGE = "siege";
    private static final String SEARCH = "search";
    private static final String SIZE = "size";
    private static final String TAILLE = "200";
    private static final String TRANCHE_ID = "trancheId";
    private static final String DOSSIER_ID = "dossierId";
    private static final String ID = "id";
    private static final String CONTENT = "$.content[*]";
    private static final String COLONNES_DOSSIER = "articleId,dossierId";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureEtatVentes etatVentes;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private StlQuoteRepository quoteRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;

    private String jeton;
    private RequestPostProcessor siege;
    private RequestPostProcessor compteA;
    private AdmUser utilisateurA;
    private CfgTranche trancheP;
    private UUID agenceA;
    private UUID agenceB;
    private String libelleA;
    private String libelleB;
    private UUID vendeurA;
    private UUID vendeurB;
    private UUID vendeurSiege;
    private Dossier dossierA;
    private Dossier dossierB;
    private Dossier dossierSiege;
    private UUID prospectionA;
    private UUID prospectionB;
    private UUID prospectionSiege;
    private UUID devisA;
    private UUID devisB;
    private UUID devisSiege;
    private CfgTranche trancheVentes;
    private UUID articleLibre;
    private UUID ventesA;
    private UUID ventesB;
    private UUID ventesSiege;

    /** Id et code gardés : {@code commercialDuDossier} vide le contexte de persistance. */
    private record Dossier(UUID id, String code) {
    }

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        siege = connecte(patrimoine.administrateur());
        trancheP = plans.tranche("AGP-" + jeton, true);

        agenceA = agences.agence("AGA-" + jeton);
        agenceB = agences.agence("AGB-" + jeton);
        libelleA = "Tiers AGA-" + jeton;
        libelleB = "Tiers AGB-" + jeton;
        vendeurA = agences.vendeur("VA-" + jeton);
        vendeurB = agences.vendeur("VB-" + jeton);
        vendeurSiege = agences.vendeur("VS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        // commercialDuDossier vide le contexte : les dossiers d'abord, puis tout le reste par référence d'id.
        dossierA = dossierVif("LA", vendeurA);
        dossierB = dossierVif("LB", vendeurB);
        dossierSiege = dossierVif("LS", vendeurSiege);
        trancheVentes();

        prospectionA = prospection("PA", vendeurA);
        prospectionB = prospection("PB", vendeurB);
        prospectionSiege = prospection("PS", vendeurSiege);
        devisA = devis("PA", prospectionA, vendeurA);
        devisB = devis("PB", prospectionB, vendeurB);
        devisSiege = devis("PS", prospectionSiege, vendeurSiege);

        utilisateurA = agences.compteAgence("CA-" + jeton, vendeurA);
        plans.habiliter(utilisateurA, trancheP.getCfgProject());
        compteA = FixtureDossierVente.connecte(utilisateurA, FixtureAgences.ROLE_AGENCE);
    }

    // ------------------------------------------------------------------ fixtures

    /**
     * Tranche de l'état des ventes (modèle EtatVentesListeTest) : codes sans le jeton, pour ne pas polluer
     * les listes filtrées par {@code search}. Un article libre, et un dossier par vendeur (A, B, siège).
     */
    private void trancheVentes() {
        String prefixe = FixtureEtatVentes.prefixe("AGEV");
        trancheVentes = etatVentes.tranche("TR-" + prefixe, etatVentes.projet("PRJ-" + prefixe, true), true);
        StkItemCategory categorie = etatVentes.categorie("CAT-" + prefixe);
        StkItemType type = etatVentes.type("TY-" + prefixe);
        articleLibre = etatVentes.article(prefixe + "-L", trancheVentes, categorie, type).getId();
        // plans.dossier a déjà créé trois statuts RESERVATION : statut(RESERVATION) ne serait plus unique.
        StrStatus enCours = etatVentes.statutEnCours("RSV-" + prefixe);
        ventesA = vente(prefixe + "-A", categorie, type, enCours);
        ventesB = vente(prefixe + "-B", categorie, type, enCours);
        ventesSiege = vente(prefixe + "-S", categorie, type, enCours);
        // commercialDuDossier vide le contexte : après toutes les créations.
        agences.commercialDuDossier(ventesA, vendeurA);
        agences.commercialDuDossier(ventesB, vendeurB);
        agences.commercialDuDossier(ventesSiege, vendeurSiege);
    }

    private UUID vente(String code, StkItemCategory categorie, StkItemType type, StrStatus statut) {
        StkItem article = etatVentes.article(code, trancheVentes, categorie, type);
        return etatVentes.vendre(article, "DOS-" + code, statut, null, null).getId();
    }

    private Dossier dossierVif(String prefixe, UUID vendeur) {
        StkItem article = plans.article(trancheP, prefixe + "-" + jeton, false, null);
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        Dossier dossier = new Dossier(cree.getId(), cree.getCode());
        agences.commercialDuDossier(dossier.id(), vendeur);
        plans.comFolderId(article.getId(), dossier.id());
        return dossier;
    }

    /** Prospection de vente dont le prospect porte le jeton dans son nom (recherche {@code search}). */
    private UUID prospection(String cle, UUID vendeur) {
        UUID prospect = prospect(cle);
        em.flush();
        LocalDateTime creee = LocalDateTime.now().minusDays(2);
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PC-" + cle + "-" + jeton);
        p.setDocDate(creee);
        p.setUserCreateDateTime(creee);
        p.setIsClosed(false);
        p.setStrEntityId(ID_PROSPECTION_VENTE);
        p.setCfgProspect(em.getReference(CfgTier.class, prospect));
        p.setCfgCommercial(em.getReference(CfgTier.class, vendeur));
        p.setStkHierarchy(em.getReference(CfgTranche.class, trancheP.getId()));
        UUID id = prospectionRepository.saveAndFlush(p).getId();
        interaction(id);
        return id;
    }

    private UUID prospect(String cle) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("T-" + cle + "-" + jeton);
        tier.setFirstName("Prénom");
        tier.setLastName(jeton + "-" + cle);
        tier.setDescription(jeton + "-" + cle);
        tier.setCfgTierType(typeProspect());
        em.persist(tier);
        return tier.getId();
    }

    private CfgTierType typeProspect() {
        return tierTypeRepository.findByCode("PRO").orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode("PRO");
            t.setDescription("PRO");
            return tierTypeRepository.saveAndFlush(t);
        });
    }

    private void interaction(UUID prospectionId) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(em.getReference(ComProspection.class, prospectionId));
        m.setDocDate(LocalDateTime.now().minusDays(1));
        m.setAlertValidityDate(LocalDateTime.now().plusDays(3));
        m.setAlertMessage("rappeler " + jeton);
        m.setDescription("interaction " + jeton);
        m.setWithoutRevival(false);
        m.setIsUnpaid(false);
        m.setIsSystem(false);
        messageRepository.saveAndFlush(m);
    }

    /** Offre : même commercial que sa prospection (l'agence se lit de l'un comme de l'autre). */
    private UUID devis(String cle, UUID prospectionId, UUID vendeur) {
        ComProspection prospection = em.find(ComProspection.class, prospectionId);
        StlQuote q = new StlQuote();
        q.setId(UUID.randomUUID());
        q.setCode("DV-" + cle + "-" + jeton);
        q.setDocDate(LocalDateTime.now().minusDays(1));
        q.setCfgTranche(em.getReference(CfgTranche.class, trancheP.getId()));
        q.setCfgProspect(prospection.getCfgProspect());
        q.setComProspection(prospection);
        q.setCfgCommercial(em.getReference(CfgTier.class, vendeur));
        return quoteRepository.saveAndFlush(q).getId();
    }

    // ------------------------------------------------------------------ lecture

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<Map<String, Object>> lignes(MockHttpServletRequestBuilder requete) throws Exception {
        return JsonPath.read(corps(requete), CONTENT);
    }

    private static String texte(Object valeur) {
        return valeur == null ? null : String.valueOf(valeur).toLowerCase(Locale.ROOT);
    }

    private static List<String> valeurs(List<Map<String, Object>> lignes, String cle) {
        return lignes.stream().map(l -> texte(l.get(cle))).filter(Objects::nonNull).toList();
    }

    private static Map<String, Object> ligne(List<Map<String, Object>> lignes, String cle, UUID id) {
        return lignes.stream().filter(l -> id.toString().equalsIgnoreCase(String.valueOf(l.get(cle))))
                .findFirst().orElseThrow(() -> new AssertionError("ligne absente : " + cle + " = " + id));
    }

    private static String id(UUID id) {
        return id.toString().toLowerCase(Locale.ROOT);
    }

    private MockHttpServletRequestBuilder dossiers(RequestPostProcessor qui, UUID... agencesFiltrees) {
        return avecAgences(get(DOSSIERS).param(SEARCH, jeton).param(SIZE, TAILLE).with(qui), agencesFiltrees);
    }

    private MockHttpServletRequestBuilder prospections(RequestPostProcessor qui, UUID... agencesFiltrees) {
        return avecAgences(get(PROSPECTIONS).param(SEARCH, jeton).param(SIZE, TAILLE).with(qui), agencesFiltrees);
    }

    private MockHttpServletRequestBuilder devis(RequestPostProcessor qui, UUID... agencesFiltrees) {
        return avecAgences(get(DEVIS).param(TRANCHE_ID, trancheP.getId().toString()).param(SIZE, TAILLE).with(qui),
                agencesFiltrees);
    }

    /** L'état des ventes ne remplit que les colonnes demandées ({@code colonnes}). */
    private MockHttpServletRequestBuilder etatVentes(String colonnes, UUID... agencesFiltrees) {
        return avecAgences(get(ETAT_VENTES).param(TRANCHE_ID, trancheVentes.getId().toString()).param(SIZE, TAILLE)
                .param("colonnes", colonnes).with(siege), agencesFiltrees);
    }

    private static MockHttpServletRequestBuilder avecAgences(MockHttpServletRequestBuilder requete, UUID... ids) {
        for (UUID agence : ids) {
            requete.param(AGENCE, agence.toString());
        }
        return requete;
    }

    // ------------------------------------------------------------------ R-S1 : liste des dossiers

    @Test
    void rS1_listeDossiers_siege_dossierDeA_porteAgenceA_etSonVendeur() throws Exception {
        Map<String, Object> ligneA = ligne(lignes(dossiers(siege)), ID, dossierA.id());

        assertThat(ligneA)
                .containsEntry(AGENCE, libelleA)
                .containsEntry(AGENCE_ID, id(agenceA))
                .containsEntry("commercialId", id(vendeurA));
    }

    @Test
    void rS1_listeDossiers_dossierDuSiege_agenceSiege_sansAgenceId() throws Exception {
        Map<String, Object> ligneSiege = ligne(lignes(dossiers(siege)), ID, dossierSiege.id());

        assertThat(ligneSiege).containsEntry(AGENCE, SIEGE).extractingByKey(AGENCE_ID).isNull();
    }

    @Test
    void rS1_filtreAgenceA_listeDossiers_neRendQueLeDossierDeA() throws Exception {
        assertThat(valeurs(lignes(dossiers(siege, agenceA)), ID))
                .containsExactly(id(dossierA.id()));
    }

    @Test
    void rS1_filtreAgence_plusieursIds_rendLesDossiersDeChacune_sansLeSiege() throws Exception {
        assertThat(valeurs(lignes(dossiers(siege, agenceA, agenceB)), ID))
                .containsExactlyInAnyOrder(id(dossierA.id()), id(dossierB.id()));
    }

    @Test
    void rS1_filtreAgenceSiege_listeDossiers_neRendQueLeDossierDuSiege() throws Exception {
        MockHttpServletRequestBuilder requete = dossiers(siege).param(AGENCE, VALEUR_SIEGE);

        assertThat(valeurs(lignes(requete), ID)).containsExactly(id(dossierSiege.id()));
    }

    @Test
    void rS1_filtreAgenceAEtSiege_listeDossiers_rendAEtSiege_sansB() throws Exception {
        MockHttpServletRequestBuilder requete = dossiers(siege, agenceA).param(AGENCE, VALEUR_SIEGE);

        assertThat(valeurs(lignes(requete), ID)).containsExactlyInAnyOrder(id(dossierA.id()), id(dossierSiege.id()));
    }

    @Test
    void rS1_filtreAgenceSiege_etatDesVentes_neRendQueLeDossierDuSiege() throws Exception {
        MockHttpServletRequestBuilder requete = etatVentes(COLONNES_DOSSIER).param(AGENCE, VALEUR_SIEGE);

        assertThat(valeurs(lignes(requete), DOSSIER_ID)).containsExactly(id(ventesSiege));
    }

    @Test
    void rS1_filtreAgence_rienCoche_rendTout() throws Exception {
        assertThat(valeurs(lignes(dossiers(siege)), ID))
                .contains(id(dossierA.id()), id(dossierB.id()), id(dossierSiege.id()));
    }

    @Test
    void rS1_exportDossiers_colonneAgence_libelleOuSiege() throws Exception {
        MockHttpServletResponse reponse = LectureExport.executer(mockMvc, get(DOSSIERS + "/export")
                .param(SEARCH, jeton).with(siege)).getResponse();
        assertThat(reponse.getStatus()).isEqualTo(200);
        List<List<Object>> lignesXlsx = LectureExport.lignesXlsx(reponse.getContentAsByteArray());

        int colonne = lignesXlsx.stream().mapToInt(l -> l.indexOf("Agence")).filter(i -> i >= 0).findFirst().orElse(-1);
        assertThat(colonne).as("en-tête « Agence » de l'export").isNotNegative();
        assertThat(List.of(celluleDuCode(lignesXlsx, dossierA.code(), colonne),
                celluleDuCode(lignesXlsx, dossierSiege.code(), colonne)))
                .containsExactly(libelleA, SIEGE);
    }

    private static Object celluleDuCode(List<List<Object>> lignes, String code, int colonne) {
        return lignes.stream().filter(l -> l.contains(code)).findFirst()
                .map(l -> l.get(colonne)).orElseThrow(() -> new AssertionError("ligne d'export absente : " + code));
    }

    @Test
    void rS1_compteAgenceA_filtreAgenceB_neRendRien() throws Exception {
        assertThat(valeurs(lignes(dossiers(compteA, agenceB)), ID)).isEmpty();
    }

    // ------------------------------------------------------------------ R-S1 : prospections et offres

    @Test
    void rS1_listeProspections_agenceDeAEtSiege() throws Exception {
        List<Map<String, Object>> lignes = lignes(prospections(siege));

        assertThat(ligne(lignes, ID, prospectionA)).containsEntry(AGENCE, libelleA).containsEntry(AGENCE_ID, id(agenceA));
        assertThat(ligne(lignes, ID, prospectionSiege)).containsEntry(AGENCE, SIEGE);
    }

    @Test
    void rS1_filtreAgenceA_listeProspections_neRendQueLaProspectionDeA() throws Exception {
        assertThat(valeurs(lignes(prospections(siege, agenceA)), ID))
                .contains(id(prospectionA))
                .doesNotContain(id(prospectionB), id(prospectionSiege));
    }

    @Test
    void rS1_compteAgenceA_filtreAgenceB_prospections_neRendRien() throws Exception {
        assertThat(valeurs(lignes(prospections(compteA, agenceB)), ID)).isEmpty();
    }

    @Test
    void rS1_listeOffres_agenceDeBEtSiege() throws Exception {
        List<Map<String, Object>> lignes = lignes(devis(siege));

        assertThat(ligne(lignes, ID, devisB)).containsEntry(AGENCE, libelleB).containsEntry(AGENCE_ID, id(agenceB));
        assertThat(ligne(lignes, ID, devisSiege)).containsEntry(AGENCE, SIEGE);
    }

    @Test
    void rS1_filtreAgenceA_listeOffres_neRendQueLOffreDeA() throws Exception {
        assertThat(valeurs(lignes(devis(siege, agenceA)), ID))
                .contains(id(devisA))
                .doesNotContain(id(devisB), id(devisSiege));
    }

    // ------------------------------------------------------------------ R-S1 : état des ventes

    @Test
    void temoin_etatDesVentes_lesTroisDossiersSontDansLaTranche() throws Exception {
        assertThat(valeurs(lignes(etatVentes(COLONNES_DOSSIER)), DOSSIER_ID))
                .containsExactlyInAnyOrder(id(ventesA), id(ventesB), id(ventesSiege));
    }

    @Test
    void rS1_etatDesVentes_agenceDeAEtSiege() throws Exception {
        List<Map<String, Object>> lignes = lignes(etatVentes(COLONNES_DOSSIER + ",agence,agenceId"));

        assertThat(ligne(lignes, DOSSIER_ID, ventesA))
                .containsEntry(AGENCE, libelleA).containsEntry(AGENCE_ID, id(agenceA));
        assertThat(ligne(lignes, DOSSIER_ID, ventesSiege)).containsEntry(AGENCE, SIEGE);
    }

    /** Réponse du manager (6) : article sans dossier, pas d'agence (ni « Siège »). */
    @Test
    void rS1_etatDesVentes_articleLibre_agenceNulle() throws Exception {
        List<Map<String, Object>> lignes = lignes(etatVentes(COLONNES_DOSSIER + ",agence"));

        assertThat(ligne(lignes, "articleId", articleLibre).get(AGENCE)).isNull();
    }

    @Test
    void rS1_filtreAgenceA_etatDesVentes_neRendQueLeDossierDeA() throws Exception {
        assertThat(valeurs(lignes(etatVentes(COLONNES_DOSSIER, agenceA)), DOSSIER_ID))
                .containsExactly(id(ventesA));
    }

    // ------------------------------------------------------------------ R-S2 : fiche dossier « Vendu par »

    @Test
    void rS2_ficheDossier_venduParAgenceAEtSonVendeur() throws Exception {
        Map<String, Object> fiche = JsonPath.read(corps(get(DOSSIERS + "/{id}", dossierA.id()).with(siege)), "$");

        assertThat(fiche)
                .containsEntry(AGENCE, libelleA)
                .containsEntry(AGENCE_ID, id(agenceA))
                .containsEntry("commercialId", id(vendeurA));
    }

    @Test
    void rS2_ficheDossier_dossierDuSiege_venduParSiege() throws Exception {
        Map<String, Object> fiche = JsonPath.read(corps(get(DOSSIERS + "/{id}", dossierSiege.id()).with(siege)), "$");

        assertThat(fiche).containsEntry(AGENCE, SIEGE).extractingByKey(AGENCE_ID).isNull();
    }

    @Test
    void rS2_ficheDossier_agenceSeCalculeDepuisLeCommercial() throws Exception {
        agences.commercialDuDossier(dossierA.id(), vendeurB);

        Map<String, Object> fiche = JsonPath.read(corps(get(DOSSIERS + "/{id}", dossierA.id()).with(siege)), "$");

        assertThat(fiche).containsEntry(AGENCE, libelleB).containsEntry(AGENCE_ID, id(agenceB));
    }

    // ------------------------------------------------------------------ R-S4 : onglet Liens de la fiche agence

    @Test
    void rS4_ongletLiensAgence_listeLeVendeurEtSonCompte() throws Exception {
        String liens = corps(get("/api/administration/tiers/{id}/liens", agenceA).with(siege));

        assertThat(liens.toLowerCase(Locale.ROOT)).contains(id(vendeurA));
        assertThat(liens).containsAnyOf(utilisateurA.getCode(), utilisateurA.getDescription());
    }
}
