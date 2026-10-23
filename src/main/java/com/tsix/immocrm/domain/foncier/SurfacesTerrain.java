package com.tsix.immocrm.domain.foncier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Surfaces et prix d'une prospection terrain, recalculés par le serveur (CalculSurfacesTerrain).")
public record SurfacesTerrain(
        @Schema(description = "Superficie (Area, m²), > 0.") BigDecimal superficie,
        @Schema(description = "Voies d'aménagement (EquipementsArea, m²).") BigDecimal superficieEquipements,
        @Schema(description = "Superficie équipement à céder (AreaEquipmentTransferred).") BigDecimal superficieEquipementACeder,
        @Schema(description = "Superficie à régulariser (AreaRegularized).") BigDecimal superficieARegler,
        @Schema(description = "Superficie cédée (AreaCeded).") BigDecimal superficieACeder,
        @Schema(description = "Lecture seule : voies / superficie nette (EquipementsCoef).") BigDecimal coefEquipements,
        @Schema(description = "Lecture seule : superficie − les quatre surfaces ci-dessus (AreaNet).")
        BigDecimal superficieNette,
        @Schema(description = "COS (Cos).") BigDecimal cos,
        @Schema(description = "Emprise (AreaCos) : COS × nette (MA) ou × superficie.") BigDecimal superficieCos,
        @Schema(description = "CUF (Cuf).") BigDecimal cuf,
        @Schema(description = "Plancher couvert (AreaCuf) : nette × CUF (MA) ou superficie × COS × CUF.")
        BigDecimal superficieCuf,
        @Schema(description = "Coefficient vendable (CoefSaleableArea).") BigDecimal coefVendable,
        @Schema(description = "Plancher vendable (SalableArea) = coefVendable × plancher couvert.") BigDecimal superficieVendable,
        @Schema(description = "Coefficient commerce (CommerceCoef).") BigDecimal coefCommerce,
        @Schema(description = "Surface commerce (CommerceArea) = coef × superficie.") BigDecimal superficieCommerce,
        @Schema(description = "Coefficient bureau (OfficeCoef).") BigDecimal coefBureau,
        @Schema(description = "Surface bureau (OfficeArea) = coef × superficie.") BigDecimal superficieBureau,
        @Schema(description = "Lecture seule : vendable − bureau − commerce (HabitatArea).") BigDecimal superficieHabitat,
        @Schema(description = "Coefficient parking sous-sol (UndergroundParkingCoef).") BigDecimal coefParking,
        @Schema(description = "Parking sous-sol (UndergroundParkingArea) = coef × superficie.") BigDecimal superficieParking,
        @Schema(description = "Prix du m² du propriétaire (ProspectionUnitCost).") BigDecimal prixM2,
        @Schema(description = "Prix total du propriétaire (ProspectionPrice) = prix du m² × superficie.") BigDecimal prix
) {
}
