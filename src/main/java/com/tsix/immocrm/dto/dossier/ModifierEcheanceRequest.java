package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Mise à jour partielle : seuls les champs non nuls sont appliqués. */
public record ModifierEcheanceRequest(
        LocalDateTime dateEcheance,
        BigDecimal montant,
        String description,
        UUID categorieId,
        Boolean exclureDuPrix,
        String notes
) {
}
