package com.tsix.immocrm.dto.prospect;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** {@code StlSettlement} filtered by TierId — feeds the prospect fiche's "Règlements" tab. */
public record ReglementProspectDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        LocalDateTime dateReglement,
        BigDecimal montant,
        String type,
        UUID dossierId,
        String dossierCode
) {
}
