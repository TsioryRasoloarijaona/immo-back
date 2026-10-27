package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Champs communs aux mêmes noms qu'{@link ArticleEcritureDto} (FEATURE-patrimoine-lotissements.md §2). */
@Schema(description = "Création ou modification d'un lot de terrain (StkItem nature TER d'une tranche). En PATCH, absent "
        + "ou null = inchangé. Taux et remises en fraction (0,2 = 20 %).")
public record LotissementEcritureDto(
        @Schema(description = "Tranche (StkHierarchyId) visible, d'entité active. Obligatoire à la création ; changée en "
                + "PATCH, la présentation est vidée (409 si le lot est occupé ou a des prospections).") UUID trancheId,
        @Schema(description = "Code, unique dans la tranche toutes natures confondues. Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Vocation (StkVocation), PROJET refusée. Obligatoire à la création.") UUID vocationId,
        @Schema(description = "Catégorie de nature TER ou LOT DE TERRAIN. Obligatoire à la création.") UUID categorieId,
        @Schema(description = "Type de nature TER ou LOT DE TERRAIN. Obligatoire à la création.") UUID typeId,
        @Schema(description = "Type de présentation (StkPresentationType) de la tranche du lot ; d'une autre tranche → 400.")
        UUID presentationId,
        @Schema(description = "Orientation (StkOrientation).") UUID orientationId,
        @Schema(description = "Catégorie de charge (StkCostCategory).") UUID categorieChargeId,
        @Schema(description = "Entité de gestion du syndic (CfgCompany, CfgCompanySyndicId).") UUID entiteSyndicId,
        @Schema(description = "Address (500 caractères au plus).") String adresse,
        @Schema(description = "Notes (1000 caractères au plus).") String notes,
        @Schema(description = "Anomalies (1000 caractères au plus).") String anomalies,
        @Schema(description = "Contenance en texte libre (1000 caractères au plus).") String contenanceLibre,
        @Schema(description = "AreaFloor : superficie provisoire, > 0, défaut 1.") BigDecimal surfaceProvisoire,
        @Schema(description = "Area : superficie définitive, > 0, défaut = provisoire.") BigDecimal surfaceDefinitive,
        @Schema(description = "ComSaleUnitCostFloorHT ; exclusif avec prixM2ProvisoireTTC.") BigDecimal prixM2ProvisoireHT,
        @Schema(description = "ComSaleUnitCostFloor ; exclusif avec prixM2ProvisoireHT.") BigDecimal prixM2ProvisoireTTC,
        @Schema(description = "ComSaleUnitCostHT ; exclusif avec prixM2DefinitifTTC.") BigDecimal prixM2DefinitifHT,
        @Schema(description = "ComSaleUnitCost ; exclusif avec prixM2DefinitifHT.") BigDecimal prixM2DefinitifTTC,
        @Schema(description = "Vat, fraction entre 0 et 1.") BigDecimal tauxTva,
        @Schema(description = "TitleNumber : n° du titre foncier.") String titreFoncier,
        @Schema(description = "IsTitled, défaut vrai.") Boolean estTitre,
        @Schema(description = "DiscountMax : remise niveau 1, fraction.") BigDecimal remiseMax1,
        @Schema(description = "DiscountMax2 : remise niveau 2, fraction.") BigDecimal remiseMax2,
        @Schema(description = "ComSaleMinAdvance : avance minimum, ≥ 0.") BigDecimal avanceMinimum,
        @Schema(description = "AmountHT : loyer mensuel HT, ≥ 0.") BigDecimal loyerMensuelHT,
        @Schema(description = "CautionNumber : nombre de cautions, ≥ 0.") Integer nombreCautions,
        @Schema(description = "IsAvailableForSaleInWebSite : disponible à la vente sur le web.") Boolean disponibleVenteWeb,
        @Schema(description = "IsAvailableForRentInWebSite : disponible à la location sur le web.")
        Boolean disponibleLocationWeb,
        // Identification foncière et compteurs : descriptions dans api-docs/schemas/LotissementEcritureDto.md.
        LocalDateTime dateEnregistrement, String numeroEnregistrement, String lieuEnregistrement,
        String cpfImmatriculation, LocalDateTime dateImmatriculation, String titreFoncierMere,
        String intituleTitreFoncier, LocalDateTime dateTitreFoncier, String proprieteDite, String fractionDivise,
        String fractionDiviseTerrain, BigDecimal partiesDivises, BigDecimal partiesIndivises, String quotaTerrainGroupe,
        String notesTitreFoncier,
        String referenceElectricite, String policeElectricite, String compteurElectricite, String releveElectricite,
        String referenceEau, String policeEau, String compteurEau, String releveEau,
        String referenceGaz, String policeGaz, String compteurGaz, String releveGaz,
        @Schema(description = "Cos : emprise au sol des bâtiments, ≥ 0.") BigDecimal cos,
        @Schema(description = "Cuf : plancher couvert, ≥ 0.") BigDecimal cuf,
        @Schema(description = "AlinmentStop : arrêté d'alignement (m), ≥ 0.") Integer arreteAlignement,
        @Schema(description = "NeighborStop : retrait voisin (m), ≥ 0.") Integer retraitVoisin,
        @Schema(description = "MaxHeight : hauteur maximale autorisée (m), ≥ 0.") Integer hauteurMax,
        @Schema(description = "MapLatitude.") BigDecimal latitude,
        @Schema(description = "MapLongitude.") BigDecimal longitude,
        @Schema(description = "PATCH : champs à vider (date, référence, notes, adresse → NULL ; nombre → 0 ; case → faux ; "
                + "autre texte → \"\"). Socle (tranche, code, classification, surfaces, prix, TVA, titre) → 400.")
        List<String> champsAVider
) {
}
