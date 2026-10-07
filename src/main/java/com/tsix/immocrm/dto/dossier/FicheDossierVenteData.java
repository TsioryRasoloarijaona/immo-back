package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Données de la « Fiche Dossier de Vente » (modèle « FICHE COMPLETE », {@code ComSaleFiche.yes}), hors JSON.
 * « Présentation » et « Décision MEILLEURTAUX » ne sont pas reprises ; l'historique a la forme de
 * {@code GET /api/dossiers/{id}/historique}, pas les colonnes Client/Article du légataire.
 */
public record FicheDossierVenteData(
        String dossierCode,
        String statut,
        String categorieLibelle,
        String typeLibelle,
        String articleCode,
        BigDecimal superficie,
        String clientNom,
        String commercialNom,
        String entrepriseNom,
        String projetNom,
        String trancheDescription,
        String titreFoncier,
        String prospectionCode,
        LocalDateTime dateDocument,
        BigDecimal partPercent,
        LocalDateTime validite,

        // -- ENTETE : prix (ComFolder, snapshot au moment de la vente) --
        BigDecimal prixDuM2,
        BigDecimal prixArticle,
        BigDecimal sousArticlesPrix,
        BigDecimal totalAvantRemise,
        BigDecimal remiseRatio,
        BigDecimal remiseMontant,
        BigDecimal prixDeVente,
        BigDecimal prixContractuel,

        // -- MONTANTS (rangées fixes, cf. ComSaleFolderView) --
        MontantsFiche montants,

        // -- DATE (jalons du dossier) --
        LocalDateTime dateReservation,
        LocalDateTime dateCompromis,
        LocalDateTime dateEnregistrementContrat,
        LocalDateTime dateVisiteTechnique,
        LocalDateTime dateReceptionEffective,
        LocalDateTime dateFicheBancaire,
        LocalDateTime dateApprobationCredit,
        LocalDateTime dateOctroiCredit,
        LocalDateTime dateIntervention,
        LocalDateTime dateAssurance,
        LocalDateTime dateCertificatPropriete,
        LocalDateTime dateLivraisonPrevue,
        LocalDateTime dateLivraisonEffective,
        LocalDateTime dateAnnulation,
        String motifAnnulation,

        // -- AUTRES --
        BigDecimal tvaRatio,
        Boolean avecCredit,
        String notaireAvocatNom,
        String banqueLeasingNom,
        String agence,
        Boolean ancienClient,
        Boolean cadeauChoisirPlusTard,
        Boolean integrerModifications,
        LocalDateTime dateEnvoiMeilleurtaux,
        LocalDateTime dateDecisionMeilleurtaux,

        // -- SCHEMA DE FINANCEMENT (échéancier persisté, avec détail des règlements) --
        List<LigneEcheance> echeancier,

        // -- SOUS ARTICLES --
        List<SousArticleDossier> sousArticles,

        // -- ACQUEREURS --
        List<Acquereur> acquereurs,

        // -- HISTORIQUE (réutilise DossierHistoriqueService#findHistorique) --
        List<HistoriqueEvenementDto> historique,

        String notes
) {
    /**
     * Lignes recalculées depuis l'échéancier par les booléens {@code IsSale*} de {@code ComSettlementCategory},
     * sauf « Prix de vente » ({@code ComFolder.PriceAfterDiscount}). Net à payer = Frais + Modification + Prix
     * de vente + Pénalité + Résiliation : Avance/Crédit/Auto-financement ventilent le prix, jamais additionnés.
     */
    public record MontantsFiche(
            BigDecimal montantFrais, BigDecimal regleFrais,
            BigDecimal montantModification, BigDecimal regleModification,
            BigDecimal montantPrixVente, BigDecimal reglePrixVente,
            BigDecimal ratioAvance, BigDecimal montantAvance, BigDecimal regleAvance,
            BigDecimal montantCredit, BigDecimal regleCredit,
            BigDecimal montantAutoFinancement, BigDecimal regleAutoFinancement,
            BigDecimal montantPenalite, BigDecimal reglePenalite,
            BigDecimal montantResiliation, BigDecimal regleResiliation,
            BigDecimal montantNetAPayer, BigDecimal regleNetAPayer
    ) {
        public BigDecimal resteFrais() { return sub(montantFrais, regleFrais); }
        public BigDecimal resteModification() { return sub(montantModification, regleModification); }
        public BigDecimal restePrixVente() { return sub(montantPrixVente, reglePrixVente); }
        public BigDecimal resteAvance() { return sub(montantAvance, regleAvance); }
        public BigDecimal resteCredit() { return sub(montantCredit, regleCredit); }
        public BigDecimal resteAutoFinancement() { return sub(montantAutoFinancement, regleAutoFinancement); }
        public BigDecimal restePenalite() { return sub(montantPenalite, reglePenalite); }
        public BigDecimal resteResiliation() { return sub(montantResiliation, regleResiliation); }
        public BigDecimal resteNetAPayer() { return sub(montantNetAPayer, regleNetAPayer); }

        private static BigDecimal sub(BigDecimal montant, BigDecimal regle) {
            BigDecimal m = montant != null ? montant : BigDecimal.ZERO;
            BigDecimal r = regle != null ? regle : BigDecimal.ZERO;
            return m.subtract(r);
        }
    }

    /** Une ligne de l'échéancier, avec le détail des règlements qui l'ont soldée (sous-lignes). */
    public record LigneEcheance(LocalDateTime date, String categorie, BigDecimal montant, BigDecimal regle,
                                 BigDecimal reste, String notes, List<DetailReglement> reglements) {
    }

    /** Une sous-ligne "CHÈQUE ... N° ... du ..." sous une ligne d'échéancier. */
    public record DetailReglement(String libelle, BigDecimal montant) {
    }

    public record SousArticleDossier(String code, String categorieLibelle, BigDecimal superficie,
                                      BigDecimal prix, BigDecimal remiseRatio, BigDecimal prixDeVente,
                                      Boolean toujoursAffecte) {
    }

    public record Acquereur(String nom, BigDecimal partPercent) {
    }
}
