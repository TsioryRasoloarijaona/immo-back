package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Onglet « Autres » : gestionnaires, crédit, notaire, note, cases à cocher et MEILLEURTAUX.")
public record AutresFicheDto(
        @Schema(description = "Gestionnaire 1 (ComFolder.CfgSaleManagerId).")
        UUID gestionnaire1Id,
        @Schema(description = "Nom du gestionnaire 1.")
        String gestionnaire1Nom,
        @Schema(description = "Gestionnaire 2 (ComFolder.CfgSaleManager2Id).")
        UUID gestionnaire2Id,
        @Schema(description = "Nom du gestionnaire 2.")
        String gestionnaire2Nom,
        @Schema(description = "Banque du crédit ou du leasing (ComFolder.CreditBankId).")
        UUID banqueCreditId,
        @Schema(description = "Nom de la banque du crédit.")
        String banqueCreditNom,
        @Schema(description = "Agence de crédit, texte libre (ComFolder.CreditAgency).")
        String agenceCredit,
        @Schema(description = "Notaire / avocat (ComFolder.CfgLawyerId).")
        UUID notaireId,
        @Schema(description = "Nom du notaire / avocat.")
        String notaireNom,
        @Schema(description = "Note libre du dossier (ComFolder.Notes).")
        String note,
        @Schema(description = "« C'est un ancien client » : applique les remises anciens clients (ComFolder.IsOldCustomer).")
        Boolean ancienClient,
        @Schema(description = "« Client veut choisir le cadeau plus tard » (ComFolder.IsWithGiftChoiceLater).")
        Boolean cadeauPlusTard,
        @Schema(description = "« Intégrer le montant des modifications dans le calcul du prix de vente » (PriceWithModification).")
        Boolean integrerModificationsAuPrix,
        @Schema(description = "Date d'envoi MEILLEURTAUX (ComFolder.loanCompanySendDate).")
        LocalDateTime meilleurTauxEnvoi,
        @Schema(description = "Décision MEILLEURTAUX (libellé de ComLoanCompanyDecision), null si non décidée.")
        String meilleurTauxDecision,
        @Schema(description = "Date de la décision MEILLEURTAUX (ComFolder.loanCompanyDecisionDate).")
        LocalDateTime meilleurTauxDecisionDate,
        @Schema(description = "« Pénalité désactivée » (ComFolder.IsPenaltyDisabled).")
        Boolean penalitesDesactivees,
        @Schema(description = "Décision MEILLEURTAUX choisie (ComFolder.ComLoanCompanyDecisionId), null si non décidée.")
        UUID meilleurTauxDecisionId,
        @Schema(description = "Commercial du dossier (ComFolder.CfgCommercialId), null si aucun.")
        UUID commercialId
) {
}
