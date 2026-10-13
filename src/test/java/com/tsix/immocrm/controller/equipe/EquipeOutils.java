package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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
 * Données des tests « Mon équipe » : les rôles Commercial Chef / Commercial n'existent pas dans le jeu de la CI,
 * chaque test les crée. Projets sur une société active : la liste des projets exclut les autres.
 */
@Component
public class EquipeOutils {

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

    public EquipeOutils(AdmUserRepository userRepository, AdmRoleRepository roleRepository,
                        AdmUserRoleRepository userRoleRepository, AdmUserHabilitationRepository habilitationRepository,
                        CfgCompanyRepository companyRepository, CfgProjectRepository projectRepository,
                        CfgTrancheRepository trancheRepository, CfgTierRepository tierRepository,
                        ComProspectionRepository prospectionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.habilitationRepository = habilitationRepository;
        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.tierRepository = tierRepository;
        this.prospectionRepository = prospectionRepository;
    }

    public CfgProject projet(String code) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-" + code);
        societe.setDescription("Société " + code);
        societe.setIsActive(true);
        companyRepository.save(societe);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Projet " + code);
        projet.setCfgCompany(societe);
        return projectRepository.save(projet);
    }

    public CfgTranche tranche(String code, CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    CfgTier tier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName("Nom " + code);
        return tierRepository.save(tier);
    }

    /** Compte actif, rattaché à un employé (CfgTier) du même code, avec ses rôles. */
    public AdmUser compte(String code, String... roles) {
        return compte(code, tier("EMP-" + code), false, roles);
    }

    AdmUser compte(String code, CfgTier employe, boolean desactive, String... roles) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code);
        user.setDescription("Compte " + code);
        user.setDisabled(desactive);
        user.setCfgEmployee(employe);
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

    public void affecter(AdmUser user, CfgProject... projets) {
        for (CfgProject projet : projets) {
            AdmUserHabilitation habilitation = new AdmUserHabilitation();
            habilitation.setId(UUID.randomUUID());
            habilitation.setAdmUser(user);
            habilitation.setCfgProject(projet);
            habilitationRepository.save(habilitation);
        }
    }

    List<UUID> projetsDe(AdmUser user) {
        return habilitationRepository.findProjetIdsByAdmUserId(user.getId());
    }

    /** Pré-prospection avec un prospect lié (transformable). */
    public ComProspection preProspection(String code, CfgTranche tranche, LocalDateTime docDate) {
        ComProspection p = ligne(code, tranche, docDate);
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setCfgProspect(tier("PROS-" + code));
        return prospectionRepository.save(p);
    }

    ComProspection prospection(String code, CfgTranche tranche, CfgTier commercial, LocalDateTime docDate) {
        ComProspection p = ligne(code, tranche, docDate);
        p.setStrEntityId(ID_PROSPECTION_VENTE);
        p.setCfgProspect(tier("PROS-" + code));
        p.setCfgCommercial(commercial);
        return prospectionRepository.save(p);
    }

    private static ComProspection ligne(String code, CfgTranche tranche, LocalDateTime docDate) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setDocDate(docDate);
        p.setStkHierarchy(tranche);
        p.setIsClosed(false);
        return p;
    }

    public static RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(),
                user.getCfgEmployee() != null ? user.getCfgEmployee().getId() : null, false, true, 0);
        List<GrantedAuthority> autorites = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, autorites));
    }
}
