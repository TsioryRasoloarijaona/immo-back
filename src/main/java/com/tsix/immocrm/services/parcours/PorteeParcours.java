package com.tsix.immocrm.services.parcours;

import com.tsix.immocrm.domain.RolesExemptesPerimetre;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.services.equipe.EquipeCommercialeService;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seul calcul de « mes lignes » du parcours commercial (FEATURE-parcours-commercial.md §3) : le commercial
 * d'une ligne est {@code ComProspection.CfgCommercialId}, comparé à l'employé du compte appelant.
 */
@Service
@Transactional(readOnly = true)
public class PorteeParcours {

    private final AdmUserRepository userRepository;
    private final AdmUserRoleRepository userRoleRepository;
    private final EquipeCommercialeService equipeService;

    public PorteeParcours(AdmUserRepository userRepository, AdmUserRoleRepository userRoleRepository,
                          EquipeCommercialeService equipeService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.equipeService = equipeService;
    }

    /** Compte appelant relu en base ; {@code employeId} nul = compte sans employé, qui n'a aucune ligne. */
    public record Appelant(UUID admUserId, @Nullable UUID employeId, boolean exempte, boolean chef) {
    }

    /** 403 si l'appelant n'a pas de compte applicatif (clé d'API, compte supprimé). */
    public Appelant appelant() {
        UUID admUserId = PerimetreCommercialService.admUserIdCourant()
                .orElseThrow(() -> new ForbiddenException("Compte applicatif non identifiable."));
        AdmUser compte = userRepository.findById(admUserId)
                .orElseThrow(() -> new ForbiddenException("Compte applicatif non identifiable."));
        Set<String> roles = userRoleRepository.findByAdmUserId(admUserId).stream()
                .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        CfgTier employe = compte.getCfgEmployee();
        return new Appelant(admUserId, employe != null ? employe.getId() : null,
                RolesExemptesPerimetre.exempte(roles), roles.contains(RolesEquipe.CHEF));
    }

    /**
     * Commercial (employé {@code CfgTier}) dont on lit les lignes. {@code commercialId} est réservé aux exemptés
     * et aux chefs pour un membre de leur équipe : 403 sinon. Nul = l'appelant n'a pas d'employé.
     */
    public @Nullable UUID commercialLu(@Nullable UUID commercialId) {
        Appelant appelant = appelant();
        if (commercialId == null || commercialId.equals(appelant.employeId())) {
            return appelant.employeId();
        }
        if (!peutVoirLesLignesDe(appelant, commercialId)) {
            throw new ForbiddenException("Vous ne pouvez consulter que vos lignes, ou celles de votre équipe.");
        }
        return commercialId;
    }

    /** 404, comme une ligne hors périmètre : la ligne n'est ni à l'appelant, ni à son équipe. */
    public void verifierLigne(ComProspection prospection) {
        Appelant appelant = appelant();
        UUID commercial = prospection.getCfgCommercial() != null ? prospection.getCfgCommercial().getId() : null;
        boolean autorisee = appelant.exempte()
                || (commercial != null && (commercial.equals(appelant.employeId())
                        || peutVoirLesLignesDe(appelant, commercial)));
        if (!autorisee) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospection.getId());
        }
    }

    private boolean peutVoirLesLignesDe(Appelant appelant, UUID commercialId) {
        if (appelant.exempte()) {
            return true;
        }
        return appelant.chef() && equipeService.perimetre().membres().stream()
                .anyMatch(m -> commercialId.equals(m.employeId()));
    }
}
