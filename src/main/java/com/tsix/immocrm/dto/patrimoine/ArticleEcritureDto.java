package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Article principal ({@code POST /api/articles}), sous-article ({@code POST /api/articles/{id}/sous-articles}) et
 * modification des deux ({@code PATCH}). Prix au m² : HT ou TTC, jamais les deux pour une même surface.
 */
@Schema(description = "Création ou modification d'un article ou d'un sous-article (StkItem). En PATCH, absent ou null "
        + "= inchangé. Taux et remises en fraction (0,2 = 20 %).")
public record ArticleEcritureDto(
        @Schema(description = "Tranche (StkHierarchyId). Obligatoire pour un article, non modifiable, interdit pour un "
                + "sous-article (400, celle du parent).") UUID trancheId,
        @Schema(description = "Code, unique dans la tranche (sous-articles compris). Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Vocation (StkVocation), défaut VENTE, PROJET refusée. Interdite pour un sous-article (400).")
        UUID vocationId,
        @Schema(description = "Catégorie nature Bâtiment ; « pour les sous-articles » selon le lot. Obligatoire.")
        UUID categorieId,
        @Schema(description = "Type, même règle que la catégorie. Obligatoire.") UUID typeId,
        @Schema(description = "Immeuble (StkBuilding). Obligatoire ; défaut = celui du parent pour un sous-article.")
        UUID immeubleId,
        @Schema(description = "Étage (StkFloor). Obligatoire.") UUID etageId,
        @Schema(description = "AreaFloor, > 0, défaut 1.") BigDecimal surfaceProvisoire,
        @Schema(description = "Area, > 0, défaut = surface provisoire.") BigDecimal surfaceDefinitive,
        @Schema(description = "ComSaleUnitCostFloorHT ; exclusif avec prixM2ProvisoireTTC.") BigDecimal prixM2ProvisoireHT,
        @Schema(description = "ComSaleUnitCostFloor ; exclusif avec prixM2ProvisoireHT.") BigDecimal prixM2ProvisoireTTC,
        @Schema(description = "ComSaleUnitCostHT ; exclusif avec prixM2DefinitifTTC.") BigDecimal prixM2DefinitifHT,
        @Schema(description = "ComSaleUnitCost ; exclusif avec prixM2DefinitifHT.") BigDecimal prixM2DefinitifTTC,
        @Schema(description = "Vat, fraction entre 0 et 1, défaut 0.") BigDecimal tauxTva,
        @Schema(description = "TitleNumber.") String titreFoncier,
        @Schema(description = "IsTitled ; défaut vrai pour un article, faux pour un sous-article.") Boolean estTitre,
        @Schema(description = "DiscountMax, fraction.") BigDecimal remiseMax1,
        @Schema(description = "DiscountMax2, fraction.") BigDecimal remiseMax2,
        @Schema(description = "Address (500 caractères au plus).") String adresse,
        @Schema(description = "Notes (1000 caractères au plus).") String notes,
        @Schema(description = "InteriorArea, ≥ 0.") BigDecimal surfaceInterieure,
        @Schema(description = "WallsArea, ≥ 0.") BigDecimal surfaceMurs,
        @Schema(description = "CommonPartsArea, ≥ 0.") BigDecimal surfacePartiesCommunes,
        @Schema(description = "AlwaysAffected, à la création d'un sous-article seulement (défaut faux).")
        Boolean toujoursAffecte,

        // -- Fiche article (FEATURE-patrimoine-article-fiche.md §1) --
        @Schema(description = "Type de présentation (StkPresentationType) de la tranche de l'article ; un changement "
                + "remplace les contenances par celles du type.") UUID presentationId,
        @Schema(description = "Orientation (StkOrientation).") UUID orientationId,
        @Schema(description = "Catégorie de charge (StkCostCategory).") UUID categorieChargeId,
        @Schema(description = "Façade (StkFacade).") UUID facadeId,
        @Schema(description = "Entité de gestion du syndic (CfgCompany, CfgCompanySyndicId).") UUID entiteSyndicId,
        @Schema(description = "Vues (StkVue) : l'ensemble remplace celui de l'article ; [] retire tout, absent = inchangé.")
        List<UUID> vueIds,
        @Schema(description = "ComSaleMinAdvance : avance minimum, ≥ 0.") BigDecimal avanceMinimum,
        @Schema(description = "AmountHT : loyer mensuel HT, ≥ 0.") BigDecimal loyerMensuelHT,
        @Schema(description = "CautionNumber : nombre de cautions, ≥ 0.") Integer nombreCautions,
        @Schema(description = "Anomalies (1000 caractères au plus).") String anomalies,
        @Schema(description = "Contenance en texte libre (1000 caractères au plus).") String contenanceLibre,
        @Schema(description = "IsAvailableForSaleInWebSite : disponible pour la vente sur le web.") Boolean disponibleVenteWeb,
        @Schema(description = "IsAvailableForRentInWebSite : disponible pour la location sur le web.")
        Boolean disponibleLocationWeb,
        @Schema(description = "RegistryDate : date d'enregistrement.") LocalDateTime dateEnregistrement,
        @Schema(description = "RegistryNumber : numéro d'enregistrement.") String numeroEnregistrement,
        @Schema(description = "RegistryPlace : lieu d'enregistrement.") String lieuEnregistrement,
        @Schema(description = "ImmatriculationCPF.") String cpfImmatriculation,
        @Schema(description = "ImmatriculationDate.") LocalDateTime dateImmatriculation,
        @Schema(description = "TitleNumberMother : titre foncier mère.") String titreFoncierMere,
        @Schema(description = "TitleName : intitulé du titre foncier.") String intituleTitreFoncier,
        @Schema(description = "TitleDate : date du titre foncier.") LocalDateTime dateTitreFoncier,
        @Schema(description = "PropertyCalled : propriété dite.") String proprieteDite,
        @Schema(description = "FractionDivided : fraction divise.") String fractionDivise,
        @Schema(description = "FractionDividedLand : fraction divise du terrain.") String fractionDiviseTerrain,
        @Schema(description = "DividedPart : parties divises, ≥ 0.") BigDecimal partiesDivises,
        @Schema(description = "UndividedPart : parties indivises, ≥ 0.") BigDecimal partiesIndivises,
        @Schema(description = "GroupLandQuota : quote-part du terrain du groupe.") String quotaTerrainGroupe,
        @Schema(description = "TitleNotes : notes du titre foncier.") String notesTitreFoncier,
        @Schema(description = "ElectricityRef (50 caractères au plus, comme chaque compteur).") String referenceElectricite,
        @Schema(description = "ElectricityMeterPolice.") String policeElectricite,
        @Schema(description = "ElectricityMeter.") String compteurElectricite,
        @Schema(description = "ElectricityMeterIndex.") String releveElectricite,
        @Schema(description = "WaterRef.") String referenceEau,
        @Schema(description = "WaterMeterPolice.") String policeEau,
        @Schema(description = "WaterMeter.") String compteurEau,
        @Schema(description = "WaterMeterIndex.") String releveEau,
        @Schema(description = "GasRef.") String referenceGaz,
        @Schema(description = "GasMeterPolice.") String policeGaz,
        @Schema(description = "GasMeter.") String compteurGaz,
        @Schema(description = "GasMeterIndex.") String releveGaz,
        @Schema(description = "PATCH : champs à vider (date, référence → NULL ; nombre → 0 ; case → faux ; texte → \"\"). "
                + "Classification, surfaces, prix, TVA, textes du socle et vues → 400.") List<String> champsAVider
) {
}
