package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Règlement dont le tiers est le tiers (TierId) ou le payeur (TierPayerId), une ligne par règlement.")
public record LigneReglementTiersDto(
        @Schema(description = "PKey du règlement.") UUID id,
        @Schema(description = "Date du règlement (DocDate), clé du tri.") LocalDateTime date,
        @Schema(description = "Référence (Code).") String reference,
        @Schema(description = "Libellé (Description).") String libelle,
        @Schema(description = "Montant (Amount), tel qu'en base.") BigDecimal montant,
        @Schema(description = "Libellé du mode de règlement.") String mode,
        @Schema(description = "Statut brut (Status), ex. IN_WORK, POINTED.") String statut,
        @Schema(description = "Encaissement (IsCredit) ; faux ou null = décaissement.") Boolean credit,
        @Schema(description = "Fiche encaissement si credit est vrai, null sinon.") LienOngletTiersDto lien)
        implements LigneOngletTiers {

    public LigneReglementTiersDto(UUID id, LocalDateTime date, String reference, String libelle, BigDecimal montant,
                                  String mode, String statut, Boolean credit) {
        this(id, date, reference, libelle, montant, mode, statut, credit,
                Boolean.TRUE.equals(credit) ? new LienOngletTiersDto("encaissement", id) : null);
    }
}
