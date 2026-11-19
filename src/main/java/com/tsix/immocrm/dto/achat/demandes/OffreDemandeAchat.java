package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Offre d'un fournisseur (devis BuyDocument QUOTE lié à la demande), avec ses lignes.")
public record OffreDemandeAchat(
        @Schema(description = "PKey du devis (BuyDocument).") UUID id,
        @Schema(description = "Code du devis.") String code,
        @Schema(description = "Fournisseur (CfgTier).") ElementDemandeAchat fournisseur,
        @Schema(description = "Date de dépôt (DocDate).") LocalDate date,
        @Schema(description = "Total HT après remise, calculé sur les lignes.") BigDecimal totalHT,
        @Schema(description = "Total TTC après remise, calculé sur les lignes.") BigDecimal totalTTC,
        @Schema(description = "Offre retenue (IsSelected).") boolean selectionnee,
        @Schema(description = "Offre écartée avec son fournisseur (IsRejected).") boolean ecartee,
        @Schema(description = "Lignes du devis par ordre ; rattachées au bordereau de la demande par le code.")
        List<LigneOffreDemandeAchat> lignes) {
}
