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
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutDossier;
import com.tsix.immocrm.fixtures.ReferentielSeme;
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
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
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
 * Couverture de base de la feature "Périmètre commercial"
 * ({@code instructions/FEATURE-perimetre-commercial.md} §8).
 *
 * <p>Les données réelles de {@code yesRems} n'étant pas présentes dans {@code yesRemsTest}, chaque
 * test construit ses propres lignes {@code AdmUserHabilitation} via les vrais repositories, et le
 * {@code @Transactional} de la classe les annule ensuite.
 *
 * <p>Le reste de la suite s'authentifie avec {@code @WithMockUser}, dont le principal n'est pas un
 * {@link AuthenticatedUser} : c'est le chemin "non restreint", et le fait que les 328 tests
 * préexistants restent verts est en soi la non-régression demandée au point 3 du §8 — le test
 * {@code sansAffectation_voitTout} en donne ici la démonstration explicite avec un vrai principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerimetreCommercialTest {

    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private FixtureStatutDossier fixtureStatutDossier;
    @Autowired private MockMvc mockMvc;
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
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;

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
        itemType.setCode("TYPE-PERIM-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-PERIM-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PERIM-TEST");
        stkVocationRepository.save(vocation);

        projetA = newProjet("PERIM-A");
        projetB = newProjet("PERIM-B");
        trancheA = newTranche("TR-PERIM-A", projetA);
        trancheB = newTranche("TR-PERIM-B", projetB);
    }

    // ------------------------------------------------------------------ fixtures

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

    private AdmUser newUser(String code) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code);
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

    /**
     * Authentifie la requête avec un vrai {@link AuthenticatedUser} — indispensable ici : le
     * périmètre est résolu depuis {@code AuthenticatedUser.admUserId()}, et {@code @WithMockUser}
     * pose un principal d'un autre type, donc non restreint. {@code mustChangePassword=false} et
     * {@code profileCompleted=true} : valeurs neutres (plus aucun portail ne les lit).
     */
    private RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getCode(), null, false, true, 0);
        List<GrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    /** Dossier rattaché à {@code tranche} via son article principal (ComFolder.StkItemId). */
    private ComFolder newDossier(String code, CfgTranche tranche) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        folder.setTotalToPay(new BigDecimal("100000"));
        folderRepository.save(folder);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-" + code);
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setComFolderId(folder.getId());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        folder.setStkItem(article);
        return folderRepository.save(folder);
    }

    private ComProspection newProspection(String code, CfgTranche tranche) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-" + code);
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        prospection.setCfgProspect(prospect);
        prospection.setStkHierarchy(tranche);
        return prospectionRepository.save(prospection);
    }

    /** Pré-prospection : CfgProspectId ET CfgCommercialId NULL (le discriminant réel du schéma). */
    private ComProspection newPreProspection(String code, CfgTranche tranche) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        prospection.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        prospection.setStkHierarchy(tranche);
        return prospectionRepository.save(prospection);
    }

    /** Règlement rattaché à une échéance du dossier fourni — la chaîne du filtre encaissement. */
    private StlSettlement newReglement(String code, ComFolder dossier) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setAmount(new BigDecimal("1000"));
        documentRepository.save(echeance);

        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode(code);
        reglement.setDocDate(java.time.LocalDateTime.now());
        reglement.setAmount(new BigDecimal("1000"));
        reglement.setStatus("IN_WORK");
        // Encaissement : GET /api/encaissements filtre IsCredit = 1 comme l'écran legacy, et un
        // NULL ne satisfait pas l'égalité. Valeur posée aussi par EcheancierService à la création.
        reglement.setIsCredit(true);
        settlementRepository.save(reglement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("1000"));
        settlementLineRepository.save(ligne);

        return reglement;
    }

    // ------------------------------------------------------------------ §8.2 — filtrage et 404

    @Test
    void commercialAffecteProjetA_neVoitQueLesDossiersDuProjetA() throws Exception {
        AdmUser commercial = newUser("perim.commercial." + UUID.randomUUID());
        affecter(commercial, projetA);

        ComFolder dossierA = newDossier("DOS-PERIM-A", trancheA);
        ComFolder dossierB = newDossier("DOS-PERIM-B", trancheB);

        mockMvc.perform(get("/api/dossiers").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").doesNotExist());
    }

    @Test
    void commercialAffecteProjetA_detailDunDossierDuProjetB_renvoie404() throws Exception {
        AdmUser commercial = newUser("perim.commercial.404." + UUID.randomUUID());
        affecter(commercial, projetA);

        ComFolder dossierB = newDossier("DOS-PERIM-404-B", trancheB);

        // 404 et non 403 : un 403 confirmerait l'existence du dossier et rendrait les ids énumérables.
        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId()).with(connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());
    }

    @Test
    void commercialAffecteProjetA_routeImbriqueeDunDossierDuProjetB_renvoie404() throws Exception {
        AdmUser commercial = newUser("perim.commercial.nested." + UUID.randomUUID());
        affecter(commercial, projetA);

        ComFolder dossierB = newDossier("DOS-PERIM-NESTED-B", trancheB);

        // Vérifie que le contrôle est bien centralisé sur le chargement par id, pas seulement
        // recopié sur le endpoint de détail : sans lui, l'échéancier fuirait le dossier.
        mockMvc.perform(get("/api/dossiers/{id}/echeancier", dossierB.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());
    }

    /**
     * Généralisation des deux contournements successifs ({@code %2D}, puis {@code %20}) : plutôt
     * que de verrouiller les deux chaînes qui ont fait défaut, ce test balaie une batterie de
     * déformations d'identifiant et exige qu'<b>aucune</b> ne serve la ressource.
     *
     * <p>C'est la propriété qui compte, et elle ne dépend d'aucune hypothèse sur la façon dont
     * Spring convertit une chaîne en UUID : l'intercepteur refuse dès qu'il ne résout pas lui-même
     * un UUID sur une route scopée. Si une future version de Spring accepte une forme de plus, ce
     * test reste vert — c'était précisément le mode de défaillance des deux versions précédentes.
     */
    @Test
    void dossierHorsPerimetre_aucuneDeformationDIdentifiantNeLeSert() throws Exception {
        AdmUser commercial = newUser("perim.balayage." + UUID.randomUUID());
        affecter(commercial, projetA);
        ComFolder dossierB = newDossier("DOS-PERIM-BALAYAGE-B", trancheB);
        String id = dossierB.getId().toString();

        List<String> deformations = List.of(
                id,                              // référence : hors périmètre, doit être 404
                "%20" + id,                      // espace encodé avant  — trim() de Spring
                id + "%20",                      // espace encodé après  — trim() de Spring
                "%09" + id,                      // tabulation encodée   — trim() coupe aussi
                "%0A" + id,                      // saut de ligne encodé — idem
                id.replace("-", "%2D"),          // tirets encodés       — décodage de segment
                id.toUpperCase(),                // casse                — UUID.fromString l'accepte
                id + ";x=1",                     // paramètre matriciel
                "%20" + id.toUpperCase() + "%20" // combinaison
        );

        for (String deformation : deformations) {
            int statut = mockMvc.perform(get(URI.create("/api/dossiers/" + deformation))
                            .with(connecte(commercial, "Commercial")))
                    .andReturn().getResponse().getStatus();
            assertThat(statut)
                    .as("un dossier hors périmètre ne doit jamais être servi, quelle que soit "
                            + "l'écriture de son id : /api/dossiers/%s", deformation)
                    .isNotEqualTo(200);
        }
    }

    @Test
    void commercialAffecteProjetA_neVoitQueLesProspectionsDuProjetA() throws Exception {
        AdmUser commercial = newUser("perim.psn." + UUID.randomUUID());
        affecter(commercial, projetA);

        ComProspection psnA = newProspection("PSN-PERIM-A", trancheA);
        ComProspection psnB = newProspection("PSN-PERIM-B", trancheB);

        mockMvc.perform(get("/api/prospections").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + psnA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + psnB.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/prospections/{id}", psnB.getId()).with(connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());
    }

    @Test
    void commercialAffecteProjetA_neVoitQueLesPreProspectionsDuProjetA() throws Exception {
        AdmUser commercial = newUser("perim.prepsn." + UUID.randomUUID());
        affecter(commercial, projetA);

        ComProspection preA = newPreProspection("PRE-PERIM-A", trancheA);
        ComProspection preB = newPreProspection("PRE-PERIM-B", trancheB);

        mockMvc.perform(get("/api/pre-prospections").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + preA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + preB.getId() + "')]").doesNotExist());
    }

    @Test
    void commercialAffecteProjetA_neVoitQueLesEncaissementsDuProjetA() throws Exception {
        AdmUser commercial = newUser("perim.enc." + UUID.randomUUID());
        affecter(commercial, projetA);

        StlSettlement reglementA = newReglement("ENC-PERIM-A", newDossier("DOS-PERIM-ENC-A", trancheA));
        StlSettlement reglementB = newReglement("ENC-PERIM-B", newDossier("DOS-PERIM-ENC-B", trancheB));

        mockMvc.perform(get("/api/encaissements").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + reglementA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + reglementB.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/encaissements/{id}", reglementB.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/encaissements/{id}", reglementA.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §8.3 — non-régression

    @Test
    void sansAffectation_voitTout() throws Exception {
        AdmUser sansPerimetre = newUser("perim.sansaffect." + UUID.randomUUID());

        ComFolder dossierA = newDossier("DOS-PERIM-LIBRE-A", trancheA);
        ComFolder dossierB = newDossier("DOS-PERIM-LIBRE-B", trancheB);
        ComProspection psnB = newProspection("PSN-PERIM-LIBRE-B", trancheB);

        // Décision produit §6.3 : un périmètre vide signifie "pas de restriction", jamais
        // "ne voit rien" — c'est ce qui protège tous les comptes existants au déploiement.
        mockMvc.perform(get("/api/dossiers").with(connecte(sansPerimetre, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").exists());

        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId()).with(connecte(sansPerimetre, "Commercial")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/prospections/{id}", psnB.getId()).with(connecte(sansPerimetre, "Commercial")))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §8.4 — rôles exemptés

    @Test
    void administrateurAffecteAUnSeulProjet_voitQuandMemeTout() throws Exception {
        AdmUser admin = newUser("perim.admin." + UUID.randomUUID());
        affecter(admin, projetA);
        donnerRole(admin, "Administrateur");

        ComFolder dossierB = newDossier("DOS-PERIM-ADMIN-B", trancheB);

        mockMvc.perform(get("/api/dossiers").with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").exists());
        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId()).with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk());
    }

    @Test
    void directionGeneraleAffecteeAUnSeulProjet_voitQuandMemeTout() throws Exception {
        AdmUser dg = newUser("perim.dg." + UUID.randomUUID());
        affecter(dg, projetA);
        donnerRole(dg, "Direction Générale");

        ComFolder dossierB = newDossier("DOS-PERIM-DG-B", trancheB);

        mockMvc.perform(get("/api/dossiers").with(connecte(dg, "Direction Générale")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").exists());
    }

    @Test
    void commercialChefNestPasExempte() throws Exception {
        AdmUser chef = newUser("perim.chef." + UUID.randomUUID());
        affecter(chef, projetA);
        // Le code exact fait foi : "Commercial Chef" ne doit surtout pas être capté par une
        // comparaison par préfixe, et il n'existe aucune table d'équipe pour "voir son équipe".
        donnerRole(chef, "Commercial Chef");

        ComFolder dossierB = newDossier("DOS-PERIM-CHEF-B", trancheB);

        mockMvc.perform(get("/api/dossiers").with(connecte(chef, "Commercial Chef")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------ §8.5 — projet non résoluble

    @Test
    void ligneSansProjetResoluble_resteVisibleDunCommercialRestreint() throws Exception {
        AdmUser commercial = newUser("perim.orphelin." + UUID.randomUUID());
        affecter(commercial, projetA);

        // Dossier sans article principal : aucun projet ne se résout.
        ComFolder dossierOrphelin = new ComFolder();
        dossierOrphelin.setId(UUID.randomUUID());
        dossierOrphelin.setCode("DOS-PERIM-ORPHELIN");
        dossierOrphelin.setStkVocation(vocation);
        folderRepository.save(dossierOrphelin);

        // Pré-prospection sans tranche : idem (3,29 % des pré-prospections réelles).
        ComProspection preSansTranche = newPreProspection("PRE-PERIM-ORPHELIN", null);

        mockMvc.perform(get("/api/dossiers").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierOrphelin.getId() + "')]").exists());

        mockMvc.perform(get("/api/dossiers/{id}", dossierOrphelin.getId())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pre-prospections").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + preSansTranche.getId() + "')]").exists());
    }

    // ------------------------------------------------------------------ §8.6 — le PUT ne casse pas le légataire

    @Test
    void putProjets_preserveLesHabilitationsLegataires() throws Exception {
        AdmUser cible = newUser("perim.put." + UUID.randomUUID());
        AdmUser admin = newUser("perim.put.admin." + UUID.randomUUID());

        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("STE-PERIM-TEST");
        societe.setIsActive(true);
        companyRepository.save(societe);

        // Les trois portées que l'application de bureau légataire (toujours en service) utilise et
        // que ce PUT ne doit jamais toucher.
        AdmUserHabilitation ligneSociete = new AdmUserHabilitation();
        ligneSociete.setId(UUID.randomUUID());
        ligneSociete.setAdmUser(cible);
        ligneSociete.setCfgCompany(societe);
        habilitationRepository.save(ligneSociete);

        AdmUserHabilitation ligneTranche = new AdmUserHabilitation();
        ligneTranche.setId(UUID.randomUUID());
        ligneTranche.setAdmUser(cible);
        ligneTranche.setCfgTranche(trancheB);
        habilitationRepository.save(ligneTranche);

        // Ligne semée, pas un UUID tiré au hasard : AdmUserHabilitation__PrjProject_FK1 exige une
        // cible réelle, et PrjProject n'est modélisé par aucune entité — voir ReferentielSeme.
        UUID prjProjectId = ReferentielSeme.PRJ_PROJECT;
        AdmUserHabilitation lignePrjProject = new AdmUserHabilitation();
        lignePrjProject.setId(UUID.randomUUID());
        lignePrjProject.setAdmUser(cible);
        lignePrjProject.setPrjProjectId(prjProjectId);
        habilitationRepository.save(lignePrjProject);

        // Ligne MIXTE (société + projet) — la forme réellement présente en base (2 lignes du compte
        // Mkamara). La première version de cette fixture n'utilisait que des lignes mono-portée et
        // ratait donc le vrai piège : un `DELETE ... WHERE CfgProjectId IS NOT NULL` emporte aussi
        // la portée société de cette ligne-là. Elle doit survivre, amputée de son seul projet.
        CfgCompany societeMixte = new CfgCompany();
        societeMixte.setId(UUID.randomUUID());
        societeMixte.setCode("STE-PERIM-MIXTE");
        societeMixte.setIsActive(true);
        companyRepository.save(societeMixte);

        AdmUserHabilitation ligneMixte = new AdmUserHabilitation();
        ligneMixte.setId(UUID.randomUUID());
        ligneMixte.setAdmUser(cible);
        ligneMixte.setCfgCompany(societeMixte);
        ligneMixte.setCfgProject(projetB);
        habilitationRepository.save(ligneMixte);

        // Et une ancienne affectation projet exclusive, elle destinée à disparaître.
        affecter(cible, projetB);

        String body = """
                {"projetIds":["%s"]}""".formatted(projetA.getId());

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(projetA.getId().toString()))
                .andExpect(jsonPath("$[0].code").value("PERIM-A"));

        List<AdmUserHabilitation> restantes = habilitationRepository.findAll().stream()
                .filter(h -> h.getAdmUser() != null && cible.getId().equals(h.getAdmUser().getId()))
                .toList();

        assertThat(restantes)
                .as("les lignes société/tranche/PrjProject du légataire doivent survivre au PUT")
                .anyMatch(h -> h.getCfgCompany() != null && societe.getId().equals(h.getCfgCompany().getId()))
                .anyMatch(h -> h.getCfgTranche() != null && trancheB.getId().equals(h.getCfgTranche().getId()))
                .anyMatch(h -> prjProjectId.equals(h.getPrjProjectId()))
                .as("la ligne mixte survit, mais amputée de son seul CfgProjectId")
                .anyMatch(h -> ligneMixte.getId().equals(h.getId())
                        && h.getCfgCompany() != null
                        && societeMixte.getId().equals(h.getCfgCompany().getId())
                        && h.getCfgProject() == null);

        List<UUID> projetsRestants = restantes.stream()
                .filter(h -> h.getCfgProject() != null)
                .map(h -> h.getCfgProject().getId())
                .toList();
        assertThat(projetsRestants)
                .as("le jeu d'affectations projet est remplacé intégralement")
                .containsExactly(projetA.getId());
    }

    @Test
    void putProjets_projetIdsAbsent_renvoie400() throws Exception {
        AdmUser cible = newUser("perim.put.null." + UUID.randomUUID());
        AdmUser admin = newUser("perim.put.null.admin." + UUID.randomUUID());
        affecter(cible, projetA);

        // Un corps tronqué ne doit pas lever une restriction en silence : seule une liste vide
        // explicite le fait (voir putProjets_listeVide_retireTouteRestriction).
        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"projetIds\":null}"))
                .andExpect(status().isBadRequest());

        assertThat(habilitationRepository.findByAdmUserIdAndCfgProjectIsNotNull(cible.getId()))
                .as("un corps invalide ne doit rien avoir supprimé")
                .hasSize(1);
    }

    @Test
    void putProjets_listeVide_retireTouteRestriction() throws Exception {
        AdmUser cible = newUser("perim.put.vide." + UUID.randomUUID());
        AdmUser admin = newUser("perim.put.vide.admin." + UUID.randomUUID());
        affecter(cible, projetA);

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"projetIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        ComFolder dossierB = newDossier("DOS-PERIM-PUTVIDE-B", trancheB);
        mockMvc.perform(get("/api/dossiers").with(connecte(cible, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + dossierB.getId() + "')]").exists());
    }

    @Test
    void putProjets_projetInconnu_renvoie400() throws Exception {
        AdmUser cible = newUser("perim.put.400." + UUID.randomUUID());
        AdmUser admin = newUser("perim.put.400.admin." + UUID.randomUUID());

        String body = """
                {"projetIds":["%s"]}""".formatted(UUID.randomUUID());

        mockMvc.perform(put("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProjets_utilisateurInconnu_renvoie404() throws Exception {
        AdmUser admin = newUser("perim.get.404.admin." + UUID.randomUUID());

        mockMvc.perform(get("/api/utilisateurs/{id}/projets", UUID.randomUUID())
                        .with(connecte(admin, "Administrateur")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProjets_renvoieLesProjetsAffectes() throws Exception {
        AdmUser cible = newUser("perim.get." + UUID.randomUUID());
        AdmUser admin = newUser("perim.get.admin." + UUID.randomUUID());
        affecter(cible, projetA);

        mockMvc.perform(get("/api/utilisateurs/{id}/projets", cible.getId())
                        .with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("PERIM-A"))
                .andExpect(jsonPath("$[0].libelle").value("Projet PERIM-A"));
    }

    // ------------------------------------------------------------------ §8.7 — réservé aux administrateurs

    @Test
    @WithMockUser // rôle USER par défaut — pas Administrateur
    void getProjets_sansRoleAdministrateur_renvoie403() throws Exception {
        mockMvc.perform(get("/api/utilisateurs/{id}/projets", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void putProjets_sansRoleAdministrateur_renvoie403() throws Exception {
        mockMvc.perform(put("/api/utilisateurs/{id}/projets", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"projetIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ §8.8 — tableau de bord

    @Test
    void dashboard_agregeUniquementLesDossiersDuPerimetre() throws Exception {
        AdmUser commercial = newUser("perim.dash." + UUID.randomUUID());
        affecter(commercial, projetA);

        newDossier("DOS-PERIM-DASH-A", trancheA);
        ComFolder dossierB = newDossier("DOS-PERIM-DASH-B", trancheB);

        // Le tableau de bord n'utilise pas de Specification : ses agrégats sont filtrés
        // explicitement — c'est le point de fuite le plus probable de la feature.
        mockMvc.perform(get("/api/dashboard").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relancesEnRetard[?(@.dossierId=='" + dossierB.getId() + "')]").doesNotExist());

        String contenuRestreint = mockMvc.perform(get("/api/dashboard").with(connecte(commercial, "Commercial")))
                .andReturn().getResponse().getContentAsString();
        String contenuGlobal = mockMvc.perform(get("/api/dashboard")
                        .with(connecte(newUser("perim.dash.libre." + UUID.randomUUID()), "Commercial")))
                .andReturn().getResponse().getContentAsString();

        BigDecimal pipelineRestreint = new BigDecimal(
                com.jayway.jsonpath.JsonPath.read(contenuRestreint, "$.valeurPipeline").toString());
        BigDecimal pipelineGlobal = new BigDecimal(
                com.jayway.jsonpath.JsonPath.read(contenuGlobal, "$.valeurPipeline").toString());

        assertThat(pipelineRestreint)
                .as("la valeur du pipeline d'un commercial restreint est strictement inférieure au total global")
                .isLessThan(pipelineGlobal);
    }

    // ------------------------------------------------------------------ §8.9 — écritures hors périmètre

    @Test
    void creationDeDossierSurUnProjetHorsPerimetre_renvoie403() throws Exception {
        AdmUser commercial = newUser("perim.create." + UUID.randomUUID());
        affecter(commercial, projetA);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-PERIM-CREATE");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-PERIM-CREATE");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        StkItem articleB = new StkItem();
        articleB.setId(UUID.randomUUID());
        articleB.setCode("LOT-PERIM-CREATE-B");
        articleB.setStkItemType(itemType);
        articleB.setStkItemCategory(itemCategory);
        articleB.setStkVocation(vocation);
        articleB.setStkHierarchy(trancheB);
        articleB.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(articleB);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("MODE-PERIM-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"101112131415"}
                """.formatted(prospection.getId(), articleB.getId(), modePaie.getId());

        // 403 et non 404 : l'appelant a lui-même nommé l'article, il n'y a rien à cacher.
        mockMvc.perform(post("/api/dossiers").with(connecte(commercial, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void creationDeDossierDansLePerimetre_reste201() throws Exception {
        AdmUser commercial = newUser("perim.create.ok." + UUID.randomUUID());
        affecter(commercial, projetA);
        fixtureStatutDossier.reservation();

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-PERIM-CREATE-OK");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-PERIM-CREATE-OK");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        StkItem articleA = new StkItem();
        articleA.setId(UUID.randomUUID());
        articleA.setCode("LOT-PERIM-CREATE-A");
        articleA.setStkItemType(itemType);
        articleA.setStkItemCategory(itemCategory);
        articleA.setStkVocation(vocation);
        articleA.setStkHierarchy(trancheA);
        articleA.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(articleA);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("MODE-PERIM-TEST-OK");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"101112131415"}
                """.formatted(prospection.getId(), articleA.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").with(connecte(commercial, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ §7.1 / §7.2 — exposition

    @Test
    void monCompte_exposeLePerimetreCourant() throws Exception {
        AdmUser commercial = newUser("perim.moncompte." + UUID.randomUUID());
        affecter(commercial, projetA);
        creerCredential(commercial);

        mockMvc.perform(get("/api/mon-compte").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perimetreRestreint").value(true))
                .andExpect(jsonPath("$.projetsAutorises.length()").value(1))
                .andExpect(jsonPath("$.projetsAutorises[0].code").value("PERIM-A"));
    }

    @Test
    void monCompte_sansAffectation_nEstPasRestreint() throws Exception {
        AdmUser commercial = newUser("perim.moncompte.libre." + UUID.randomUUID());
        creerCredential(commercial);

        mockMvc.perform(get("/api/mon-compte").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perimetreRestreint").value(false))
                .andExpect(jsonPath("$.projetsAutorises.length()").value(0));
    }

    @Test
    void listeDesUtilisateurs_exposeNombreProjets() throws Exception {
        AdmUser cible = newUser("perim.nbprojets." + UUID.randomUUID());
        AdmUser admin = newUser("perim.nbprojets.admin." + UUID.randomUUID());
        affecter(cible, projetA);
        affecter(cible, projetB);

        mockMvc.perform(get("/api/utilisateurs").param("search", cible.getCode())
                        .with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombreProjets").value(2));
    }

    @Autowired
    private com.tsix.immocrm.repositories.app.AppUserCredentialRepository credentialRepository;

    /** {@code GET /api/mon-compte} exige un AppUserCredential pour ce compte. */
    private void creerCredential(AdmUser user) {
        com.tsix.immocrm.entities.app.AppUserCredential credential =
                new com.tsix.immocrm.entities.app.AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setTokenVersion(0);
        credential.setUserCreateDateTime(java.time.LocalDateTime.now());
        credentialRepository.save(credential);
    }
}
