package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Les 8 dates d'appel d'offre de l'onglet Modalités / dates ; chacune facultative.")
public record DatesDemandeAchat(
        @Schema(description = "Date de lancement (LaunchOfTenderDate).") LocalDate lancement,
        @Schema(description = "Retrait du dossier de sélection (WithdrawalTenderDocumentsDate).") LocalDate retraitDossier,
        @Schema(description = "Échéance de remise des offres (TenderMaturityDate).") LocalDate echeanceOffres,
        @Schema(description = "Date de report (TendersResortDate).") LocalDate report,
        @Schema(description = "Ouverture des plis (BidOpeningDate).") LocalDate ouverturePlis,
        @Schema(description = "Rapport de dépouillement (TendersReportDate).") LocalDate rapportDepouillement,
        @Schema(description = "Adjudication (TenderDate).") LocalDate adjudication,
        @Schema(description = "Signature du marché (ContractSignatureDate).") LocalDate signatureMarche) {
}
