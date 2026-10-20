package com.tsix.immocrm.dto.etatventes;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneEtatVentes;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonFilter(ColonneEtatVentes.FILTRE)
@Schema(description = "Un article racine en vente et son statut ; seules les colonnes demandées par ?colonnes= sont "
        + "présentes.")
public record LigneEtatVentesDto(
        @Schema(description = "StkItem.PKey de l'article racine.") UUID articleId,
        String articleCode,
        String articleLibelle,
        String trancheCode,
        @Schema(description = "Code de la catégorie (StkItemCategory : APP, PAR, COM…).") String categorieCode,
        String categorieLibelle,
        @Schema(description = "Code du type d'article (StkItemType : T2, T3…).") String typeCode,
        @Schema(description = "Code de l'étage (StkFloor).") String etage,
        @Schema(description = "Code de l'immeuble (StkBuilding).") String immeuble,
        @Schema(description = "StkItem.Area, en m².") BigDecimal surface,
        @Schema(description = "Prix catalogue de l'article (ComSalePrice) si libre, prix du dossier (ComFolder.Price) "
                + "sinon.") BigDecimal prixVenteTTC,
        @Schema(description = "LIBRE, SANS_STATUT ou StrStatus.Code du dossier.", example = "RESERVATION")
        String statutCode,
        @Schema(example = "Réservation") String statutLibelle,
        @Schema(description = "Null si l'article est libre (dossier annulé ou désisté compris).") UUID dossierId,
        String dossierCode,
        @Schema(description = "Nom affiché du client du dossier.") String clientNom,
        @Schema(description = "Nom affiché du commercial du dossier.") String commercial,
        @Schema(description = "ComFolder.DocDate.") LocalDateTime dateReservation,
        @Schema(description = "Prix de vente réglé du dossier (prixRegle de la liste des dossiers) ; null si libre.")
        BigDecimal montantRegle,
        @Schema(description = "Prix de vente − réglé (prixReste de la liste des dossiers) ; null si libre.")
        BigDecimal resteARegler,

        @Schema(description = "HT après remise : ComFolder.ItemPriceAfterHT ; libre : SalePrice / (1 + Vat).")
        BigDecimal prixArticleHt,
        @Schema(description = "ComFolder.ItemPriceAfter ; libre : SalePrice.") BigDecimal prixArticleTtc,
        @Schema(description = "HT après remise : ComFolder.UnderItemsPriceAfterHT ; libre : (ComSalePrice − SalePrice) / (1 + Vat).")
        BigDecimal prixSousArticlesHt,
        @Schema(description = "ComFolder.UnderItemsPriceAfter ; libre : ComSalePrice − SalePrice.")
        BigDecimal prixSousArticlesTtc,
        @Schema(description = "ComFolder.PriceHT ; libre : StkItem.ComSalePriceHT.") BigDecimal prixHt,
        @Schema(description = "ComFolder.DiscountAmount ; libre : 0.") BigDecimal remiseMontant,
        @Schema(description = "ComFolder.AgreementPrice ; libre : ComSalePrice.") BigDecimal prixContractuel,

        // Montants du dossier, calcul de la liste des dossiers : descriptions dans api-docs/schemas.
        BigDecimal prixVenteM2Ttc,
        BigDecimal prixVenteM2Ht,
        BigDecimal netAPayer,
        BigDecimal netAPayerRegle,
        BigDecimal netAPayerReste,
        BigDecimal avance,
        BigDecimal avanceReglee,
        BigDecimal avanceReste,
        BigDecimal autofinancement,
        BigDecimal autofinancementRegle,
        BigDecimal autofinancementReste,
        BigDecimal credit,
        BigDecimal creditRegle,
        BigDecimal creditReste,
        BigDecimal frais,
        BigDecimal fraisRegles,
        BigDecimal fraisReste,
        BigDecimal modifications,
        BigDecimal modificationsReglees,
        BigDecimal modificationsReste,
        BigDecimal tropPercu,
        BigDecimal tropPercuRegle,
        BigDecimal tropPercuReste,
        BigDecimal penalites,
        BigDecimal penalitesRegles,
        BigDecimal penalitesReste,
        BigDecimal prixContractuelRegle,
        BigDecimal prixContractuelReste,
        BigDecimal tauxAvance,
        BigDecimal tauxAvanceReel) {
}
