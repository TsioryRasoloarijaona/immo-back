package com.tsix.immocrm.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Catalogue des colonnes de {@code GET /api/suivi-affectations}, sous les noms de {@code LigneSuiviAffectationsDto}
 * (FEATURE-suivi-affectations.md §5). Les chemins sont ceux de {@code StlSettlementLineSuivi}.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ColonneSuiviAffectations implements Colonne {

    ID("id", "Identifiant", Groupe.AFFECTATION, TypeColonne.TEXTE, true, null),
    CATEGORIE_ID("categorieId", "Id catégorie", Groupe.AFFECTATION, TypeColonne.TEXTE, false, null),
    CATEGORIE_CODE("categorieCode", "Code catégorie", Groupe.AFFECTATION, TypeColonne.TEXTE, false,
            Chemin.groupable("categorieCode")),
    CATEGORIE_LIBELLE("categorieLibelle", "Catégorie", Groupe.AFFECTATION, TypeColonne.TEXTE, true,
            Chemin.groupable("categorieLibelle")),
    ENTITE_ID("entiteId", "Id entité", Groupe.PATRIMOINE, TypeColonne.TEXTE, false, null),
    ENTITE_CODE("entiteCode", "Code entité", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("entiteCode")),
    ENTITE_LIBELLE("entiteLibelle", "Entité", Groupe.PATRIMOINE, TypeColonne.TEXTE, true,
            Chemin.groupable("entiteLibelle")),
    PROJET_ID("projetId", "Id programme", Groupe.PATRIMOINE, TypeColonne.TEXTE, false, null),
    PROJET_LIBELLE("projetLibelle", "Programme", Groupe.PATRIMOINE, TypeColonne.TEXTE, true,
            Chemin.groupable("projetLibelle")),
    TRANCHE_ID("trancheId", "Id tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false, null),
    TRANCHE_CODE("trancheCode", "Tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, true,
            Chemin.groupable("trancheCode")),
    TRANCHE_LIBELLE("trancheLibelle", "Libellé tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("trancheLibelle")),
    ARTICLE_CODE("articleCode", "Article", Groupe.PATRIMOINE, TypeColonne.TEXTE, true,
            Chemin.groupable("articleCode")),
    TIERS_ID("tiersId", "Id tiers", Groupe.PIECE, TypeColonne.TEXTE, false, null),
    TIERS_NOM("tiersNom", "Tiers", Groupe.PIECE, TypeColonne.TEXTE, true, Chemin.tier("tiers")),
    MONTANT_AFFECTE("montantAffecte", "Montant affecté", Groupe.AFFECTATION, TypeColonne.MONTANT, true,
            Chemin.agregeable("montantAffecte").sauf("etatPiece", StatutReglement.CANCELED.name())),
    SENS("sens", "Sens", Groupe.AFFECTATION, TypeColonne.TEXTE, false, Chemin.groupable("sens")),
    DATE_MOUVEMENT("dateMouvement", "Date", Groupe.AFFECTATION, TypeColonne.DATE, true,
            Chemin.date("dateMouvement")),
    DOSSIER_ID("dossierId", "Id dossier", Groupe.VENTE, TypeColonne.TEXTE, false, null),
    DOSSIER_CODE("dossierCode", "Dossier de vente", Groupe.VENTE, TypeColonne.TEXTE, true,
            Chemin.groupable("dossierCode")),
    STATUT_DOSSIER_CODE("statutDossierCode", "Code état dossier", Groupe.VENTE, TypeColonne.TEXTE, false,
            Chemin.groupable("statutDossierCode")),
    STATUT_DOSSIER_LIBELLE("statutDossierLibelle", "État dossier de vente", Groupe.VENTE, TypeColonne.TEXTE, true,
            Chemin.groupable("statutDossierLibelle")),
    PIECE_ID("pieceId", "Id pièce", Groupe.PIECE, TypeColonne.TEXTE, false, null),
    PIECE_OUVRABLE("pieceOuvrable", "Pièce ouvrable", Groupe.PIECE, TypeColonne.BOOLEEN, false, null),
    PIECE_LIBELLE("pieceLibelle", "Pièce de règlement", Groupe.PIECE, TypeColonne.TEXTE, true,
            Chemin.groupable("pieceLibelle")),
    PIECE_REFERENCE("pieceReference", "Référence pièce", Groupe.PIECE, TypeColonne.TEXTE, false,
            Chemin.groupable("pieceReference")),
    RECU_REFERENCE("recuReference", "N° de reçu", Groupe.PIECE, TypeColonne.TEXTE, false,
            Chemin.groupable("recuReference")),
    DATE_PIECE("datePiece", "Date pièce", Groupe.PIECE, TypeColonne.DATE, false, Chemin.date("datePiece")),
    DATE_ECHEANCE_PIECE("dateEcheancePiece", "Échéance pièce", Groupe.PIECE, TypeColonne.DATE, false,
            Chemin.date("dateEcheancePiece")),
    DATE_OPERATION_PIECE("dateOperationPiece", "Date d'opération", Groupe.PIECE, TypeColonne.DATE, false,
            Chemin.date("dateOperationPiece")),
    ETAT_PIECE("etatPiece", "État pièce", Groupe.PIECE, TypeColonne.TEXTE, false, Chemin.groupable("etatPiece")),
    MONTANT_PIECE("montantPiece", "Montant pièce", Groupe.PIECE, TypeColonne.MONTANT, false, null),
    TYPE_PIECE_ID("typePieceId", "Id type de pièce", Groupe.PIECE, TypeColonne.TEXTE, false, null),
    TYPE_PIECE_CODE("typePieceCode", "Code type de pièce", Groupe.PIECE, TypeColonne.TEXTE, false,
            Chemin.groupable("typePieceCode")),
    TYPE_PIECE_LIBELLE("typePieceLibelle", "Type de pièce", Groupe.PIECE, TypeColonne.TEXTE, false,
            Chemin.groupable("typePieceLibelle")),
    BANQUE_NOM("banqueNom", "Banque", Groupe.PIECE, TypeColonne.TEXTE, true, Chemin.tier("banque")),
    COMPTE_CODE("compteCode", "Compte", Groupe.PIECE, TypeColonne.TEXTE, false, Chemin.groupable("compteCode")),
    COMPTE_LIBELLE("compteLibelle", "Libellé compte", Groupe.PIECE, TypeColonne.TEXTE, false,
            Chemin.groupable("compteLibelle")),
    NOTES_PIECE("notesPiece", "Notes pièce de règlement", Groupe.PIECE, TypeColonne.TEXTE, true, null),
    DATE_ECHEANCE("dateEcheance", "Date échéance", Groupe.AFFECTATION, TypeColonne.DATE, false,
            Chemin.date("dateEcheance")),
    MONTANT_ECHEANCE("montantEcheance", "Montant échéance", Groupe.AFFECTATION, TypeColonne.MONTANT, false,
            Chemin.agregeable("montantEcheance")),
    NOTES_ECHEANCE("notesEcheance", "Notes document", Groupe.AFFECTATION, TypeColonne.TEXTE, true, null);

    public enum Groupe { AFFECTATION, PATRIMOINE, VENTE, PIECE }

    public enum TypeColonne { TEXTE, MONTANT, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson de {@code LigneSuiviAffectationsDto}. */
    public static final String FILTRE = "colonnesSuiviAffectations";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;
    private final Chemin chemin;

    @Override
    public TypeExport typeExport() {
        return TypeExport.valueOf(type.name());
    }
}
