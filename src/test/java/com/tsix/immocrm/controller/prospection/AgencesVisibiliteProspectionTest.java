package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.LectureExport;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité prospection / prospects / offres pour un compte agence — FEATURE-agences-vente.md §4 V4, V5, V6, V7, R-V2,
 * R-V4, R-V8. Projet P affecté à l'agence A : prospections PA (vendeur de A), PB (vendeur de B), PS (vendeur du siège),
 * PN (sans commercial), une interaction chacune, une offre sur PA et sur PB, une pré-prospection assignée au vendeur de A,
 * un dossier de A (client CA) et un dossier de B (client CB).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesVisibiliteProspectionTest {

    private static final UUID ID_PROSPECTION_VENTE = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");
    private static final String PROSPECTIONS = "/api/prospections";
    private static final String PROSPECTION = "/api/prospections/{id}";
    private static final String PROSPECTS = "/api/prospects";
    private static final String PROSPECT = "/api/prospects/{id}";
    private static final String CLIENT = "/api/clients/{id}";
    private static final String DEVIS = "/api/devis";
    private static final String SEARCH = "search";
    private static final String SIZE = "size";
    private static final String CONTENT_IDS = "$.content[*].id";
    private static final String MESSAGE_DOUBLON = "Ce prospect est déjà suivi, contactez le siège";
    private static final String PA = "PA";
    private static final String PB = "PB";
    private static final String PS = "PS";
    private static final String PN = "PN";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private EntityManager em;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private StlQuoteRepository quoteRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;
    @Autowired private AdmCountryRepository countryRepository;

    private String jeton;
    private RequestPostProcessor siege;
    private RequestPostProcessor compteA;
    private CfgTranche trancheP;
    private final Map<String, UUID> prospects = new HashMap<>();
    private final Map<String, UUID> prospections = new HashMap<>();
    private UUID clientA;
    private UUID clientB;
    private UUID devisA;
    private UUID devisB;
    private String telPreProspection;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        garantirIndicatifMaroc();
        siege = connecte(patrimoine.administrateur());
        trancheP = plans.tranche("AGP-" + jeton, true);

        UUID agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        UUID vendeurA = agences.vendeur("VA-" + jeton);
        UUID vendeurB = agences.vendeur("VB-" + jeton);
        UUID vendeurSiege = agences.vendeur("VS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        // commercialDuDossier vide le contexte : les dossiers d'abord, puis tout le reste par référence d'id.
        UUID dossierA = dossier("DA", vendeurA);
        UUID dossierB = dossier("DB", vendeurB);
        clientA = tier("CA");
        clientB = tier("CB");
        em.flush();
        client(dossierA, clientA);
        client(dossierB, clientB);

        prospectionDe(PA, vendeurA);
        prospectionDe(PB, vendeurB);
        prospectionDe(PS, vendeurSiege);
        prospectionDe(PN, null);
        devisA = devis(PA);
        devisB = devis(PB);
        telPreProspection = mobileAleatoire();
        preProspection(vendeurA);

        compteA = compteAgence(vendeurA);
    }

    // ------------------------------------------------------------------ fixtures

    private UUID dossier(String prefixe, UUID vendeur) {
        StkItem article = plans.article(trancheP, prefixe + "-" + jeton, false, null);
        ComFolder dossier = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        UUID id = dossier.getId();
        agences.commercialDuDossier(id, vendeur);
        plans.comFolderId(article.getId(), id);
        return id;
    }

    private void client(UUID dossierId, UUID tierId) {
        ComFolder dossier = em.find(ComFolder.class, dossierId);
        dossier.setCfgCustomer(em.getReference(CfgTier.class, tierId));
        em.flush();
    }

    /** Tiers dont le nom commence par le jeton (recherches {@code search} / {@code nom}). */
    private UUID tier(String suffixe) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("T-" + suffixe + "-" + jeton);
        tier.setFirstName("Prénom");
        tier.setLastName(nom(suffixe));
        tier.setDescription(nom(suffixe));
        tier.setCfgTierType(typeTiers(suffixe.startsWith("C") ? "CLI" : "PRO"));
        em.persist(tier);
        return tier.getId();
    }

    /** La liste, l'export et les doublons reconnaissent un prospect/client à son CfgTierType (PRO, CLI). */
    private CfgTierType typeTiers(String code) {
        return tierTypeRepository.findByCode(code).orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode(code);
            t.setDescription(code);
            return tierTypeRepository.saveAndFlush(t);
        });
    }

    /** Le référentiel de test ne porte pas forcément l'indicatif 212 exigé par la validation du mobile. */
    private void garantirIndicatifMaroc() {
        boolean present = countryRepository.findAll().stream().anyMatch(c -> "212".equals(c.getPhoneCode()));
        if (!present) {
            AdmCountry pays = new AdmCountry();
            pays.setId(UUID.randomUUID());
            pays.setCode("MA-" + UUID.randomUUID());
            pays.setDescription("MAROC");
            pays.setPhoneCode("212");
            countryRepository.saveAndFlush(pays);
        }
    }

    private String nom(String suffixe) {
        return jeton + "-" + suffixe;
    }

    private void prospectionDe(String cle, UUID vendeur) {
        UUID prospect = tier(cle);
        em.flush();
        ComProspection p = ligne("PC-" + cle);
        p.setStrEntityId(ID_PROSPECTION_VENTE);
        p.setCfgProspect(em.getReference(CfgTier.class, prospect));
        p.setCfgCommercial(vendeur == null ? null : em.getReference(CfgTier.class, vendeur));
        p.setStkHierarchy(em.getReference(CfgTranche.class, trancheP.getId()));
        UUID id = prospectionRepository.saveAndFlush(p).getId();
        interaction(id);
        prospects.put(cle, prospect);
        prospections.put(cle, id);
    }

    private ComProspection ligne(String prefixe) {
        LocalDateTime creee = LocalDateTime.now().minusDays(2);
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(prefixe + "-" + jeton);
        p.setDocDate(creee);
        p.setUserCreateDateTime(creee);
        p.setIsClosed(false);
        return p;
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

    private void preProspection(UUID vendeur) {
        ComProspection p = ligne("PP");
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setTempDescription("LEAD-" + jeton);
        p.setTempTel(telPreProspection);
        p.setCfgCommercial(em.getReference(CfgTier.class, vendeur));
        prospectionRepository.saveAndFlush(p);
    }

    private UUID devis(String cle) {
        StlQuote q = new StlQuote();
        q.setId(UUID.randomUUID());
        q.setCode("DV-" + cle + "-" + jeton);
        q.setDocDate(LocalDateTime.now().minusDays(1));
        q.setCfgTranche(em.getReference(CfgTranche.class, trancheP.getId()));
        q.setCfgProspect(em.getReference(CfgTier.class, prospects.get(cle)));
        q.setComProspection(em.getReference(ComProspection.class, prospections.get(cle)));
        return quoteRepository.saveAndFlush(q).getId();
    }

    private RequestPostProcessor compteAgence(UUID vendeur) {
        AdmUser compte = agences.compteAgence("CA-" + jeton, vendeur);
        plans.habiliter(compte, trancheP.getCfgProject());
        return FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
    }

    private static String mobileAleatoire() {
        return String.format("06%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    // ------------------------------------------------------------------ lecture

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> ids(MockHttpServletRequestBuilder requete) throws Exception {
        List<Object> lus = JsonPath.read(corps(requete), CONTENT_IDS);
        return lus.stream().map(String::valueOf).toList();
    }

    private String id(String cle) {
        return prospections.get(cle).toString();
    }

    private String prospect(String cle) {
        return prospects.get(cle).toString();
    }

    private MockHttpServletRequestBuilder recherche(String url, RequestPostProcessor qui) {
        return get(url).param(SEARCH, jeton).param(SIZE, "200").with(qui);
    }

    private List<String> nomsExportes(String url, RequestPostProcessor qui, List<String> candidats) throws Exception {
        MockHttpServletResponse reponse = LectureExport.executer(mockMvc, get(url).param(SEARCH, jeton).with(qui))
                .getResponse();
        assertThat(reponse.getStatus()).isEqualTo(200);
        return LectureExport.codesDesLignes(LectureExport.lignesXlsx(reponse.getContentAsByteArray()), candidats);
    }

    private String codeProspection(String cle) {
        return "PC-" + cle + "-" + jeton;
    }

    private String codeProspect(String cle) {
        return "T-" + cle + "-" + jeton;
    }

    private List<String> codes(java.util.function.Function<String, String> code) {
        return List.of(code.apply(PA), code.apply(PB), code.apply(PS), code.apply(PN));
    }

    // ------------------------------------------------------------------ V4 : prospections, liste et export

    @Test
    void v4_listeProspections_agenceA_neVoitQueLaProspectionDeSonVendeur() throws Exception {
        assertThat(ids(recherche(PROSPECTIONS, compteA)))
                .contains(id(PA))
                .doesNotContain(id(PB), id(PS), id(PN));
    }

    @Test
    void v4_listeProspections_temoinSiege_voitLesQuatre() throws Exception {
        assertThat(ids(recherche(PROSPECTIONS, siege))).contains(id(PA), id(PB), id(PS), id(PN));
    }

    @Test
    void v4_exportProspections_agenceA_sansLesProspectionsDeBDuSiegeNiSansCommercial() throws Exception {
        assertThat(nomsExportes(PROSPECTIONS + "/export", compteA, codes(this::codeProspection)))
                .contains(codeProspection(PA))
                .doesNotContain(codeProspection(PB), codeProspection(PS), codeProspection(PN));
    }

    @Test
    void v4_exportProspections_temoinSiege_contientLaProspectionDeB() throws Exception {
        assertThat(nomsExportes(PROSPECTIONS + "/export", siege, codes(this::codeProspection)))
                .contains(codeProspection(PB), codeProspection(PN));
    }

    // ------------------------------------------------------------------ V4 / R-V8 : fiche et sous-ressources par id → 404

    @ParameterizedTest(name = "prospection {0}")
    @ValueSource(strings = {PB, PS, PN})
    void v4_rV8_ficheProspection_agenceA_horsAgenceOuSansCommercial_404(String cle) throws Exception {
        mockMvc.perform(get(PROSPECTION, prospections.get(cle)).with(compteA)).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "sous-ressource {0}")
    @ValueSource(strings = {"interactions", "historique", "devis"})
    void v4_rV8_sousRessourcesDeLaProspectionDeB_agenceA_404(String sousRessource) throws Exception {
        mockMvc.perform(get(PROSPECTION + "/" + sousRessource, prospections.get(PB)).with(compteA))
                .andExpect(status().isNotFound());
    }

    @Test
    void v4_rV4_interactionsDeLaProspectionSansCommercial_agenceA_404() throws Exception {
        mockMvc.perform(get(PROSPECTION + "/interactions", prospections.get(PN)).with(compteA))
                .andExpect(status().isNotFound());
    }

    @Test
    void v4_ficheEtInteractionsDeSaProspection_agenceA_200() throws Exception {
        mockMvc.perform(get(PROSPECTION, prospections.get(PA)).with(compteA)).andExpect(status().isOk());
        mockMvc.perform(get(PROSPECTION + "/interactions", prospections.get(PA)).with(compteA))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "prospection {0}")
    @ValueSource(strings = {PB, PS, PN})
    void v4_ficheEtInteractions_temoinSiege_200(String cle) throws Exception {
        mockMvc.perform(get(PROSPECTION, prospections.get(cle)).with(siege)).andExpect(status().isOk());
        mockMvc.perform(get(PROSPECTION + "/interactions", prospections.get(cle)).with(siege))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ V5 / R-V8 : leads du siège, aucune fuite
    // A1d fermera ces routes (403) ; A1c accepte 403 ou 200 sans la donnée.

    @Test
    void v5_rV8_preProspections_agenceA_preProspectionAssigneeASonVendeurAbsente() throws Exception {
        MockHttpServletResponse reponse = mockMvc.perform(get("/api/pre-prospections").param(SIZE, "200").with(compteA))
                .andReturn().getResponse();

        assertThat(reponse.getStatus()).isIn(200, 403);
        assertThat(reponse.getContentAsString(StandardCharsets.UTF_8)).doesNotContain(jeton, telPreProspection);
    }

    @Test
    void v5_rV8_boiteDeTri_agenceA_vide() throws Exception {
        MockHttpServletResponse reponse = mockMvc.perform(get("/api/boite-de-tri").with(compteA)).andReturn().getResponse();

        assertThat(reponse.getStatus()).isIn(200, 403);
        if (reponse.getStatus() == 200) {
            Number total = JsonPath.read(reponse.getContentAsString(StandardCharsets.UTF_8), "$.totalElements");
            assertThat(total.longValue()).isZero();
        }
    }

    @ParameterizedTest(name = "critère {0}")
    @ValueSource(strings = {"nom", "telephone"})
    void v5_rV8_hotline_agenceA_aucuneFiche(String critere) throws Exception {
        String valeur = "nom".equals(critere) ? jeton : telPreProspection;
        MockHttpServletResponse reponse = mockMvc.perform(get("/api/hotline/recherche").param(critere, valeur)
                .with(compteA)).andReturn().getResponse();

        assertThat(reponse.getStatus()).isIn(200, 403);
        if (reponse.getStatus() == 200) {
            List<Object> fiches = JsonPath.read(reponse.getContentAsString(StandardCharsets.UTF_8), "$.fiches");
            assertThat(fiches).isEmpty();
        }
    }

    // ------------------------------------------------------------------ V6 : prospects

    @Test
    void v6_listeProspects_agenceA_sansProspectsDeBDuSiegeNiSuivisParPersonne() throws Exception {
        assertThat(ids(recherche(PROSPECTS, compteA)))
                .contains(prospect(PA))
                .doesNotContain(prospect(PB), prospect(PS), prospect(PN), clientB.toString());
    }

    @Test
    void v6_listeProspects_temoinSiege_voitLeProspectDeB() throws Exception {
        assertThat(ids(recherche(PROSPECTS, siege))).contains(prospect(PA), prospect(PB), prospect(PS));
    }

    @ParameterizedTest(name = "prospect {0}")
    @ValueSource(strings = {PB, PS, PN})
    void v6_ficheProspect_agenceA_aucuneProspectionNiDossierDeSonAgence_404(String cle) throws Exception {
        mockMvc.perform(get(PROSPECT, prospects.get(cle)).with(compteA)).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "prospect {0}")
    @ValueSource(strings = {PB, PS, PN})
    void v6_ficheProspect_temoinSiege_200(String cle) throws Exception {
        mockMvc.perform(get(PROSPECT, prospects.get(cle)).with(siege)).andExpect(status().isOk());
    }

    @Test
    void v6_ficheProspect_agenceA_prospectDeSaProspection_200() throws Exception {
        mockMvc.perform(get(PROSPECT, prospects.get(PA)).with(compteA)).andExpect(status().isOk());
    }

    @Test
    void v6_ficheProspect_agenceA_clientDeSonDossier_200_clientDuDossierDeB_404() throws Exception {
        mockMvc.perform(get(PROSPECT, clientA).with(compteA)).andExpect(status().isOk());
        mockMvc.perform(get(PROSPECT, clientB).with(compteA)).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "sous-ressource {0}")
    @ValueSource(strings = {"prospections", "dossiers", "reglements", "historique"})
    void v6_sousRessourcesDuProspectDeB_agenceA_404(String sousRessource) throws Exception {
        mockMvc.perform(get(PROSPECT + "/" + sousRessource, prospects.get(PB)).with(compteA))
                .andExpect(status().isNotFound());
    }

    @Test
    void v6_exportProspects_agenceA_sansProspectsDeBDuSiegeNiSuivisParPersonne() throws Exception {
        assertThat(nomsExportes(PROSPECTS + "/export", compteA, codes(this::codeProspect)))
                .contains(codeProspect(PA))
                .doesNotContain(codeProspect(PB), codeProspect(PS), codeProspect(PN));
    }

    @Test
    void v6_exportProspects_temoinSiege_contientLeProspectDeB() throws Exception {
        assertThat(nomsExportes(PROSPECTS + "/export", siege, codes(this::codeProspect))).contains(codeProspect(PB));
    }

    @Test
    void rV8_doublons_agenceA_neRendRienDuProspectDeB() throws Exception {
        String reponse = corps(get(PROSPECTS + "/doublons").param("nom", nom(PB)).with(compteA));

        assertThat(reponse).doesNotContain(prospect(PB), nom(PB));
    }

    @Test
    void rV8_doublons_temoinSiege_rendLeProspectDeB() throws Exception {
        assertThat(corps(get(PROSPECTS + "/doublons").param("nom", nom(PB)).with(siege))).contains(prospect(PB));
    }

    // ------------------------------------------------------------------ V6 : clients

    @Test
    void v6_ficheClient_agenceA_clientDuDossierDeB_404_etSesDossiers_404() throws Exception {
        mockMvc.perform(get(CLIENT, clientB).with(compteA)).andExpect(status().isNotFound());
        mockMvc.perform(get(CLIENT + "/dossiers", clientB).with(compteA)).andExpect(status().isNotFound());
    }

    @Test
    void v6_ficheClient_agenceA_clientDeSonDossier_200() throws Exception {
        mockMvc.perform(get(CLIENT, clientA).with(compteA)).andExpect(status().isOk());
        mockMvc.perform(get(CLIENT + "/dossiers", clientA).with(compteA)).andExpect(status().isOk());
    }

    @Test
    void v6_ficheClient_temoinSiege_clientDuDossierDeB_200() throws Exception {
        mockMvc.perform(get(CLIENT, clientB).with(siege)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ V7 : offres

    private MockHttpServletRequestBuilder devisDeP(RequestPostProcessor qui) {
        return get(DEVIS).param("trancheId", trancheP.getId().toString()).param(SIZE, "200").with(qui);
    }

    @Test
    void v7_listeDevis_agenceA_offreDeSaProspectionSeulement() throws Exception {
        assertThat(ids(devisDeP(compteA))).contains(devisA.toString()).doesNotContain(devisB.toString());
    }

    @Test
    void v7_listeDevis_temoinSiege_voitLOffreDeB() throws Exception {
        assertThat(ids(devisDeP(siege))).contains(devisA.toString(), devisB.toString());
    }

    @Test
    void v7_ficheDevis_agenceA_offreDeB_404_offreDeA_200() throws Exception {
        mockMvc.perform(get(DEVIS + "/{id}", devisB).with(compteA)).andExpect(status().isNotFound());
        mockMvc.perform(get(DEVIS + "/{id}", devisA).with(compteA)).andExpect(status().isOk());
    }

    @Test
    void v7_ficheDevis_temoinSiege_offreDeB_200() throws Exception {
        mockMvc.perform(get(DEVIS + "/{id}", devisB).with(siege)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ R-V2 : doublon de prospect hors agence → 409 muet

    private String corpsProspect(String nom, String mobile, String cin, boolean ignorerDoublons) {
        StringBuilder json = new StringBuilder("{\"nom\":\"").append(nom).append('"');
        if (mobile != null) {
            json.append(",\"mobile\":\"").append(mobile).append("\",\"indicatifMobile\":\"212\"");
        }
        if (cin != null) {
            json.append(",\"pieceIdentite\":{\"numero\":\"").append(cin).append("\"}");
        }
        return json.append(",\"ignorerDoublons\":").append(ignorerDoublons).append('}').toString();
    }

    private MockHttpServletRequestBuilder creer(String corps, RequestPostProcessor qui) {
        return post(PROSPECTS).contentType(MediaType.APPLICATION_JSON).content(corps).with(qui);
    }

    /** Prospect créé par le siège (contournement des homonymes), suivi par B si {@code suiviParB}. */
    private UUID prospectExistant(String suffixe, String mobile, String cin, boolean suiviParB) throws Exception {
        String reponse = mockMvc.perform(creer(corpsProspect(nom(suffixe), mobile, cin, true), siege))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID id = UUID.fromString(JsonPath.read(reponse, "$.id"));
        if (suiviParB) {
            ComProspection p = ligne("PX-" + suffixe);
            p.setStrEntityId(ID_PROSPECTION_VENTE);
            p.setCfgProspect(em.getReference(CfgTier.class, id));
            p.setCfgCommercial(em.find(ComProspection.class, prospections.get(PB)).getCfgCommercial());
            p.setStkHierarchy(em.getReference(CfgTranche.class, trancheP.getId()));
            prospectionRepository.saveAndFlush(p);
        }
        return id;
    }

    private void attendDoublonMuet(String corps, UUID existant, String nomExistant) throws Exception {
        String reponse = mockMvc.perform(creer(corps, compteA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(MESSAGE_DOUBLON))
                .andExpect(jsonPath("$.doublons").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(reponse).doesNotContain(existant.toString(), nomExistant);
    }

    @Test
    void rV2_agenceA_memeMobileQuUnProspectDeB_409SansDonnee() throws Exception {
        String mobile = mobileAleatoire();
        UUID existant = prospectExistant("XB", mobile, null, true);

        attendDoublonMuet(corpsProspect(nom("NOUVEAU"), mobile, null, false), existant, nom("XB"));
    }

    @Test
    void rV2_rV8_agenceA_ignorerDoublonsNeContournePas_409SansDonnee() throws Exception {
        String mobile = mobileAleatoire();
        UUID existant = prospectExistant("XB", mobile, null, true);

        attendDoublonMuet(corpsProspect(nom("NOUVEAU"), mobile, null, true), existant, nom("XB"));
    }

    @Test
    void rV2_agenceA_memeCinQuUnProspectDeB_409SansDonnee() throws Exception {
        String cin = "CIN" + jeton;
        UUID existant = prospectExistant("XC", null, cin, true);

        attendDoublonMuet(corpsProspect(nom("NOUVEAU"), null, cin, false), existant, nom("XC"));
    }

    @Test
    void rV2_rV8_agenceA_memeMobileQuUnProspectSuiviParPersonne_409SansDonnee() throws Exception {
        String mobile = mobileAleatoire();
        UUID existant = prospectExistant("XN", mobile, null, false);

        attendDoublonMuet(corpsProspect(nom("NOUVEAU"), mobile, null, false), existant, nom("XN"));
    }

    @Test
    void rV2_temoinSiege_memeMobileAvecIgnorerDoublons_201() throws Exception {
        String mobile = mobileAleatoire();
        prospectExistant("XB", mobile, null, true);

        mockMvc.perform(creer(corpsProspect(nom("NOUVEAU"), mobile, null, true), siege)).andExpect(status().isCreated());
    }
}
