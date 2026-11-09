package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Saisie d'un projet (création ou remplacement complet) ; statut et type d'objet sont posés par le serveur.")
public record ProjetConfigRequest(
        @Schema(description = "Code, obligatoire, unique (casse et espaces ignorés), espaces des bords retirés.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "ARS-PRJ") String code,
        @Schema(description = "Libellé, obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(description = "Description détaillée, 500 caractères au plus.") String descriptionLongue,
        @Schema(description = "Adresse, 500 caractères au plus.") String adresse,
        @Schema(description = "Notes, 1000 caractères au plus.") String notes,
        @Schema(description = "Société (CfgCompany.PKey), obligatoire.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID societeId,
        @Schema(description = "Gestionnaire (CfgTier.PKey d'un membre du personnel), facultatif.") UUID gestionnaireId,
        @Schema(description = "Projet ventilé ; absent = faux.") Boolean ventile,
        @Schema(description = "Taux de ventilation par défaut, positif ou nul ; absent = 0.") BigDecimal tauxVentilationDefaut,
        @Schema(description = "Parties communes ; absent = faux.") Boolean partiesCommunes
) {
}
