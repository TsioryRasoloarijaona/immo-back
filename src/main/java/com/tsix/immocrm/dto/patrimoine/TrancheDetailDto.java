package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mêmes noms que {@link TrancheEcritureDto} : une fiche relue se renvoie telle quelle en PATCH. Construite par le
 * builder seulement : un constructeur positionnel de cette taille compile avec deux montants inversés.
 */
@Schema(description = "Fiche d'une tranche (CfgTranche). Taux en fraction : 0.2 = 20 %.")
@Builder
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
        @Schema(description = "Date d'octroi des titres fonciers (DeliveryDate, « Date octroi des T.F » d'OMRAN).")
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
        BigDecimal advancePercent,
        @Schema(description = "Toujours égal à advancePercent (OMRAN l'écrit par accesseur) ; non saisissable.")
        BigDecimal concretisationPercent,
        Integer maxVisitNumber,
        Boolean isWithVerifCredit,
        BigDecimal creditPercent,

        // -- Champs saisissables ajoutés (voir TrancheEcritureDto) --
        LocalDateTime datePermisHabiter,
        LocalDateTime dateReception,
        LocalDateTime datePremiereRelance,
        LocalDateTime dateFinConcretisation,
        LocalDateTime dateDebutFinalisation,
        BigDecimal montantPreReservation,
        BigDecimal montantPreReservationRemise,
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

        // -- Contexte et trace --
        @Schema(description = "Code du programme de la tranche.")
        String projetCode,
        @Schema(description = "Entité juridique du programme (CfgCompany.id).")
        UUID entiteId,
        @Schema(description = "Code de l'entité juridique du programme.")
        String entiteCode,
        @Schema(description = "Pays (AdmCountry.id) ; null si non renseigné.")
        UUID paysId,
        @Schema(description = "Libellé du pays.")
        String paysLibelle,
        @Schema(description = "Auteur de la création (UserCreate).")
        String creePar,
        @Schema(description = "Date de création (UserCreateDateTime).")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification (UserUpdate).")
        String modifiePar,
        @Schema(description = "Date de la dernière modification (UserUpdateDateTime).")
        LocalDateTime modifieLe,

        // -- Ajouts FEATURE-patrimoine-tranche-fiche.md §4.1 (mêmes noms que TrancheEcritureDto) --
        @Schema(description = "Libellé du programme de la tranche.")
        String projetLibelle,
        @Schema(description = "Libellé de l'entité juridique du programme.")
        String entiteLibelle,
        UUID standingId,
        String standingLibelle,
        UUID niveauTerrainId,
        @Schema(description = "Libellé du « Nbr. étage » (LndLevel).")
        String niveauTerrainLibelle,
        UUID rubriqueId,
        String rubriqueLibelle,
        UUID responsableSavId,
        String responsableSavLibelle,
        String adresseSav,
        @Schema(description = "Plancher couvert en m² (CoveredFloorArea), lecture seule : Σ des planchers couverts saisis.")
        BigDecimal plancherCouvert,
        Boolean visibleProjet,
        Boolean visibleAchat,
        Boolean visibleSav,
        Boolean visiblePrm,
        Boolean visibleRom,
        Boolean cadeauParrain,
        Boolean cadeauAncienClient,
        Boolean cadeauOrigineProspection,
        Boolean cadeauxCumules,
        UUID tiersAutruiId,
        String tiersAutruiLibelle,
        UUID projetFacturationId,
        String projetFacturationLibelle,
        UUID planComptableId,
        @Schema(description = "Code et libellé du compte (« 4111 — Clients »).")
        String planComptableLibelle,
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
        LocalDateTime dateRepartitionCharges
) {
}
