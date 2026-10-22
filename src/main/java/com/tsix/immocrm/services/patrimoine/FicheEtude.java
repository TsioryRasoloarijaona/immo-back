package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.ValeursFaisabilite;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteListeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.SaisieEtudeDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatus;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.utils.Champs;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;

/** Correspondance étude ↔ {@link ValeursFaisabilite} et lignes de lecture (FEATURE-patrimoine-faisabilite.md §2.1-2.3). */
final class FicheEtude {

    private FicheEtude() {
    }

    /**
     * Entrées du bloc A et des phases saisies, posées sur l'étude avant le calcul. Paire : le coefficient saisi, sinon le
     * montant saisi (coefficient effacé pour que le calcul le reprenne), sinon le coefficient stocké.
     */
    static void appliquerEntrees(StkFeasibilityStudy e, SaisieEtudeDto d, ChampsAVider v) {
        e.setArea(v.ou("superficie", positif(d.superficie(), "superficie"), e.getArea()));
        e.setEquipementsCoef(coef(d.coefEquipements(), d.superficieEquipements(), e.getEquipementsCoef()));
        e.setEquipementsAmount(ou(d.superficieEquipements(), e.getEquipementsAmount()));
        e.setOccupationSolRatio(coef(d.coefEmprise(), d.empriseAuSol(), e.getOccupationSolRatio()));
        e.setOccupationGroundArea(ou(d.empriseAuSol(), e.getOccupationGroundArea()));
        e.setCuf(coef(d.cuf(), d.plancherCouvert(), e.getCuf()));
        e.setTotalCoveredArea(ou(d.plancherCouvert(), e.getTotalCoveredArea()));
        e.setCoefSaleableArea(coef(d.coefVendable(), d.plancherVendable(), e.getCoefSaleableArea()));
        e.setSalableArea(ou(d.plancherVendable(), e.getSalableArea()));
        e.setPurchaseUnitCost(coef(d.prixAchatM2(), d.prixTerrain(), e.getPurchaseUnitCost()));
        e.setPriceArea(ou(d.prixTerrain(), e.getPriceArea()));
        e.setLandVatRatio(v.ou("tauxTvaTerrain", d.tauxTvaTerrain(), e.getLandVatRatio()));
        e.setRegistryCostsRatio(v.ou("tauxFraisEnregistrement", d.tauxFraisEnregistrement(), e.getRegistryCostsRatio()));
        e.setOtherCostsLandRatio(v.ou("tauxAutresFraisTerrain", d.tauxAutresFraisTerrain(), e.getOtherCostsLandRatio()));
        e.setUnitCostConstructionBuilt(v.ou("coutConstructionM2Bati", d.coutConstructionM2Bati(),
                e.getUnitCostConstructionBuilt()));
        e.setUnitCostConstructionNoBuilt(v.ou("coutConstructionM2NonBati", d.coutConstructionM2NonBati(),
                e.getUnitCostConstructionNoBuilt()));
        e.setOtherCostsExecutionRatio(v.ou("tauxAutresFraisExecution", d.tauxAutresFraisExecution(),
                e.getOtherCostsExecutionRatio()));
        e.setStartDatePurchaseLand(v.ou("debutAchatTerrain", d.debutAchatTerrain(), e.getStartDatePurchaseLand()));
        e.setDelayPurchaseLand(v.ou("dureeAchatTerrain", d.dureeAchatTerrain(), e.getDelayPurchaseLand()));
        e.setDelayPeriodeOfValidation(v.ou("dureePeriodeValidation", d.dureePeriodeValidation(), e.getDelayPeriodeOfValidation()));
        e.setDelayPermissionBatir(v.ou("dureePermisBatir", d.dureePermisBatir(), e.getDelayPermissionBatir()));
        e.setDelayStudy(v.ou("dureeEtudes", d.dureeEtudes(), e.getDelayStudy()));
        e.setDelayExpenses(v.ou("dureeTravaux", d.dureeTravaux(), e.getDelayExpenses()));
        e.setDelayConcretisation(v.ou("dureeConcretisation", d.dureeConcretisation(), e.getDelayConcretisation()));
        e.setDelayFinalization(v.ou("dureeFinalisation", d.dureeFinalisation(), e.getDelayFinalization()));
        e.setDelayPurchaseLandToValidation(v.ou("delaiAchatTerrainPeriodeValidation",
                d.delaiAchatTerrainPeriodeValidation(), e.getDelayPurchaseLandToValidation()));
        e.setDelayValidationToPermission(v.ou("delaiPeriodeValidationPermisBatir", d.delaiPeriodeValidationPermisBatir(),
                e.getDelayValidationToPermission()));
        e.setDelayPermissionToStudy(v.ou("delaiPermisBatirEtudes", d.delaiPermisBatirEtudes(), e.getDelayPermissionToStudy()));
        e.setDelayPermissionToExpenses(v.ou("delaiPermisBatirTravaux", d.delaiPermisBatirTravaux(),
                e.getDelayPermissionToExpenses()));
        e.setDelayExpensesToConcretisation(v.ou("delaiTravauxConcretisation", d.delaiTravauxConcretisation(),
                e.getDelayExpensesToConcretisation()));
        e.setDelayExpensesToFinalization(v.ou("delaiTravauxFinalisation", d.delaiTravauxFinalisation(),
                e.getDelayExpensesToFinalization()));
    }

    /** Coefficient d'une paire avant calcul : null quand seul le montant est saisi (§8 bis.6). */
    private static BigDecimal coef(BigDecimal coefSaisi, BigDecimal montantSaisi, BigDecimal stocke) {
        if (coefSaisi != null) {
            return coefSaisi;
        }
        return montantSaisi != null ? null : stocke;
    }

    /** Valeurs telles que posées sur l'étude ; {@code erreurs} vide. */
    static ValeursFaisabilite valeurs(StkFeasibilityStudy e) {
        return new ValeursFaisabilite(e.getArea(), e.getEquipementsCoef(), e.getEquipementsAmount(), e.getAreaNet(),
                e.getOccupationSolRatio(), e.getOccupationGroundArea(), e.getCuf(), e.getTotalCoveredArea(),
                e.getCoefSaleableArea(), e.getSalableArea(), e.getNoBuiltArea(), e.getLandVatRatio(),
                e.getPurchaseUnitCost(), e.getPurchaseUnitCostHt(), e.getPurchaseUnitCostVatAmount(),
                e.getPriceArea(), e.getPriceAreaHt(), e.getPriceAreaVatAmount(),
                e.getRegistryCostsRatio(), e.getRegistryCosts(), e.getRegistryCostsHt(), e.getRegistryCostsVatAmount(),
                e.getOtherCostsLandRatio(), e.getOtherCostsLand(), e.getOtherCostsLandHt(), e.getOtherCostsLandVatAmount(),
                e.getTotalCostLand(), somme(e.getOtherCostsLandHt(), e.getPriceAreaHt(), e.getRegistryCostsHt()),
                e.getUnitCostConstructionBuilt(), e.getCostAreaBuilt(), e.getUnitCostConstructionNoBuilt(),
                e.getCostAreaNoBuilt(), e.getOtherCostsExecutionRatio(), e.getOtherCostsExecution(),
                e.getStartDatePurchaseLand(), e.getDelayPurchaseLand(), e.getEndDatePurchaseLand(),
                e.getDelayPurchaseLandToValidation(), e.getStartDatePeriodeOfValidation(), e.getDelayPeriodeOfValidation(),
                e.getEndDatePeriodeOfValidation(), e.getDelayValidationToPermission(), e.getStartDatePermissionBatir(),
                e.getDelayPermissionBatir(), e.getEndDatePermissionBatir(), e.getDelayPermissionToStudy(),
                e.getStartDateStudy(), e.getDelayStudy(), e.getEndDateStudy(), e.getDelayPermissionToExpenses(),
                e.getStartDateExpenses(), e.getDelayExpenses(), e.getEndDateExpenses(),
                e.getDelayExpensesToConcretisation(), e.getStartDateConcretisation(), e.getDelayConcretisation(),
                e.getEndDateConcretisation(), e.getDelayExpensesToFinalization(), e.getStartDateFinalization(),
                e.getDelayFinalization(), e.getEndDateFinalization(), List.of());
    }

    /** Écrit le résultat du calcul partagé : toutes les colonnes du bloc A et des phases. */
    static void ecrire(StkFeasibilityStudy e, ValeursFaisabilite r) {
        e.setArea(r.superficie());
        e.setEquipementsCoef(r.coefEquipements());
        e.setEquipementsAmount(r.superficieEquipements());
        e.setAreaNet(r.superficieNette());
        e.setOccupationSolRatio(r.coefEmprise());
        e.setOccupationGroundArea(r.empriseAuSol());
        e.setCuf(r.cuf());
        e.setTotalCoveredArea(r.plancherCouvert());
        e.setCoefSaleableArea(r.coefVendable());
        e.setSalableArea(r.plancherVendable());
        e.setNoBuiltArea(r.surfaceNonBatie());
        e.setLandVatRatio(r.tauxTvaTerrain());
        e.setPurchaseUnitCost(r.prixAchatM2());
        e.setPurchaseUnitCostHt(r.prixAchatM2Ht());
        e.setPurchaseUnitCostVatAmount(r.prixAchatM2Tva());
        e.setPriceArea(r.prixTerrain());
        e.setPriceAreaHt(r.prixTerrainHt());
        e.setPriceAreaVatAmount(r.prixTerrainTva());
        e.setRegistryCostsRatio(r.tauxFraisEnregistrement());
        e.setRegistryCosts(r.fraisEnregistrement());
        e.setRegistryCostsHt(r.fraisEnregistrementHt());
        e.setRegistryCostsVatAmount(r.fraisEnregistrementTva());
        e.setOtherCostsLandRatio(r.tauxAutresFraisTerrain());
        e.setOtherCostsLand(r.autresFraisTerrain());
        e.setOtherCostsLandHt(r.autresFraisTerrainHt());
        e.setOtherCostsLandVatAmount(r.autresFraisTerrainTva());
        e.setTotalCostLand(r.coutTotalTerrain());
        e.setUnitCostConstructionBuilt(r.coutConstructionM2Bati());
        e.setCostAreaBuilt(r.coutBati());
        e.setUnitCostConstructionNoBuilt(r.coutConstructionM2NonBati());
        e.setCostAreaNoBuilt(r.coutNonBati());
        e.setOtherCostsExecutionRatio(r.tauxAutresFraisExecution());
        e.setOtherCostsExecution(r.autresFraisExecution());
        e.setEndDatePurchaseLand(r.finAchatTerrain());
        e.setStartDatePeriodeOfValidation(r.debutPeriodeValidation());
        e.setEndDatePeriodeOfValidation(r.finPeriodeValidation());
        e.setStartDatePermissionBatir(r.debutPermisBatir());
        e.setEndDatePermissionBatir(r.finPermisBatir());
        e.setStartDateStudy(r.debutEtudes());
        e.setEndDateStudy(r.finEtudes());
        e.setStartDateExpenses(r.debutTravaux());
        e.setEndDateExpenses(r.finTravaux());
        e.setStartDateConcretisation(r.debutConcretisation());
        e.setEndDateConcretisation(r.finConcretisation());
        e.setStartDateFinalization(r.debutFinalisation());
        e.setEndDateFinalization(r.finFinalisation());
    }

    static BilanEtudeDto bilan(StkFeasibilityStudy e) {
        return new BilanEtudeDto(e.getTurnover(), e.getTotalCostExecution(), e.getTotalCostExecutionHt(), e.getTotalCost(),
                e.getTotalCostHt(), e.getUnitBackCost(), e.getUnitSalePrice(), e.getPercentageConcretisation(),
                e.getConretisationAmount(), e.getFinalizationAmount(), e.getTotalCostFunding(), e.getAutofinancementRate(),
                e.getAutofinancementAmount(), e.getClientfinancementRate(), e.getCreditfinancementRate(),
                e.getGrossProfitMargin(), e.getImpotRatio(), e.getImpotAmount(), e.getNetProfitMargin(),
                e.getProfitRatioByProjectCost(), e.getProfitRatioByTurnover(), e.getProfitRatioByAutofinancementAmount());
    }

    static EtudeFaisabiliteListeDto ligne(StkFeasibilityStudy e) {
        StkFeasibilityStudyStatus s = e.getStatus();
        return new EtudeFaisabiliteListeDto(e.getId(), e.getCode(), e.getDescription(), e.getDocDate(),
                Champs.de(e.getCfgCompany(), CfgCompany::getId), Champs.de(e.getCfgCompany(), CfgCompany::getDescription),
                Champs.de(e.getStkItem(), StkItem::getDescription), Champs.de(s, StkFeasibilityStudyStatus::getId),
                Champs.de(s, StkFeasibilityStudyStatus::getDescription), Champs.de(s, StkFeasibilityStudyStatus::getColorName),
                Boolean.TRUE.equals(e.getIsLocked()), e.getArea(), e.getTotalCostLand(), e.getTotalCost(), e.getTurnover(),
                e.getAutofinancementAmount(), e.getNetProfitMargin(), e.getConretisationAmount(), e.getFinalizationAmount(),
                e.getPercentageConcretisation(), e.getProfitRatioByProjectCost(), e.getProfitRatioByTurnover(),
                e.getProfitRatioByAutofinancementAmount());
    }

    private static BigDecimal somme(BigDecimal... termes) {
        return Stream.of(termes).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
