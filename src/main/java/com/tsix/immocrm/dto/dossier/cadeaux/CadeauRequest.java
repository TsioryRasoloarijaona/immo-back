package com.tsix.immocrm.dto.dossier.cadeaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Saisie d'un cadeau ; l'enregistrement remplace tous les champs.")
public record CadeauRequest(
        @Schema(description = "Cadeau du catalogue (`GET …/cadeaux/catalogue`) ; peut rester vide (« choisir plus tard »).")
        UUID cadeauId,
        @Schema(description = "Prix ; absent, celui du catalogue.")
        BigDecimal prix,
        @Schema(description = "Livré ; impose `dateLivraison`.")
        Boolean livre,
        @Schema(description = "Raison (`GET …/cadeaux/raisons`).")
        UUID raisonId,
        LocalDateTime dateBon,
        @Schema(description = "Date de livraison ; obligatoire si `livre` (400 sinon).")
        LocalDateTime dateLivraison,
        String notes
) {
}
