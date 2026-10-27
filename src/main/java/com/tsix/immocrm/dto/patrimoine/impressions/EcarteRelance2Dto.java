package com.tsix.immocrm.dto.patrimoine.impressions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dossier retenu par son statut mais sans lettre : reste du prix nul ou négatif (trop-perçu).")
public record EcarteRelance2Dto(
        @Schema(description = "Id du dossier (ComFolder.PKey).")
        UUID dossierId,
        @Schema(description = "Code du dossier.", example = "APF 2022/0012")
        String codeDossier,
        @Schema(description = "Code de l'article du dossier.", example = "APF-0-12")
        String article,
        @Schema(description = "Libellé du statut du dossier.", example = "CONCRETISATION")
        String statut,
        @Schema(description = "Nom du client principal.")
        String client,
        @Schema(description = "Prix de vente du dossier (prixVente.montant).", example = "200000.00")
        BigDecimal prixVente,
        @Schema(description = "Réglé sur le prix (prixVente.regle).", example = "200000.00")
        BigDecimal prixRegle,
        @Schema(description = "Reste du prix (prixVente.reste), ≤ 0.", example = "0.00")
        BigDecimal reste) {
}
