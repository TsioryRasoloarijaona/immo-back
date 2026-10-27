package com.tsix.immocrm.dto.commission;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de commission calculée (ComCommissionLine).")
public record LigneCommissionCalculeeDto(
        UUID id,
        @Schema(description = "Date du fait (concrétisation, finalisation ou annulation).") LocalDate date,
        @Schema(description = "Libellé stocké : Concrétisation, Finalisation 1, Finalisation 2, Désistement, ou variante (support).",
                example = "Concrétisation") String type,
        @Schema(description = "Montant ; négatif pour un désistement.") BigDecimal montant,
        @Schema(description = "Mode de calcul : « Montant » ou « 0.03 % du prix de vente TTC ».") String modeCalcul,
        UUID dossierId,
        String dossierCode,
        String client,
        String articleCode,
        @Schema(description = "Libellé de l'article.") String article,
        @Schema(description = "Catégorie de l'article.") String categorie,
        @Schema(description = "Libellé de la tranche.") String tranche,
        @Schema(description = "Statut actuel du dossier.") String statut,
        @Schema(description = "Personne commissionnée (commercial ou gestionnaire).") UUID commercialId,
        String commercial,
        @Schema(description = "Part réglée de la ligne : réglé du document au prorata du montant de ses lignes.")
        BigDecimal regle,
        @Schema(description = "montant − regle.") BigDecimal reste,
        @Schema(description = "Document à payer ; null si aucun.") UUID documentId,
        String documentCode,
        @Schema(description = "Faux si la ligne a un document à payer : montant et suppression refusés.") boolean modifiable) {
}
