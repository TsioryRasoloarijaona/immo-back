package com.tsix.immocrm.controller.perimetre;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Troisième passe adverse — attaque le <b>refus en fermeture</b> (§9ter) et la réécriture de la
 * hotline, pas les contournements déjà corrigés.
 *
 * <p>Trois questions posées au nouveau design :
 * <ul>
 *   <li>Le refus arrête-t-il réellement la chaîne, et le corps écrit à la main est-il indiscernable
 *       de celui de {@code GlobalExceptionHandler} (le front n'a qu'une seule façon de lire une
 *       erreur) ?</li>
 *   <li>Le changement de contrat « 404 au lieu de 400 sur id malformé » est-il resté <b>confiné</b>
 *       aux quatre collections scopées, et n'ouvre-t-il pas d'oracle ?</li>
 *   <li>Le filtre hotline, désormais dans la requête, plafonne-t-il bien à 50 lignes
 *       <em>visibles</em> — et le {@code coalesce} prospection→dossier est-il correct en SQL sur les
 *       formes réelles ?</li>
 * </ul>
 * L'invariant structurel (« l'id est au segment 3 »), lui, est verrouillé par
 * {@link PerimetreInterceptorInvarianteTest}, qui échoue au build si une route le viole.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerimetreCommercialRevision2Test {

    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComActionMessageRepository actionMessageRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private CfgProject projetA;
    private CfgProject projetB;
    private CfgTranche trancheA;
    private CfgTranche trancheB;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-R2-" + suffixe());
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-R2-" + suffixe());
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-R2-" + suffixe());
        stkVocationRepository.save(vocation);

        projetA = newProjet("R2-A");
        projetB = newProjet("R2-B");
        trancheA = newTranche("TR-R2-A", projetA);
        trancheB = newTranche("TR-R2-B", projetB);
    }

    // ================================================================= fixtures

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private CfgProject newProjet(String code) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Projet " + code);
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(String code, CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private AdmUser newUser(String prefixe) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(prefixe + "." + suffixe());
        user.setDisabled(false);
        return userRepository.save(user);
    }

    private void affecter(AdmUser user, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(user);
        habilitation.setCfgProject(projet);
        habilitationRepository.save(habilitation);
    }

    private RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private CfgTier newTier(String code, String telephone) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setPhone(telephone);
        return tierRepository.save(tier);
    }

    private StkItem newArticle(String code, CfgTranche tranche) {
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        return stkItemRepository.save(article);
    }

    private ComFolder newDossier(String code, CfgTranche tranche, CfgTier client) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        folder.setDocDate(LocalDateTime.now());
        folder.setTotalToPay(new BigDecimal("100000"));
        folder.setCfgCustomer(client);
        folderRepository.save(folder);

        StkItem article = newArticle("LOT-" + code, tranche);
        article.setComFolderId(folder.getId());
        stkItemRepository.save(article);

        folder.setStkItem(article);
        return folderRepository.save(folder);
    }

    private ComProspection newProspection(String code, CfgTranche tranche, CfgTier prospect) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        prospection.setCfgProspect(prospect);
        prospection.setStkHierarchy(tranche);
        prospection.setDocDate(LocalDateTime.now());
        return prospectionRepository.save(prospection);
    }

    /** Pré-prospection identifiée uniquement par son TempTel — la source de {@code appelsSansFiche}. */
    private ComProspection newPreProspectionTempTel(String code, CfgTranche tranche, String telephone) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        prospection.setStkHierarchy(tranche);
        prospection.setTempTel(telephone);
        prospection.setDocDate(LocalDateTime.now());
        return prospectionRepository.save(prospection);
    }

    private ComActionMessage newInteraction(CfgTier tier, ComProspection prospection, ComFolder dossier,
                                             LocalDateTime date, String description) {
        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setCfgTier(tier);
        message.setComProspection(prospection);
        message.setComFolder(dossier);
        message.setDocDate(date);
        message.setDescription(description);
        return actionMessageRepository.save(message);
    }

    // ============================================ A. le refus arrête-t-il vraiment la chaîne ?

    /**
     * Le refus s'écrit avec {@code setStatus} + {@code return false}, pas par une exception. Il faut
     * donc vérifier que le handler n'a bel et bien pas tourné : un {@code preHandle} qui renverrait
     * false après que quelque chose ait déjà écrit dans la réponse produirait un corps mixte.
     */
    @Test
    void idMalformeSurCollectionScopee_renvoie404EtRienDautre() throws Exception {
        AdmUser commercial = newUser("r2.malforme");
        affecter(commercial, projetA);

        var reponse = mockMvc.perform(get(URI.create("/api/dossiers/pas-un-uuid"))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus()).isEqualTo(404);
        assertThat(reponse.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(reponse.getContentAsString())
                .as("le corps ne doit pas refléter la valeur fournie par l'appelant")
                .doesNotContain("pas-un-uuid");
    }

    /**
     * Le corps écrit à la main doit être indiscernable, en forme, de celui que
     * {@code GlobalExceptionHandler} produit pour un 404 — le front n'a qu'une seule façon de lire
     * une erreur RFC 7807. On compare les clés, pas le texte.
     */
    @Test
    void corpsDuRefus_memeFormeQueGlobalExceptionHandler() throws Exception {
        AdmUser commercial = newUser("r2.forme");
        affecter(commercial, projetA);

        var refus = mockMvc.perform(get(URI.create("/api/dossiers/pas-un-uuid"))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();
        var handler = mockMvc.perform(get("/api/dossiers/{id}", UUID.randomUUID())
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(handler.getStatus()).isEqualTo(404);
        assertThat(clesJson(refus.getContentAsString()))
                .as("clés RFC 7807 du refus intercepteur vs celles de GlobalExceptionHandler")
                .isEqualTo(clesJson(handler.getContentAsString()));
        assertThat(refus.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(handler.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    private static List<String> clesJson(String json) {
        return java.util.regex.Pattern.compile("\"([a-zA-Z]+)\"\\s*:").matcher(json).results()
                .map(r -> r.group(1)).sorted().toList();
    }

    /**
     * Le changement de contrat (404 au lieu de 400 sur id malformé) doit rester <b>confiné</b> aux
     * quatre collections scopées : ailleurs, un id malformé reste un 400. Sinon la régression
     * s'étend silencieusement à toute l'API.
     */
    @Test
    void idMalformeHorsCollectionScopee_resteUn400() throws Exception {
        AdmUser commercial = newUser("r2.horschamp");
        affecter(commercial, projetA);

        for (String route : List.of("/api/prospects/pas-un-uuid", "/api/clients/pas-un-uuid",
                "/api/articles/pas-un-uuid", "/api/projets/pas-un-uuid", "/api/tranches/pas-un-uuid")) {
            int statut = mockMvc.perform(get(URI.create(route)).with(connecte(commercial, "Commercial")))
                    .andReturn().getResponse().getStatus();
            assertThat(statut).as("route non scopée : %s", route).isEqualTo(400);
        }
    }

    /**
     * Le refus ne doit pas créer d'oracle : pour un appelant restreint, un id malformé, un id
     * inconnu et un id hors périmètre doivent tous les trois répondre 404. Et un appelant
     * <b>non</b> restreint doit recevoir exactement le même statut sur l'id malformé — sinon le
     * code de retour trahit à lui seul l'état d'affectation du compte.
     */
    @Test
    void refus_nOuvreAucunOracle() throws Exception {
        AdmUser restreint = newUser("r2.oracle.restreint");
        affecter(restreint, projetA);
        AdmUser libre = newUser("r2.oracle.libre");
        ComFolder dossierB = newDossier("DOS-R2-ORACLE-B", trancheB, newTier("CLI-R2-ORACLE", null));

        UUID idInconnu = UUID.randomUUID();
        var malformeRestreint = reponse(get(URI.create("/api/dossiers/pas-un-uuid")), restreint);
        var inconnuRestreint = reponse(get("/api/dossiers/{id}", idInconnu), restreint);
        var horsPerimetre = reponse(get("/api/dossiers/{id}", dossierB.getId()), restreint);
        var malformeLibre = reponse(get(URI.create("/api/dossiers/pas-un-uuid")), libre);

        assertThat(malformeRestreint.getStatus()).isEqualTo(404);
        assertThat(inconnuRestreint.getStatus()).isEqualTo(404);
        assertThat(horsPerimetre.getStatus()).isEqualTo(404);
        assertThat(malformeLibre.getStatus())
                .as("le statut sur id malformé ne doit pas dépendre de l'état d'affectation du compte")
                .isEqualTo(malformeRestreint.getStatus());

        // L'oracle qui compte : un id hors périmètre doit être indiscernable d'un id inexistant,
        // corps compris — pas seulement au niveau du statut. Sinon les ids restent énumérables.
        assertThat(horsPerimetre.getContentAsString().replace(dossierB.getId().toString(), "<ID>"))
                .as("corps d'un dossier hors périmètre vs corps d'un id inexistant")
                .isEqualTo(inconnuRestreint.getContentAsString().replace(idInconnu.toString(), "<ID>"));
    }

    private org.springframework.mock.web.MockHttpServletResponse reponse(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
            AdmUser user) throws Exception {
        return mockMvc.perform(requete.with(connecte(user, "Commercial"))).andReturn().getResponse();
    }

    /**
     * Le saut structurel doit rester opérant : les routes scopées à segment littéral ne doivent pas
     * être happées par le refus. {@code simulation-financement} ne charge aucune ressource scopée.
     */
    @Test
    void routeScopeeASegmentLitteral_nEstPasRefusee() throws Exception {
        AdmUser commercial = newUser("r2.litteral");
        affecter(commercial, projetA);
        StkItem articleA = newArticle("LOT-R2-SIMU-A", trancheA);

        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .with(connecte(commercial, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[\"%s\"],\"prix\":100000}".formatted(articleA.getId())))
                .andExpect(status().isOk());
    }

    /**
     * Les interceptors MVC tournent aussi sur la requête de pré-vol CORS (Spring conserve la chaîne
     * d'interceptors pour le {@code PreFlightHandler}). Un pré-vol ne porte jamais le cookie, donc
     * jamais de principal : il doit continuer à répondre 200, sinon tout le front casse en
     * cross-origin sur les routes de dossier.
     */
    @Test
    void prevolCors_surUneRouteScopee_resteAutorise() throws Exception {
        ComFolder dossier = newDossier("DOS-R2-PREVOL", trancheA, newTier("CLI-R2-PREVOL", null));

        mockMvc.perform(options("/api/dossiers/{id}", dossier.getId())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    // ============================================ B. hotline : le plafond compte-t-il les lignes visibles ?

    /**
     * Le scénario d'éviction de §9ter.2, rejoué : 50 interactions hors périmètre plus récentes,
     * 1 interaction du périmètre plus ancienne. Filtre en base ⇒ la ligne du périmètre doit
     * apparaître.
     */
    @Test
    void hotline_interactionDuPerimetre_nEstPlusEvinceeParLePlafond() throws Exception {
        AdmUser commercial = newUser("r2.hotline.evince");
        affecter(commercial, projetA);
        CfgTier tier = newTier("CLI-R2-EVINCE", "0611111111");

        ComProspection psnA = newProspection("PSN-R2-EVINCE-A", trancheA, tier);
        ComProspection psnB = newProspection("PSN-R2-EVINCE-B", trancheB, tier);
        for (int i = 0; i < 50; i++) {
            newInteraction(tier, psnB, null, LocalDateTime.now().minusMinutes(i), "hors perimetre " + i);
        }
        ComActionMessage duPerimetre = newInteraction(tier, psnA, null,
                LocalDateTime.now().minusDays(30), "dans le perimetre");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0611111111")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + duPerimetre.getId() + "')]").exists());
    }

    /**
     * Le plafond de 50 doit compter des lignes <b>visibles</b> : avec 60 interactions du périmètre
     * et 60 hors périmètre, on attend 50 lignes, toutes du périmètre — pas 50 moins celles qui ont
     * été filtrées après coup.
     */
    @Test
    void hotline_lePlafondDe50NeCompteQueDesLignesVisibles() throws Exception {
        AdmUser commercial = newUser("r2.hotline.cap");
        affecter(commercial, projetA);
        CfgTier tier = newTier("CLI-R2-CAP", "0622222222");

        ComProspection psnA = newProspection("PSN-R2-CAP-A", trancheA, tier);
        ComProspection psnB = newProspection("PSN-R2-CAP-B", trancheB, tier);
        for (int i = 0; i < 60; i++) {
            newInteraction(tier, psnB, null, LocalDateTime.now().minusMinutes(i), "B" + i);
            newInteraction(tier, psnA, null, LocalDateTime.now().minusMinutes(i), "A" + i);
        }

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0622222222")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions.length()").value(50))
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.description =~ /^B.*/)]").doesNotExist());
    }

    /** §9ter.3 : le projet se résout aussi par le dossier quand la prospection est absente. */
    @Test
    void hotline_interactionRattacheeAUnDossierHorsPerimetre_resteMasquee() throws Exception {
        AdmUser commercial = newUser("r2.hotline.dossier");
        affecter(commercial, projetA);
        CfgTier tier = newTier("CLI-R2-DOSSIER", "0633333333");

        ComFolder dossierB = newDossier("DOS-R2-HOT-B", trancheB, tier);
        ComFolder dossierA = newDossier("DOS-R2-HOT-A", trancheA, tier);
        ComActionMessage surB = newInteraction(tier, null, dossierB, LocalDateTime.now(), "note projet B");
        ComActionMessage surA = newInteraction(tier, null, dossierA, LocalDateTime.now(), "note projet A");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0633333333")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + surB.getId() + "')]").doesNotExist())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + surA.getId() + "')]").exists());
    }

    /**
     * §6.4 ne doit s'appliquer qu'une fois les <b>deux</b> chemins épuisés : une interaction sans
     * prospection ni dossier reste visible (1581 lignes réelles sont dans ce cas), et une
     * interaction dont la prospection existe mais n'a pas de tranche reste visible elle aussi —
     * c'est le rôle des jointures LEFT, une jointure interne la ferait disparaître.
     */
    @Test
    void hotline_interactionSansProjetResoluble_resteVisible() throws Exception {
        AdmUser commercial = newUser("r2.hotline.orphelin");
        affecter(commercial, projetA);
        CfgTier tier = newTier("CLI-R2-ORPHELIN", "0644444444");

        ComActionMessage sansRien = newInteraction(tier, null, null, LocalDateTime.now(), "appel libre");
        ComProspection psnSansTranche = newProspection("PSN-R2-SANSTR", null, tier);
        ComActionMessage surProspectionSansTranche =
                newInteraction(tier, psnSansTranche, null, LocalDateTime.now(), "prospection sans tranche");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0644444444")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + sansRien.getId() + "')]").exists())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='"
                        + surProspectionSansTranche.getId() + "')]").exists());
    }

    /**
     * {@code appelsSansFiche} : le filtre doit être dans la requête. Avec 50 pré-prospections hors
     * périmètre portant le même numéro et 1 dans le périmètre, cette dernière doit remonter — elle
     * est justement la seule que la requête filtrée doit ramener.
     */
    @Test
    void hotline_appelsSansFiche_filtresDansLaRequeteAvantLePlafond() throws Exception {
        AdmUser commercial = newUser("r2.hotline.temptel");
        affecter(commercial, projetA);

        for (int i = 0; i < 50; i++) {
            newPreProspectionTempTel("PRE-R2-TT-B" + i, trancheB, "0655555555");
        }
        ComProspection dansPerimetre = newPreProspectionTempTel("PRE-R2-TT-A", trancheA, "0655555555");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0655555555")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsSansFiche[?(@.id=='" + dansPerimetre.getId() + "')]").exists())
                .andExpect(jsonPath("$.appelsSansFiche.length()").value(1));
    }

    /** Et une pré-prospection sans tranche reste visible (§6.4, jointure LEFT dans la requête filtrée). */
    @Test
    void hotline_appelSansFicheSansTranche_resteVisible() throws Exception {
        AdmUser commercial = newUser("r2.hotline.tt.null");
        affecter(commercial, projetA);

        ComProspection sansTranche = newPreProspectionTempTel("PRE-R2-TT-NULL", null, "0666666666");
        newPreProspectionTempTel("PRE-R2-TT-NULL-B", trancheB, "0666666666");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0666666666")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsSansFiche[?(@.id=='" + sansTranche.getId() + "')]").exists())
                .andExpect(jsonPath("$.appelsSansFiche.length()").value(1));
    }

    /** Un compte non restreint doit continuer à tout voir sur la hotline (non-régression §6.3). */
    @Test
    void hotline_compteNonRestreint_voitToutesLesInteractions() throws Exception {
        AdmUser libre = newUser("r2.hotline.libre");
        CfgTier tier = newTier("CLI-R2-LIBRE", "0677777777");

        ComProspection psnB = newProspection("PSN-R2-LIBRE-B", trancheB, tier);
        ComActionMessage surB = newInteraction(tier, psnB, null, LocalDateTime.now(), "note B");
        ComFolder dossierB = newDossier("DOS-R2-LIBRE-B", trancheB, tier);
        ComActionMessage surDossierB = newInteraction(tier, null, dossierB, LocalDateTime.now(), "note dossier B");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0677777777")
                        .with(connecte(libre, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + surB.getId() + "')]").exists())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + surDossierB.getId() + "')]").exists());
    }
}
