package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/prospects} : liste blanche de {@code ?colonnes=}.
 * Tranches d'intérêt exclues (N+1) : FEATURE-selecteur-colonnes-prospect-dossier.md §3.1.
 */
public enum ColonneProspect implements Colonne {

    // -- Colonnes historiques : la sélection par défaut, à ne pas modifier.
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    CODE("code", "Code", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    PRENOM("prenom", "Prénom", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    NOM("nom", "Nom", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    RAISON_SOCIALE("raisonSociale", "Raison sociale", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    TELEPHONE("telephone", "Téléphone", Groupe.CONTACT, TypeColonne.TEXTE, true),
    MOBILE("mobile", "Mobile", Groupe.CONTACT, TypeColonne.TEXTE, true),
    EMAIL("email", "Email", Groupe.CONTACT, TypeColonne.TEXTE, true),
    VILLE("ville", "Ville", Groupe.ADRESSE, TypeColonne.TEXTE, true),
    TYPE("type", "Type", Groupe.IDENTITE, TypeColonne.TEXTE, true),

    // -- Identité. Scalaires de CfgTier : coût nul.
    CIN("cin", "CIN", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    ICE("ice", "ICE", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    DATE_NAISSANCE("dateNaissance", "Date de naissance", Groupe.IDENTITE, TypeColonne.DATE, false),
    LIEU_NAISSANCE("lieuNaissance", "Lieu de naissance", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    NOMBRE_ENFANTS("nombreEnfants", "Nombre d'enfants", Groupe.IDENTITE, TypeColonne.NOMBRE, false),
    EST_ANCIEN_CLIENT("estAncienClient", "Ancien client", Groupe.IDENTITE, TypeColonne.BOOLEEN, false),
    FILS_DE("filsDe", "Fils de", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    ET_DE("etDe", "Et de", Groupe.IDENTITE, TypeColonne.TEXTE, false),

    // -- Contact. Scalaires : coût nul.
    INDICATIF_TELEPHONE("indicatifTelephone", "Indicatif fixe", Groupe.CONTACT, TypeColonne.TEXTE, false),
    TELEPHONE1("telephone1", "Téléphone 2", Groupe.CONTACT, TypeColonne.TEXTE, false),
    TELEPHONE2("telephone2", "Téléphone 3", Groupe.CONTACT, TypeColonne.TEXTE, false),
    INDICATIF_MOBILE("indicatifMobile", "Indicatif mobile", Groupe.CONTACT, TypeColonne.TEXTE, false),
    FAX("fax", "Fax", Groupe.CONTACT, TypeColonne.TEXTE, false),
    SITE_WEB("siteWeb", "Site web", Groupe.CONTACT, TypeColonne.TEXTE, false),

    // -- Adresse. Scalaires : coût nul.
    ADRESSE("adresse", "Adresse", Groupe.ADRESSE, TypeColonne.TEXTE, false),
    ADRESSE2("adresse2", "Adresse (suite)", Groupe.ADRESSE, TypeColonne.TEXTE, false),
    ADRESSE3("adresse3", "Adresse (fin)", Groupe.ADRESSE, TypeColonne.TEXTE, false),
    CODE_POSTAL("codePostal", "Code postal", Groupe.ADRESSE, TypeColonne.TEXTE, false),

    // -- Situation professionnelle.
    EMPLOYEUR("employeur", "Employeur", Groupe.PROFESSION, TypeColonne.TEXTE, false),
    ANCIENNETE("anciennete", "Ancienneté", Groupe.PROFESSION, TypeColonne.NOMBRE, false),

    // -- Référentiels : une jointure paresseuse de plus par ligne, payée seulement si demandée.
    KIND("kind", "Genre", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    TITRE("titre", "Titre", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    SEXE("sexe", "Sexe", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    NATIONALITE("nationalite", "Nationalité", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    SITUATION_FAMILIALE("situationFamiliale", "Situation familiale", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    REGIME_MATRIMONIAL("regimeMatrimonial", "Régime matrimonial", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    PAYS("pays", "Pays", Groupe.ADRESSE, TypeColonne.TEXTE, false),
    PAYS_RESIDENCE("paysResidence", "Pays de résidence", Groupe.ADRESSE, TypeColonne.TEXTE, false),
    TYPE_RESIDENCE("typeResidence", "Type de résidence", Groupe.ADRESSE, TypeColonne.TEXTE, false),
    /** {@code CfgTier.Quality}, toujours vide ; gardée parce que la fiche l'expose déjà. */
    PROFESSION("profession", "Profession", Groupe.PROFESSION, TypeColonne.TEXTE, false),
    SALAIRE("salaire", "Salaire", Groupe.PROFESSION, TypeColonne.TEXTE, false),
    COMMERCIAL_ID("commercialId", "Id commercial", Groupe.IDENTITE, TypeColonne.TEXTE, false),
    COMMERCIAL_NOM("commercialNom", "Commercial", Groupe.IDENTITE, TypeColonne.TEXTE, false),

    // -- Piste d'audit.
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false);

    /** Regroupement d'affichage du sélecteur. */
    public enum Groupe { IDENTITE, CONTACT, ADRESSE, PROFESSION, AUDIT }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson qui porte la sélection de colonnes des prospects. */
    public static final String FILTRE = "colonnesProspect";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;

    ColonneProspect(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut) {
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

    public static Set<ColonneProspect> parDefautSet() {
        return Colonnes.parDefaut(ColonneProspect.class);
    }

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneProspect.class);
    }

    public static List<String> nomsValides() {
        return Colonnes.nomsValides(ColonneProspect.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneProspect> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneProspect.class, demandees, ID);
    }
}
