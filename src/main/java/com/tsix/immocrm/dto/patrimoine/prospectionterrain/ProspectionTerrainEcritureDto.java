package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Création ou modification d'une prospection terrain (PATCH : absent ou null = inchangé). Surfaces et "
        + "prix : mêmes noms et sens que SurfacesTerrain. Le statut ne se saisit jamais (transitions).")
public record ProspectionTerrainEcritureDto(
        @Schema(description = "Unique parmi les prospections terrain (409). Obligatoire.") String code,
        @Schema(description = "Obligatoire.") String description,
        @Schema(description = "DocDate, défaut aujourd'hui.") LocalDate date,
        @Schema(description = "Catégorie de nature PROS TER. Obligatoire.") UUID categorieId,
        @Schema(description = "Type de nature TER. Obligatoire.") UUID typeId,
        @Schema(description = "LndLevel.") UUID niveauId,
        @Schema(description = "LndVocation.") UUID vocationId,
        @Schema(description = "Défaut AdmParamsGuid.DefaultCountry.") UUID paysId,
        @Schema(description = "Ville du pays choisi. Obligatoire.") UUID villeId,
        @Schema(description = "Tiers courtier (CfgMiddlemanId).") UUID courtierId,
        @Schema(description = "Tiers notaire (CfgLawyerId).") UUID notaireId,
        @Schema(description = "StkOrientation.") UUID orientationId,
        @Schema(description = "Vide, ou commence par https://www.google.") String lienGoogle,
        @Schema(description = "255 caractères au plus.") String adresse,
        @Schema(description = "Propriétaires, 255 caractères au plus.") String proprietaires,
        @Schema(description = "1000 caractères au plus.") String notes,
        @Schema(description = "1000 caractères au plus.") String anomalies,
        @Schema(description = "Aménagé (vrai, défaut) ou Brut (faux).") Boolean estAmenage,
        @Schema(description = "Archivée ; explicite, l'emporte sur l'archivage automatique.") Boolean estArchivee,
        @Schema(description = "Date validité de statut, libre.") LocalDate dateValiditeStatut,
        @Schema(description = "> 0, défaut 1.") BigDecimal superficie,
        BigDecimal superficieEquipements,
        BigDecimal superficieEquipementACeder,
        BigDecimal superficieARegler,
        BigDecimal superficieACeder,
        BigDecimal cos,
        BigDecimal superficieCos,
        BigDecimal cuf,
        BigDecimal superficieCuf,
        BigDecimal coefVendable,
        BigDecimal superficieVendable,
        BigDecimal coefCommerce,
        BigDecimal superficieCommerce,
        BigDecimal coefBureau,
        BigDecimal superficieBureau,
        BigDecimal coefParking,
        BigDecimal superficieParking,
        @Schema(description = "Arrêté d'alignement (AlinmentStop).") Integer arreteAlignement,
        @Schema(description = "Hauteur maximale (MaxHeight).") Integer hauteurMax,
        BigDecimal prixM2,
        BigDecimal prix,
        @Schema(description = "Prix proposé (ProspectionPriceProposed), saisie libre.") BigDecimal prixPropose,
        LocalDate dateEnregistrement,
        @Schema(description = "255 caractères au plus.") String numeroEnregistrement,
        @Schema(description = "255 caractères au plus.") String lieuEnregistrement,
        @Schema(description = "255 caractères au plus.") String cpfImmatriculation,
        LocalDate dateImmatriculation,
        @Schema(description = "Identification de la zone (brut), 1000 caractères au plus.") String identificationZone,
        @Schema(description = "Conditions de raccordement (brut), 1000 caractères au plus.") String conditionsRaccordement,
        @Schema(description = "Assiette foncière (brut), 1000 caractères au plus.") String assietteFonciere,
        @Schema(description = "Levée topographique (brut).") Boolean leveeTopographique,
        @Schema(description = "PATCH : champs facultatifs à vider (référence ou date → NULL, texte → \"\").")
        List<String> champsAVider
) {
}
