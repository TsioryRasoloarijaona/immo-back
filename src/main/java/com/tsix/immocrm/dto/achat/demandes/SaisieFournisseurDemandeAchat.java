package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Saisie d'un fournisseur présélectionné ; seul fournisseurId est obligatoire.")
public record SaisieFournisseurDemandeAchat(
        @Schema(description = "Fournisseur (CfgTier.PKey), unique dans la demande.") UUID fournisseurId,
        @Schema(description = "Référence O.D.T. (255 caractères au plus).") String referenceOdt,
        @Schema(description = "Lettre de consultation envoyée le.") LocalDate lettreEnvoyee,
        @Schema(description = "Retrait du dossier le.") LocalDate retraitDossier,
        @Schema(description = "Dépôt de l'offre le.") LocalDate depot,
        @Schema(description = "Notes (1000 caractères au plus).") String notes) {
}
