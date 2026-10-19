package com.tsix.immocrm.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Catalogue des colonnes de {@code GET /api/etat-des-ventes}, sous les noms de {@code LigneEtatVentesDto}
 * (FEATURE-etat-des-ventes.md §3.2). Le chemin sert au groupement, aux agrégats et au tri ; sans chemin, la colonne
 * n'est ni groupable ni triable (nom d'un tiers, montants réglés calculés en Java).
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
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
    PRIX_VENTE_TTC("prixVenteTTC", "Prix de vente TTC", Groupe.FINANCIER, TypeColonne.MONTANT,
            Chemin.agregeable("prixVenteTTC")),
    STATUT_CODE("statutCode", "Code statut", Groupe.VENTE, TypeColonne.TEXTE, Chemin.groupable("statutCode")),
    STATUT_LIBELLE("statutLibelle", "Statut", Groupe.VENTE, TypeColonne.TEXTE, null),
    DOSSIER_ID("dossierId", "Id dossier", Groupe.VENTE, TypeColonne.TEXTE, null),
    DOSSIER_CODE("dossierCode", "Dossier", Groupe.VENTE, TypeColonne.TEXTE, Chemin.groupable("dossier.code")),
    CLIENT_NOM("clientNom", "Client", Groupe.VENTE, TypeColonne.TEXTE, null),
    COMMERCIAL("commercial", "Commercial", Groupe.VENTE, TypeColonne.TEXTE, null),
    DATE_RESERVATION("dateReservation", "Date réservation", Groupe.VENTE, TypeColonne.DATE,
            Chemin.date("dossier.docDate")),
    MONTANT_REGLE("montantRegle", "Réglé", Groupe.FINANCIER, TypeColonne.MONTANT, null),
    RESTE_A_REGLER("resteARegler", "Reste à régler", Groupe.FINANCIER, TypeColonne.MONTANT, null);

    public enum Groupe { ARTICLE, VENTE, FINANCIER }

    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE }

    /** Identifiant du filtre Jackson de {@code LigneEtatVentesDto}. */
    public static final String FILTRE = "colonnesEtatVentes";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final Chemin chemin;

    @Override
    public TypeExport typeExport() {
        return TypeExport.valueOf(type.name());
    }

    /** Toutes les colonnes sont servies sans {@code ?colonnes=}. */
    @Override
    public boolean parDefaut() {
        return true;
    }
}
