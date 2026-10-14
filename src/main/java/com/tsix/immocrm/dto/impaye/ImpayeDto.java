package com.tsix.immocrm.dto.impaye;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Une échéance de dossier de vente échue et non soldée ({@code FEATURE-impayes.md} R1-R4). */
@Schema(description = "Échéance (ComDocument) d'un dossier de vente actif, échue, dont le reste à payer est positif.")
public record ImpayeDto(
        @Schema(description = "Id de l'échéance (ComDocument.PKey).")
        UUID echeanceId,
        @Schema(description = "Id du dossier de vente (ComFolder.PKey).")
        UUID dossierId,
        @Schema(description = "Code du dossier de vente.", example = "RES-2024-0012")
        String dossierCode,
        @Schema(description = "Code du statut du dossier (StrStatus.Code).", example = "FORMALISE")
        String statutDossier,
        @Schema(description = "Date d'échéance (ComDocument.DocDate).")
        LocalDateTime dateEcheance,
        @Schema(description = "Calculée : date d'échéance + RecoveryAge jours (90 par défaut).")
        LocalDateTime dateRecouvrement,
        @Schema(description = "Libellé de la catégorie de règlement de l'échéance.", example = "AVANCE")
        String categorie,
        @Schema(description = "Libellé de l'échéance (ComDocument.Description).")
        String description,
        @Schema(description = "Id du client du dossier (CfgTier.PKey).")
        UUID clientId,
        @Schema(description = "Nom affiché du client du dossier.")
        String clientNom,
        @Schema(description = "Libellé du projet, par l'article principal du dossier puis sa tranche.")
        String projet,
        @Schema(description = "Libellé de la tranche de l'article principal du dossier.")
        String tranche,
        @Schema(description = "Code de l'article principal du dossier.")
        String article,
        @Schema(description = "Société du projet ; null si la chaîne article → tranche → projet est incomplète.")
        String societe,
        @Schema(description = "Nom affiché du commercial du dossier.")
        String commercial,
        @Schema(description = "Montant de l'échéance (ComDocument.Amount, TTC).")
        BigDecimal montantAPayer,
        @Schema(description = "Calculé : règlements affectés, annulés exclus, décaissements soustraits (comme l'échéancier).")
        BigDecimal montantRegle,
        @Schema(description = "Calculé : montantAPayer − montantRegle, arrondi au centime ; toujours > 0 ici.")
        BigDecimal montantReste,
        @Schema(description = "Calculé : jours écoulés depuis la date d'échéance, jamais négatif.", example = "42")
        long joursRetard,
        @Schema(description = "Id de l'échéance pénalité de retard liée (ComPenaltyDocumentId) ; null si aucune.")
        UUID penaliteId,
        @Schema(description = "Notes de l'échéance.")
        String notes
) {
}
