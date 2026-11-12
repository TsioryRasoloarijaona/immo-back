package com.tsix.immocrm.services.droits;

import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Droits OMRAN ({@code AdmRight.Code}) du compte courant, lus en base par ses rôles à chaque appel, jamais dans le JWT ;
 * le rôle Administrateur a tous les droits. Sans {@code @Transactional} : joint la transaction de l'appelant.
 */
@Component
public class DroitsOmran {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String DROITS_DU_COMPTE = """
            SELECT DISTINCT r.code FROM AdmRight r, AdmUserRole ur
            WHERE ur.admRole.id = r.admRoleId AND ur.admUser.id = :compte AND r.code IN :codes""";

    private final AdmUserRoleRepository roles;
    private final EntityManager em;

    public DroitsOmran(AdmUserRoleRepository roles, EntityManager em) {
        this.roles = roles;
        this.em = em;
    }

    /** Codes parmi {@code codes} que porte le compte courant (tous pour l'Administrateur, aucun sans compte). */
    public Set<String> parmi(String... codes) {
        UUID compte = PerimetreCommercialService.admUserIdCourant().orElse(null);
        if (compte == null) {
            return Set.of();
        }
        if (roles.existsByAdmUserIdAndAdmRole_Code(compte, ADMINISTRATEUR)) {
            return Set.of(codes);
        }
        return em.createQuery(DROITS_DU_COMPTE, String.class).setParameter("compte", compte)
                .setParameter("codes", List.of(codes)).getResultList().stream().collect(Collectors.toUnmodifiableSet());
    }

    /** 403 si l'un des codes manque. */
    public void exiger(String... codes) {
        if (!parmi(codes).containsAll(List.of(codes))) {
            throw new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action.");
        }
    }
}
