package com.tsix.immocrm.controller.perimetre;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité transversale par entité juridique
 * ({@code instructions/FEATURE-visibilite-transversale-entites.md} §6).
 *
 * <p>Ce que cette classe verrouille, et qui n'était vrai d'aucune version précédente : le filtre
 * {@code CfgCompany.IsActive} ne porte plus seulement sur le patrimoine et les référentiels, mais
 * sur les <b>collections métier</b> — dossiers, prospections, clients, encaissements — et sur
 * l'accès par identifiant.
 *
 * <p>Trois propriétés distinctes s'y jouent, et il faut les trois pour que la règle tienne :
 * <ol>
 *   <li>la ligne masquée l'est <b>aussi par id</b>, sinon le filtre de liste se contourne en
 *       énumérant les identifiants ;</li>
 *   <li>la ligne <b>sans entité résoluble</b> reste visible (R2) — c'est le contre-test, et sans
 *       lui une implémentation par jointure interne passerait tous les autres ;</li>
 *   <li>un rôle exempté de <b>périmètre</b> subit quand même le filtre d'<b>entité</b>, les deux
 *       restrictions n'ayant ni la même portée ni la même raison d'être.</li>
 * </ol>
 *
 * <p>Comme {@code PerimetreCommercialTest}, chaque test bâtit ses propres lignes par les vrais
 * repositories, et le {@code @Transactional} de la classe les annule ensuite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisibiliteEntitesActivesTest {

    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;

    private CfgTranche trancheActive;
    private CfgTranche trancheInactive;
    private CfgProject projetInactif;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-VIS-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-VIS-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-VIS-TEST");
        stkVocationRepository.save(vocation);

        CfgCompany societeActive = newSociete("VIS-ACTIVE", true);
        CfgCompany societeInactive = newSociete("VIS-INACTIVE", false);

        CfgProject projetActif = newProjet("VIS-P-ACTIF", societeActive);
        projetInactif = newProjet("VIS-P-INACTIF", societeInactive);

        trancheActive = newTranche("VIS-TR-ACTIVE", projetActif);
        trancheInactive = newTranche("VIS-TR-INACTIVE", projetInactif);
    }

    // ------------------------------------------------------------------ §6.1 et §6.2 — masquage

    @Test
    @WithMockUser
    void dossierSurEntiteInactive_absentDeLaListeEtIntrouvableParId() throws Exception {
        ComFolder actif = newDossier("DOS-VIS-ACTIF", trancheActive);
        ComFolder inactif = newDossier("DOS-VIS-INACTIF", trancheInactive);

        mockMvc.perform(get("/api/dossiers").param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + actif.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + inactif.getId() + "')]").doesNotExist());

        // 404 et non 403 : une entité hors exploitation ne doit pas être distinguable d'une
        // ressource inexistante, sinon les identifiants redeviennent énumérables.
        mockMvc.perform(get("/api/dossiers/{id}", inactif.getId()))
                .andExpect(status().isNotFound());
    }

    /**
     * Le contrôle est-il centralisé sur le chargement par id, ou seulement recopié sur le endpoint
     * de détail ? Sans ce test, l'échéancier — et avec lui pièces jointes, règlements et
     * générateurs de documents — continuerait de servir un dossier masqué.
     */
    @Test
    @WithMockUser
    void dossierSurEntiteInactive_routeImbriquee_renvoie404() throws Exception {
        ComFolder inactif = newDossier("DOS-VIS-NESTED", trancheInactive);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier", inactif.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void prospectionSurEntiteInactive_absenteDeLaListe() throws Exception {
        ComProspection active = newProspection("PSN-VIS-ACTIVE", trancheActive);
        ComProspection inactive = newProspection("PSN-VIS-INACTIVE", trancheInactive);

        mockMvc.perform(get("/api/prospections").param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + active.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + inactive.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------ §6.3 — R2, le contre-test

    /**
     * <b>Le test qui distingue une implémentation correcte d'une jointure interne.</b> 192
     * prospections de la base réelle n'ont aucune tranche, donc aucune entité : les masquer
     * perdrait autant de leads pour protéger une donnée que personne ne revendique.
     *
     * <p>Une implémentation posant {@code JOIN … cfgCompany} au lieu de {@code LEFT JOIN}, ou
     * écrivant {@code isActive = true} sans la clause {@code OR société IS NULL}, passe tous les
     * autres tests de cette classe et échoue uniquement ici.
     */
    @Test
    @WithMockUser
    void prospectionSansTranche_doncAucuneEntite_resteVisible() throws Exception {
        ComProspection orpheline = newProspection("PSN-VIS-ORPHELINE", null);

        mockMvc.perform(get("/api/prospections").param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + orpheline.getId() + "')]").exists());

        mockMvc.perform(get("/api/prospections/{id}", orpheline.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void dossierSansArticle_doncSansEntite_resteVisible() throws Exception {
        ComFolder orphelin = newDossierSansArticle("DOS-VIS-ORPHELIN");

        mockMvc.perform(get("/api/dossiers/{id}", orphelin.getId()))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §6.4 — clients

    @Test
    @WithMockUser
    void client_presentSeulementSiUnDeSesDossiersEstSurUneEntiteActive() throws Exception {
        CfgTier clientVisible = newTier("CLI-VIS-OK");
        CfgTier clientMasque = newTier("CLI-VIS-KO");

        rattacher(newDossier("DOS-CLI-VIS-OK", trancheActive), clientVisible);
        rattacher(newDossier("DOS-CLI-VIS-KO", trancheInactive), clientMasque);

        mockMvc.perform(get("/api/clients").param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + clientVisible.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + clientMasque.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------ §6.5 — portée du filtre

    /**
     * {@code Administrateur} appartient à {@code RolesExemptesPerimetre} : il ne subit aucun
     * périmètre commercial. Il subit malgré tout le filtre d'entité — c'est ce qui distingue les
     * deux restrictions. Une implémentation qui aurait branché la visibilité sur la résolution du
     * périmètre (donc sautée pour les exemptés) échoue ici.
     */
    @Test
    void administrateur_exempteDePerimetre_subitQuandMemeLeFiltreDentite() throws Exception {
        AdmUser admin = newUser("vis.admin." + UUID.randomUUID());
        donnerRole(admin, "Administrateur");

        ComFolder inactif = newDossier("DOS-VIS-ADMIN", trancheInactive);

        mockMvc.perform(get("/api/dossiers").param("size", "2000")
                        .with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + inactif.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/dossiers/{id}", inactif.getId())
                        .with(connecte(admin, "Administrateur")))
                .andExpect(status().isNotFound());
    }

    /**
     * Les deux restrictions se composent en {@code ET}, pas en {@code OU} : un compte affecté à un
     * projet d'une entité hors exploitation ne voit rien, alors que le périmètre seul lui aurait
     * ouvert ce projet.
     */
    @Test
    void perimetreSurUnProjetDentiteInactive_neVoitRien() throws Exception {
        AdmUser commercial = newUser("vis.commercial." + UUID.randomUUID());
        affecter(commercial, projetInactif);

        ComFolder inactif = newDossier("DOS-VIS-COMPOSE", trancheInactive);

        mockMvc.perform(get("/api/dossiers").param("size", "2000")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + inactif.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------ fixtures

    private CfgCompany newSociete(String code, boolean active) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        societe.setDescription("Société " + code);
        societe.setIsActive(active);
        return companyRepository.save(societe);
    }

    private CfgProject newProjet(String code, CfgCompany societe) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Projet " + code);
        projet.setCfgCompany(societe);
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(String code, CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        tier.setLastName("Testeur");
        return tierRepository.save(tier);
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

    private RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getCode(), null, false, true, 0);
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private ComFolder newDossierSansArticle(String code) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        folder.setStkVocation(vocation);
        folder.setTotalToPay(new BigDecimal("100000"));
        return folderRepository.save(folder);
    }

    /** Dossier rattaché à {@code tranche} par son article principal ({@code ComFolder.StkItemId}). */
    private ComFolder newDossier(String code, CfgTranche tranche) {
        ComFolder folder = newDossierSansArticle(code);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-" + folder.getCode());
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setComFolderId(folder.getId());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());

        // Reprendre l'instance renvoyée par save() : l'id étant assigné, Spring Data passe par
        // merge() et rend une *autre* instance gérée. Rattacher l'originale ferait voir à
        // Hibernate un ComFolder géré pointant une StkItem transiente, et le flush échouerait.
        folder.setStkItem(stkItemRepository.save(article));
        return folderRepository.save(folder);
    }

    private void rattacher(ComFolder dossier, CfgTier client) {
        dossier.setCfgCustomer(client);
        folderRepository.save(dossier);
    }

    /** {@code tranche} peut être {@code null} — c'est le cas « aucune entité résoluble » (R2). */
    private ComProspection newProspection(String code, CfgTranche tranche) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        prospection.setCfgProspect(newTier("PRO-" + code));
        prospection.setStkHierarchy(tranche);
        return prospectionRepository.save(prospection);
    }
}
