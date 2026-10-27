package com.tsix.immocrm.dto.commission;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Document à payer d'un commercial (BuyDocument SALE_COMMISSION) et ses lignes de commission.")
public record DocumentCommissionDto(
        UUID id,
        @Schema(description = "Code du document (séquence « Facture fournisseur »).", example = "27/03014") String code,
        LocalDate date,
        @Schema(description = "Tiers du document : la personne commissionnée.") UUID commercialId,
        String commercial,
        @Schema(description = "Montant TTC du document.") BigDecimal montant,
        @Schema(description = "Σ des règlements du document, annulés exclus.") BigDecimal regle,
        @Schema(description = "montant − regle.") BigDecimal reste,
        @Schema(description = "Statut du document.") String statut,
        @Schema(description = "Lignes de commission du document.") List<LigneDocumentCommissionDto> lignes) {

    @Schema(description = "Ligne de commission portée par le document.")
    public record LigneDocumentCommissionDto(UUID ligneId,
                                             @Schema(description = "« {type} du dossier N° {code} ».") String libelle,
                                             BigDecimal montant) {
    }
}
