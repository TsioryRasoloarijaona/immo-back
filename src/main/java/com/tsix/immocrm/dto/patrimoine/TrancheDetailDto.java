package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Taux et pourcentages en fraction (0,20 = 20 %), comme en base. */
public record TrancheDetailDto(
        UUID id,
        UUID projetId,
        String code,
        String description,
        String descriptionLongue,
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        String ville,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,
        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinCommercialisation,
        @Schema(description = "CfgTranche.DeliveryDate : date d'octroi des titres fonciers (ex-dateLivraison).")
        LocalDateTime dateOctroiTitresFonciers,
        Boolean estEnVente,
        Boolean estEnLocation,

        // -- Pénalités --
        BigDecimal tauxPenaliteAnnulation,
        BigDecimal tauxPenaliteAnnulationRemise,
        BigDecimal penaliteTransfertArticle,
        BigDecimal penaliteTransfertArticleRemise,
        BigDecimal penaliteTransfertTranche,
        BigDecimal penaliteTransfertTrancheRemise,
        BigDecimal penaliteTransfertClient,
        BigDecimal penaliteTransfertClientRemise,
        BigDecimal tauxPenalitePremierEngagement,
        BigDecimal tauxPenaliteDeuxiemeEngagement,

        // -- Délais --
        Integer delaiGarantie,
        Integer delaiGarantieSupplement,
        Integer delaiPremierEngagement,
        Integer delaiDeuxiemeEngagement,
        Integer delaiSoldePaiement,
        Integer delaiNotaireMinutes,
        Integer delaiToutSoldeMinutes,
        Integer delaiRetractation,
        Integer delaiMinimumIntervention,
        Integer delaiMaxEcheance,

        // -- Règles commerciales --
        @Schema(description = "AdvancePercent, libellé OMRAN « Concrétisation ».") BigDecimal advancePercent,
        @Schema(description = "Toujours égal à advancePercent.") BigDecimal concretisationPercent,
        Integer maxVisitNumber,
        Boolean isWithVerifCredit,
        BigDecimal creditPercent,

        // -- Ajouts de l'écriture (FEATURE-patrimoine-ecriture.md §4.3) --
        String projetCode,
        UUID entiteId,
        String entiteCode,
        UUID paysId,
        String paysLibelle,
        LocalDateTime datePermisHabiter,
        LocalDateTime dateReception,
        LocalDateTime datePremiereRelance,
        LocalDateTime dateFinConcretisation,
        LocalDateTime dateDebutFinalisation,
        BigDecimal montantPreReservation,
        BigDecimal montantPreReservationRemise,
        @Schema(description = "AdvanceBasedOnAmount : l'avance est un montant fixe (montantAvance) plutôt qu'un taux.")
        Boolean avanceSurMontant,
        BigDecimal montantAvance,
        BigDecimal advancePercentRemise,
        Boolean advancePercentRemiseActive,
        BigDecimal fraisDossier,
        BigDecimal fraisDossierRemise,
        Boolean penaliteAnnulationSurPrixArticle,
        BigDecimal autresFrais,
        BigDecimal autresFraisRemise,
        BigDecimal montantSyndic,
        BigDecimal montantSyndicRemise,
        Integer periodeSyndic,
        String codeDroitRetractation,
        String tribunalCommerce,
        Boolean verifierDelaiMaxEcheance,
        BigDecimal ecartTypeConcretisation,
        BigDecimal ecartTypeFinalisation,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
