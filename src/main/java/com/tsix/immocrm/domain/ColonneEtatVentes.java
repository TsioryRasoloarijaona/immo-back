package com.tsix.immocrm.domain;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Catalogue des colonnes de {@code GET /api/etat-des-ventes}, sous les noms de {@code LigneEtatVentesDto}
 * (FEATURE-etat-des-ventes.md §3.2, FEATURE-etat-des-ventes-parite.md §3). Le chemin sert au groupement, aux agrégats
 * et au tri ; sans chemin, ni groupable ni triable. {@code source} : montant du dossier calculé en Java par la liste des
 * dossiers, jamais refait ici.
 */
@Getter
@Accessors(fluent = true)
public enum ColonneEtatVentes implements Colonne {

    ARTICLE_ID("articleId", "Identifiant", Groupe.ARTICLE, TypeColonne.TEXTE, null),
    ARTICLE_CODE("articleCode", "Article", Groupe.ARTICLE, TypeColonne.TEXTE, Chemin.groupable("articleCode")),
    ARTICLE_LIBELLE("articleLibelle", "Libellé article", Groupe.ARTICLE, TypeColonne.TEXTE,
            Chemin.groupable("articleLibelle")),
    TRANCHE_CODE("trancheCode", "Tranche", Groupe.ARTICLE, TypeColonne.TEXTE, Chemin.groupable("tranche.code")),
    CATEGORIE_CODE("categorieCode", "Cat.", Groupe.ARTICLE, TypeColonne.TEXTE, Chemin.groupable("categorie.code")),
    CATEGORIE_LIBELLE("categorieLibelle", "Catégorie", Groupe.ARTICLE, TypeColonne.TEXTE,
            Chemin.groupable("categorie.description")),
    TYPE_CODE("typeCode", "Type", Groupe.ARTICLE, TypeColonne.TEXTE, Chemin.groupable("type.code")),
    ETAGE("etage", "Étage", Groupe.ARTICLE, TypeColonne.TEXTE, Chemin.groupable("etage.code")),
    IMMEUBLE("immeuble", "Immeuble", Groupe.ARTICLE, TypeColonne.TEXTE, Chemin.groupable("immeuble.code")),
    SURFACE("surface", "Surface (m²)", Groupe.ARTICLE, TypeColonne.NOMBRE, Chemin.agregeable("surface")),
    PRIX_VENTE_TTC("prixVenteTTC", "Prix de vente TTC", Groupe.PRIX, TypeColonne.MONTANT,
            Chemin.agregeable("prixVenteTTC")),
    STATUT_CODE("statutCode", "Code statut", Groupe.VENTE, TypeColonne.TEXTE, Chemin.groupable("statutCode")),
    STATUT_LIBELLE("statutLibelle", "Statut", Groupe.VENTE, TypeColonne.TEXTE, null),
    DOSSIER_ID("dossierId", "Id dossier", Groupe.VENTE, TypeColonne.TEXTE, null),
    DOSSIER_CODE("dossierCode", "Dossier", Groupe.VENTE, TypeColonne.TEXTE, Chemin.groupable("dossier.code")),
    CLIENT_NOM("clientNom", "Client", Groupe.VENTE, TypeColonne.TEXTE, null),
    COMMERCIAL("commercial", "Commercial", Groupe.VENTE, TypeColonne.TEXTE, null),
    DATE_RESERVATION("dateReservation", "Date réservation", Groupe.VENTE, TypeColonne.DATE,
            Chemin.date("dossier.docDate")),
    MONTANT_REGLE("montantRegle", "Réglé", ColonneDossier.PRIX_REGLE, true),
    RESTE_A_REGLER("resteARegler", "Reste à régler", ColonneDossier.PRIX_RESTE, true),

    // -- Prix du dossier, ou du catalogue sur une ligne libre (@Subselect de StkItemEtatVentes).
    PRIX_ARTICLE_HT("prixArticleHt", "Prix de vente article HT", Chemin.agregeable("prixArticleHt")),
    PRIX_ARTICLE_TTC("prixArticleTtc", "Prix de vente article TTC", Chemin.agregeable("prixArticleTtc")),
    PRIX_SOUS_ARTICLES_HT("prixSousArticlesHt", "Prix de vente sous-articles HT",
            Chemin.agregeable("prixSousArticlesHt")),
    PRIX_SOUS_ARTICLES_TTC("prixSousArticlesTtc", "Prix de vente sous-articles TTC",
            Chemin.agregeable("prixSousArticlesTtc")),
    PRIX_HT("prixHt", "Prix de vente HT", Chemin.agregeable("prixHt")),
    REMISE_MONTANT("remiseMontant", "Montant remise", Chemin.agregeable("remiseMontant")),
    PRIX_CONTRACTUEL("prixContractuel", "Prix contractuel", Chemin.agregeable("prixContractuel")),

    // -- Montants du dossier (liste des dossiers) : null sur une ligne libre.
    PRIX_VENTE_M2_TTC(ColonneDossier.PRIX_VENTE_M2_TTC),
    PRIX_VENTE_M2_HT(ColonneDossier.PRIX_VENTE_M2_HT),
    NET_A_PAYER(ColonneDossier.NET_A_PAYER),
    NET_A_PAYER_REGLE(ColonneDossier.NET_A_PAYER_REGLE),
    NET_A_PAYER_RESTE(ColonneDossier.NET_A_PAYER_RESTE),
    AVANCE(ColonneDossier.AVANCE),
    AVANCE_REGLEE(ColonneDossier.AVANCE_REGLEE),
    AVANCE_RESTE(ColonneDossier.AVANCE_RESTE),
    AUTOFINANCEMENT(ColonneDossier.AUTOFINANCEMENT),
    AUTOFINANCEMENT_REGLE(ColonneDossier.AUTOFINANCEMENT_REGLE),
    AUTOFINANCEMENT_RESTE(ColonneDossier.AUTOFINANCEMENT_RESTE),
    CREDIT(ColonneDossier.CREDIT),
    CREDIT_REGLE(ColonneDossier.CREDIT_REGLE),
    CREDIT_RESTE(ColonneDossier.CREDIT_RESTE),
    FRAIS(ColonneDossier.FRAIS),
    FRAIS_REGLES(ColonneDossier.FRAIS_REGLES),
    FRAIS_RESTE(ColonneDossier.FRAIS_RESTE),
    FRAIS_SYNDIC(ColonneDossier.FRAIS_SYNDIC),
    FRAIS_SYNDIC_REGLES(ColonneDossier.FRAIS_SYNDIC_REGLES),
    FRAIS_SYNDIC_RESTE(ColonneDossier.FRAIS_SYNDIC_RESTE),
    MODIFICATIONS(ColonneDossier.MODIFICATIONS),
    MODIFICATIONS_REGLEES(ColonneDossier.MODIFICATIONS_REGLEES),
    MODIFICATIONS_RESTE(ColonneDossier.MODIFICATIONS_RESTE),
    TROP_PERCU(ColonneDossier.TROP_PERCU),
    TROP_PERCU_REGLE(ColonneDossier.TROP_PERCU_REGLE),
    TROP_PERCU_RESTE(ColonneDossier.TROP_PERCU_RESTE),
    PENALITES(ColonneDossier.PENALITES),
    PENALITES_REGLES(ColonneDossier.PENALITES_REGLES),
    PENALITES_RESTE(ColonneDossier.PENALITES_RESTE),
    PRIX_CONTRACTUEL_REGLE(ColonneDossier.PRIX_CONTRACTUEL_REGLE),
    PRIX_CONTRACTUEL_RESTE(ColonneDossier.PRIX_CONTRACTUEL_RESTE),
    TAUX_AVANCE(ColonneDossier.TAUX_AVANCE),
    TAUX_AVANCE_REEL(ColonneDossier.TAUX_AVANCE_REEL),

    // -- Valeurs calculées du dossier (ColonnesCalculeesDossier) : null sur une ligne libre.
    ALERTE(ColonneDossier.ALERTE),
    SOUS_ARTICLES("sousArticles", "Sous-articles", Groupe.VENTE, TypeColonne.TEXTE, null,
            ColonneDossier.SOUS_ARTICLES_CODES, false);

    public enum Groupe { ARTICLE, VENTE, PRIX, FINANCIER }

    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE }

    /** Identifiant du filtre Jackson de {@code LigneEtatVentesDto}. */
    public static final String FILTRE = "colonnesEtatVentes";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final Chemin chemin;
    private final ColonneDossier source;
    private final boolean parDefaut;

    /** Colonne du lot 1, servie sans {@code ?colonnes=}. */
    ColonneEtatVentes(String nom, String libelle, Groupe groupe, TypeColonne type, Chemin chemin) {
        this(nom, libelle, groupe, type, chemin, null, true);
    }

    /** Prix du {@code @Subselect}, sommé en SQL. */
    ColonneEtatVentes(String nom, String libelle, Chemin chemin) {
        this(nom, libelle, Groupe.PRIX, TypeColonne.MONTANT, chemin, null, false);
    }

    ColonneEtatVentes(String nom, String libelle, ColonneDossier source, boolean parDefaut) {
        this(nom, libelle, Groupe.FINANCIER, TypeColonne.valueOf(source.type().name()), null, source, parDefaut);
    }

    /** Même nom et même libellé que la colonne de la liste des dossiers. */
    ColonneEtatVentes(ColonneDossier source) {
        this(source.nom(), source.libelle(), source, false);
    }

    ColonneEtatVentes(String nom, String libelle, Groupe groupe, TypeColonne type, Chemin chemin,
                      ColonneDossier source, boolean parDefaut) {
        this.nom = nom;
        this.libelle = libelle;
        this.groupe = groupe;
        this.type = type;
        this.chemin = chemin;
        this.source = source;
        this.parDefaut = parDefaut;
    }

    @Override
    public TypeExport typeExport() {
        return TypeExport.valueOf(type.name());
    }
}
