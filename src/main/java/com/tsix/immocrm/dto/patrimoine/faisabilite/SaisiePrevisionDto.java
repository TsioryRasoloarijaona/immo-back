package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de prévision saisie (FSPrevision), clé = catégorie. Priorité ratio > montantHt > montant.")
public record SaisiePrevisionDto(
        @Schema(description = "Catégorie de trésorerie (StlCategory), obligatoire, hors « sans prévision ».") UUID categorieId,
        @Schema(description = "Mode par unité : ligne d'unité de l'étude ; base = sa quantité.") UUID uniteEtudeId,
        @Schema(description = "Mode pourcentage : catégorie de l'arbre ; base = son total HT, enfants compris.")
        UUID categorieReferenceId,
        @Schema(description = "Prix unitaire (unité) ou fraction (pourcentage), ≥ 0.") BigDecimal ratio,
        @Schema(description = "Montant HT propre (≥ 0) ; recalcule le ratio si ratio absent.") BigDecimal montantHt,
        @Schema(description = "Montant TTC propre (≥ 0) ; utilisé si ratio et montantHt absents.") BigDecimal montant,
        @Schema(description = "TVA (fraction 0-1) ; absente = TVA par défaut de l'étude.") BigDecimal tauxTva,
        @Schema(description = "Visible dans le module Projet (vrai si absent).") Boolean estPourProjet,
        @Schema(description = "« Traiter au début du projet » : la dépense est datée ce jour-là (indivise).")
        LocalDateTime dateApplication) {
}
