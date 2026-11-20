package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Référence citée par une demande interne d'achat (procédure, société, projet, lot…).")
public record ElementDemandeAchat(
        @Schema(description = "PKey de la ligne citée.") UUID id,
        @Schema(description = "Code de la ligne citée ; null si la table n'en a pas.") String code,
        @Schema(description = "Libellé (Description, ou raison sociale d'un tiers).") String libelle) {
}
