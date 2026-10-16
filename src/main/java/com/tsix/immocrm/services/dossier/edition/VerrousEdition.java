package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.dto.dossier.edition.ModifierDossierRequest;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ForbiddenException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Champs verrouillés pour l'appelant ({@code Sale.cs} : {@code toolModifyAdvanceDate}, {@code toolModifyPrintDate},
 * {@code Sale_Load}) : seule source de {@code champsVerrouilles} et des 403 correspondants. Le groupe restreint est
 * {@link GroupeEdition#PRIX}. Voir FEATURE-dossier-modification.md B3–B5, B8.
 */
final class VerrousEdition {

    static final String CONCRETISATION = "concretisation";
    static final String IMPRESSION_CONTRAT = "impressionContrat";
    static final String REMISE = "remise";

    private VerrousEdition() {
    }

    static List<String> verrouilles(ComFolder f, Set<String> roles) {
        List<String> champs = new ArrayList<>();
        boolean restreint = GroupeEdition.PRIX.autorise(roles);
        if (!restreint && f.getAdvanceDate() != null) {
            champs.add(CONCRETISATION);
        }
        if (!restreint) {
            champs.add(IMPRESSION_CONTRAT);
        }
        if (PlafondRemise.verrouillee(premierPlafond(f), PlafondRemise.niveauDe(roles))) {
            champs.add(REMISE);
        }
        return List.copyOf(champs);
    }

    /** 403 si la requête change un champ daté verrouillé ; la remise est contrôlée par {@link PrixEdition}. */
    static void verifier(ComFolder f, ModifierDossierRequest r, Set<String> aVider, Set<String> roles) {
        List<String> verrouilles = verrouilles(f, roles);
        refuserSiChange(verrouilles, CONCRETISATION, r.concretisation(), f.getAdvanceDate(), aVider);
        refuserSiChange(verrouilles, IMPRESSION_CONTRAT, r.impressionContrat(), f.getContractPrintDate(), aVider);
    }

    private static void refuserSiChange(List<String> verrouilles, String champ, LocalDateTime saisie,
                                        LocalDateTime actuelle, Set<String> aVider) {
        boolean change = aVider.contains(champ) ? actuelle != null
                : saisie != null && !Objects.equals(saisie, actuelle);
        if (change && verrouilles.contains(champ)) {
            throw new ForbiddenException("Modification de « %s » réservée aux rôles : %s."
                    .formatted(champ, String.join(", ", GroupeEdition.PRIX.roles().stream().sorted().toList())));
        }
    }

    static BigDecimal premierPlafond(ComFolder f) {
        return f.getStkItem() != null ? f.getStkItem().getDiscountMax() : null;
    }
}
