package com.tsix.immocrm.dto.patrimoine;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.domain.StatutArticle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Une composante par colonne de {@link ColonneArticle}, même nom et même ordre. */
@JsonFilter(ColonneArticle.FILTRE)
@Schema(description = "Ligne d'article ; seules les colonnes de ?colonnes= sont présentes (défaut : celles "
        + "d'ArticleListItemDto).")
public record LigneArticleDto(
        // -- Identification.
        @Schema(description = "StkItem.PKey, toujours présent.") UUID id,
        @Schema(description = "StkItem.Code, unique dans la tranche.") String code,
        @Schema(description = "StkItem.Description.") String description,
        @Schema(description = "Code de la tranche (CfgTranche).") String trancheCode,
        @Schema(description = "Libellé de la tranche.") String trancheDescription,
        @Schema(description = "Code du programme (CfgProject) de la tranche.") String programmeCode,
        @Schema(description = "Libellé du programme.") String programmeLibelle,
        @Schema(description = "Code de l'entité juridique (CfgCompany) du programme.") String entiteCode,
        @Schema(description = "Libellé de l'entité juridique.") String entiteLibelle,
        @Schema(description = "StkItem.ParentId : article auquel le sous-article est rattaché.") UUID lotParentId,
        @Schema(description = "Code de l'article parent ; null pour un sous-article libre.") String parentCode,
        @Schema(description = "Libellé de l'article parent.") String parentDescription,
        @Schema(description = "Vrai si la ligne a un article parent (pas IsUnderItem, voir estSousArticle).")
        boolean sousArticle,
        @Schema(description = "StkItem.IsUnderItem : lot annexe, rattaché ou libre.") Boolean estSousArticle,
        @Schema(description = "StkItem.AlwaysAffected : suit son article parent dans la vente.") Boolean toujoursAffecte,
        @Schema(description = "Codes des sous-articles rattachés, séparés par « , » ; vide si aucun.")
        String sousArticlesCodes,
        @Schema(description = "StkBuilding.PKey.") UUID immeubleId,
        @Schema(description = "Code de l'immeuble.") String immeubleCode,
        @Schema(description = "Libellé de l'immeuble.") String immeubleLibelle,
        @Schema(description = "StkFloor.PKey.") UUID etageId,
        @Schema(description = "Code de l'étage.") String etageCode,
        @Schema(description = "Libellé de l'étage.") String etageLibelle,
        @Schema(description = "Code de la vocation (StkVocation).") String vocationCode,
        @Schema(description = "Libellé de la vocation.") String vocationLibelle,
        @Schema(description = "StkKind.PKey : nature physique du bien.") UUID natureId,
        @Schema(description = "Code de la nature (BAT, TER…).") String natureCode,
        @Schema(description = "Libellé de la nature.") String natureLibelle,
        @Schema(description = "StkItemCategory.PKey.") UUID categorieId,
        @Schema(description = "Code de la catégorie.") String categorieCode,
        @Schema(description = "Libellé de la catégorie.") String categorieLibelle,
        @Schema(description = "StkItemType.PKey.") UUID typeId,
        @Schema(description = "Code du type.") String typeCode,
        @Schema(description = "Libellé du type.") String typeLibelle,
        @Schema(description = "StkPresentationType.PKey, propre à la tranche.") UUID presentationTypeId,
        @Schema(description = "Code du type de présentation.") String presentationTypeCode,
        @Schema(description = "Libellé du type de présentation.") String presentationTypeLibelle,
        @Schema(description = "Code de l'orientation (StkOrientation).") String orientationCode,
        @Schema(description = "Libellé de l'orientation.") String orientationLibelle,
        @Schema(description = "Libellé de la façade (StkItem.StkFacadeId → StkFacade).", nullable = true) String facade,
        @Schema(description = "Libellés des vues (StkItemVue → StkVue), séparés par « , » ; null si aucune.",
                nullable = true) String vues,
        @Schema(description = "Code de la catégorie de charge (StkCostCategory).") String categorieChargeCode,
        @Schema(description = "Libellé de la catégorie de charge.") String categorieChargeLibelle,
        @Schema(description = "StkItem.Address.") String adresse,
        @Schema(description = "StkItem.Notes.") String notes,
        @Schema(description = "StkItem.Anomalies.") String anomalies,
        @Schema(description = "StkItem.Contenance, texte libre.") String contenanceLibre,

        // -- État et dossier.
        @Schema(description = "OCCUPE si l'article porte un dossier de vente ou de location, DISPONIBLE sinon.")
        StatutArticle statut,
        @Schema(description = "Libellé du statut du dossier de vente, « Loué » sous location, « Libre » sans dossier.",
                example = "Libre") String etat,
        @Schema(description = "Vrai si l'article parent est occupé : la ligne ne se modifie plus.") Boolean parentOccupe,
        @Schema(description = "StkItem.ComFolderId.") UUID dossierId,
        @Schema(description = "Code du dossier de vente.") String dossierCode,
        @Schema(description = "StrStatus.Code brut du dossier de vente.") String dossierStatut,
        @Schema(description = "Client du dossier de vente (CfgTier.PKey).") UUID clientId,
        @Schema(description = "Nom affiché du client du dossier de vente.") String clientNom,
        @Schema(description = "prixReste du dossier dossierCode ; null sans dossier, ou dossier non visible, annulé ou désisté.",
                nullable = true) BigDecimal resteARegler,
        @Schema(description = "Codes PAR du dossier et des sous-articles rattachés, séparés par « , » ; null si aucun.",
                nullable = true) String parkingChoisi,

        // -- Superficies, taux, avance, loyer.
        @Schema(description = "StkItem.Area : superficie définitive, m².") BigDecimal area,
        @Schema(description = "StkItem.AreaFloor : superficie provisoire, m².") BigDecimal surfaceProvisoire,
        @Schema(description = "Superficie + somme des superficies des sous-articles rattachés (calculée).")
        BigDecimal superficieNette,
        @Schema(description = "Somme des superficies des sous-articles rattachés, 0 si aucun.")
        BigDecimal superficieSousArticles,
        @Schema(description = "Surface si titré + surfaces des sous-articles rattachés titrés (calcul de la fiche).")
        BigDecimal superficieTitree,
        @Schema(description = "Somme des superficies des sous-articles rattachés de catégorie TER, 0 si aucun.")
        BigDecimal superficieTerrasse,
        @Schema(description = "StkItem.Vat, fraction (0,2 = 20 %).") BigDecimal tauxTVA,
        @Schema(description = "StkItem.DiscountMax, fraction.") BigDecimal remiseMax1,
        @Schema(description = "StkItem.DiscountMax2, fraction.") BigDecimal remiseMax2,
        @Schema(description = "StkItem.ComSaleMinAdvance (colonne « Avance » d'OMRAN).") BigDecimal avanceMinimum,
        @Schema(description = "StkItem.AmountHT.") BigDecimal loyerMensuelHT,
        @Schema(description = "Loyer mensuel HT × (1 + TVA), calculé.") BigDecimal loyerMensuelTTC,
        @Schema(description = "StkItem.CautionNumber.") Integer nombreCautions,

        // -- Prix.
        @Schema(description = "StkItem.ComSaleUnitCostHT.") BigDecimal prixM2DefinitifHT,
        @Schema(description = "StkItem.ComSaleUnitCost.") BigDecimal prixM2DefinitifTTC,
        @Schema(description = "StkItem.ComSaleUnitCostFloorHT.") BigDecimal prixM2ProvisoireHT,
        @Schema(description = "StkItem.ComSaleUnitCostFloor.") BigDecimal prixM2ProvisoireTTC,
        @Schema(description = "StkItem.SalePriceHT : article seul.") BigDecimal prixArticleDefinitifHT,
        @Schema(description = "StkItem.SalePrice : article seul.") BigDecimal salePrice,
        @Schema(description = "StkItem.SalePriceFloorHT : article seul.") BigDecimal prixArticleProvisoireHT,
        @Schema(description = "StkItem.SalePriceFloor : article seul.") BigDecimal salePriceFloor,
        @Schema(description = "Somme des SalePriceHT des sous-articles rattachés (calculée).") BigDecimal prixSousArticlesHT,
        @Schema(description = "Somme des SalePrice des sous-articles rattachés (calculée).") BigDecimal prixSousArticlesTTC,
        @Schema(description = "Somme des SalePriceFloorHT des sous-articles rattachés (calculée).")
        BigDecimal prixSousArticlesProvisoireHT,
        @Schema(description = "Somme des SalePriceFloor des sous-articles rattachés (calculée).")
        BigDecimal prixSousArticlesProvisoireTTC,
        @Schema(description = "StkItem.ComSalePriceHT : article + sous-articles.") BigDecimal prixVenteDefinitifHT,
        @Schema(description = "StkItem.ComSalePrice : article + sous-articles.") BigDecimal comSalePrice,
        @Schema(description = "StkItem.ComSalePriceFloorHT : article + sous-articles.") BigDecimal prixVenteProvisoireHT,
        @Schema(description = "StkItem.ComSalePriceFloor : article + sous-articles.") BigDecimal comSalePriceFloor,
        @Schema(description = "comSalePrice s'il est > 0, sinon comSalePriceFloor s'il est > 0, sinon null.",
                nullable = true) BigDecimal prixSuggere,
        @Schema(description = "Vrai quand prixSuggere est le prix plancher.") boolean prixSuggereEstPlancher,

        // -- Identification foncière.
        @Schema(description = "StkItem.TitleNumber.") String titreFoncier,
        @Schema(description = "StkItem.RegistryDate.") LocalDateTime dateEnregistrement,
        @Schema(description = "StkItem.RegistryNumber.") String numeroEnregistrement,
        @Schema(description = "StkItem.RegistryPlace.") String lieuEnregistrement,
        @Schema(description = "StkItem.ImmatriculationCPF.") String cpfImmatriculation,
        @Schema(description = "StkItem.ImmatriculationDate.") LocalDateTime dateImmatriculation,
        @Schema(description = "StkItem.TitleNumberMother.") String titreFoncierMere,
        @Schema(description = "StkItem.TitleName.") String intituleTitreFoncier,
        @Schema(description = "StkItem.TitleDate.") LocalDateTime dateTitreFoncier,
        @Schema(description = "StkItem.TitleNotes.") String notesTitreFoncier,
        @Schema(description = "StkItem.PropertyCalled.") String proprieteDite,
        @Schema(description = "StkItem.FractionDivided.") String fractionDivise,
        @Schema(description = "StkItem.FractionDividedLand.") String fractionDiviseTerrain,
        @Schema(description = "StkItem.DividedPart.") BigDecimal partiesDivises,
        @Schema(description = "StkItem.UndividedPart.") BigDecimal partiesIndivises,
        @Schema(description = "StkItem.GroupLandQuota.") String quotaTerrain,

        // -- Compteurs.
        @Schema(description = "StkItem.ElectricityRef.") String referenceElectricite,
        @Schema(description = "StkItem.ElectricityMeterPolice.") String policeElectricite,
        @Schema(description = "StkItem.ElectricityMeter.") String compteurElectricite,
        @Schema(description = "StkItem.ElectricityMeterIndex.") String releveElectricite,
        @Schema(description = "StkItem.WaterRef.") String referenceEau,
        @Schema(description = "StkItem.WaterMeterPolice.") String policeEau,
        @Schema(description = "StkItem.WaterMeter.") String compteurEau,
        @Schema(description = "StkItem.WaterMeterIndex.") String releveEau,
        @Schema(description = "StkItem.GasRef.") String referenceGaz,
        @Schema(description = "StkItem.GasMeterPolice.") String policeGaz,
        @Schema(description = "StkItem.GasMeter.") String compteurGaz,
        @Schema(description = "StkItem.GasMeterIndex.") String releveGaz,

        // -- Piste d'audit.
        @Schema(description = "StkItem.UserCreate.") String creePar,
        @Schema(description = "StkItem.UserCreateDateTime.") LocalDateTime creeLe,
        @Schema(description = "StkItem.UserUpdate.") String modifiePar,
        @Schema(description = "StkItem.UserUpdateDateTime.") LocalDateTime modifieLe) {
}
