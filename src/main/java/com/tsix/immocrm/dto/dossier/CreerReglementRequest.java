package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code typeId} et {@code payeurId} requis : sans eux l'« Autorisation d'encaissement » sort trouée.
 * {@code dateReglement} ({@code SettlementDate}) est refusée ici : elle n'existe que sur les règlements
 * pointés, posée par {@code PATCH .../statut}. Voir FEATURE-reglement-statut-preuve.md §2 et §4.
 */
public record CreerReglementRequest(

        @Schema(example = "50000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal montant,

        @Schema(description = "Date d'échéance du règlement (`StlSettlement.EcheanceDate`). "
                + "**Requise** — il n'y a plus de valeur par défaut.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dateEcheance,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID typeId,

        UUID banqueId,

        @Schema(example = "CHQ-000123")
        String reference,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID payeurId,

        @Schema(description = "Commentaire libre. Facultatif.")
        String notes,

        @Schema(example = "IN_WORK", allowableValues = {"IN_WORK"})
        String statut,

        @Schema(description = "**Refusée à la création** (400 si fournie) — voir la description de "
                + "cette requête. Se renseigne au passage à `POINTED`.",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime dateReglement
) {
}
