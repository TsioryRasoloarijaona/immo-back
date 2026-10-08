package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreerEcheanceRequest(
        LocalDateTime dateEcheance,
        BigDecimal montant,
        String description,
        UUID categorieId,
        Boolean exclureDuPrix,
        String notes
) {
}
