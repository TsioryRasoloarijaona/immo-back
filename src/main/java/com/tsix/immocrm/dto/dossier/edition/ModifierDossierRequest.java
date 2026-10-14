package com.tsix.immocrm.dto.dossier.edition;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Modification partielle de la fiche : seuls les champs présents et non nuls changent ; vider = champsAVider.")
public record ModifierDossierRequest(
        @Schema(description = "Onglet Dates — concrétisation (ComFolder.AdvanceDate).")
        LocalDateTime concretisation,
        @Schema(description = "Onglet Dates — promesse de vente (ComFolder.ReservationDate).")
        LocalDateTime promesseVente,
        @Schema(description = "Onglet Dates — visite technique (ComFolder.ReceptionDate).")
        LocalDateTime visiteTechnique,
        @Schema(description = "Onglet Dates — envoi au notaire prévisionnel (ComFolder.AgreementDatePrevisionel).")
        LocalDateTime envoiNotairePrevisionnel,
        @Schema(description = "Onglet Dates — case « Confirmé » de l'envoi prévisionnel (ComFolder.IsConfirmedAgreementDate).")
        Boolean envoiNotaireConfirme,
        @Schema(description = "Onglet Dates — enregistrement du contrat (ComFolder.AgreementRegistryDate).")
        LocalDateTime enregistrementContrat,
        @Schema(description = "Onglet Dates — envoi au notaire / avocat (ComFolder.AgreementDate).")
        LocalDateTime envoiNotaire,
        @Schema(description = "Onglet Dates — minute (ComFolder.AgreementSavingDate).")
        LocalDateTime minute,
        @Schema(description = "Onglet Dates — expédition (ComFolder.OwnershipCertificateDate).")
        LocalDateTime expedition,
        @Schema(description = "Onglet Dates — relance 1 titre foncier (ComFolder.Revival1).")
        LocalDateTime relanceTitreFoncier1,
        @Schema(description = "Onglet Dates — échéance de la relance 1 (ComFolder.Revival1ValidityDate).")
        LocalDateTime echeanceRelance1,
        @Schema(description = "Onglet Dates — relance 2 titre foncier (ComFolder.Revival2).")
        LocalDateTime relanceTitreFoncier2,
        @Schema(description = "Onglet Dates — échéance de la relance 2 (ComFolder.Revival2ValidityDate).")
        LocalDateTime echeanceRelance2,
        @Schema(description = "Onglet Dates — fiche bancaire (ComFolder.BankRecordDate).")
        LocalDateTime ficheBancaire,
        @Schema(description = "Onglet Dates — compromis (ComFolder.CreditApprovalDate).")
        LocalDateTime compromis,
        @Schema(description = "Onglet Dates — attestation (ComFolder.CreditPossessionDate).")
        LocalDateTime attestation,
        @Schema(description = "Onglet Dates — intervention (ComFolder.InterventionDate).")
        LocalDateTime intervention,
        @Schema(description = "Onglet Dates — assurance (ComFolder.InsuranceDate).")
        LocalDateTime assurance,
        @Schema(description = "Onglet Dates — remise des clés effective (ComFolder.DeliveryDateReal).")
        LocalDateTime remiseClesEffective,
        @Schema(description = "Onglet Dates — acte de précision (ComFolder.PrecisionActDate).")
        LocalDateTime actePrecision,

        @Schema(description = "Dates autres — promesse de vente client (ComFolder.ReservationDateCustomer).")
        LocalDateTime promesseClient,
        @Schema(description = "Dates autres — enregistrement de la réservation (ComFolder.ReservationRegistryDate).")
        LocalDateTime promesseEnregistrement,
        @Schema(description = "Dates autres — impression du contrat (ComFolder.ContractPrintDate).")
        LocalDateTime impressionContrat,
        @Schema(description = "Dates autres — minute client (ComFolder.AgreementSavingDateCustomer).")
        LocalDateTime minuteClient,
        @Schema(description = "Dates autres — minute avocat (ComFolder.AgreementSavingDateLawyer).")
        LocalDateTime minuteAvocat,
        @Schema(description = "Dates autres — remise pour légalisation du contrat (AgreementSavingDateForLegalization).")
        LocalDateTime minuteRemiseLegalisation,
        @Schema(description = "Dates autres — légalisation du contrat (ComFolder.AgreementSavingDateLegalization).")
        LocalDateTime minuteLegalisation,
        @Schema(description = "Dates autres — dépôt pour enregistrement du contrat (AgreementSavingDateForRegistry).")
        LocalDateTime minuteDepotEnregistrement,
        @Schema(description = "Dates autres — remise des clés client (ComFolder.DeliveryDateRealCustomer).")
        LocalDateTime remiseClesClient,
        @Schema(description = "Dates autres — enregistrement de la remise des clés (ComFolder.DeliveryRegistryDate).")
        LocalDateTime remiseClesEnregistrement,
        @Schema(description = "Dates autres — acte de précision client (ComFolder.PrecisionActDateCustomer).")
        LocalDateTime actePrecisionClient,
        @Schema(description = "Dates autres — acte de précision avocat (ComFolder.PrecisionActDateLawyer).")
        LocalDateTime actePrecisionAvocat,
        @Schema(description = "Dates autres — remise pour légalisation de l'acte (PrecisionActDateForLegalization).")
        LocalDateTime actePrecisionRemiseLegalisation,
        @Schema(description = "Dates autres — légalisation de l'acte de précision (PrecisionActDateLegalization).")
        LocalDateTime actePrecisionLegalisation,
        @Schema(description = "Dates autres — dépôt pour enregistrement de l'acte (PrecisionActDateForRegistry).")
        LocalDateTime actePrecisionDepotEnregistrement,
        @Schema(description = "Dates autres — enregistrement de l'acte de précision (PrecisionActRegistryDate).")
        LocalDateTime actePrecisionEnregistrement,
        @Schema(description = "Dates autres — dépôt à la CPF (ComFolder.FinalTitleDateDeposit).")
        LocalDateTime cpfDepot,
        @Schema(description = "Dates autres — retrait de la CPF (ComFolder.FinalTitleDateWithdrawal).")
        LocalDateTime cpfRetrait,
        @Schema(description = "Dates autres — remise du certificat de propriété (ComFolder.FinalTitleDate).")
        LocalDateTime remiseCertificatPropriete,
        @Schema(description = "Dates autres — n° de titre, 255 caractères au plus (ComFolder.FinalTitleNumber).")
        String numeroTitre,

        @Schema(description = "Annulation — date d'annulation / résiliation (ComFolder.CancelDate).")
        LocalDateTime annulationDate,
        @Schema(description = "Annulation — causes, 1 000 caractères au plus (ComFolder.CancelDescription).")
        String annulationCauses,

        @Schema(description = "Client principal (CfgTier) : devient l'acquéreur principal du dossier.")
        UUID clientId,
        @Schema(description = "Commercial du dossier (CfgTier employé, ComFolder.CfgCommercialId).")
        UUID commercialId,
        @Schema(description = "Gestionnaire 1 (CfgTier, ComFolder.CfgSaleManagerId).")
        UUID gestionnaire1Id,
        @Schema(description = "Gestionnaire 2 (CfgTier, ComFolder.CfgSaleManager2Id).")
        UUID gestionnaire2Id,
        @Schema(description = "Notaire / avocat (CfgTier, ComFolder.CfgLawyerId).")
        UUID notaireId,

        @Schema(description = "Banque du crédit (CfgTier, ComFolder.CreditBankId).")
        UUID banqueCreditId,
        @Schema(description = "Agence de crédit, texte libre de 255 caractères au plus (ComFolder.CreditAgency).")
        String agenceCredit,
        @Schema(description = "Date d'envoi MEILLEURTAUX (ComFolder.loanCompanySendDate).")
        LocalDateTime meilleurTauxEnvoi,
        @Schema(description = "Décision MEILLEURTAUX (ComLoanCompanyDecision.PKey), voir GET /api/dossiers/decisions-meilleurtaux.")
        UUID meilleurTauxDecisionId,
        @Schema(description = "Date de la décision MEILLEURTAUX (ComFolder.loanCompanyDecisionDate).")
        LocalDateTime meilleurTauxDecisionDate,

        @Schema(description = "« C'est un ancien client » (ComFolder.IsOldCustomer).")
        Boolean ancienClient,
        @Schema(description = "« Client veut choisir le cadeau plus tard » (ComFolder.IsWithGiftChoiceLater).")
        Boolean cadeauPlusTard,
        @Schema(description = "Intégrer les modifications au prix de vente (ComFolder.PriceWithModification) ; recalcule le prix.")
        Boolean integrerModificationsAuPrix,
        @Schema(description = "« Pénalité désactivée » (ComFolder.IsPenaltyDisabled) ; Administrateur seulement.")
        Boolean penalitesDesactivees,

        @Schema(description = "Montant de la remise HT ; exclusif de remisePourcentage. Le taux est recalculé.")
        BigDecimal remiseMontantHT,
        @Schema(description = "Remise en pourcentage 0–100 du total HT avant remise ; exclusif de remiseMontantHT.")
        BigDecimal remisePourcentage,
        @Schema(description = "Taux de TVA de l'article en ratio 0–1 (0,2 = 20 %) ; les prix TTC sont recalculés.")
        BigDecimal tva,
        @Schema(description = "Vrai pour dépasser les plafonds de remise de l'article (droit toolOverDiscount).")
        Boolean forcerRemise,

        @Schema(description = "Note du dossier (ComFolder.Notes, 1 000 caractères au plus) ; enregistrée aussi en action Note.")
        String note,

        @Schema(description = "Noms des champs à remettre à null (ex. [\"minute\"]) ; clientId ne peut pas être vidé.")
        List<String> champsAVider
) {
}
