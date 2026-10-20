package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Mêmes noms que {@link EtudeFaisabiliteDto} et {@code ValeursFaisabilite} ; taux en fraction. */
@Schema(description = "Création, modification ou aperçu de calcul d'une étude. En PATCH, absent ou null = inchangé. "
        + "Paires coefficient ↔ montant : l'un ou l'autre ; si les deux, le coefficient gagne.")
public record SaisieEtudeDto(
        @Schema(description = "Code unique (obligatoire à la création).", example = "ETUDE-01") String code,
        @Schema(description = "Libellé (obligatoire à la création).") String description,
        @Schema(description = "Date du document (obligatoire à la création).") LocalDateTime dateDocument,
        @Schema(description = "Société (CfgCompany) active, obligatoire à la création.") UUID societeId,
        @Schema(description = "Terrain : article de nature TER (liste /choix/terrains).") UUID terrainId,
        @Schema(description = "Description du terrain (texte libre, 255 caractères).") String descriptionTerrain,
        @Schema(description = "Nombre d'articles à vendre.") Integer nombreArticlesAVendre,
        @Schema(description = "Utiliser la quote-part du terrain pour le calcul de la TVA.") Boolean proportionTerrainPourTva,
        @Schema(description = "TVA par défaut (fraction ; 0,20 à la création si absente).") BigDecimal tauxTvaDefaut,
        @Schema(description = "TVA sur intérêts (fraction ; 0,10 à la création si absente).") BigDecimal tauxTvaInterets,
        @Schema(description = "Superficie du terrain (m²).") BigDecimal superficie,
        @Schema(description = "Coef équipements (paire avec superficieEquipements).") BigDecimal coefEquipements,
        @Schema(description = "Superficie équipements (m², paire avec coefEquipements).") BigDecimal superficieEquipements,
        @Schema(description = "COS (paire avec empriseAuSol).") BigDecimal coefEmprise,
        @Schema(description = "Emprise au sol (m², paire avec coefEmprise).") BigDecimal empriseAuSol,
        @Schema(description = "CUF (paire avec plancherCouvert).") BigDecimal cuf,
        @Schema(description = "Plancher couvert (m², paire avec cuf).") BigDecimal plancherCouvert,
        @Schema(description = "Coef vendable (paire avec plancherVendable).") BigDecimal coefVendable,
        @Schema(description = "Plancher vendable (m², paire avec coefVendable).") BigDecimal plancherVendable,
        @Schema(description = "Prix d'achat du m² TTC (paire avec prixTerrain).") BigDecimal prixAchatM2,
        @Schema(description = "Prix du terrain TTC (paire avec prixAchatM2).") BigDecimal prixTerrain,
        @Schema(description = "TVA du terrain (fraction).") BigDecimal tauxTvaTerrain,
        @Schema(description = "Coef des frais d'enregistrement.") BigDecimal tauxFraisEnregistrement,
        @Schema(description = "Coef des autres frais de terrain.") BigDecimal tauxAutresFraisTerrain,
        @Schema(description = "Coût de construction du m² bâti.") BigDecimal coutConstructionM2Bati,
        @Schema(description = "Coût de construction du m² non bâti.") BigDecimal coutConstructionM2NonBati,
        @Schema(description = "Coef des autres frais d'exécution.") BigDecimal tauxAutresFraisExecution,
        @Schema(description = "Début de l'achat du terrain ; les autres dates de phase sont calculées.")
        LocalDateTime debutAchatTerrain,
        @Schema(description = "Durée de l'achat du terrain (mois).") Integer dureeAchatTerrain,
        @Schema(description = "Durée de la période de validation (mois).") Integer dureePeriodeValidation,
        @Schema(description = "Durée du permis de bâtir (mois).") Integer dureePermisBatir,
        @Schema(description = "Durée des études (mois).") Integer dureeEtudes,
        @Schema(description = "Durée des travaux (mois).") Integer dureeTravaux,
        @Schema(description = "Durée de la concrétisation (mois).") Integer dureeConcretisation,
        @Schema(description = "Durée de la finalisation (mois).") Integer dureeFinalisation,
        @Schema(description = "Délai fin d'achat → début de validation (mois).") Integer delaiAchatTerrainPeriodeValidation,
        @Schema(description = "Délai fin de validation → début du permis (mois).") Integer delaiPeriodeValidationPermisBatir,
        @Schema(description = "Délai début du permis → début des études (mois).") Integer delaiPermisBatirEtudes,
        @Schema(description = "Délai fin du permis → début des travaux (mois).") Integer delaiPermisBatirTravaux,
        @Schema(description = "Délai début des travaux → début de la concrétisation (mois).") Integer delaiTravauxConcretisation,
        @Schema(description = "Délai fin des travaux → début de la finalisation (mois).") Integer delaiTravauxFinalisation,
        @Schema(description = "Écart-type des travaux.") BigDecimal ecartTypeTravaux,
        @Schema(description = "Écart-type de la concrétisation.") BigDecimal ecartTypeConcretisation,
        @Schema(description = "Écart-type de la finalisation.") BigDecimal ecartTypeFinalisation,
        @Schema(description = "Délai de retour de la retenue de garantie (jours).") Integer delaiRetourJours,
        @Schema(description = "Taux de retenue de garantie (fraction).") BigDecimal tauxRetenueGarantie,
        @Schema(description = "Taux d'application de la retenue de garantie (fraction).")
        BigDecimal tauxApplicationRetenueGarantie,
        @Schema(description = "PATCH : champs à vider (terrainId, debutAchatTerrain → NULL ; nombre → 0 ; texte → \"\").")
        List<String> champsAVider,
        @Schema(description = "POST /calcul seulement (ignoré ailleurs) : unités de base à contrôler par Verif.")
        List<LigneUniteEtudeDto> unites,
        @Schema(description = "POST /calcul seulement (ignoré ailleurs) : tranches et ventilations à contrôler par Verif.")
        List<LigneTrancheEtudeDto> tranches) {
}
