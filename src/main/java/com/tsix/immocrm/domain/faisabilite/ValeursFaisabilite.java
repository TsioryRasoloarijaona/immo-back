package com.tsix.immocrm.domain.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bloc A d'une étude (surfaces, coûts du terrain et de construction) et ses phases : entrées saisies et valeurs calculées
 * par {@code CalculFaisabilite} (FEATURE-patrimoine-faisabilite.md §2.2). Durées et délais en mois.
 */
@Schema(description = "Surfaces, coûts et phases d'une étude, recalculés par le serveur (calcul partagé).")
public record ValeursFaisabilite(
        @Schema(description = "Superficie du terrain (m², Area).") BigDecimal superficie,
        @Schema(description = "Coef équipements et voies d'aménagement = superficie équipements / superficie.")
        BigDecimal coefEquipements,
        @Schema(description = "Superficie équipements et voies d'aménagement (m²).") BigDecimal superficieEquipements,
        @Schema(description = "Calculé : superficie − superficie équipements (AreaNet).") BigDecimal superficieNette,
        @Schema(description = "Coef d'emprise au sol des bâtiments (COS).") BigDecimal coefEmprise,
        @Schema(description = "Emprise au sol des bâtiments (m²) = COS × superficie nette (Maroc) ou × superficie.")
        BigDecimal empriseAuSol,
        @Schema(description = "Coef de plancher couvert (CUF).") BigDecimal cuf,
        @Schema(description = "Plancher couvert (m²) = superficie nette × CUF (Maroc), sinon superficie × COS × CUF.")
        BigDecimal plancherCouvert,
        @Schema(description = "Coef de superficie vendable.") BigDecimal coefVendable,
        @Schema(description = "Plancher vendable (m²) = coef vendable × plancher couvert.") BigDecimal plancherVendable,
        @Schema(description = "Calculé : superficie − emprise au sol (NoBuiltArea).") BigDecimal surfaceNonBatie,
        @Schema(description = "Taux de TVA du terrain (fraction, 0,2 = 20 %).") BigDecimal tauxTvaTerrain,
        @Schema(description = "Prix d'achat du m² TTC.") BigDecimal prixAchatM2,
        @Schema(description = "Calculé : prix d'achat du m² / (1 + TVA terrain).") BigDecimal prixAchatM2Ht,
        @Schema(description = "Calculé : TVA du prix d'achat du m².") BigDecimal prixAchatM2Tva,
        @Schema(description = "Prix du terrain TTC = prix du m² × superficie.") BigDecimal prixTerrain,
        @Schema(description = "Calculé : prix du m² HT × superficie.") BigDecimal prixTerrainHt,
        @Schema(description = "Calculé : prix du terrain − prix HT.") BigDecimal prixTerrainTva,
        @Schema(description = "Coef des frais d'enregistrement (fraction du prix du terrain).")
        BigDecimal tauxFraisEnregistrement,
        @Schema(description = "Calculé : coef × prix du terrain.") BigDecimal fraisEnregistrement,
        @Schema(description = "Calculé : coef × prix du terrain HT.") BigDecimal fraisEnregistrementHt,
        @Schema(description = "Calculé : frais d'enregistrement − HT.") BigDecimal fraisEnregistrementTva,
        @Schema(description = "Coef des autres frais de terrain (fraction du prix du terrain).")
        BigDecimal tauxAutresFraisTerrain,
        @Schema(description = "Calculé : coef × prix du terrain (OtherCostsLand).") BigDecimal autresFraisTerrain,
        @Schema(description = "Calculé : coef × prix HT (colonne OMRAN OtherCostsExecutionHT).") BigDecimal autresFraisTerrainHt,
        @Schema(description = "Calculé : autres frais − HT (colonne OMRAN OtherCostsExecutionVatAmount).")
        BigDecimal autresFraisTerrainTva,
        @Schema(description = "Calculé : autres frais + prix du terrain + frais d'enregistrement (TTC).")
        BigDecimal coutTotalTerrain,
        @Schema(description = "Calculé : mêmes termes en HT (non stocké).") BigDecimal coutTotalTerrainHt,
        @Schema(description = "Coût de construction du m² bâti.") BigDecimal coutConstructionM2Bati,
        @Schema(description = "Calculé : coût du m² bâti × plancher couvert.") BigDecimal coutBati,
        @Schema(description = "Coût de construction du m² non bâti.") BigDecimal coutConstructionM2NonBati,
        @Schema(description = "Calculé : coût du m² non bâti × surface non bâtie.") BigDecimal coutNonBati,
        @Schema(description = "Coef des autres frais d'exécution.") BigDecimal tauxAutresFraisExecution,
        @Schema(description = "Calculé : coef × (coût bâti + coût non bâti).") BigDecimal autresFraisExecution,
        @Schema(description = "Début de l'achat du terrain : départ de toutes les phases.") LocalDateTime debutAchatTerrain,
        @Schema(description = "Durée de l'achat du terrain (mois).") Integer dureeAchatTerrain,
        @Schema(description = "Calculé : début + durée.") LocalDateTime finAchatTerrain,
        @Schema(description = "Délai fin d'achat → début de la période de validation (mois).")
        Integer delaiAchatTerrainPeriodeValidation,
        @Schema(description = "Calculé : fin d'achat + délai.") LocalDateTime debutPeriodeValidation,
        @Schema(description = "Durée de la période de validation (mois).") Integer dureePeriodeValidation,
        @Schema(description = "Calculé : début + durée.") LocalDateTime finPeriodeValidation,
        @Schema(description = "Délai fin de validation → début du permis de bâtir (mois).")
        Integer delaiPeriodeValidationPermisBatir,
        @Schema(description = "Calculé : fin de validation + délai.") LocalDateTime debutPermisBatir,
        @Schema(description = "Durée du permis de bâtir (mois).") Integer dureePermisBatir,
        @Schema(description = "Calculé : début + durée.") LocalDateTime finPermisBatir,
        @Schema(description = "Délai début du permis → début des études (mois).") Integer delaiPermisBatirEtudes,
        @Schema(description = "Calculé : début du permis + délai.") LocalDateTime debutEtudes,
        @Schema(description = "Durée des études (mois).") Integer dureeEtudes,
        @Schema(description = "Calculé : début + durée.") LocalDateTime finEtudes,
        @Schema(description = "Délai fin du permis → début des travaux (mois).") Integer delaiPermisBatirTravaux,
        @Schema(description = "Calculé : fin du permis + délai.") LocalDateTime debutTravaux,
        @Schema(description = "Durée des travaux (mois).") Integer dureeTravaux,
        @Schema(description = "Calculé : début + durée.") LocalDateTime finTravaux,
        @Schema(description = "Délai début des travaux → début de la concrétisation (mois).")
        Integer delaiTravauxConcretisation,
        @Schema(description = "Calculé : début des travaux + délai.") LocalDateTime debutConcretisation,
        @Schema(description = "Durée de la concrétisation (mois).") Integer dureeConcretisation,
        @Schema(description = "Calculé : début + durée.") LocalDateTime finConcretisation,
        @Schema(description = "Délai fin des travaux → début de la finalisation (mois).") Integer delaiTravauxFinalisation,
        @Schema(description = "Calculé : fin des travaux + délai.") LocalDateTime debutFinalisation,
        @Schema(description = "Durée de la finalisation (mois).") Integer dureeFinalisation,
        @Schema(description = "Calculé : début + durée.") LocalDateTime finFinalisation,
        @Schema(description = "Messages Verif OMRAN, dans l'ordre ; vide dans la fiche (Verif passe à chaque écriture).")
        List<String> erreurs) {
}
