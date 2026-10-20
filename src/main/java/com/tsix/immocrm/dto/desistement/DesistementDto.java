package com.tsix.immocrm.dto.desistement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Désistement d'un dossier de vente (`ComSaleWithdrawalCancellation`), ligne de liste et fiche.")
public record DesistementDto(
        @Schema(description = "Identifiant (`PKey`).")
        UUID id,
        @Schema(description = "N° de demande (`Code`, séquence « Désistement »).", example = "26/00001")
        String code,
        @Schema(description = "État : BROUILLON (non validé), VALIDE (date de validation posée), LEGALISE (date de légalisation posée).",
                allowableValues = {"BROUILLON", "VALIDE", "LEGALISE"})
        String etat,
        @Schema(description = "Dossier de vente (`ComSaleId`).")
        UUID dossierId,
        @Schema(description = "Code du dossier.")
        String dossierCode,
        @Schema(description = "Client du dossier au moment de la demande (`CfgOldCustomerId`).")
        UUID clientId,
        @Schema(description = "Nom du client.")
        String clientNom,
        @Schema(description = "Tranche de l'article.")
        UUID trancheId,
        @Schema(description = "Libellé de la tranche.")
        String trancheLibelle,
        @Schema(description = "Article du dossier (`StkItemId`).")
        UUID articleId,
        @Schema(description = "Code de l'article.")
        String articleCode,
        @Schema(description = "Date de la demande (`ClaimDate`).")
        LocalDateTime dateDemande,
        @Schema(description = "Date de décision (`DecisionDate`), null si non saisie.")
        LocalDateTime dateDecision,
        @Schema(description = "Date de validation (`ValidityDate`), null tant que non validé.")
        LocalDateTime dateValidation,
        @Schema(description = "Date de légalisation (`LegalizationDate`), null tant que non légalisé.")
        LocalDateTime dateLegalisation,
        @Schema(description = "Motif (`ComSaleWithdrawalReasonId`).")
        UUID motifId,
        @Schema(description = "Libellé du motif.")
        String motifLibelle,
        @Schema(description = "Observations (`Notes`).")
        String observations,
        @Schema(description = "Prix de vente (`TotalToPay`).")
        BigDecimal prixVente,
        @Schema(description = "Total réglé (`TotalSettled`) : avance, crédit, autofinancement.")
        BigDecimal totalRegle,
        @Schema(description = "Reste du prix (`TotalRest`) = prix − total réglé.")
        BigDecimal resteARegler,
        @Schema(description = "Taux de pénalité en fraction (`TransfertExpenseAmount`, un taux malgré son nom).")
        BigDecimal taux,
        @Schema(description = "Remise sur la pénalité en fraction (`Discount`).")
        BigDecimal remise,
        @Schema(description = "Pénalité après remise (`TransfertExpenseAmountAfterDiscount`).")
        BigDecimal penaliteApresRemise,
        @Schema(description = "Reste frais à régler (`NewStkItemTotalToPay`).")
        BigDecimal resteFraisARegler,
        @Schema(description = "À rembourser (`ReimburseAmount`), figé à la validation.")
        BigDecimal aRembourser,
        @Schema(description = "Reste à rembourser : à rembourser − décaissements déjà enregistrés (0 avant validation).")
        BigDecimal resteARembourser,
        @Schema(description = "Type de pièce du remboursement (`StlSettlementTypeId`).")
        UUID typeReglementId,
        @Schema(description = "Libellé du type de pièce.")
        String typeReglementLibelle,
        @Schema(description = "Référence du paiement (`Reference`).")
        String reference,
        @Schema(description = "Banque (`CfgBankId`).")
        UUID banqueId,
        @Schema(description = "Nom de la banque.")
        String banqueNom,
        @Schema(description = "Contact à rembourser (`CfgContactId`).")
        UUID contactId,
        @Schema(description = "Nom du contact.")
        String contactNom
) {
}
