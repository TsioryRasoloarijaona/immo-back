package com.tsix.immocrm.dto.patrimoine;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneTranche;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filtré par {@link ColonneTranche#FILTRE} : les noms des composants sont exactement ceux du catalogue, et ceux de
 * {@link TrancheDetailDto} pour un même champ. Sans {@code ?colonnes=}, seules les colonnes par défaut sont servies.
 */
@JsonFilter(ColonneTranche.FILTRE)
@Schema(description = "Ligne d'une liste de tranches ; colonnes choisies par ?colonnes= (GET /api/tranches/colonnes).")
@Builder
public record TrancheListItemDto(
        @Schema(description = "Identifiant de la tranche (CfgTranche.PKey). Toujours servi.")
        UUID id,
        String code,
        String description,
        String descriptionLongue,
        @Schema(description = "Programme de la tranche (CfgProject.id).")
        UUID projetId,
        @Schema(description = "Code du programme de la tranche.")
        String projetCode,
        String projetLibelle,
        String entiteLibelle,
        String standingLibelle,
        String niveauTerrainLibelle,
        BigDecimal plancherCouvert,
        String responsableSavLibelle,
        Boolean estEnVente,
        Boolean estEnLocation,
        Boolean visibleProjet,
        Boolean visibleAchat,
        Boolean visibleSav,
        Boolean visiblePrm,
        Boolean visibleRom,

        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        @Schema(description = "Date d'octroi des titres fonciers (CfgTranche.DeliveryDate, « Date octroi des T.F » d'OMRAN).")
        LocalDateTime dateOctroiTitresFonciers,
        LocalDateTime datePermisHabiter,
        LocalDateTime datePremiereRelance,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinConcretisation,
        LocalDateTime dateDebutFinalisation,
        LocalDateTime dateFinCommercialisation,
        LocalDateTime dateReception,

        @Schema(description = "Ville, adresses 1 à 3, code postal et pays de la tranche mis bout à bout.")
        String adresseComplete,
        String ville,
        String paysLibelle,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,
        String adresseSav,
        @Schema(description = "« Courrier fait à » du programme.")
        String villeSignature,
        @Schema(description = "Représentant légal de l'entité juridique du programme.")
        String representantLegal,
        @Schema(description = "Signataire du programme.")
        String signataireLibelle,
        @Schema(description = "Responsable technique du programme.")
        String responsableTechniqueLibelle,
        @Schema(description = "Responsable commercial du programme.")
        String responsableCommercialLibelle,

        BigDecimal montantPreReservation,
        Boolean avanceSurMontant,
        @Schema(description = "« Concrétisation », fraction.")
        BigDecimal advancePercent,
        BigDecimal montantAvance,
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
        Integer delaiRetractation,
        Integer delaiMaxEcheance,
        Boolean verifierDelaiMaxEcheance,
        BigDecimal creditPercent,
        Boolean isWithVerifCredit,
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
        BigDecimal autresFraisRemise,
        BigDecimal montantSyndicRemise,

        Integer delaiNotaireMinutes,
        Integer delaiToutSoldeMinutes,
        BigDecimal ecartTypeConcretisation,
        BigDecimal ecartTypeFinalisation,
        String tiersAutruiLibelle,
        String projetFacturationLibelle,
        String planComptableLibelle,
        String rubriqueLibelle,

        LocalDateTime dateMainLeveeAfh,
        LocalDateTime dateCertificatDefinitif1,
        LocalDateTime dateMainLeveeBancaire,
        LocalDateTime dateCertificatDefinitif2,
        LocalDateTime dateDepotLotissementMunicipalite,
        LocalDateTime dateRetraitLotissementMunicipalite,
        LocalDateTime dateDepotLotissementNonApprouveOtc,
        LocalDateTime dateReglementFraisOtc,
        LocalDateTime dateDepotLotissementApprouveOtc,
        LocalDateTime dateRetraitPlansMutationOtc,
        LocalDateTime dateAchevementPartages,
        LocalDateTime dateDepotPartageCpf,
        LocalDateTime datePaiementFraisPartageCpf,
        LocalDateTime dateRetraitPartageCpf,
        LocalDateTime dateRetraitCertificatsIndividuels,
        LocalDateTime dateVerificationCertificats,
        LocalDateTime dateRepartitionCharges,

        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
