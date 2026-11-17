package com.tsix.immocrm.dto.achat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Formulaire vierge d'une pièce d'achat : les champs de la saisie pré-remplis, puis les valeurs lues.")
public record ModelePieceAchatDto(
        @JsonUnwrapped SaisiePieceAchatRequest saisie,
        @Schema(description = "Code de la gestion de création (StrEntity) ; gestion d'arrivée d'une génération.", example = "BuyOrder") String gestion,
        @Schema(description = "Statut de départ du workflow de la gestion ; null sans workflow.") UUID statutDepartId,
        @Schema(description = "Libellé du statut de départ ; null sans workflow (devis).") String statutDepartLibelle,
        @Schema(description = "Devise de la 1re source d'une génération, sinon devise par défaut (DefaultCurrency).")
        UUID deviseId,
        @Schema(description = "TVA proposée pour une nouvelle ligne, en fraction.", example = "0.2") BigDecimal tvaParDefaut) {
}
