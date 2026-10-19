package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Mêmes noms que {@link TrancheDetailDto} ; taux en fraction. Les défauts OMRAN s'appliquent aux champs absents. */
@Schema(description = "Création ou modification d'une tranche (CfgTranche). En PATCH, absent ou null = inchangé, "
        + "\"\" = vidé. Taux en fraction (0,2 = 20 %).")
public record TrancheEcritureDto(
        @Schema(description = "Programme (CfgProject). Obligatoire à la création, non modifiable ensuite.") UUID projetId,
        @Schema(description = "Code, unique sur toutes les tranches. Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        String descriptionLongue,
        @Schema(description = "IsForSale, défaut vrai.") Boolean estEnVente,
        @Schema(description = "IsForRent, défaut faux.") Boolean estEnLocation,
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        String ville,
        @Schema(description = "Pays (AdmCountry).") UUID paysId,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,
        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinCommercialisation,
        @Schema(description = "DeliveryDate : date d'octroi des titres fonciers.") LocalDateTime dateOctroiTitresFonciers,
        LocalDateTime datePermisHabiter,
        LocalDateTime dateReception,
        LocalDateTime datePremiereRelance,
        LocalDateTime dateFinConcretisation,
        LocalDateTime dateDebutFinalisation,
        BigDecimal montantPreReservation,
        BigDecimal montantPreReservationRemise,
        Boolean avanceSurMontant,
        BigDecimal montantAvance,
        @Schema(description = "Taux de concrétisation ; ConcretisationPercent le suit.") BigDecimal advancePercent,
        BigDecimal advancePercentRemise,
        Boolean advancePercentRemiseActive,
        BigDecimal fraisDossier,
        BigDecimal fraisDossierRemise,
        BigDecimal tauxPenaliteAnnulation,
        BigDecimal tauxPenaliteAnnulationRemise,
        @Schema(description = "Défaut vrai.") Boolean penaliteAnnulationSurPrixArticle,
        BigDecimal penaliteTransfertArticle,
        BigDecimal penaliteTransfertArticleRemise,
        BigDecimal penaliteTransfertTranche,
        BigDecimal penaliteTransfertTrancheRemise,
        BigDecimal penaliteTransfertClient,
        BigDecimal penaliteTransfertClientRemise,
        BigDecimal autresFrais,
        BigDecimal autresFraisRemise,
        BigDecimal montantSyndic,
        BigDecimal montantSyndicRemise,
        Integer periodeSyndic,
        @Schema(description = "Défaut 60 jours.") Integer delaiPremierEngagement,
        @Schema(description = "Défaut 30 jours.") Integer delaiDeuxiemeEngagement,
        BigDecimal tauxPenalitePremierEngagement,
        BigDecimal tauxPenaliteDeuxiemeEngagement,
        Integer delaiSoldePaiement,
        Integer delaiGarantie,
        Integer delaiGarantieSupplement,
        String codeDroitRetractation,
        String tribunalCommerce,
        @Schema(description = "Défaut 2.") Integer delaiMinimumIntervention,
        @Schema(description = "Défaut 4.") Integer maxVisitNumber,
        @Schema(description = "Défaut 48.") Integer delaiRetractation,
        Integer delaiMaxEcheance,
        Boolean verifierDelaiMaxEcheance,
        @Schema(description = "Crédit maximum, fraction ≤ 1.") BigDecimal creditPercent,
        Boolean isWithVerifCredit,
        @Schema(description = "Défaut 60.") Integer delaiNotaireMinutes,
        @Schema(description = "Défaut 30.") Integer delaiToutSoldeMinutes,
        @Schema(description = "Défaut 5.") BigDecimal ecartTypeConcretisation,
        @Schema(description = "Défaut 3.") BigDecimal ecartTypeFinalisation,

        // -- Lot P1a (FEATURE-patrimoine-tranche-fiche.md §4.1) --
        @Schema(description = "Standing (CfgTrancheStanding), voir /api/referentiels-patrimoine/standings.") UUID standingId,
        @Schema(description = "« Nbr. étage » (LndLevel), voir /api/referentiels-patrimoine/niveaux-terrain.")
        UUID niveauTerrainId,
        @Schema(description = "Rubrique (StkTrancheHeading).") UUID rubriqueId,
        @Schema(description = "Responsable S.A.V (CfgClaimsManagerId), un personnel.") UUID responsableSavId,
        @Schema(description = "Adresse S.A.V.") String adresseSav,
        @Schema(description = "IsForProject, défaut faux.") Boolean visibleProjet,
        @Schema(description = "IsForRom, défaut faux.") Boolean visibleRom,
        @Schema(description = "IsForBuy (module Achat), défaut faux.") Boolean visibleAchat,
        @Schema(description = "IsForSav, défaut faux.") Boolean visibleSav,
        @Schema(description = "IsForPrm, défaut faux.") Boolean visiblePrm,
        @Schema(description = "IsWithGiftForProposer : cadeau au parrain.") Boolean cadeauParrain,
        @Schema(description = "IsWithGiftForOldCustomer : cadeau à l'ancien client.") Boolean cadeauAncienClient,
        @Schema(description = "IsWithGiftForProspectionOrigin : cadeau selon la source de contact.")
        Boolean cadeauOrigineProspection,
        @Schema(description = "AccumulatedGifts : cadeaux cumulables.") Boolean cadeauxCumules,
        @Schema(description = "« Tiers autrui » (CfgTierOtherId), un fournisseur.") UUID tiersAutruiId,
        @Schema(description = "« Projet de facturation pour les dépenses » (PrjProject).") UUID projetFacturationId,
        @Schema(description = "« Plan compt. » (ActAccount).") UUID planComptableId,
        @Schema(description = "RaisedHandDateAFH : date main levée AFH.") LocalDateTime dateMainLeveeAfh,
        @Schema(description = "TitleDate1 : date certificat définitif 1.") LocalDateTime dateCertificatDefinitif1,
        @Schema(description = "RaisedHandDateBank : date main levée bancaire.") LocalDateTime dateMainLeveeBancaire,
        @Schema(description = "TitleDate2 : date certificat définitif 2.") LocalDateTime dateCertificatDefinitif2,
        @Schema(description = "MunicipalityDateDeposit : dépôt du dossier de lotissement à la municipalité.")
        LocalDateTime dateDepotLotissementMunicipalite,
        @Schema(description = "MunicipalityDateWithdrawal : retrait du dossier de lotissement de la municipalité.")
        LocalDateTime dateRetraitLotissementMunicipalite,
        @Schema(description = "OTCDateNonApprovedDeposit : dépôt du lotissement non approuvé à l'OTC.")
        LocalDateTime dateDepotLotissementNonApprouveOtc,
        @Schema(description = "OTCDateSettlement : règlement des frais de l'OTC.") LocalDateTime dateReglementFraisOtc,
        @Schema(description = "OTCDateApprovedDeposit : dépôt du lotissement approuvé à l'OTC.")
        LocalDateTime dateDepotLotissementApprouveOtc,
        @Schema(description = "OTCDateMutationWithdrawal : retrait des plans de mutation de l'OTC.")
        LocalDateTime dateRetraitPlansMutationOtc,
        @Schema(description = "SharingDate : achèvement des partages.") LocalDateTime dateAchevementPartages,
        @Schema(description = "SharingDateDeposit : dépôt du dossier de partage à la CPF.") LocalDateTime dateDepotPartageCpf,
        @Schema(description = "CPFSharingSettlementDate : paiement des frais de partage CPF.")
        LocalDateTime datePaiementFraisPartageCpf,
        @Schema(description = "CPFSharingWithdrawalDate : retrait du dossier de partage à la CPF.")
        LocalDateTime dateRetraitPartageCpf,
        @Schema(description = "TitleDate : retrait des certificats de propriété individuels.")
        LocalDateTime dateRetraitCertificatsIndividuels,
        @Schema(description = "TitleDateVerification : vérification des certificats.") LocalDateTime dateVerificationCertificats,
        @Schema(description = "RepartitionDate : répartition des charges.") LocalDateTime dateRepartitionCharges,
        @Schema(description = "PATCH : noms de champs à vider (date, référence → NULL ; nombre → 0 ; case → faux ; texte → \"\").")
        List<String> champsAVider
) {
}
