package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Création d'une demande interne d'achat depuis un lot ; le reste est copié du lot.")
public record SaisieDemandeAchatLot(
        @Schema(description = "Procédure d'achat (BuySelectionKind.PKey) ; remplacée par celle du montant si l'estimé TTC du lot ≠ 0.")
        UUID procedureId,
        @Schema(description = "Description de la demande, obligatoire.") String description) {
}
