package com.tsix.immocrm.dto.tresorerie;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.ModuleTresorerie;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Saisie d'un décaissement (règlement débit PRJ ou BUY), à la création comme à la modification.")
public record SaisieDecaissementDto(
        @Schema(description = "PRJ ou BUY ; requis à la création, ignoré à la modification.") ModuleTresorerie module,
        @Schema(description = "Entité juridique (CfgCompany.PKey), habilitée ; requise.") UUID societeId,
        @Schema(description = "Date pièce (DocDate) ; absente = jour.", example = "2026-04-01") LocalDate dateDocument,
        @Schema(description = "Date échéance (EcheanceDate), ≥ date pièce ; absente = date pièce.", example = "2026-04-30")
        LocalDate dateEcheance,
        @Schema(description = "Type de règlement (StlSettlementType.PKey) ; requis.") UUID typeId,
        @Schema(description = "Référence (Code) ; obligatoire ou unique selon le type.") String reference,
        @Schema(description = "Compte (StlAccount.PKey) de la société du règlement.") UUID compteId,
        @Schema(description = "Banque (CfgBankId).") UUID banqueId,
        @Schema(description = "Montant brut du règlement (AmountBase), > 0 ; le net est montantNet.", example = "800")
        BigDecimal montant,
        @Schema(description = "Fournisseur (TierId, CfgTier.PKey) ; requis.") UUID fournisseurId,
        @Schema(description = "Tiers payeur (TierPayerId) ; absent = fournisseur.") UUID payeurId,
        @Schema(description = "Notaire / avocat (CfgLawyerId) ; obligatoire selon le type.") UUID notaireId,
        @Schema(description = "Tiers autrui (CfgTierOtherId).") UUID tiersAutruiId,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Date de pointage (SettlementDate) ; exige un compte et le statut POINTED.")
        LocalDate dateDecaissement,
        @Schema(description = "IN_WORK, POINTED ou NOTPAYED ; CANCELED refusé (400).") StatutReglement statut,
        @Schema(description = "Code reçu (ReceiptCode).") String codeRecu,
        @Schema(description = "Sortie seule : code P.P (PropositionSheetCode), attribué par POST …/code-feuille-paiement.")
        String codeFeuillePaiement,
        @Schema(description = "Sortie seule : code C.R.A.S (RSSheetCode), attribué par POST …/code-feuille-rs.")
        String codeFeuilleRs,
        @Schema(description = "Affectations ; au PUT, remplacent les lignes visibles (les lignes masquées sont gardées).")
        List<LigneSaisieDecaissementDto> lignes,
        @Schema(description = "Retenues ; absent = lignes gardées (PUT) ou ligne du fournisseur (POST) ; [] = aucune.")
        List<LigneRetenueDto> retenues,
        @Schema(description = "RS déclarée (RSAmountSettled).") Boolean rsDeclaree,
        @Schema(description = "Date de déclaration de la RS (RSSettlementDate) ; mise à null si rsDeclaree est faux.")
        LocalDate dateDeclarationRs,
        @Schema(description = "Sortie seule : net (Amount) = montant − montantRs.", example = "720") BigDecimal montantNet,
        @Schema(description = "Sortie seule : total retenu (AmountRS) = Σ des lignes de retenue.", example = "80")
        BigDecimal montantRs,
        @Schema(description = "Sortie seule : montantRs / montant en fraction (RsRatio), 0 si montant nul.", example = "0.1")
        BigDecimal tauxRs) {

    public SaisieDecaissementDto {
        lignes = lignes == null ? List.of() : List.copyOf(lignes);
        retenues = retenues == null ? null : List.copyOf(retenues);
    }
}
