package com.tsix.immocrm.controller.perimetre;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Passe adverse sur la frontière d'autorisation "Périmètre commercial".
 *
 * <p>{@code PerimetreCommercialTest} couvre le chemin nominal de la feature ; cette classe attaque
 * ce que ce chemin ne montre pas :
 * <ul>
 *   <li><b>Groupe A</b> — {@code PerimetreInterceptor} découpe l'URL brute à la main
 *       ({@code getRequestURI().split("/")}) alors que Spring MVC, lui, <i>décode</i> et
 *       <i>déparamètre</i> chaque segment avant d'alimenter {@code @PathVariable}. Les deux
 *       lectures de la même URL peuvent donc diverger : c'est le mode de défaillance classique du
 *       contrôle d'accès par analyse de chaîne d'URL.</li>
 *   <li><b>Groupe B</b> — surfaces qui exposent des dossiers / prospections / règlements
 *       <i>en dehors</i> des quatre préfixes interceptés.</li>
 *   <li><b>Groupe C</b> — écritures dont seule une partie des projets nommés est contrôlée.</li>
 *   <li><b>Groupe D</b> — {@code PUT /api/utilisateurs/{id}/projets} face aux formes réelles de
 *       {@code AdmUserHabilitation} (lignes multi-portées, doublons).</li>
 *   <li><b>Groupe E</b> — résolution du périmètre (unicité, encodage des codes de rôle).</li>
 * </ul>
 *
 * <p>Chaque test affirme le comportement <b>attendu</b> d'une frontière d'autorisation correcte :
 * un échec ici est un défaut du code de production, pas un test à assouplir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerimetreCommercialAdversarialTest {

    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private MockMvc mockMvc;
    @Autowired private PerimetreCommercialService perimetreService;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;
    @Autowired private StlQuoteRepository quoteRepository;

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
        itemType.setCode("TYPE-ADV-" + suffixe());
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-ADV-" + suffixe());
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-ADV-" + suffixe());
        stkVocationRepository.save(vocation);

        projetA = newProjet("ADV-A");
        projetB = newProjet("ADV-B");
        trancheA = newTranche("TR-ADV-A", projetA);
        trancheB = newTranche("TR-ADV-B", projetB);
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

    private AdmUserHabilitation affecter(AdmUser user, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(user);
        habilitation.setCfgProject(projet);
        return habilitationRepository.save(habilitation);
    }

    private void donnerRole(AdmUser user, String code) {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        roleRepository.save(role);

        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);
    }

    private RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
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

    /** Dossier rattaché à {@code tranche} par son article principal, et à {@code client}. */
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

    private StlSettlement newReglement(String code, ComFolder dossier, CfgTier tier) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setAmount(new BigDecimal("1000"));
        documentRepository.save(echeance);

        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode(code);
        reglement.setDocDate(LocalDateTime.now());
        reglement.setAmount(new BigDecimal("1000"));
        reglement.setStatus("IN_WORK");
        reglement.setTier(tier);
        settlementRepository.save(reglement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("1000"));
        settlementLineRepository.save(ligne);

        return reglement;
    }

    // ============================================================= A. interceptor / analyse d'URL

    /**
     * <b>Paramètre matriciel.</b> Vérifié : Spring conserve ici {@code ;x=1} dans la valeur du
     * {@code @PathVariable}, la conversion en UUID échoue et la requête se termine en 400 — la
     * ressource n'est donc pas servie. Ce test verrouille ce constat : il doit rester impossible
     * d'obtenir une réponse 2xx par cette forme d'URL, quelle que soit la version de Spring.
     */
    @Test
    void dossierHorsPerimetre_avecParametreMatriciel_nEstPasServi() throws Exception {
        AdmUser commercial = newUser("adv.matrix");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-ADV-MATRIX-B", trancheB, newTier("CLI-ADV-MATRIX"));

        mockMvc.perform(get(URI.create("/api/dossiers/" + dossierB.getId() + ";x=1"))
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().is4xxClientError());
    }

    /**
     * <b>Encodage pourcent — contournement confirmé.</b> {@code %2D} se décode en {@code -} :
     * {@code RequestPath} décode chaque segment avant d'alimenter le {@code @PathVariable}, donc le
     * contrôleur reçoit un UUID parfaitement valide et sert le dossier ; {@code PerimetreInterceptor},
     * lui, lit {@code HttpServletRequest#getRequestURI()} qui reste <b>non décodé</b>, n'y reconnaît
     * pas un UUID et laisse passer sans aucun contrôle. C'est le mode de défaillance attendu d'un
     * contrôle d'accès basé sur l'analyse de la chaîne d'URL.
     */
    @Test
    void dossierHorsPerimetre_avecTiretsEncodes_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("adv.encode");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-ADV-ENC-B", trancheB, newTier("CLI-ADV-ENC"));

        String encode = dossierB.getId().toString().replace("-", "%2D");
        var reponse = mockMvc.perform(get(URI.create("/api/dossiers/" + encode))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("dossier du projet B servi à un commercial restreint au projet A : %s",
                        reponse.getContentAsString())
                .isEqualTo(404);
    }

    /** Même attaque sur les prospections, pour vérifier que ce n'est pas propre aux dossiers. */
    @Test
    void prospectionHorsPerimetre_avecTiretsEncodes_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("adv.encode.psn");
        affecter(commercial, projetA);
        ComProspection psnB = newProspection("PSN-ADV-ENC-B", trancheB, newTier("CLI-ADV-ENC-PSN"));

        String encode = psnB.getId().toString().replace("-", "%2D");
        var reponse = mockMvc.perform(get(URI.create("/api/prospections/" + encode))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("prospection du projet B servie à un commercial restreint au projet A : %s",
                        reponse.getContentAsString())
                .isEqualTo(404);
    }

    /**
     * Variante d'écriture du même contournement : l'échéancier d'un dossier hors périmètre, atteint
     * par une route imbriquée dont le premier segment est encodé.
     */
    @Test
    void routeImbriqueeDunDossierHorsPerimetre_avecTiretsEncodes_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("adv.encode.nested");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-ADV-ENC-NESTED-B", trancheB, newTier("CLI-ADV-ENC-N"));

        String encode = dossierB.getId().toString().replace("-", "%2D");
        var reponse = mockMvc.perform(get(URI.create("/api/dossiers/" + encode + "/echeancier"))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("échéancier du projet B servi à un commercial restreint : %s",
                        reponse.getContentAsString())
                .isEqualTo(404);
    }

    /** Contrôle : un UUID en majuscules doit rester filtré (UUID.fromString l'accepte). */
    @Test
    void dossierHorsPerimetre_uuidEnMajuscules_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("adv.majuscules");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-ADV-MAJ-B", trancheB, newTier("CLI-ADV-MAJ"));

        mockMvc.perform(get(URI.create("/api/dossiers/" + dossierB.getId().toString().toUpperCase()))
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());
    }

    // ============================================== B. surfaces hors des quatre préfixes interceptés

    /**
     * La fiche client renvoie le <b>même</b> {@code DossierListItemDto} que {@code GET /api/dossiers}.
     * {@code GET /api/clients} n'étant pas filtré, il suffit d'énumérer les clients pour reconstituer
     * la liste des dossiers de tous les projets — la frontière posée sur {@code /api/dossiers}
     * devient contournable sans même deviner un id.
     */
    @Test
    void dossierHorsPerimetre_nonExposeParLaFicheClient() throws Exception {
        AdmUser commercial = newUser("adv.client");
        affecter(commercial, projetA);
        CfgTier client = newTier("CLI-ADV-FICHE");
        ComFolder dossierB = newDossier("DOS-ADV-CLIENT-B", trancheB, client);

        mockMvc.perform(get("/api/clients/{id}/dossiers", client.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").doesNotExist());
    }

    /** Idem via la fiche prospect (même service {@code ProspectSubResourcesService}). */
    @Test
    void dossierHorsPerimetre_nonExposeParLaFicheProspect() throws Exception {
        AdmUser commercial = newUser("adv.prospect.dos");
        affecter(commercial, projetA);
        CfgTier prospect = newTier("PRO-ADV-FICHE");
        ComFolder dossierB = newDossier("DOS-ADV-PROSPECT-B", trancheB, prospect);

        mockMvc.perform(get("/api/prospects/{id}/dossiers", prospect.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").doesNotExist());
    }

    @Test
    void prospectionHorsPerimetre_nonExposeeParLaFicheProspect() throws Exception {
        AdmUser commercial = newUser("adv.prospect.psn");
        affecter(commercial, projetA);
        CfgTier prospect = newTier("PRO-ADV-PSN");
        ComProspection psnB = newProspection("PSN-ADV-PROSPECT-B", trancheB, prospect);

        mockMvc.perform(get("/api/prospects/{id}/prospections", prospect.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + psnB.getId() + "')]").doesNotExist());
    }

    @Test
    void reglementHorsPerimetre_nonExposeParLaFicheProspect() throws Exception {
        AdmUser commercial = newUser("adv.prospect.reg");
        affecter(commercial, projetA);
        CfgTier prospect = newTier("PRO-ADV-REG");
        ComFolder dossierB = newDossier("DOS-ADV-REG-B", trancheB, prospect);
        StlSettlement reglementB = newReglement("ENC-ADV-REG-B", dossierB, prospect);

        mockMvc.perform(get("/api/prospects/{id}/reglements", prospect.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + reglementB.getId() + "')]").doesNotExist());
    }

    /**
     * {@code GET /api/devis} est une liste globale d'offres portant tranche, commercial et montants,
     * et pointant vers la prospection d'origine. Elle n'est pas sous un préfixe intercepté.
     */
    @Test
    void devisHorsPerimetre_nonExposeParLaListeGlobale() throws Exception {
        AdmUser commercial = newUser("adv.devis");
        affecter(commercial, projetA);
        CfgTier prospect = newTier("PRO-ADV-DEVIS");
        ComProspection psnB = newProspection("PSN-ADV-DEVIS-B", trancheB, prospect);

        StlQuote devisB = new StlQuote();
        devisB.setId(UUID.randomUUID());
        devisB.setCode("DEV-ADV-B");
        devisB.setDocDate(LocalDateTime.now());
        devisB.setCfgTranche(trancheB);
        devisB.setCfgProspect(prospect);
        devisB.setComProspection(psnB);
        quoteRepository.save(devisB);

        mockMvc.perform(get("/api/devis").param("trancheId", trancheB.getId().toString())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + devisB.getId() + "')]").doesNotExist());
    }

    /**
     * {@code GET /api/hotline/recherche} agrège, pour un numéro de téléphone, <b>tout</b> ce que le
     * CRM sait du tier : prospections, dossiers, règlements, interactions — via le même
     * {@code ProspectSubResourcesService} que la fiche prospect, donc sans périmètre. Un commercial
     * restreint y lit le pipeline complet des autres projets en une requête, sans connaître un seul
     * id.
     */
    @Test
    void dossierHorsPerimetre_nonExposeParLaHotline() throws Exception {
        AdmUser commercial = newUser("adv.hotline");
        affecter(commercial, projetA);

        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("CLI-ADV-HOTLINE");
        tier.setPhone("0612345678");
        tierRepository.save(tier);

        ComFolder dossierB = newDossier("DOS-ADV-HOTLINE-B", trancheB, tier);

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0612345678")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[*].dossiers[?(@.id=='" + dossierB.getId() + "')]").doesNotExist());
    }

    /**
     * Les générateurs de documents sont ouverts en {@code permitAll} par {@code SecurityConfig}
     * ({@code /api/dossiers/*}{@code /documents/**}). Sans authentification il n'y a pas de
     * principal, donc pas de périmètre : tout le contenu d'un dossier (client, prix, échéancier)
     * est servi en PDF à un appelant anonyme, quel que soit le projet. Le point de contrôle unique
     * revendiqué par {@code PerimetreInterceptor} n'a alors rien à contrôler.
     */
    @Test
    void documentPdfDunDossier_exigeUneAuthentification() throws Exception {
        ComFolder dossierB = newDossier("DOS-ADV-PDF-B", trancheB, newTier("CLI-ADV-PDF"));

        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", dossierB.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================= C. écritures

    /**
     * {@code POST /api/dossiers} ne contrôle que {@code articleIds[0]}. Avec un article principal
     * dans le périmètre et une annexe hors périmètre, la création réussit <b>et</b> l'annexe du
     * projet B est marquée occupée ({@code StkItem.ComFolderId}) — une écriture dans le stock d'un
     * projet auquel l'appelant n'a pas accès.
     */
    @Test
    void creationDeDossier_avecUneAnnexeHorsPerimetre_renvoie403() throws Exception {
        AdmUser commercial = newUser("adv.create.annexe");
        affecter(commercial, projetA);

        CfgTier prospect = newTier("PRO-ADV-CREATE");
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-ADV-CREATE");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        StkItem principalA = newArticle("LOT-ADV-PRINCIPAL-A", trancheA);
        StkItem annexeB = newArticle("LOT-ADV-ANNEXE-B", trancheB);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("MODE-ADV-" + suffixe());
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s","%s"],"modePaieId":"%s","cin":"101112131415"}
                """.formatted(prospection.getId(), principalA.getId(), annexeB.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").with(connecte(commercial, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        assertThat(stkItemRepository.findById(annexeB.getId()).orElseThrow().getComFolderId())
                .as("un article du projet B ne doit pas être rattaché à un dossier créé hors de son périmètre")
                .isNull();
    }

    // ================================================================= D. PUT .../projets

    /**
     * Forme réelle mesurée sur {@code yesRems} : 2 lignes portent à la fois {@code CfgCompanyId} et
     * {@code CfgProjectId} (compte {@code Mkamara}). {@code findByAdmUserIdAndCfgProjectIsNotNull}
     * les sélectionne, {@code deleteAll} les efface, et les lignes recréées ne portent que le
     * projet : la portée société du légataire disparaît silencieusement. C'est exactement le
     * scénario que §7.2 interdit.
     */
    @Test
    void putProjets_preserveLaPorteeSocieteDuneLigneMixte() throws Exception {
        AdmUser cible = newUser("adv.put.mixte");
        AdmUser admin = newUser("adv.put.mixte.admin");

        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("STE-ADV-" + suffixe());
        companyRepository.save(societe);

        AdmUserHabilitation ligneMixte = new AdmUserHabilitation();
        ligneMixte.setId(UUID.randomUUID());
        ligneMixte.setAdmUser(cible);
        ligneMixte.setCfgCompany(societe);
        ligneMixte.setCfgProject(projetB);
        habilitationRepository.save(ligneMixte);

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projetIds\":[\"%s\"]}".formatted(projetA.getId())))
                .andExpect(status().isOk());

        List<AdmUserHabilitation> restantes = habilitationRepository.findAll().stream()
                .filter(h -> h.getAdmUser() != null && cible.getId().equals(h.getAdmUser().getId()))
                .toList();

        assertThat(restantes)
                .as("la portée société portée par une ligne mixte doit survivre au remplacement des projets")
                .anyMatch(h -> h.getCfgCompany() != null && societe.getId().equals(h.getCfgCompany().getId()));
    }

    /** Un même projet répété dans le corps ne doit pas créer deux lignes d'habilitation. */
    @Test
    void putProjets_idsDupliques_neCreeQuUneSeuleLigne() throws Exception {
        AdmUser cible = newUser("adv.put.doublon");
        AdmUser admin = newUser("adv.put.doublon.admin");

        String body = "{\"projetIds\":[\"%s\",\"%s\"]}".formatted(projetA.getId(), projetA.getId());
        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        assertThat(habilitationRepository.findByAdmUserIdAndCfgProjectIsNotNull(cible.getId()))
                .as("un projetId répété ne doit produire qu'une ligne")
                .hasSize(1);

        // Et la colonne nombreProjets de la liste d'administration doit rester cohérente.
        assertThat(perimetreService.nombreProjetsParUtilisateur(List.of(cible.getId())).get(cible.getId()))
                .isEqualTo(1);
    }

    /** §7.2 : 404 si l'utilisateur ciblé n'existe pas — même règle que le GET. */
    @Test
    void putProjets_utilisateurInconnu_renvoie404() throws Exception {
        AdmUser admin = newUser("adv.put.404.admin");

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", UUID.randomUUID())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projetIds\":[\"%s\"]}".formatted(projetA.getId())))
                .andExpect(status().isNotFound());
    }

    // ================================================================= E. résolution du périmètre

    /**
     * {@code AdmUser.CfgEmployeeId} n'est pas unique en base réelle (59 renseignés / 58 distincts).
     * Deux comptes partageant le même tier doivent conserver des périmètres strictement
     * indépendants : le compte non affecté reste non restreint, le compte affecté ne voit pas
     * doubler ses lignes.
     */
    @Test
    void deuxComptesPartageantUnCfgEmployee_ontDesPerimetresIndependants() throws Exception {
        CfgTier employe = newTier("EMP-ADV-PARTAGE");

        AdmUser restreint = new AdmUser();
        restreint.setId(UUID.randomUUID());
        restreint.setCode("adv.partage.1." + suffixe());
        restreint.setDisabled(false);
        restreint.setCfgEmployee(employe);
        userRepository.save(restreint);

        AdmUser libre = new AdmUser();
        libre.setId(UUID.randomUUID());
        libre.setCode("adv.partage.2." + suffixe());
        libre.setDisabled(false);
        libre.setCfgEmployee(employe);
        userRepository.save(libre);

        affecter(restreint, projetA);

        assertThat(perimetreService.perimetreDe(restreint.getId()))
                .as("le périmètre du compte affecté ne doit contenir que ses propres lignes")
                .contains(List.of(projetA.getId()));
        assertThat(perimetreService.perimetreDe(libre.getId()))
                .as("un compte sans habilitation propre reste non restreint, même s'il partage le tier")
                .isEmpty();

        ComFolder dossierB = newDossier("DOS-ADV-PARTAGE-B", trancheB, newTier("CLI-ADV-PARTAGE"));
        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId()).with(connecte(libre, "Commercial")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId()).with(connecte(restreint, "Commercial")))
                .andExpect(status().isNotFound());
    }

    /**
     * Le code de rôle exempté est comparé caractère par caractère à une constante Java. Ce test
     * écrit le code avec des échappements Unicode ({@code Générale}), forme identique aux
     * octets réellement stockés en base ({@code 0x...4700E9006E00E900...}, vérifié sur
     * {@code yesRems}) : il échoue si le fichier source est un jour compilé dans un autre encodage
     * que celui de la constante, cas qu'un test écrivant le littéral des deux côtés ne peut pas voir.
     */
    @Test
    void roleDirectionGenerale_compareSurLesOctetsReelsDeLaBase() throws Exception {
        AdmUser dg = newUser("adv.dg");
        affecter(dg, projetA);
        donnerRole(dg, "Direction Générale");

        assertThat(perimetreService.estExempte(dg.getId()))
                .as("le code de rôle réel de yesRems doit exempter du périmètre")
                .isTrue();

        ComFolder dossierB = newDossier("DOS-ADV-DG-B", trancheB, newTier("CLI-ADV-DG"));
        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId())
                        .with(connecte(dg, "Direction Générale")))
                .andExpect(status().isOk());
    }

    /**
     * §6.3 tel qu'implémenté : un compte dont toutes les habilitations sont de portée
     * société/tranche (aucune ligne projet) est <b>non restreint</b>. Aucun compte réel n'est dans
     * ce cas aujourd'hui (mesuré : 0 utilisateur sur les 66 habilités n'a que des lignes non-projet),
     * mais l'écran d'administration permet de le produire — ce test verrouille la lecture retenue
     * pour qu'un changement de sémantique soit un échec visible et non une surprise en production.
     */
    @Test
    void habilitationsUniquementTrancheEtSociete_donnentUnCompteNonRestreint() throws Exception {
        AdmUser user = newUser("adv.tranche.seule");

        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("STE-ADV2-" + suffixe());
        companyRepository.save(societe);

        AdmUserHabilitation ligneSociete = new AdmUserHabilitation();
        ligneSociete.setId(UUID.randomUUID());
        ligneSociete.setAdmUser(user);
        ligneSociete.setCfgCompany(societe);
        habilitationRepository.save(ligneSociete);

        AdmUserHabilitation ligneTranche = new AdmUserHabilitation();
        ligneTranche.setId(UUID.randomUUID());
        ligneTranche.setAdmUser(user);
        ligneTranche.setCfgTranche(trancheA);
        habilitationRepository.save(ligneTranche);

        assertThat(perimetreService.perimetreDe(user.getId()))
                .as("aucune ligne projet ⇒ pas de restriction (§6.3)")
                .isEmpty();

        ComFolder dossierB = newDossier("DOS-ADV-TRSEULE-B", trancheB, newTier("CLI-ADV-TRSEULE"));
        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId()).with(connecte(user, "Commercial")))
                .andExpect(status().isOk());
    }

    /**
     * Le tableau de bord n'utilise pas de {@code Specification} : chaque agrégat est filtré à la
     * main. Ce test vise les compteurs de prospections, que le test de base ne contrôle pas
     * (il ne compare que {@code valeurPipeline}).
     */
    @Test
    void dashboard_compteursDeProspectionsFiltresParPerimetre() throws Exception {
        AdmUser commercial = newUser("adv.dash");
        affecter(commercial, projetA);
        AdmUser libre = newUser("adv.dash.libre");

        CfgTier prospect = newTier("PRO-ADV-DASH");
        newProspection("PSN-ADV-DASH-A", trancheA, prospect);
        newProspection("PSN-ADV-DASH-B1", trancheB, prospect);
        newProspection("PSN-ADV-DASH-B2", trancheB, prospect);

        long totalRestreint = lireCompteur(commercial, "prospectionsTotal");
        long totalGlobal = lireCompteur(libre, "prospectionsTotal");
        long ouvertesRestreint = lireCompteur(commercial, "prospectionsOuvertes");
        long ouvertesGlobal = lireCompteur(libre, "prospectionsOuvertes");

        assertThat(totalGlobal - totalRestreint)
                .as("les 2 prospections du projet B doivent sortir du compteur d'un commercial restreint")
                .isGreaterThanOrEqualTo(2);
        assertThat(ouvertesGlobal - ouvertesRestreint).isGreaterThanOrEqualTo(2);
    }

    private long lireCompteur(AdmUser user, String champ) throws Exception {
        String json = mockMvc.perform(get("/api/dashboard").with(connecte(user, "Commercial")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(json, "$." + champ)).longValue();
    }

    /**
     * Un id inconnu et un id hors périmètre doivent être indiscernables — sinon le 404/200 sert
     * d'oracle d'existence et les ids redeviennent énumérables.
     */
    @Test
    void idInconnuEtIdHorsPerimetre_donnentLaMemeReponse() throws Exception {
        AdmUser commercial = newUser("adv.oracle");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-ADV-ORACLE-B", trancheB, newTier("CLI-ADV-ORACLE"));

        int statutInconnu = mockMvc.perform(get("/api/dossiers/{id}", UUID.randomUUID())
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse().getStatus();
        int statutHorsPerimetre = mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId())
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse().getStatus();

        assertThat(statutHorsPerimetre).isEqualTo(statutInconnu);
    }

    /**
     * Génération PDF en lot : les ids arrivent dans le corps, hors de portée de l'intercepteur.
     * Un seul id hors périmètre doit faire échouer l'appel, sinon le lot devient le contournement
     * le plus simple de la feature.
     */
    @Test
    void documentEnLot_avecUnDossierHorsPerimetre_renvoie404() throws Exception {
        AdmUser commercial = newUser("adv.lot");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-ADV-LOT-B", trancheB, newTier("CLI-ADV-LOT"));

        mockMvc.perform(post("/api/dossiers/documents/fiche-reservation")
                        .with(connecte(commercial, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"%s\"]}".formatted(dossierB.getId())))
                .andExpect(status().isNotFound());
    }
}
