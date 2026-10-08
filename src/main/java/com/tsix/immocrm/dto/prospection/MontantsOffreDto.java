package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Onglet « Montants » du formulaire Offre légataire ; TTC = HT × (1 + tvaRatio). */
@Schema(description = "Détail HT/TTC des montants d'une offre, reproduisant l'onglet \"Montants\" "
        + "du formulaire Offre legacy.")
public record MontantsOffreDto(

        @Schema(description = "Taux de TVA appliqué (ex. 0.20 = 20%), repris de l'article au "
                + "moment de la création de l'offre.")
        BigDecimal tvaRatio,

        @Schema(description = "Prix du m² HT = prix article HT / superficie de l'article.")
        BigDecimal prixM2HT,
        @Schema(description = "Prix du m² TTC.")
        BigDecimal prixM2TTC,

        @Schema(description = "Prix de l'article principal, HT.")
        BigDecimal prixArticleHT,
        @Schema(description = "Prix de l'article principal, TTC.")
        BigDecimal prixArticleTTC,

        @Schema(description = "Somme des prix des sous-articles inclus dans l'offre, HT. 0 si "
                + "aucun sous-article.")
        BigDecimal prixSousArticlesHT,
        @Schema(description = "Somme des prix des sous-articles inclus dans l'offre, TTC.")
        BigDecimal prixSousArticlesTTC,

        @Schema(description = "Total HT = prix article HT + prix sous-articles HT, avant remise.")
        BigDecimal totalHT,
        @Schema(description = "Total TTC, avant remise.")
        BigDecimal totalTTC,

        @Schema(description = "Taux de remise appliqué au total, en %.")
        BigDecimal remisePercent,
        @Schema(description = "Montant de la remise, HT.")
        BigDecimal remiseHT,
        @Schema(description = "Montant de la remise, TTC.")
        BigDecimal remiseTTC,

        @Schema(description = "Prix du m² HT après remise (remise répartie au prorata entre "
                + "article et sous-articles).")
        BigDecimal prixM2ApresRemiseHT,
        @Schema(description = "Prix du m² TTC après remise.")
        BigDecimal prixM2ApresRemiseTTC,

        @Schema(description = "Prix de l'article HT après remise.")
        BigDecimal prixArticleApresRemiseHT,
        @Schema(description = "Prix de l'article TTC après remise.")
        BigDecimal prixArticleApresRemiseTTC,

        @Schema(description = "Prix des sous-articles HT après remise.")
        BigDecimal prixSousArticlesApresRemiseHT,
        @Schema(description = "Prix des sous-articles TTC après remise.")
        BigDecimal prixSousArticlesApresRemiseTTC,

        @Schema(description = "Prix de vente final HT = total HT − remise HT. Ligne finale du "
                + "formulaire Offre legacy.")
        BigDecimal prixVenteHT,
        @Schema(description = "Prix de vente final TTC = total TTC − remise TTC.")
        BigDecimal prixVenteTTC,

        @Schema(description = "Taux de commission (ex. 0.03 = 3%), repris de l'offre. Null si non renseigné.")
        BigDecimal commissionPercent
) {
}
