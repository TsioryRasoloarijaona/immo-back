package com.tsix.immocrm.dto.patrimoine;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneTranche;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ligne de la liste des tranches : colonnes du catalogue {@link ColonneTranche}, mêmes noms que
 * {@link TrancheDetailDto}. Seules les colonnes demandées par {@code ?colonnes=} sont sérialisées.
 */
@JsonFilter(ColonneTranche.FILTRE)
@Schema(description = "Ligne de liste de tranche ; sans ?colonnes=, seules les colonnes par défaut du catalogue sont "
        + "présentes.")
public record TrancheListItemDto(
        UUID id,
        @Schema(description = "Programme (CfgProject) de la tranche.") UUID projetId,
        String code,
        String description,
        String projetCode,
        String ville,
        @Schema(description = "CfgTranche.DeliveryDate : date d'octroi des titres fonciers (ex-dateLivraison).")
        LocalDateTime dateOctroiTitresFonciers,
        String standingLibelle,
        String entiteCode,
        Boolean estEnVente,

        String descriptionLongue,
        String niveauTerrainLibelle,
        String rubriqueLibelle,
        BigDecimal plancherCouvert,
        String responsableSavLibelle,

        @Schema(description = "SignatureCity du programme.") String villeSignature,
        @Schema(description = "Signataire du programme.") String signataireLibelle,
        @Schema(description = "Représentant légal de l'entité.") String representantLegalLibelle,
        @Schema(description = "Responsable commercial du programme.") String responsableCommercialLibelle,
        @Schema(description = "Responsable technique du programme.") String responsableTechniqueLibelle,

        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        LocalDateTime datePermisHabiter,
        LocalDateTime datePremiereRelance,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinConcretisation,
        LocalDateTime dateDebutFinalisation,
        LocalDateTime dateFinCommercialisation,

        String adresse,
        String adresseSav,
        String paysLibelle,

        BigDecimal montantPreReservation,
        BigDecimal advancePercent,
        BigDecimal fraisDossier,
        BigDecimal tauxPenaliteAnnulation,
        Boolean penaliteAnnulationSurPrixArticle,
        BigDecimal penaliteTransfertArticle,
        BigDecimal penaliteTransfertTranche,
        BigDecimal penaliteTransfertClient,
        BigDecimal autresFrais,
        BigDecimal montantSyndic,
        Integer periodeSyndic,

        Integer delaiPremierEngagement,
        Integer delaiDeuxiemeEngagement,
        BigDecimal tauxPenalitePremierEngagement,
        BigDecimal tauxPenaliteDeuxiemeEngagement,
        Integer delaiSoldePaiement,
        Integer delaiGarantie,
        Integer delaiGarantieSupplement,
        String codeDroitRetractation,
        String tribunalCommerce,
        Integer delaiMinimumIntervention,
        Integer maxVisitNumber,
        Boolean cadeauParrain,
        Boolean cadeauAncienClient,
        Boolean cadeauOrigineProspection,
        Boolean cadeauxCumules,

        BigDecimal montantPreReservationRemise,
        BigDecimal advancePercentRemise,
        Boolean advancePercentRemiseActive,
        BigDecimal fraisDossierRemise,
        BigDecimal tauxPenaliteAnnulationRemise,
        BigDecimal penaliteTransfertArticleRemise,
        BigDecimal penaliteTransfertTrancheRemise,
        BigDecimal penaliteTransfertClientRemise,
        BigDecimal montantSyndicRemise,
        BigDecimal autresFraisRemise,

        Integer delaiNotaireMinutes,
        Integer delaiToutSoldeMinutes,
        BigDecimal ecartTypeConcretisation,
        BigDecimal ecartTypeFinalisation,
        String tiersAutruiLibelle,
        String projetFacturationLibelle,
        String planComptableLibelle,

        Boolean estEnLocation,
        Boolean visibleProjet,
        Boolean visibleAchat,
        Boolean visibleSav,
        Boolean visibleRom,
        Boolean visiblePrm,

        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
