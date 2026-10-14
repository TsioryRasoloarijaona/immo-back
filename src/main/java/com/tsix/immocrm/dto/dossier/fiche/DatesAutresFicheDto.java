package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Onglet « Dates autres » : dates client, avocat, légalisation et enregistrement de chaque acte, CPF.")
public record DatesAutresFicheDto(
        @Schema(description = "Promesse de vente client (ComFolder.ReservationDateCustomer).")
        LocalDateTime promesseClient,
        @Schema(description = "Date d'enregistrement de la réservation (ComFolder.ReservationRegistryDate).")
        LocalDateTime promesseEnregistrement,
        @Schema(description = "Date d'impression du contrat de réservation / promesse (ComFolder.ContractPrintDate).")
        LocalDateTime impressionContrat,
        @Schema(description = "Minute / signature du contrat client (ComFolder.AgreementSavingDateCustomer).")
        LocalDateTime minuteClient,
        @Schema(description = "Minute / signature du contrat avocat (ComFolder.AgreementSavingDateLawyer).")
        LocalDateTime minuteAvocat,
        @Schema(description = "Remise du contrat pour légalisation (ComFolder.AgreementSavingDateForLegalization).")
        LocalDateTime minuteRemiseLegalisation,
        @Schema(description = "Légalisation du contrat (ComFolder.AgreementSavingDateLegalization).")
        LocalDateTime minuteLegalisation,
        @Schema(description = "Dépôt pour enregistrement du contrat (ComFolder.AgreementSavingDateForRegistry).")
        LocalDateTime minuteDepotEnregistrement,
        @Schema(description = "Remise des clés effective client (ComFolder.DeliveryDateRealCustomer).")
        LocalDateTime remiseClesClient,
        @Schema(description = "Date d'enregistrement de la remise des clés (ComFolder.DeliveryRegistryDate).")
        LocalDateTime remiseClesEnregistrement,
        @Schema(description = "Acte de précision client (ComFolder.PrecisionActDateCustomer).")
        LocalDateTime actePrecisionClient,
        @Schema(description = "Acte de précision avocat (ComFolder.PrecisionActDateLawyer).")
        LocalDateTime actePrecisionAvocat,
        @Schema(description = "Remise pour légalisation de l'acte de précision (ComFolder.PrecisionActDateForLegalization).")
        LocalDateTime actePrecisionRemiseLegalisation,
        @Schema(description = "Légalisation de l'acte de précision (ComFolder.PrecisionActDateLegalization).")
        LocalDateTime actePrecisionLegalisation,
        @Schema(description = "Dépôt pour enregistrement de l'acte de précision (ComFolder.PrecisionActDateForRegistry).")
        LocalDateTime actePrecisionDepotEnregistrement,
        @Schema(description = "Date d'enregistrement de l'acte de précision (ComFolder.PrecisionActRegistryDate).")
        LocalDateTime actePrecisionEnregistrement,
        @Schema(description = "Dépôt à la CPF (ComFolder.FinalTitleDateDeposit).")
        LocalDateTime cpfDepot,
        @Schema(description = "Retrait du dossier client de la CPF (ComFolder.FinalTitleDateWithdrawal).")
        LocalDateTime cpfRetrait,
        @Schema(description = "Remise du certificat de propriété au client (ComFolder.FinalTitleDate).")
        LocalDateTime remiseCertificatPropriete,
        @Schema(description = "N° de titre (ComFolder.FinalTitleNumber).")
        String numeroTitre
) {
}
