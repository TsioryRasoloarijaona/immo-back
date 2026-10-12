package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Seule règle de visibilité du module Chantier (FEATURE-projet-chantier-lecture.md §2) : rôle exempté → tout ;
 * sinon les {@code AdmUserHabilitation.PrjProjectId} de l'appelant, relus à chaque requête. Appelant non
 * identifiable → rien : on échoue fermé.
 */
@Service
@Transactional(readOnly = true)
public class PerimetreChantierService {

    private final AdmUserHabilitationRepository habilitationRepository;
    private final PerimetreCommercialService perimetreCommercialService;

    public PerimetreChantierService(AdmUserHabilitationRepository habilitationRepository,
                                    PerimetreCommercialService perimetreCommercialService) {
        this.habilitationRepository = habilitationRepository;
        this.perimetreCommercialService = perimetreCommercialService;
    }

    public PerimetreChantier perimetreChantierCourant() {
        return admUserIdCourant().map(this::perimetreChantierDe).orElse(PerimetreChantier.RIEN);
    }

    public PerimetreChantier perimetreChantierDe(UUID admUserId) {
        if (admUserId == null) {
            return PerimetreChantier.RIEN;
        }
        // Même liste de rôles exemptés que le périmètre commercial : une seule source.
        if (perimetreCommercialService.estExempte(admUserId)) {
            return PerimetreChantier.TOUT;
        }
        return new PerimetreChantier(false, new HashSet<>(habilitationRepository.findPrjProjetIdsByAdmUserId(admUserId)));
    }

    private static Optional<UUID> admUserIdCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.ofNullable(user.admUserId());
        }
        return Optional.empty();
    }
}
