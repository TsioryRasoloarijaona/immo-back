package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Données du « Reçu de règlement » d'un {@code StlSettlement}, hors JSON : page 1 du modèle
 * {@code instructions/templates/RECEIP/Yes_Stl_Front_Credit_Settlement_20240301102347.yes} (la page location est hors périmètre).
 */
public record RecuReglementData(
        String dossierCode,
        String receiptCode,
        /** {@code StlSettlement.isCredit} : true = remboursement (société→client), false = versement. */
        boolean remboursement,
        /** {@code StlSettlementLine.amount} de ce dossier, jamais {@code StlSettlement.amount} (qui peut couvrir d'autres dossiers). */
        BigDecimal montantRegleDossier,
        /** {@code StlSettlement.description} : c'est là que le légataire met le libellé du mode de paiement. */
        String modePaiement,
        /** Payeur du règlement ({@code PayeurReglement} : {@code TierPayerId}, puis {@code TierId}), à défaut le client. */
        Beneficiaire beneficiaire,
        /** {@code ComFolderCustomer}, cités après {@code beneficiaire} (« et de … »). */
        List<Beneficiaire> coAcquereurs,
        /** Entité sur laquelle le règlement a été saisi ({@code StlSettlement.cfgCompany}) ; jamais en dur. */
        String entrepriseNom,
        String entrepriseFormeJuridique,
        String entrepriseAdresse,
        String villeSignature,
        /** {@code ComFolder.docDate}. */
        LocalDate dateSignature,
        String articleCategorieLibelle,
        String articleCode,
        String projetDescription,
        String typeLibelle,
        BigDecimal superficie,
        /** {@code StkItem.titleNumber} vide : mention « approximative » et astérisque, comme la Fiche Trop Perçu. */
        boolean superficieApproximative,
        BigDecimal prixApresRemise,
        /** Tous les règlements du dossier, pas seulement celui de ce reçu. */
        List<LigneReglement> lignesReglement,
        /** Reste dû hors FRAIS, repris de {@code DossierService#montants}, jamais recalculé ici. */
        BigDecimal resteAPayer
) {
    public record Beneficiaire(String titre, String nom, String pieceIdentite) {
    }

    public record LigneReglement(LocalDate date, BigDecimal montant, boolean credit, String mode) {
    }
}
