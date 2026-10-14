package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/impayes} (sélecteur, export). Défaut dans l'ordre de l'écran
 * OMRAN. Aucune n'est groupable : le reste se calcule hors SQL ({@code FEATURE-impayes.md} R2).
 */
public enum ColonneImpaye implements Colonne {

    DATE_ECHEANCE("dateEcheance", "Date échéance", Groupe.ECHEANCE, TypeColonne.DATE, true),
    DATE_RECOUVREMENT("dateRecouvrement", "Date recouvrement", Groupe.ECHEANCE, TypeColonne.DATE, true),
    DOSSIER_CODE("dossierCode", "Dossier", Groupe.DOSSIER, TypeColonne.TEXTE, true),
    CLIENT_NOM("clientNom", "Client", Groupe.DOSSIER, TypeColonne.TEXTE, true),
    PROJET("projet", "Projet", Groupe.DOSSIER, TypeColonne.TEXTE, true),
    TRANCHE("tranche", "Tranche", Groupe.DOSSIER, TypeColonne.TEXTE, true),
    ARTICLE("article", "Article", Groupe.DOSSIER, TypeColonne.TEXTE, true),
    CATEGORIE("categorie", "Catégorie", Groupe.ECHEANCE, TypeColonne.TEXTE, true),
    MONTANT_A_PAYER("montantAPayer", "Montant à payer", Groupe.FINANCIER, TypeColonne.MONTANT, true),
    MONTANT_RESTE("montantReste", "Montant reste", Groupe.FINANCIER, TypeColonne.MONTANT, true),
    JOURS_RETARD("joursRetard", "Jours de retard", Groupe.ECHEANCE, TypeColonne.NOMBRE, true),
    COMMERCIAL("commercial", "Commercial", Groupe.DOSSIER, TypeColonne.TEXTE, true),
    NOTES("notes", "Notes", Groupe.ECHEANCE, TypeColonne.TEXTE, true),

    MONTANT_REGLE("montantRegle", "Montant réglé", Groupe.FINANCIER, TypeColonne.MONTANT, false),
    STATUT_DOSSIER("statutDossier", "Statut dossier", Groupe.DOSSIER, TypeColonne.TEXTE, false),
    SOCIETE("societe", "Société", Groupe.DOSSIER, TypeColonne.TEXTE, false),
    DESCRIPTION("description", "Description", Groupe.ECHEANCE, TypeColonne.TEXTE, false),
    PENALITE_ID("penaliteId", "Pénalité", Groupe.ECHEANCE, TypeColonne.TEXTE, false),

    ID("echeanceId", "Id échéance", Groupe.ECHEANCE, TypeColonne.TEXTE, false),
    DOSSIER_ID("dossierId", "Id dossier", Groupe.DOSSIER, TypeColonne.TEXTE, false),
    CLIENT_ID("clientId", "Id client", Groupe.DOSSIER, TypeColonne.TEXTE, false);

    /** Regroupement d'affichage du sélecteur. */
    public enum Groupe { ECHEANCE, DOSSIER, FINANCIER }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE }

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;

    ColonneImpaye(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut) {
        this.nom = nom;
        this.libelle = libelle;
        this.groupe = groupe;
        this.type = type;
        this.parDefaut = parDefaut;
    }

    @Override
    public String nom() {
        return nom;
    }

    @Override
    public String libelle() {
        return libelle;
    }

    public Groupe groupe() {
        return groupe;
    }

    public TypeColonne type() {
        return type;
    }

    @Override
    public TypeExport typeExport() {
        return TypeExport.valueOf(type.name());
    }

    @Override
    public boolean parDefaut() {
        return parDefaut;
    }

    public static List<String> nomsValides() {
        return Colonnes.nomsValides(ColonneImpaye.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneImpaye> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneImpaye.class, demandees, ID);
    }
}
