package com.tsix.immocrm.dto.desistement;

import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** En-tête de {@code CreerReglementDossierRequest} : la pièce de résiliation du désistement est la seule affectation. */
@Schema(description = "Remboursement effectif (décaissement) d'un désistement validé.")
public record RembourserDesistementRequest(
        @Schema(description = "Montant remboursé, au plus le reste à rembourser.", example = "100000",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal montant,
        @Schema(description = "Date d'échéance du décaissement (`StlSettlement.EcheanceDate`).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dateEcheance,
        @Schema(description = "Type de règlement (`StlSettlementType.PKey`), voir `GET /api/referentiels/types-reglement`.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID typeId,
        @Schema(description = "Banque (`CfgTier.PKey`), requise si le type a `banqueObligatoire = true`.")
        UUID banqueId,
        @Schema(description = "Référence (`StlSettlement.Code`), requise si le type a `referenceObligatoire = true`.")
        String reference,
        @Schema(description = "Commentaire libre. Facultatif.")
        String notes
) {

    public CreerReglementRequest entete() {
        return new CreerReglementRequest(montant, dateEcheance, typeId, banqueId, reference, null, notes, null, null);
    }
}
