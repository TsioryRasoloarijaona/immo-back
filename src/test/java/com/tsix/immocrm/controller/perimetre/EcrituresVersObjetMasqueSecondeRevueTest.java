package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureChargementTranche;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue (règle 6) de FEATURE-patrimoine-reliquat-v2.md §B : Commercial affecté au projet masqué → même 400 que
 * l'Administrateur ; id inconnu et objet masqué → même {@code detail} ; entité réactivée → l'appel réussit ; aucune
 * réponse (aperçu, rapport, message) ne reflète une tranche ou un article masqué.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcrituresVersObjetMasqueSecondeRevueTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String ARTICLE_ID = "articleId";
    private static final String ARTICLE_IDS = "articleIds";
    private static final String PRIX = "prix";
    private static final String DETAIL = "$.detail";
    private static final String DEVIS = "/api/prospections/{id}/devis";
    private static final String PROSPECTIONS = "/api/prospections";
    private static final String PRE_PROSPECTIONS = "/api/pre-prospections";
    private static final String SIMULATION = "/api/dossiers/simulation-financement";
    private static final String IMPORT = "/api/pre-prospections/import";
    private static final String ENTETE_CSV = "Créé,Nom,Adresse e-mail,Source,Formulaire,Canal,Étape,Propriétaire,"
            + "Étiquettes,Téléphone,Numéro de téléphone secondaire,Numéro WhatsApp\n";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private FixtureReferentielStk referentielStk;
    @Autowired private FixtureChargementTranche lecture;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;
    @Autowired private ComProspectionOriginRepository origineRepository;
    @Autowired private ComActionMessageTypeRepository typeContactRepository;
    @Autowired private ComProspectionKindRepository natureRepository;
    @Autowired private ComPaymentMethodRepository modePaieRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private StlQuoteRepository quoteRepository;

    private String jeton;
    private AdmUser admin;
    private AdmUser commercial;
    private StkItemType type;
    private StkItemCategory categorie;
    private StkVocation vocation;
    private CfgTranche trancheActive;
    private CfgTranche trancheMasquee;
    private StkItem articleActif;
    private StkItem articleMasque;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        type = patrimoine.type("TYP-MSQ2-" + jeton, referentielStk.natureParDefaut(), false);
        categorie = patrimoine.categorie("CAT-MSQ2-" + jeton, referentielStk.natureParDefaut(), false);
        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-MSQ2-" + jeton);
        vocation = vocationRepository.save(vocation);
        // Codes sans tiret : l'import lit le premier mot du formulaire comme code de tranche.
        trancheActive = vente.tranche("MSQA" + jeton, true);
        trancheMasquee = vente.tranche("MSQM" + jeton, false);
        articleActif = article("LOT-ACT2-" + jeton, trancheActive, null);
        articleMasque = article("LOT-MSQ2-" + jeton, trancheMasquee, null);

        commercial = new AdmUser();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("msq2.commercial." + jeton);
        commercial.setDisabled(false);
        commercial = userRepository.save(commercial);
        affecter(commercial, trancheActive);
        affecter(commercial, trancheMasquee);
    }

    // ------------------------------------------------------------------ outils

    private void affecter(AdmUser user, CfgTranche tranche) {
        AdmUserHabilitation h = new AdmUserHabilitation();
        h.setId(UUID.randomUUID());
        h.setAdmUser(user);
        h.setCfgProject(tranche.getCfgProject());
        habilitationRepository.save(h);
    }

    private RequestPostProcessor commeCommercial() {
        AuthenticatedUser principal = new AuthenticatedUser(commercial.getId(), commercial.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private void reactiver() {
        CfgCompany societe = trancheMasquee.getCfgProject().getCfgCompany();
        societe.setIsActive(true);
        companyRepository.saveAndFlush(societe);
    }

    private StkItem article(String code, CfgTranche tranche, StkItem parent) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemType(type);
        item.setStkItemCategory(categorie);
        item.setStkVocation(vocation);
        item.setStkHierarchy(tranche);
        item.setParent(parent);
        item.setComSalePrice(new BigDecimal("500000"));
        item.setVat(new BigDecimal("0.2"));
        item.setStkKind(referentielStk.natureParDefaut());
        return stkItemRepository.save(item);
    }

    private CfgTier prospect() {
        CfgTierType pro = tierTypeRepository.findByCode("PRO").orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode("PRO");
            t.setDescription("PROSPECT");
            return tierTypeRepository.save(t);
        });
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("PRO-MSQ2-" + UUID.randomUUID().toString().substring(0, 8));
        tier.setFirstName("Prenom");
        tier.setLastName("Nom MSQ2 " + UUID.randomUUID().toString().substring(0, 8));
        tier.setCfgTierType(pro);
        return tierRepository.save(tier);
    }

    private ComProspection prospection(CfgTier prospect, CfgTranche tranche) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-MSQ2-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        p.setCfgProspect(prospect);
        p.setStkHierarchy(tranche);
        return prospectionRepository.save(p);
    }

    private ComProspection preProspection() {
        ComProspection p = prospection(prospect(), null);
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        return prospectionRepository.save(p);
    }

    private String corpsCreation(UUID trancheId, UUID articleId) {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORI-MSQ2-" + suffixe);
        origine.setDescription("Facebook");
        origineRepository.save(origine);
        ComActionMessageType typeContact = new ComActionMessageType();
        typeContact.setId(UUID.randomUUID());
        typeContact.setCode("TYP-MSQ2-" + suffixe);
        typeContact.setDescription("Message entrant");
        typeContactRepository.save(typeContact);
        ComProspectionKind nature = new ComProspectionKind();
        nature.setId(UUID.randomUUID());
        nature.setCode("NAT-MSQ2-" + suffixe);
        nature.setDescription("Qualifie");
        natureRepository.save(nature);
        return corps().avec("prospectId", prospect().getId()).avec(TRANCHE_ID, trancheId).avec(ARTICLE_ID, articleId)
                .avec("origineId", origine.getId()).avec("typeContactId", typeContact.getId())
                .avec("etapeId", nature.getId()).avec("notes", "Interesse par un T3").json();
    }

    private String corpsDossier(UUID articleId) {
        statuts.reservation();
        ComProspection p = prospection(prospect(), null);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-MSQ2-" + UUID.randomUUID().toString().substring(0, 8));
        modePaie = modePaieRepository.save(modePaie);
        return corps().avec("prospectionId", p.getId()).avec(ARTICLE_IDS, List.of(articleId))
                .avec("modePaieId", modePaie.getId()).avec("cin", "CIN-MSQ2").json();
    }

    private String csv(String formulaire) {
        return ENTETE_CSV + "09/23/2026 1:45am,Imane Msq " + UUID.randomUUID().toString().substring(0, 8)
                + ",imane.msq2." + UUID.randomUUID().toString().substring(0, 8) + "@test.local,Payé," + formulaire
                + ",E-mail,À évaluer,Unassigned,,+2126634" + (10000 + Math.floorMod(UUID.randomUUID().hashCode(), 89999)) + ",,\n";
    }

    private MockMultipartFile fichier(String contenu) {
        return new MockMultipartFile("fichier", "leads.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions json(MockHttpServletRequestBuilder requete, String corps, RequestPostProcessor qui)
            throws Exception {
        return mockMvc.perform(requete.with(qui).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ResultActions json(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui)
            throws Exception {
        return json(requete, corps.json(), qui);
    }

    /** Requête rejouable : chaque appel bâtit une requête neuve pour l'appelant donné. */
    @FunctionalInterface
    private interface Appel {
        ResultActions par(RequestPostProcessor qui) throws Exception;
    }

    private static String refuseSurLeChamp(ResultActions resultat, String champ, String codeMasque) throws Exception {
        MvcResult r = resultat.andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(allOf(containsString(champ), not(containsString(codeMasque)),
                        not(containsStringIgnoringCase("inactive")))))
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), DETAIL);
    }

    /** B.3 n° 2 et 3 : Administrateur puis Commercial affecté au projet masqué → le même 400 (pas de 403). */
    private void memeRefusPourAdminEtCommercial(Appel appel, String champ, String codeMasque) throws Exception {
        String pourAdmin = refuseSurLeChamp(appel.par(connecte(admin)), champ, codeMasque);
        String pourCommercial = refuseSurLeChamp(appel.par(commeCommercial()), champ, codeMasque);
        assertThat(pourCommercial).isEqualTo(pourAdmin);
    }

    /** B.3 n° 4 : entité réactivée → l'appel réussit. */
    private void reussitApresReactivation(Supplier<Appel> appel) throws Exception {
        reactiver();
        appel.get().par(connecte(admin)).andExpect(status().is2xxSuccessful());
    }

    private static String detail(ResultActions r) throws Exception {
        return JsonPath.read(r.andExpect(status().isBadRequest()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), DETAIL);
    }

    // ------------------------------------------------------------------ défauts de fuite (réponses qui reflètent le masqué)

    /**
     * B.1 « aucune simulation ne doit … refléter (DTO de réponse) une tranche … masqué » : l'aperçu d'import propose la
     * tranche désignée par le premier mot du formulaire, lue par {@code findFirstByCode} sans filtre d'entité.
     */
    @Test
    void site7_apercuImport_formulaireDUneTrancheMasquee_aucuneTrancheSuggeree() throws Exception {
        mockMvc.perform(multipart(IMPORT + "/apercu").file(fichier(csv(trancheMasquee.getCode() + " FORM")))
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheSuggereeId").doesNotExist())
                .andExpect(jsonPath("$.trancheSuggereeCode").doesNotExist());
    }

    /** Même lecture à l'import réel : ni suggestion ni avertissement ne citent l'id ou le code de la tranche masquée. */
    @Test
    void site7_import_trancheActive_formulaireMasque_rapportSansLaTrancheMasquee() throws Exception {
        String rapport = mockMvc.perform(multipart(IMPORT).file(fichier(csv(trancheMasquee.getCode() + " FORM")))
                        .param(TRANCHE_ID, trancheActive.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat((Object) JsonPath.read(rapport, "$.trancheSuggereeId")).isNull();
        List<String> avertissements = JsonPath.read(rapport, "$.avertissements");
        assertThat(String.join(" | ", avertissements)).doesNotContain(trancheMasquee.getCode());
    }

    /**
     * Site 12, chemin « depuis une offre » : {@code OffreOrigineDossier.verifier} lit l'article de l'offre avant
     * {@code ObjetsVisibles} ; si l'offre porte un article masqué et que le corps en désigne un autre, le 400 cite le
     * code de l'article masqué.
     */
    @Test
    void site12_creationDossierDepuisOffreSurArticleMasque_messageNeCitePasLArticleMasque() throws Exception {
        statuts.reservation();
        ComProspection p = prospection(prospect(), null);
        StlQuote offre = new StlQuote();
        offre.setId(UUID.randomUUID());
        offre.setCode("OFF-MSQ2-" + jeton);
        offre.setDocDate(LocalDateTime.of(2026, 9, 2, 10, 0));
        offre.setComProspection(p);
        offre.setCfgTranche(trancheMasquee);
        offre.setStkItem(articleMasque);
        offre.setStkVocation(vocation);
        quoteRepository.save(offre);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-MSQ2-" + jeton);
        modePaie = modePaieRepository.save(modePaie);

        json(post("/api/dossiers"), corps().avec("prospectionId", p.getId()).avec("offreId", offre.getId())
                        .avec(ARTICLE_IDS, List.of(articleActif.getId())).avec("modePaieId", modePaie.getId())
                        .avec("cin", "CIN-MSQ2"), connecte(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(not(containsString(articleMasque.getCode()))));
    }

    // ------------------------------------------------------------------ inconnu = masqué (réponse 5 du test-writer)

    @Test
    void site1_trancheInconnueEtTrancheMasquee_memeDetail() throws Exception {
        String masquee = detail(json(post(PROSPECTIONS), corpsCreation(trancheMasquee.getId(), null), connecte(admin)));
        String inconnue = detail(json(post(PROSPECTIONS), corpsCreation(UUID.randomUUID(), null), connecte(admin)));
        assertThat(masquee).isEqualTo(inconnue);
    }

    @Test
    void site2_articleInconnuEtArticleMasque_memeDetail() throws Exception {
        String masque = detail(json(post(PROSPECTIONS), corpsCreation(trancheActive.getId(), articleMasque.getId()),
                connecte(admin)));
        String inconnu = detail(json(post(PROSPECTIONS), corpsCreation(trancheActive.getId(), UUID.randomUUID()),
                connecte(admin)));
        assertThat(masque).isEqualTo(inconnu);
    }

    @Test
    void site12_articleInconnuEtArticleMasque_memeDetail() throws Exception {
        String masque = detail(json(post("/api/dossiers"), corpsDossier(articleMasque.getId()), connecte(admin)));
        String inconnu = detail(json(post("/api/dossiers"), corpsDossier(UUID.randomUUID()), connecte(admin)));
        assertThat(masque).isEqualTo(inconnu);
    }

    // ------------------------------------------------------------------ Commercial affecté : même 400, pas de 403

    @Test
    void site1_commercialAffecte_memeRefus() throws Exception {
        memeRefusPourAdminEtCommercial(qui -> json(post(PROSPECTIONS), corpsCreation(trancheMasquee.getId(), null), qui),
                TRANCHE_ID, trancheMasquee.getCode());
    }

    @Test
    void site2_commercialAffecte_memeRefus() throws Exception {
        memeRefusPourAdminEtCommercial(qui -> json(post(PROSPECTIONS),
                corpsCreation(trancheActive.getId(), articleMasque.getId()), qui), ARTICLE_ID, articleMasque.getCode());
    }

    @Test
    void site3_commercialAffecte_memeRefus() throws Exception {
        memeRefusPourAdminEtCommercial(qui -> json(post(PRE_PROSPECTIONS),
                        corps().avec("nom", "Lead MSQ2 " + UUID.randomUUID()).avec(TRANCHE_ID, trancheMasquee.getId()), qui),
                TRANCHE_ID, trancheMasquee.getCode());
    }

    @Test
    void site5_commercialAffecte_memeRefus() throws Exception {
        ComProspection p = prospection(null, trancheActive);
        memeRefusPourAdminEtCommercial(qui -> json(patch("/api/prospections/{id}/tranche-principale", p.getId()),
                corps().avec(TRANCHE_ID, trancheMasquee.getId()), qui), TRANCHE_ID, trancheMasquee.getCode());
    }

    @Test
    void site6_commercialAffecte_memeRefus() throws Exception {
        ComProspection p = prospection(null, trancheActive);
        memeRefusPourAdminEtCommercial(qui -> json(post("/api/prospections/{id}/autres-tranches", p.getId()),
                corps().avec(TRANCHE_ID, trancheMasquee.getId()), qui), TRANCHE_ID, trancheMasquee.getCode());
    }

    @Test
    void site7_commercialAffecte_memeRefus() throws Exception {
        memeRefusPourAdminEtCommercial(qui -> mockMvc.perform(multipart(IMPORT).file(fichier(csv("FORM")))
                .param(TRANCHE_ID, trancheMasquee.getId().toString()).with(qui)), TRANCHE_ID, trancheMasquee.getCode());
    }

    @Test
    void site8_commercialAffecte_memeRefus() throws Exception {
        ComProspection p = prospection(prospect(), trancheActive);
        memeRefusPourAdminEtCommercial(qui -> json(post(DEVIS, p.getId()),
                        corps().avec(TRANCHE_ID, trancheMasquee.getId()).avec(PRIX, new BigDecimal("300000")), qui),
                TRANCHE_ID, trancheMasquee.getCode());
    }

    @Test
    void site10_commercialAffecte_memeRefus() throws Exception {
        ComProspection p = prospection(prospect(), trancheActive);
        memeRefusPourAdminEtCommercial(qui -> json(post(DEVIS, p.getId()),
                        corps().avec(TRANCHE_ID, trancheActive.getId()).avec(ARTICLE_ID, articleMasque.getId()), qui),
                ARTICLE_ID, articleMasque.getCode());
    }

    @Test
    void site11_commercialAffecte_memeRefus() throws Exception {
        ComProspection p = prospection(prospect(), trancheActive);
        StkItem sousArticle = article("SA-MSQ2-" + jeton, trancheMasquee, articleActif);
        memeRefusPourAdminEtCommercial(qui -> json(post(DEVIS, p.getId()), corps().avec(TRANCHE_ID, trancheActive.getId())
                        .avec(ARTICLE_ID, articleActif.getId()).avec("sousArticleIds", List.of(sousArticle.getId())), qui),
                "sousArticleIds", sousArticle.getCode());
    }

    @Test
    void site12_commercialAffecte_memeRefus() throws Exception {
        memeRefusPourAdminEtCommercial(qui -> json(post("/api/dossiers"), corpsDossier(articleMasque.getId()), qui),
                ARTICLE_IDS, articleMasque.getCode());
    }

    @Test
    void site13_commercialAffecte_memeRefus() throws Exception {
        memeRefusPourAdminEtCommercial(qui -> json(post(SIMULATION),
                        corps().avec(ARTICLE_IDS, List.of(articleMasque.getId())).avec(PRIX, new BigDecimal("1548000")), qui),
                ARTICLE_IDS, articleMasque.getCode());
    }

    // ------------------------------------------------------------------ entité réactivée : l'appel réussit

    @Test
    void site1_reactivee_succes() throws Exception {
        reussitApresReactivation(() -> qui -> json(post(PROSPECTIONS), corpsCreation(trancheMasquee.getId(), null), qui));
    }

    @Test
    void site2_reactivee_succes() throws Exception {
        reussitApresReactivation(() -> qui -> json(post(PROSPECTIONS),
                corpsCreation(trancheMasquee.getId(), articleMasque.getId()), qui));
    }

    @Test
    void site3_reactivee_succes() throws Exception {
        reussitApresReactivation(() -> qui -> json(post(PRE_PROSPECTIONS),
                corps().avec("nom", "Lead MSQ2 " + UUID.randomUUID()).avec(TRANCHE_ID, trancheMasquee.getId()), qui));
    }

    @Test
    void site4_reactivee_succes() throws Exception {
        ComProspection pre = preProspection();
        CfgTier vendeur = new CfgTier();
        vendeur.setId(UUID.randomUUID());
        vendeur.setCode("COM-MSQ2-" + jeton);
        vendeur.setFirstName("Prenom");
        vendeur.setLastName("Commercial " + jeton);
        vendeur = tierRepository.save(vendeur);
        CfgTier commercialId = vendeur;
        reussitApresReactivation(() -> qui -> json(post("/api/prospections/{id}/transformer", pre.getId()),
                corps().avec("commercialId", commercialId.getId()).avec(TRANCHE_ID, trancheMasquee.getId()), qui));
    }

    @Test
    void site5_reactivee_succes() throws Exception {
        ComProspection p = prospection(null, trancheActive);
        reussitApresReactivation(() -> qui -> json(patch("/api/prospections/{id}/tranche-principale", p.getId()),
                corps().avec(TRANCHE_ID, trancheMasquee.getId()).avec(ARTICLE_ID, articleMasque.getId()), qui));
    }

    @Test
    void site6_reactivee_succes() throws Exception {
        ComProspection p = prospection(null, trancheActive);
        reussitApresReactivation(() -> qui -> json(post("/api/prospections/{id}/autres-tranches", p.getId()),
                corps().avec(TRANCHE_ID, trancheMasquee.getId()).avec(ARTICLE_ID, articleMasque.getId()), qui));
    }

    @Test
    void site7_reactivee_succes() throws Exception {
        reussitApresReactivation(() -> qui -> mockMvc.perform(multipart(IMPORT).file(fichier(csv("FORM")))
                .param(TRANCHE_ID, trancheMasquee.getId().toString()).with(qui)));
    }

    @Test
    void site8et10_reactivee_succes() throws Exception {
        ComProspection p = prospection(prospect(), null);
        reussitApresReactivation(() -> qui -> json(post(DEVIS, p.getId()), corps().avec(TRANCHE_ID, trancheMasquee.getId())
                .avec(ARTICLE_ID, articleMasque.getId()).avec(PRIX, new BigDecimal("300000")), qui));
    }

    @Test
    void site9_reactivee_succes() throws Exception {
        ComProspection p = prospection(prospect(), null);
        String creee = json(post(DEVIS, p.getId()),
                corps().avec(TRANCHE_ID, trancheActive.getId()).avec(PRIX, new BigDecimal("300000")), connecte(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID offre = UUID.fromString(JsonPath.read(creee, "$.id"));
        reussitApresReactivation(() -> qui -> json(patch("/api/devis/{id}", offre),
                corps().avec(TRANCHE_ID, trancheMasquee.getId()), qui));
    }

    @Test
    void site11_reactivee_succes() throws Exception {
        ComProspection p = prospection(prospect(), null);
        StkItem sousArticle = article("SA-MSQ2-" + jeton, trancheMasquee, articleMasque);
        reussitApresReactivation(() -> qui -> json(post(DEVIS, p.getId()), corps().avec(TRANCHE_ID, trancheMasquee.getId())
                .avec(ARTICLE_ID, articleMasque.getId()).avec("sousArticleIds", List.of(sousArticle.getId()))
                .avec(PRIX, new BigDecimal("300000")), qui));
    }

    @Test
    void site12_reactivee_succes() throws Exception {
        String corps = corpsDossier(articleMasque.getId());
        reussitApresReactivation(() -> qui -> json(post("/api/dossiers"), corps, qui));
    }

    @Test
    void site13_reactivee_succes() throws Exception {
        reussitApresReactivation(() -> qui -> json(post(SIMULATION),
                corps().avec(ARTICLE_IDS, List.of(articleMasque.getId())).avec(PRIX, new BigDecimal("1548000")), qui));
    }
}
