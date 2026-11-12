package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Bloc « Retenue de garantie » de l'onglet Suivi.")
public record RetenueGarantieDto(
        @Schema(description = "Taux de retenue de garantie en pourcentage 0–100.") BigDecimal taux,
        @Schema(description = "Retenue sur l'Est. de base.") BigDecimal estBase,
        @Schema(description = "Retenue sur l'Est. confidentielle.") BigDecimal confidentielle,
        @Schema(description = "Retenue sur le marché ajusté.") BigDecimal marcheAjuste,
        @Schema(description = "Retenue des avenants.") BigDecimal avenants,
        @Schema(description = "Retenue prévue : avenants + marché + dépassements et travaux supp. facturés.") BigDecimal factures,
        @Schema(description = "Retenue effective sur les factures et avenants facturés.") BigDecimal retenue,
        @Schema(description = "Validée : Σ TTC des pièces de restitution de garantie.") BigDecimal validee,
        @Schema(description = "Restituée : Σ des règlements de ces pièces.") BigDecimal restituee,
        @Schema(description = "Reste à restituer ; 0 si la restitution est bloquée.") BigDecimal resteARestituer,
        @Schema(description = "Vrai si ce lot gère le compte prorata du projet (PrjProject.ManageProrataPrjMarketId).")
        boolean gestionnaireCompteProrata,
        @Schema(description = "Compte prorata à rembourser.") BigDecimal aRembourser,
        @Schema(description = "Compte prorata remboursé (factures du projet).") BigDecimal remboursement
) {
}
