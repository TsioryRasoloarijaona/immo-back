package com.tsix.immocrm.dto.dossier.reajustement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dossier dont la superficie diffère du stock, avec l'écart et la ligne du schéma qui le reçoit.")
public record LigneReajustementDto(
        @Schema(description = "Identifiant du dossier (ComFolder.PKey), à cocher pour le POST.")
        UUID dossierId,
        @Schema(description = "Code du dossier de vente.")
        String dossierCode,
        @Schema(description = "Code de l'article principal.")
        String articleCode,
        @Schema(description = "Nom du client.")
        String client,
        @Schema(description = "Ancienne superficie (m²), celle du dossier.")
        BigDecimal ancienneSuperficie,
        @Schema(description = "Nouvelle superficie (m²), celle du stock.")
        BigDecimal nouvelleSuperficie,
        @Schema(description = "Nouvelle superficie − ancienne superficie (m²).")
        BigDecimal differenceSuperficie,
        @Schema(description = "Ancien prix de vente TTC du dossier (ComFolder.Price).")
        BigDecimal ancienPrix,
        @Schema(description = "Nouveau prix de vente TTC après recalcul de la remise.")
        BigDecimal nouveauPrix,
        @Schema(description = "Nouveau prix − ancien prix.")
        BigDecimal differencePrix,
        @Schema(description = "Montant ajouté (ou retranché si négatif) à la ligne cible du schéma de financement.")
        BigDecimal differenceSchema,
        @Schema(description = "Ligne cible : AUTOFINANCEMENT, CREDIT ou NOUVELLE_LIGNE_AUTOFINANCEMENT.")
        LigneCible ligneCible,
        @Schema(description = "Échéance modifiée (ComDocument.PKey), null si une ligne est créée.")
        UUID ligneCibleId,
        @Schema(description = "Date de l'échéance modifiée, null si une ligne est créée.")
        LocalDateTime ligneCibleDate,
        @Schema(description = "Détail des superficies modifiées (article et sous-articles), une par ligne.")
        String notes,
        @Schema(description = "Raison pour laquelle le dossier ne peut pas être réajusté (POST = 409) ; null sinon.")
        String motifExclusion
) {

    /** Où va la différence : « auto financement, sinon … crédit », sinon une nouvelle ligne d'autofinancement. */
    public enum LigneCible { AUTOFINANCEMENT, CREDIT, NOUVELLE_LIGNE_AUTOFINANCEMENT }
}
