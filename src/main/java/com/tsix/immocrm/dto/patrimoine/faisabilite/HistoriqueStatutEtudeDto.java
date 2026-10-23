package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne d'historique des statuts d'une étude (StkFeasibilityStudyStatusHistory).")
public record HistoriqueStatutEtudeDto(
        @Schema(description = "PKey de la ligne.") UUID id,
        @Schema(description = "Date du changement.") LocalDateTime date,
        @Schema(description = "Code du statut atteint.") String statutCode,
        @Schema(description = "Libellé du statut atteint.") String statutLibelle,
        @Schema(description = "Ordre du statut (Sorting).") Integer ordre,
        @Schema(description = "Utilisateur (AdmUser.Description) ; null si inconnu.") String utilisateur) {
}
