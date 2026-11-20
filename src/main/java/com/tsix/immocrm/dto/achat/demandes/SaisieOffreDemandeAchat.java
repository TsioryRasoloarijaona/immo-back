package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Création d'une offre : le fournisseur présélectionné qui la dépose.")
public record SaisieOffreDemandeAchat(
        @Schema(description = "Fournisseur (CfgTier.PKey), présélectionné, non écarté et avec une date de dépôt.") UUID fournisseurId) {
}
