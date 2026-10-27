package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.dto.commission.DemandesCommission.DroitsCommissionDto;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Droits de l'écran Commissions lus dans {@code AdmRight} par {@code AdmUserRole} à chaque appel, jamais le JWT
 * (FEATURE-vente-commissions.md §3.2). Un droit absent d'{@code AdmRight} est refusé ; l'outil exige aussi l'écran.
 */
@Component
public class DroitsCommission {

    public enum Droit {
        LECTURE(""), ECRITURE(".toolNew"), SUPPRESSION(".toolDelete"), IMPRESSION(".toolPrint");

        private final String code;

        Droit(String suffixe) {
            this.code = ECRAN + suffixe;
        }
    }

    static final String ECRAN = "Yes.Com.Front.Commission.CommissionList";

    private static final String DROITS_DU_COMPTE = """
            SELECT DISTINCT r.code FROM AdmRight r, AdmUserRole ur
            WHERE ur.admRole.id = r.admRoleId AND ur.admUser.id = :compte AND r.code LIKE :ecran""";

    private final EntityManager em;

    public DroitsCommission(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public DroitsCommissionDto lire() {
        Set<String> codes = codesExigeantLEcran();
        return new DroitsCommissionDto(codes.contains(Droit.ECRITURE.code), codes.contains(Droit.SUPPRESSION.code),
                codes.contains(Droit.IMPRESSION.code));
    }

    /** 403 sans l'écran ou sans l'outil. */
    @Transactional(readOnly = true)
    public void exiger(Droit droit) {
        if (!codesExigeantLEcran().contains(droit.code)) {
            throw new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action sur les commissions.");
        }
    }

    private Set<String> codesExigeantLEcran() {
        UUID compte = PerimetreCommercialService.admUserIdCourant()
                .orElseThrow(() -> new ForbiddenException("Compte inconnu."));
        List<String> codes = em.createQuery(DROITS_DU_COMPTE, String.class).setParameter("compte", compte)
                .setParameter("ecran", ECRAN + "%").getResultList();
        if (!codes.contains(ECRAN)) {
            throw new ForbiddenException("Vous n'avez pas accès aux commissions.");
        }
        return codes.stream().collect(Collectors.toUnmodifiableSet());
    }
}
