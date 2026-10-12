package com.tsix.immocrm.controller.parcours;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Données des tests du parcours commercial ; tout est créé par le test (yesRemsTest n'a ni étapes, ni types,
 * ni résultats d'interaction). Les codes de résultat sont ceux d'OMRAN, la PKey est propre au test.
 */
@Component
public class ParcoursOutils {

    /** StrEntity « ComProspection » (prospection de vente), même PKey en test et en CI. */
    static final UUID ID_PROSPECTION_VENTE = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");

    private final AdmUserRepository userRepository;
    private final AdmRoleRepository roleRepository;
    private final AdmUserRoleRepository userRoleRepository;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final CfgCompanyRepository companyRepository;
    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final CfgTierRepository tierRepository;
    private final ComProspectionRepository prospectionRepository;
    private final ComProspectionKindRepository kindRepository;
    private final ComActionMessageRepository messageRepository;
    private final ComActionMessageSubCategoryRepository sousCategorieRepository;
    private final FixtureInteraction fixtureInteraction;

    @SuppressWarnings("java:S107") // fixture de test : un dépôt par table écrite
    public ParcoursOutils(AdmUserRepository userRepository, AdmRoleRepository roleRepository,
                          AdmUserRoleRepository userRoleRepository, AdmUserHabilitationRepository habilitationRepository,
                          CfgCompanyRepository companyRepository, CfgProjectRepository projectRepository,
                          CfgTrancheRepository trancheRepository, CfgTierRepository tierRepository,
                          ComProspectionRepository prospectionRepository, ComProspectionKindRepository kindRepository,
                          ComActionMessageRepository messageRepository,
                          ComActionMessageSubCategoryRepository sousCategorieRepository,
                          FixtureInteraction fixtureInteraction) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.habilitationRepository = habilitationRepository;
        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.tierRepository = tierRepository;
        this.prospectionRepository = prospectionRepository;
        this.kindRepository = kindRepository;
        this.messageRepository = messageRepository;
        this.sousCategorieRepository = sousCategorieRepository;
        this.fixtureInteraction = fixtureInteraction;
    }

    CfgTranche tranche(String code) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-" + code);
        societe.setDescription("Société " + code);
        societe.setIsActive(true);
        companyRepository.save(societe);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-" + code);
        projet.setDescription("Projet " + code);
        projet.setCfgCompany(societe);
        projectRepository.save(projet);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    CfgTier tier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code + "-" + UUID.randomUUID());
        tier.setFirstName("Prénom" + code);
        tier.setLastName("Nom " + code);
        tier.setMobile("0661248735");
        return tierRepository.save(tier);
    }

    /** Compte actif rattaché à un employé, avec ses rôles {@code AdmUserRole}. */
    AdmUser compte(String code, String... roles) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code + "-" + UUID.randomUUID());
        user.setDescription("Compte " + code);
        user.setDisabled(false);
        user.setCfgEmployee(tier("EMP-" + code));
        userRepository.save(user);
        for (String codeRole : roles) {
            AdmRole role = new AdmRole();
            role.setId(UUID.randomUUID());
            role.setCode(codeRole);
            roleRepository.save(role);
            AdmUserRole userRole = new AdmUserRole();
            userRole.setId(UUID.randomUUID());
            userRole.setAdmUser(user);
            userRole.setAdmRole(role);
            userRoleRepository.save(userRole);
        }
        return user;
    }

    void affecter(AdmUser user, CfgTranche tranche) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(user);
        habilitation.setCfgProject(tranche.getCfgProject());
        habilitationRepository.save(habilitation);
    }

    ComProspection preProspection(AdmUser commercial, LocalDateTime recue) {
        ComProspection p = ligne(recue);
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setTempDescription("Lead TEST");
        p.setTempTel("06 61 24 87 35");
        p.setCfgCommercial(commercial != null ? commercial.getCfgEmployee() : null);
        return prospectionRepository.save(p);
    }

    ComProspection prospection(AdmUser commercial, CfgTranche tranche, LocalDateTime creee) {
        ComProspection p = ligne(creee);
        p.setStrEntityId(ID_PROSPECTION_VENTE);
        p.setCfgProspect(tier("PROS"));
        p.setCfgCommercial(commercial.getCfgEmployee());
        p.setStkHierarchy(tranche);
        return prospectionRepository.save(p);
    }

    private static ComProspection ligne(LocalDateTime docDate) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PC-" + UUID.randomUUID());
        p.setDocDate(docDate);
        p.setUserCreateDateTime(docDate);
        p.setIsClosed(false);
        return p;
    }

    /** Interaction datée d'une relance, non désarmée, avec son résultat (nul = sans résultat). */
    ComActionMessage relance(ComProspection p, LocalDateTime echeance, ComActionMessageSubCategory resultat) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setDocDate(LocalDateTime.now().minusDays(1));
        m.setAlertValidityDate(echeance);
        m.setAlertMessage("rappeler le client");
        m.setDescription("libelle");
        m.setWithoutRevival(false);
        m.setIsUnpaid(false);
        m.setIsSystem(false);
        m.setComActionMessageSubCategory(resultat);
        return messageRepository.saveAndFlush(m);
    }

    /** Résultat de la catégorie Prospection, créé si absent (même code qu'OMRAN). */
    ComActionMessageSubCategory resultat(String code, boolean dateObligatoire) {
        var categorie = fixtureInteraction.categorieProspection();
        return sousCategorieRepository.findFirstByCodeAndComActionMessageCategoryId(code, categorie.getId())
                .orElseGet(() -> {
                    ComActionMessageSubCategory s = new ComActionMessageSubCategory();
                    s.setId(UUID.randomUUID());
                    s.setCode(code);
                    s.setDescription(code);
                    s.setComActionMessageCategory(categorie);
                    s.setIsRevivalMandatory(dateObligatoire);
                    return sousCategorieRepository.save(s);
                });
    }

    ComProspectionKind etape(String code, boolean projetObligatoire) {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        kind.setDescription(code);
        kind.setIsWithProjectMandatory(projetObligatoire);
        return kindRepository.save(kind);
    }

    static RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(),
                user.getCfgEmployee() != null ? user.getCfgEmployee().getId() : null, false, true, 0);
        List<GrantedAuthority> autorites = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, autorites));
    }
}
