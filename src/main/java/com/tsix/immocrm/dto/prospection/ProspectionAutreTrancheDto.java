package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/** {@code ComProspectionProject} row — the "Autres tranches" tab. */
public record ProspectionAutreTrancheDto(
        UUID id,
        UUID trancheId,
        String tranche,
        UUID articleId,
        String article
) {
}
