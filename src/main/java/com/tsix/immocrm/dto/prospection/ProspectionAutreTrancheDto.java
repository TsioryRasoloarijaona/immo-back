package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/** Ligne {@code ComProspectionProject} : intérêt secondaire, indépendant de la tranche/l'article principal. */
public record ProspectionAutreTrancheDto(
        UUID id,
        UUID trancheId,
        String tranche,
        String trancheDescription,
        UUID articleId,
        String article,
        String articleDescription
) {
}
