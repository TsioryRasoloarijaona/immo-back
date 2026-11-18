package com.tsix.immocrm.domain.tresorerie;

import com.tsix.immocrm.domain.rapport.TypeColonne;

import static com.tsix.immocrm.domain.rapport.TypeColonne.BOOLEEN;
import static com.tsix.immocrm.domain.rapport.TypeColonne.DATE;
import static com.tsix.immocrm.domain.rapport.TypeColonne.MONTANT;
import static com.tsix.immocrm.domain.rapport.TypeColonne.NOMBRE;
import static com.tsix.immocrm.domain.rapport.TypeColonne.TEXTE;

/**
 * Colonnes des grilles Recettes d'OMRAN : {@code StlDocumentView} (proforma, factures, avoirs : famille P) et
 * {@code StlSettlementView} (encaissements : famille E). La clé est l'alias SQL et le nom du champ sur le fil ;
 * {@code visibles} = familles où OMRAN l'affiche par défaut (sinon masquée). Voir FEATURE-n3-recettes-depenses.md.
 */
public enum ColonneRecette {

    DATE_AUTORISATION_PAIEMENT("dateAutorisationPaiement", "Date d'autorisation de paiement", DATE, "P", ""),
    NATURE("nature", "Nature", TEXTE, "P", ""),
    CODE("code", "Référence", TEXTE, "PE", "PE"),
    DESCRIPTION("description", "Description", TEXTE, "PE", ""),
    DATE_PIECE("datePiece", "Date", DATE, "PE", "PE"),
    ECHEANCE("echeance", "Échéance", DATE, "PE", "PE"),
    DATE_POINTAGE("datePointage", "Date de pointage", DATE, "E", ""),
    DATE_MOUVEMENT_ORIGINE("dateMouvementOrigine", "Date de mouvement d'origine", DATE, "E", ""),
    MOUVEMENT_REPORTE("mouvementReporte", "Mouvement reporté", BOOLEEN, "E", ""),
    DATE_MOUVEMENT("dateMouvement", "Date de mouvement", DATE, "E", "E"),
    MOIS_MOUVEMENT("moisMouvement", "Mois", NOMBRE, "E", ""),
    ANNEE_MOUVEMENT("anneeMouvement", "Année", NOMBRE, "E", ""),
    TYPE_CODE("typeCode", "Code type", TEXTE, "E", ""),
    TYPE("type", "Type", TEXTE, "E", "E"),
    NON_COMPTABILISABLE("nonComptabilisable", "Non comptabilisable", BOOLEEN, "E", ""),
    COMPTABILISE("comptabilise", "Comptabilisé", BOOLEEN, "PE", "E"),
    STATUT_CODE("statutCode", "Code statut", TEXTE, "E", ""),
    STATUT("statut", "Statut", TEXTE, "E", "E"),
    SOCIETE_CODE("societeCode", "Code société", TEXTE, "PE", ""),
    SOCIETE("societe", "Société", TEXTE, "PE", "PE"),
    DEVISE_CODE("deviseCode", "Devise", TEXTE, "PE", "E"),
    DEVISE("devise", "Libellé devise", TEXTE, "P", ""),
    MODULE_CODE("moduleCode", "Code module", TEXTE, "E", ""),
    MODULE("module", "Module", TEXTE, "PE", "PE"),
    TIERS_CODE("tiersCode", "Code tiers", TEXTE, "PE", ""),
    TIERS("tiers", "Tiers", TEXTE, "PE", "PE"),
    PERSONNE_A_CONTACTER("personneAContacter", "Personne à contacter", TEXTE, "P", "P"),
    REFERENCE_EXTERNE("referenceExterne", "Référence externe", TEXTE, "P", "P"),
    COMMANDE("commande", "Commande", TEXTE, "P", "P"),
    MONTANT_HT("montantHT", "Montant HT", MONTANT, "P", ""),
    MONTANT_TVA("montantTVA", "Montant TVA", MONTANT, "P", ""),
    MONTANT_TTC("montantTTC", "Montant TTC", MONTANT, "P", ""),
    AVOIR_HT("avoirHT", "Avoirs HT", MONTANT, "P", ""),
    AVOIR_TVA("avoirTVA", "Avoirs TVA", MONTANT, "P", ""),
    AVOIR_TTC("avoirTTC", "Avoirs TTC", MONTANT, "P", ""),
    TOTAL_HT("totalHT", "Total HT", MONTANT, "P", "P"),
    TOTAL_TVA("totalTVA", "Total TVA", MONTANT, "P", "P"),
    TOTAL_TTC("totalTTC", "Total TTC", MONTANT, "P", "P"),
    REGULARISATION("regularisation", "Régularisation", MONTANT, "P", ""),
    TIMBRE_FISCAL("timbreFiscal", "Timbre fiscal", MONTANT, "P", ""),
    MONTANT_A_PAYER("montantAPayer", "Net à payer", MONTANT, "P", "P"),
    MONTANT_REGLE("montantRegle", "Réglé", MONTANT, "P", "P"),
    RESTE_A_REGLER("resteARegler", "Reste", MONTANT, "P", "P"),
    MONTANT_BASE("montantBase", "Montant de base", MONTANT, "E", "E"),
    RESTE_A_AFFECTER("resteAAffecter", "Reste à affecter", MONTANT, "E", "E"),
    TAUX_RS("tauxRS", "Taux RS", NOMBRE, "E", ""),
    MONTANT_RS("montantRS", "Montant RS", MONTANT, "E", ""),
    TIMBRE("timbre", "Timbre", MONTANT, "E", "E"),
    MONTANT_REGLEMENT("montant", "Montant", MONTANT, "E", "E"),
    COMPTE_CODE("compteCode", "Code compte", TEXTE, "E", ""),
    COMPTE("compte", "Compte", TEXTE, "E", "E"),
    BANQUE_CODE("banqueCode", "Code banque", TEXTE, "E", ""),
    BANQUE("banque", "Banque", TEXTE, "E", "E"),
    DEPOT_CODE("depotCode", "Code dépôt", TEXTE, "E", ""),
    DEPOT("depot", "Dépôt", TEXTE, "E", "E"),
    DOCUMENT_REMIS("documentRemisAuCentre", "Document remis au centre", BOOLEEN, "E", ""),
    DATE_REMISE("dateRemiseAuCentre", "Date de remise au centre", DATE, "E", ""),
    BORDEREAU_CODE("bordereau", "Bordereau", TEXTE, "E", ""),
    BORDEREAU_DATE("dateBordereau", "Date du bordereau", DATE, "E", ""),
    CODE_RECU("codeRecu", "Code reçu", TEXTE, "E", ""),
    FICHE_PROPOSITION("ficheProposition", "Fiche de proposition", TEXTE, "E", ""),
    NOTES("notes", "Notes", TEXTE, "PE", "PE"),
    NOTES1("notes1", "Notes 2", TEXTE, "P", ""),
    // Tranche, article et document lié : dérivés des pièces de vente / location (StlDocumentCfgTrancheView), vides en PRJ / BUY.
    TRANCHE("tranche", "Tranche", TEXTE, "P", ""),
    ARTICLE_CODE("articleCode", "Code article", TEXTE, "P", ""),
    ARTICLE("article", "Article", TEXTE, "P", ""),
    AVOIRS_CODES("avoirs", "Avoirs", TEXTE, "P", ""),
    FACTURE_ORIGINE("factureOrigine", "Facture d'origine", TEXTE, "A", "P"),
    DOCUMENT_LIE("documentLie", "Document lié", TEXTE, "P", ""),
    CATEGORIE("categorie", "Catégorie", TEXTE, "P", ""),
    RS_REGLE("rsRegle", "RS réglée", BOOLEEN, "E", ""),
    DATE_REGLEMENT_RS("dateReglementRS", "Date de règlement RS", DATE, "E", ""),
    FICHE_RS("ficheRS", "Fiche RS", TEXTE, "E", ""),
    PAYEUR("payeur", "Payeur", TEXTE, "E", ""),
    AUTRE_TIERS("autreTiers", "Autre tiers", TEXTE, "E", ""),
    AVOCAT("avocat", "Avocat", TEXTE, "E", ""),
    DOSSIER_ACHAT_CODE("dossierAchatCode", "Code dossier", TEXTE, "PE", ""),
    DOSSIER_ACHAT("dossierAchat", "Dossier", TEXTE, "PE", ""),
    CREE_PAR("creePar", "Créé par", TEXTE, "PE", ""),
    CREE_LE("creeLe", "Créé le", DATE, "PE", ""),
    MODIFIE_PAR("modifiePar", "Modifié par", TEXTE, "PE", ""),
    MODIFIE_LE("modifieLe", "Modifié le", DATE, "PE", "");

    /** P = pièces de recette ({@code StlDocument}), E = encaissements ({@code StlSettlement}), A = avoirs seulement. */
    public enum Famille { PIECE, ENCAISSEMENT, AVOIR }

    private final String cle;
    private final String libelle;
    private final TypeColonne type;
    private final String familles;
    private final String visibles;

    ColonneRecette(String cle, String libelle, TypeColonne type, String familles, String visibles) {
        this.cle = cle;
        this.libelle = libelle;
        this.type = type;
        this.familles = familles;
        this.visibles = visibles;
    }

    public String cle() {
        return cle;
    }

    public String libelle() {
        return libelle;
    }

    public TypeColonne type() {
        return type;
    }

    public boolean de(Famille famille) {
        return familles.indexOf(famille.name().charAt(0)) >= 0;
    }

    public boolean visibleDans(Famille famille) {
        return visibles.indexOf(famille.name().charAt(0)) >= 0;
    }

    /** Calculée en Java ({@code ConversionReglement}, {@code SoldeDocumentAchat}, libellé du statut), pas lue en SQL. */
    public boolean calculee() {
        return this == RESTE_A_REGLER || this == RESTE_A_AFFECTER || this == STATUT;
    }
}
