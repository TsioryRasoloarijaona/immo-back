package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Retenue d'un avenant (garantie, prorata, assurance DEC ou TRC) : au taux ou au montant saisi.")
public record RetenueAvenantDto(
        @Schema(description = "Vrai = montant saisi (taux = montant / TTC) ; faux = taux appliqué au TTC.") boolean parMontant,
        @Schema(description = "Taux en pourcentage 0–100 ; si parMontant, ignoré et recalculé (peut dépasser 100).", example = "10") BigDecimal taux,
        @Schema(description = "Montant de la retenue ; lu en écriture seulement si parMontant, calculé sinon.") BigDecimal montant
) {
}
