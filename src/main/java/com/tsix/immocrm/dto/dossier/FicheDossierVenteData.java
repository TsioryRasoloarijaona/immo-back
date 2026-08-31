package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * All fields needed to render the "Fiche Dossier de Vente" document (PDF), reproducing the legacy
 * "FICHE COMPLETE" Stimulsoft template ({@code ComSaleFiche.yes}, bound to {@code ComSaleFolderView})
 * — a comprehensive internal dossier summary/audit sheet (entête, montants fixes, tous les jalons
 * de dates, annulation/résiliation, autres informations, schéma de financement détaillé,
 * sous-articles, historique). See {@code FicheDossierVenteService} for how each field is sourced,
 * and {@code FicheDossierVentePdfRenderer} for the actual layout.
 *
 * <p>Two legacy fields have no equivalent in this app's data model and are deliberately left
 * unmapped rather than guessed: "Présentation" ({@code StkPresentationType}, no lookup table
 * mapped anywhere in this app) and "Décision MEILLEURTAUX" ({@code ComLoanCompanyDecision}, same
 * situation — only its two dates are mapped). The HISTORIQUE table also reuses the app-wide
 * {@code GET /api/dossiers/{id}/historique} shape (Date/Type/Utilisateur/Description) rather than
 * the legacy's Utilisateur/Date/Etat/Client/Article/Détail columns, since there is no per-event
 * Client/Article tracking in this app's unified historique (a deliberate design choice, see
 * {@code DossierHistoriqueService}) and duplicating that public endpoint's contract just for this
 * PDF was judged out of scope.
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
     * MONTANTS box — 9 lignes fixes de l'original (contrairement au reste de l'app, qui calcule
     * les montants dynamiquement par catégorie de règlement libre texte). Frais/Modification/
     * Avance/Crédit/Auto-financement/Pénalité/Résiliation sont recalculés à partir de
     * l'échéancier via les booléens dédiés confirmés en base sur {@code ComSettlementCategory}
     * (IsSaleExpense/IsSaleModification/IsSaleAdvance/IsSaleCredit/IsSaleAutoFinancment/
     * IsSalePenality/IsSaleResiliation) — montant/réglé/reste tous réels, jamais stockés. Seul
     * "Prix de vente" vient du snapshot {@code ComFolder} (PriceAfterDiscount), faute de
     * catégorie d'échéancier dédiée — ce n'est pas un règlement en soi mais le prix affiché.
     * Net à payer = Frais + Modification + Prix de vente + Pénalité + Résiliation
     * (Avance/Crédit/Auto-financement ne sont qu'une ventilation informative du Prix de vente,
     * jamais additionnés dedans — vérifié sur l'exemple réel du template : 900 000 de Prix de
     * vente = 900 000 d'Auto-financement, pas 1 800 000).
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
