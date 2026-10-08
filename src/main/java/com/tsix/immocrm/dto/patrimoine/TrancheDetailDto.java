package com.tsix.immocrm.dto.patrimoine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
        LocalDateTime dateLivraison,
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
        BigDecimal advancePercent,
        BigDecimal concretisationPercent,
        Integer maxVisitNumber,
        Boolean isWithVerifCredit,
        BigDecimal creditPercent
) {
}
