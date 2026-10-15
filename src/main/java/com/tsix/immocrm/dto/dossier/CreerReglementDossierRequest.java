package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** En-tête identique à {@link CreerReglementRequest}, plus la répartition sur les échéances. */
@Schema(description = "Règlement d'un dossier réparti sur une ou plusieurs de ses échéances.")
public record CreerReglementDossierRequest(

        @Schema(example = "52200.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal montant,

        @Schema(description = "Date d'échéance du règlement (`StlSettlement.EcheanceDate`). **Requise**.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dateEcheance,

        @Schema(description = "Type de règlement (`StlSettlementType.PKey`), voir `GET /api/referentiels/types-reglement`.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID typeId,

        @Schema(description = "Banque (`CfgTier.PKey`), requise si le type a `banqueObligatoire = true`.")
        UUID banqueId,

        @Schema(description = "Référence (`StlSettlement.Code`), requise si le type a `referenceObligatoire = true`.",
                example = "CHQ-000123")
        String reference,

        @Schema(description = "Tiers ayant réglé (`TierPayerId`). Absent = le client du dossier (400 s'il n'y en a pas).")
        UUID payeurId,

        @Schema(description = "Commentaire libre. Facultatif.")
        String notes,

        @Schema(description = "Statut à la création : seul `IN_WORK` (ou l'absence) est accepté.",
                example = "IN_WORK", allowableValues = {"IN_WORK"})
        String statut,

        @Schema(description = "**Refusée à la création** (400 si fournie) : se renseigne au passage à `POINTED`.",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime dateReglement,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<AffectationReglementRequest> affectations
) {

    /** L'en-tête seul, contrôlé par les mêmes règles que l'ancien endpoint. */
    public CreerReglementRequest entete() {
        return new CreerReglementRequest(montant, dateEcheance, typeId, banqueId, reference, payeurId, notes,
                statut, dateReglement);
    }
}
