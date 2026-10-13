package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Onglet « Dates » de la fiche OMRAN ; chaque date est null tant que le jalon n'est pas atteint.")
public record DatesFicheDto(
        @Schema(description = "Concrétisation (ComFolder.AdvanceDate).")
        LocalDateTime concretisation,
        @Schema(description = "Promesse de vente (ComFolder.ReservationDate).")
        LocalDateTime promesseVente,
        @Schema(description = "Visite technique / réception (ComFolder.ReceptionDate).")
        LocalDateTime visiteTechnique,
        @Schema(description = "Envoi au notaire prévisionnel (ComFolder.AgreementDatePrevisionel).")
        LocalDateTime envoiNotairePrevisionnel,
        @Schema(description = "Case « Confirmé » de l'envoi au notaire prévisionnel (ComFolder.IsConfirmedAgreementDate).")
        Boolean envoiNotaireConfirme,
        @Schema(description = "Date d'enregistrement du contrat (ComFolder.AgreementRegistryDate).")
        LocalDateTime enregistrementContrat,
        @Schema(description = "Envoi au notaire / avocat (ComFolder.AgreementDate).")
        LocalDateTime envoiNotaire,
        @Schema(description = "Minute / signature du contrat (ComFolder.AgreementSavingDate).")
        LocalDateTime minute,
        @Schema(description = "Expédition / certificat de propriété (ComFolder.OwnershipCertificateDate).")
        LocalDateTime expedition,
        @Schema(description = "Relance 1 titre foncier (ComFolder.Revival1).")
        LocalDateTime relanceTitreFoncier1,
        @Schema(description = "Échéance de la relance 1 (ComFolder.Revival1ValidityDate).")
        LocalDateTime echeanceRelance1,
        @Schema(description = "Relance 2 titre foncier (ComFolder.Revival2).")
        LocalDateTime relanceTitreFoncier2,
        @Schema(description = "Échéance de la relance 2 (ComFolder.Revival2ValidityDate).")
        LocalDateTime echeanceRelance2,
        @Schema(description = "Fiche bancaire (ComFolder.BankRecordDate).")
        LocalDateTime ficheBancaire,
        @Schema(description = "Compromis / approbation du crédit (ComFolder.CreditApprovalDate).")
        LocalDateTime compromis,
        @Schema(description = "Attestation de consignation / octroi du crédit (ComFolder.CreditPossessionDate).")
        LocalDateTime attestation,
        @Schema(description = "Intervention (ComFolder.InterventionDate).")
        LocalDateTime intervention,
        @Schema(description = "Assurance (ComFolder.InsuranceDate).")
        LocalDateTime assurance,
        @Schema(description = "Remise des clés prévue : date de livraison de l'article, sinon début de finalisation de la tranche.")
        LocalDateTime remiseClesPrevue,
        @Schema(description = "Remise des clés effective (ComFolder.DeliveryDateReal).")
        LocalDateTime remiseClesEffective,
        @Schema(description = "Permis d'habiter, lu sur la tranche de l'article (CfgTranche.OccupancyPermitsDate).")
        LocalDateTime permisHabiter,
        @Schema(description = "Acte de précision (ComFolder.PrecisionActDate).")
        LocalDateTime actePrecision
) {
}
