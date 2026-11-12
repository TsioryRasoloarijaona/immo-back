package com.tsix.immocrm.dto.projet;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Fiche d'un lot : les champs de la ligne de liste, plus estimation, taux, options, dates et onglets de la fiche.")
public record LotDetailDto(
        @JsonUnwrapped LotListItemDto lot,
        @Schema(description = "Libellé du projet.") String projetDescription,
        @Schema(description = "Code du genre de lot (StrEntity).", example = "PrjMarket") String entiteCode,
        @Schema(description = "Libellé du genre de lot.", example = "Lot Entrepreneurs") String entiteDescription,
        @Schema(description = "Montant forfaitaire HT saisi (EstimatedPriceHTManual).") BigDecimal montantForfaitaireHT,
        @Schema(description = "TVA forfaitaire saisie (EstimatedVatAmountManual).") BigDecimal montantForfaitaireTVA,
        @Schema(description = "Montant forfaitaire TTC saisi (EstimatedPriceTTCManual).") BigDecimal montantForfaitaireTTC,
        @Schema(description = "Estimation HT : Σ bordereau estimatif, sinon montant forfaitaire HT.") BigDecimal estimationHT,
        @Schema(description = "Estimation TVA : Σ bordereau estimatif, sinon TVA forfaitaire.") BigDecimal estimationTVA,
        @Schema(description = "Estimation TTC : Σ bordereau estimatif, sinon montant forfaitaire TTC.") BigDecimal estimationTTC,
        @Schema(description = "Vrai si l'estimation vient du bordereau estimatif.") boolean estimationDepuisBordereau,
        @Schema(description = "Marché TTC × taux de retenue de garantie.") BigDecimal retenueGarantieTheorique,
        @Schema(description = "Marché TTC × taux d'avance.") BigDecimal avanceTheorique,
        @Schema(description = "Taux d'avance en pourcentage (0–100).") BigDecimal tauxAvance,
        @Schema(description = "Taux de compte prorata en pourcentage (ProportionRatio).") BigDecimal tauxCompteProrata,
        @Schema(description = "Taux d'assurance décennale en pourcentage.") BigDecimal tauxAssuranceDEC,
        @Schema(description = "Taux d'assurance tous risques chantier en pourcentage.") BigDecimal tauxAssuranceTRC,
        @Schema(description = "TVA par défaut en pourcentage (DefaultVat).", example = "20") BigDecimal tvaDefaut,
        @Schema(description = "Délai de paiement en jours (PaymentDelay).") Integer delaiPaiementJours,
        @Schema(description = "Durée de garantie en mois (WarrantyInMonths).") BigDecimal garantieMois,
        @Schema(description = "« Bloquer la restitution de la retenue de garantie » (BlokWarrantyRestitution).")
        boolean restitutionGarantieBloquee,
        @Schema(description = "Lot avec décomposition (WithMarketView).") boolean avecDecomposition,
        @Schema(description = "Lot clôturé (IsClosed).") boolean clos,
        @Schema(description = "Σ net à payer des décomptes (AmountToPay).") BigDecimal cumulNetAPayer,
        @Schema(description = "Date de début prévisionnelle (StartDate).") LocalDate dateDebutPrevisionnelle,
        @Schema(description = "Délai prévisionnel en jours (Delay).") Integer delaiPrevisionnelJours,
        @Schema(description = "Date de fin prévisionnelle (EndDate).") LocalDate dateFinPrevisionnelle,
        @Schema(description = "Date de lancement de l'appel d'offres (LaunchOfTenderDate).") LocalDate dateLancementAppelOffres,
        @Schema(description = "Date de retrait du dossier de consultation.") LocalDate dateRetraitDossiers,
        @Schema(description = "Date d'échéance de remise des offres (TenderMaturityDate).") LocalDate dateEcheanceRemiseOffres,
        @Schema(description = "Date d'ouverture des plis (BidOpeningDate).") LocalDate dateOuverturePlis,
        @Schema(description = "Date d'adjudication (TenderDate).") LocalDate dateAdjudication,
        @Schema(description = "Date de signature du marché (ContractSignatureDate).") LocalDate dateSignatureMarche,
        @Schema(description = "Date d'ordre de service (ServiceOrderDate).") LocalDate dateOrdreService,
        @Schema(description = "Date de lancement des travaux (WorkLaunchDate).") LocalDate dateLancementTravaux,
        @Schema(description = "Création (UserCreateDateTime).") LocalDateTime dateCreation,
        @Schema(description = "Auteur de la création (UserCreate).") String creePar,
        @Schema(description = "Dernière modification (UserUpdateDateTime).") LocalDateTime dateModification,
        @Schema(description = "Auteur de la dernière modification (UserUpdate).") String modifiePar,
        @JsonUnwrapped FicheEtendueLotDto fiche
) {
}
