package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * All fields needed to render the "Reçu de règlement" (sale settlement receipt) — modeled on the
 * "Page1" of the legacy Stimulsoft report source
 * {@code instructions/templates/RECEIP/Yes_Stl_Front_Credit_Settlement_20240301102347.yes}
 * (the other page of that file, RntDocumentView-based rental receipts, is out of scope). An
 * "ATTESTATION DE RÉCEPTION CHEQUE" acknowledging one specific règlement ({@code StlSettlement}),
 * followed by the item designation and the dossier's full "situation des chèques reçus". Not
 * exposed as JSON — feeds {@code RecuReglementPdfRenderer} only.
 */
public record RecuReglementData(
        String dossierCode,
        String receiptCode,
        /** {@code StlSettlement.isCredit} — true = remboursement (société→client), false = versement (client→société). */
        boolean remboursement,
        /** {@code StlSettlementLine.amount} spécifique à ce dossier pour ce règlement — jamais {@code StlSettlement.amount} (peut couvrir d'autres dossiers). */
        BigDecimal montantRegleDossier,
        /** {@code StlSettlement.description} — libellé du mode de paiement (ex. "Chèque", "Virement bancaire"). */
        String modePaiement,
        /** {@code StlSettlement.tier}, avec repli sur {@code ComFolder.cfgCustomer} — la personne "Nous soussignés ... reçu de/reconnaissons avoir reçu". */
        Beneficiaire beneficiaire,
        /** Co-acquéreurs du dossier ({@code ComFolderCustomer}), ajoutés après {@code beneficiaire} dans la phrase ("et de ..."). */
        List<Beneficiaire> coAcquereurs,
        /** {@code StlSettlement.cfgCompany.description} — jamais codé en dur, propre à l'entité juridique sur laquelle le règlement a été saisi. */
        String entrepriseNom,
        String entrepriseFormeJuridique,
        String entrepriseAdresse,
        String villeSignature,
        /** {@code ComFolder.docDate} — date de signature en pied de document. */
        LocalDate dateSignature,
        String articleCategorieLibelle,
        String articleCode,
        String projetDescription,
        String typeLibelle,
        BigDecimal superficie,
        /** true quand {@code StkItem.titleNumber} est vide — déclenche la mention "approximative" et l'astérisque, comme la Fiche Trop Perçu. */
        boolean superficieApproximative,
        BigDecimal prixApresRemise,
        /** "Situation des chèques reçus" — tous les règlements du dossier, pas seulement celui de ce reçu. */
        List<LigneReglement> lignesReglement,
        /** Reste dû du dossier (hors FRAIS), réutilise {@code DossierService#montants} — jamais re-dérivé. */
        BigDecimal resteAPayer
) {
    public record Beneficiaire(String titre, String nom, String pieceIdentite) {
    }

    public record LigneReglement(LocalDate date, BigDecimal montant, boolean credit, String mode) {
    }
}
