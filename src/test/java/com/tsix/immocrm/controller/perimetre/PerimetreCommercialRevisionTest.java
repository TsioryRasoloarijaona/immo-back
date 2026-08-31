package com.tsix.immocrm.controller.perimetre;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
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
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
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
 * Deuxième passe adverse — attaque les <b>correctifs</b> de §9bis, pas la première implémentation.
 *
 * <p>Trois angles, un par affirmation load-bearing du correctif :
 * <ul>
 *   <li><b>« intercepteur et contrôleur ne peuvent plus diverger, par construction »</b> : ils lisent
 *       bien la même chaîne, mais ils ne la <i>convertissent</i> pas de la même façon. Spring passe
 *       par {@code StringToUUIDConverter}, qui fait un {@code trim()} ; l'intercepteur appelle
 *       {@code UUID.fromString} directement. Un espace encodé suffit à les désaccorder.</li>
 *   <li><b>filtrage en mémoire dans la hotline</b> : filtrer <i>après</i> une requête déjà plafonnée
 *       à 50 lignes évince des lignes du périmètre au lieu d'évincer celles hors périmètre.</li>
 *   <li><b>{@code PUT .../projets} par mise à NULL</b> : formes multi-portées et cycles répétés.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerimetreCommercialRevisionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComActionMessageRepository actionMessageRepository;
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
        itemType.setCode("TYPE-REV-" + suffixe());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-REV-" + suffixe());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-REV-" + suffixe());
        stkVocationRepository.save(vocation);

        projetA = newProjet("REV-A");
        projetB = newProjet("REV-B");
        trancheA = newTranche("TR-REV-A", projetA);
        trancheB = newTranche("TR-REV-B", projetB);
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

    private StlSettlement newReglement(String code, ComFolder dossier, CfgTier tier, LocalDateTime date) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setAmount(new BigDecimal("1000"));
        documentRepository.save(echeance);

        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode(code);
        reglement.setDocDate(date);
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

    // ========================== A. « par construction » : trim du convertisseur Spring vs UUID.fromString

    /**
     * <b>Le correctif de §9bis.1 ne suffit pas.</b> L'intercepteur et le contrôleur lisent bien la
     * même chaîne — mais ils ne la convertissent pas pareil :
     * <ul>
     *   <li>intercepteur : {@code UUID.fromString(valeur)} — lève sur {@code " <uuid>"} ⇒ skip ;</li>
     *   <li>contrôleur : {@code StringToUUIDConverter}, qui fait
     *       {@code UUID.fromString(source.trim())} ⇒ lie un UUID valide et sert la ressource.</li>
     * </ul>
     * Un simple {@code %20} devant l'id rejoue donc exactement la faille de la version précédente.
     * C'est aussi le contre-exemple au « skip silencieux » : ici la valeur n'est pas un UUID pour
     * l'intercepteur, et pourtant le contrôleur la lie et sert la donnée — le raisonnement
     * « unparseable ⇒ 400 ⇒ rien n'est servi » est faux.
     */
    @Test
    void dossierHorsPerimetre_avecEspaceEncodeAvantLId_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("rev.trim");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-REV-TRIM-B", trancheB, newTier("CLI-REV-TRIM"));

        var reponse = mockMvc.perform(get(URI.create("/api/dossiers/%20" + dossierB.getId()))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("dossier du projet B servi à un commercial restreint au projet A : %s",
                        reponse.getContentAsString())
                .isEqualTo(404);
    }

    /** Même divergence avec l'espace encodé <em>après</em> l'id — {@code trim()} rogne les deux bouts. */
    @Test
    void dossierHorsPerimetre_avecEspaceEncodeApresLId_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("rev.trim.fin");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-REV-TRIMFIN-B", trancheB, newTier("CLI-REV-TRIMFIN"));

        var reponse = mockMvc.perform(get(URI.create("/api/dossiers/" + dossierB.getId() + "%20"))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("dossier du projet B servi : %s", reponse.getContentAsString())
                .isEqualTo(404);
    }

    @Test
    void prospectionHorsPerimetre_avecEspaceEncode_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("rev.trim.psn");
        affecter(commercial, projetA);
        ComProspection psnB = newProspection("PSN-REV-TRIM-B", trancheB, newTier("CLI-REV-TRIM-PSN"));

        var reponse = mockMvc.perform(get(URI.create("/api/prospections/%20" + psnB.getId()))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("prospection du projet B servie : %s", reponse.getContentAsString())
                .isEqualTo(404);
    }

    /** Les ~30 routes imbriquées héritent de la même divergence. */
    @Test
    void routeImbriqueeDunDossierHorsPerimetre_avecEspaceEncode_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("rev.trim.nested");
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-REV-TRIMN-B", trancheB, newTier("CLI-REV-TRIMN"));

        var reponse = mockMvc.perform(get(URI.create("/api/dossiers/%20" + dossierB.getId() + "/montants"))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("montants du dossier B servis : %s", reponse.getContentAsString())
                .isEqualTo(404);
    }

    @Test
    void encaissementHorsPerimetre_avecEspaceEncode_resteInaccessible() throws Exception {
        AdmUser commercial = newUser("rev.trim.enc");
        affecter(commercial, projetA);
        CfgTier tier = newTier("CLI-REV-TRIM-ENC");
        StlSettlement reglementB = newReglement("ENC-REV-TRIM-B",
                newDossier("DOS-REV-TRIM-ENC-B", trancheB, tier), tier, LocalDateTime.now());

        var reponse = mockMvc.perform(get(URI.create("/api/encaissements/%20" + reglementB.getId()))
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse();

        assertThat(reponse.getStatus())
                .as("encaissement du projet B servi : %s", reponse.getContentAsString())
                .isEqualTo(404);
    }

    // ================================================================= B. hotline filtrée en mémoire

    /**
     * {@code toFiche} charge les 50 interactions les plus récentes du tier <b>puis</b> filtre en
     * Java. Avec 50 interactions hors périmètre plus récentes qu'une interaction du périmètre,
     * cette dernière n'entre jamais dans la page chargée : le commercial restreint voit une liste
     * <b>vide</b> alors qu'une interaction de son propre projet existe. Le filtre n'évince pas les
     * lignes hors périmètre, il évince celles du périmètre.
     *
     * <p>Atteignable en données réelles : 2 tiers de {@code yesRems} portent plus de 50 interactions
     * (max 69).
     */
    @Test
    void hotline_uneInteractionDuPerimetre_nEstPasEvinceeParLePlafondDe50() throws Exception {
        AdmUser commercial = newUser("rev.hotline.page");
        affecter(commercial, projetA);

        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("CLI-REV-HOTPAGE");
        tier.setPhone("0688776655");
        tierRepository.save(tier);

        ComProspection psnA = newProspection("PSN-REV-HOTPAGE-A", trancheA, tier);
        ComProspection psnB = newProspection("PSN-REV-HOTPAGE-B", trancheB, tier);

        // 50 interactions hors périmètre, toutes plus récentes.
        for (int i = 0; i < 50; i++) {
            newInteraction(tier, psnB, null, LocalDateTime.now().minusMinutes(i), "hors perimetre " + i);
        }
        // 1 interaction du périmètre, plus ancienne — donc hors de la page des 50 plus récentes.
        ComActionMessage duPerimetre = newInteraction(tier, psnA,
                null, LocalDateTime.now().minusDays(30), "dans le perimetre");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0688776655")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + duPerimetre.getId() + "')]").exists());
    }

    /**
     * Le filtre hotline ne regarde que {@code comProspection}. Une interaction rattachée à un
     * <b>dossier</b> hors périmètre mais sans prospection est traitée comme « projet non résolu »
     * (§6.4) et reste visible. Mesuré sur {@code yesRems} : 13 interactions sont exactement dans ce
     * cas, et leur projet se résout parfaitement via le dossier.
     */
    @Test
    void hotline_interactionRattacheeAUnDossierHorsPerimetre_nEstPasVisible() throws Exception {
        AdmUser commercial = newUser("rev.hotline.folder");
        affecter(commercial, projetA);

        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("CLI-REV-HOTFOLDER");
        tier.setPhone("0677665544");
        tierRepository.save(tier);

        ComFolder dossierB = newDossier("DOS-REV-HOTFOLDER-B", trancheB, tier);
        ComActionMessage interaction = newInteraction(tier, null, dossierB,
                LocalDateTime.now(), "note interne sur le dossier du projet B");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0677665544")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + interaction.getId() + "')]")
                        .doesNotExist());
    }

    // ================================================================= C. PUT .../projets, seconde passe

    /** Une ligne portant société + tranche + PrjProject + projet doit garder ses trois autres portées. */
    @Test
    void putProjets_ligneAQuatrePortees_conserveLesTroisAutres() throws Exception {
        AdmUser cible = newUser("rev.put.triple");
        AdmUser admin = newUser("rev.put.triple.admin");

        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("STE-REV-" + suffixe());
        companyRepository.save(societe);

        UUID prjProjectId = UUID.randomUUID();
        AdmUserHabilitation mixte = new AdmUserHabilitation();
        mixte.setId(UUID.randomUUID());
        mixte.setAdmUser(cible);
        mixte.setCfgCompany(societe);
        mixte.setCfgTranche(trancheB);
        mixte.setPrjProjectId(prjProjectId);
        mixte.setCfgProject(projetB);
        habilitationRepository.save(mixte);

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projetIds\":[\"%s\"]}".formatted(projetA.getId())))
                .andExpect(status().isOk());

        AdmUserHabilitation apres = habilitationRepository.findById(mixte.getId()).orElseThrow();
        assertThat(apres.getCfgCompany()).as("portée société").isNotNull();
        assertThat(apres.getCfgTranche()).as("portée tranche").isNotNull();
        assertThat(apres.getPrjProjectId()).as("portée PrjProject").isEqualTo(prjProjectId);
        assertThat(apres.getCfgProject()).as("seule la portée projet est retirée").isNull();
    }

    /**
     * Trois cycles d'affectation ne doivent laisser ni ligne orpheline (toutes portées nulles) ni
     * doublon {@code (utilisateur, projet)} — la table légataire est partagée avec l'application de
     * bureau, une accumulation y serait invisible mais permanente.
     */
    @Test
    void putProjets_cyclesRepetes_nAccumulentNiOrphelinesNiDoublons() throws Exception {
        AdmUser cible = newUser("rev.put.cycles");
        AdmUser admin = newUser("rev.put.cycles.admin");

        for (UUID projetId : List.of(projetA.getId(), projetB.getId(), projetA.getId(), projetA.getId())) {
            mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                            .with(connecte(admin, "Administrateur"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"projetIds\":[\"%s\"]}".formatted(projetId)))
                    .andExpect(status().isOk());
        }

        List<AdmUserHabilitation> lignes = habilitationRepository.findAll().stream()
                .filter(h -> h.getAdmUser() != null && cible.getId().equals(h.getAdmUser().getId()))
                .toList();

        assertThat(lignes)
                .as("après 4 cycles il ne doit rester qu'une seule ligne d'habilitation")
                .hasSize(1);
        assertThat(lignes.get(0).getCfgProject()).isNotNull();
        assertThat(lignes.stream().noneMatch(h -> h.getCfgProject() == null && h.getCfgCompany() == null
                        && h.getCfgTranche() == null && h.getPrjProjectId() == null))
                .as("aucune ligne entièrement nulle ne doit subsister")
                .isTrue();
    }

    /** §9bis.6 : un corps sans {@code projetIds} ne doit pas lever silencieusement la restriction. */
    @Test
    void putProjets_corpsSansProjetIds_renvoie400() throws Exception {
        AdmUser cible = newUser("rev.put.null");
        AdmUser admin = newUser("rev.put.null.admin");
        affecter(cible, projetA);

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(habilitationRepository.findByAdmUserIdAndCfgProjectIsNotNull(cible.getId()))
                .as("un corps invalide ne doit rien avoir supprimé")
                .hasSize(1);
    }

    // ============================================ D. sous-ressources : pagination, comptage et tri

    /**
     * Le passage de requêtes dérivées à un {@code Specification} doit filtrer <b>aussi</b> la
     * requête de comptage : sinon {@code totalElements} annonce des lignes que la page ne contient
     * pas, et le front pagine dans le vide.
     */
    @Test
    void prospectDossiers_totalElementsRefleteLePerimetre() throws Exception {
        AdmUser commercial = newUser("rev.pagination");
        affecter(commercial, projetA);
        CfgTier client = newTier("CLI-REV-PAGINATION");

        newDossier("DOS-REV-PAG-A1", trancheA, client);
        newDossier("DOS-REV-PAG-A2", trancheA, client);
        newDossier("DOS-REV-PAG-B1", trancheB, client);
        newDossier("DOS-REV-PAG-B2", trancheB, client);
        newDossier("DOS-REV-PAG-B3", trancheB, client);

        mockMvc.perform(get("/api/prospects/{id}/dossiers", client.getId())
                        .param("size", "10")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    /**
     * {@code findByTierIdOrderByDocDateDesc} a été remplacé par un {@code Specification} : le tri
     * historique doit rester {@code docDate} décroissant quand l'appelant n'en demande pas d'autre.
     */
    @Test
    void prospectReglements_conserventLeTriDocDateDecroissant() throws Exception {
        AdmUser libre = newUser("rev.tri");
        CfgTier client = newTier("CLI-REV-TRI");
        ComFolder dossier = newDossier("DOS-REV-TRI-A", trancheA, client);

        newReglement("ENC-REV-TRI-VIEUX", dossier, client, LocalDateTime.now().minusDays(30));
        newReglement("ENC-REV-TRI-RECENT", dossier, client, LocalDateTime.now());
        newReglement("ENC-REV-TRI-MOYEN", dossier, client, LocalDateTime.now().minusDays(10));

        mockMvc.perform(get("/api/prospects/{id}/reglements", client.getId())
                        .with(connecte(libre, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("ENC-REV-TRI-RECENT"))
                .andExpect(jsonPath("$.content[1].code").value("ENC-REV-TRI-MOYEN"))
                .andExpect(jsonPath("$.content[2].code").value("ENC-REV-TRI-VIEUX"));
    }

    /**
     * §6.4 sur les offres : {@code StlQuote.CfgTrancheId} est renseigné sur 312/312 lignes réelles,
     * donc cette branche est morte en production — mais elle est dans le prédicat, et un devis sans
     * tranche ne doit être masqué à personne, comme partout ailleurs.
     */
    @Test
    void devisSansTranche_resteVisibleDunCommercialRestreint() throws Exception {
        AdmUser commercial = newUser("rev.devis.null");
        affecter(commercial, projetA);
        CfgTier prospect = newTier("PRO-REV-DEVISNULL");

        StlQuote devisSansTranche = new StlQuote();
        devisSansTranche.setId(UUID.randomUUID());
        devisSansTranche.setCode("DEV-REV-SANSTRANCHE");
        devisSansTranche.setDocDate(LocalDateTime.now());
        devisSansTranche.setCfgProspect(prospect);
        quoteRepository.save(devisSansTranche);

        mockMvc.perform(get("/api/devis").param("search", "DEV-REV-SANSTRANCHE")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + devisSansTranche.getId() + "')]").exists());
    }

    // ================================================================= E. plus aucun accès anonyme

    /**
     * §9bis.3 : balayage de toutes les surfaces qui exposent du dossier ou de la prospection. Sans
     * principal, {@code PerimetreCommercialService} considère l'appelant comme non restreint — donc
     * toute route atteignable anonymement est une route sans périmètre. Aucune ne doit l'être.
     */
    @Test
    void anonyme_neTouchePlusAucuneDonneeDeDossierOuDeProspection() throws Exception {
        CfgTier client = newTier("CLI-REV-ANON");
        ComFolder dossier = newDossier("DOS-REV-ANON", trancheA, client);
        ComProspection prospection = newProspection("PSN-REV-ANON", trancheA, client);

        List<String> routes = List.of(
                "/api/dossiers",
                "/api/dossiers/" + dossier.getId(),
                "/api/dossiers/" + dossier.getId() + "/documents/fiche-reservation",
                "/api/dossiers/" + dossier.getId() + "/documents/acte-reservation",
                "/api/dossiers/" + dossier.getId() + "/echeancier",
                "/api/prospections/" + prospection.getId(),
                "/api/prospections/" + prospection.getId() + "/documents/nda",
                "/api/prospections/" + prospection.getId() + "/documents/fiche",
                "/api/prospections/" + prospection.getId() + "/devis/" + UUID.randomUUID() + "/document",
                "/api/pre-prospections",
                "/api/encaissements",
                "/api/devis",
                "/api/dashboard",
                "/api/clients/" + client.getId() + "/dossiers",
                "/api/prospects/" + client.getId() + "/dossiers",
                "/api/prospects/" + client.getId() + "/prospections",
                "/api/prospects/" + client.getId() + "/reglements",
                "/api/hotline/recherche?telephone=0600000000");

        for (String route : routes) {
            int statut = mockMvc.perform(get(URI.create(route))).andReturn().getResponse().getStatus();
            assertThat(statut)
                    .as("route accessible sans authentification (donc sans périmètre) : %s", route)
                    .isEqualTo(401);
        }
    }

    // ================================================================= F. écriture, article au milieu

    /**
     * {@code POST /api/dossiers} boucle désormais sur tous les articles. Vérifie que l'article
     * fautif est détecté quelle que soit sa position, et qu'aucun article n'a été rattaché au
     * dossier avant l'échec (la validation doit précéder toute écriture).
     */
    @Test
    void creationDeDossier_articleHorsPerimetreAuMilieu_renvoie403SansRienEcrire() throws Exception {
        AdmUser commercial = newUser("rev.create.milieu");
        affecter(commercial, projetA);

        CfgTier prospect = newTier("PRO-REV-CREATE");
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-REV-CREATE");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        StkItem a1 = newArticle("LOT-REV-A1", trancheA);
        StkItem b = newArticle("LOT-REV-B", trancheB);
        StkItem a2 = newArticle("LOT-REV-A2", trancheA);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("MODE-REV-" + suffixe());
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s","%s","%s"],"modePaieId":"%s","cin":"101112131415"}
                """.formatted(prospection.getId(), a1.getId(), b.getId(), a2.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").with(connecte(commercial, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        for (StkItem article : List.of(a1, b, a2)) {
            assertThat(stkItemRepository.findById(article.getId()).orElseThrow().getComFolderId())
                    .as("aucun article ne doit être rattaché quand la création est refusée : %s", article.getCode())
                    .isNull();
        }
    }
}
