package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/suivi-actions} (écran OMRAN « Suivi des actions »),
 * filtrées par la mesure : voir FEATURE-suivi-actions.md §2. Colonnes sans chemin : calculées pour
 * la page servie seulement (la vue les calcule sur toutes les lignes), ou {@code nvarchar(max)}.
 */
public enum ColonneSuiviAction implements Colonne {

    // -- L'action : la grille OMRAN.
    UTILISATEUR("utilisateur", "Utilisateur", Groupe.ACTION, TypeColonne.TEXTE, true,
            Chemin.groupable("userName")),
    TYPE_ACTION("typeAction", "Action", Groupe.ACTION, TypeColonne.TEXTE, true,
            Chemin.groupable("comActionMessageType.description")),
    /** Rang de l'action parmi celles du même type pour ce tiers, 1 = la plus récente. Page servie seulement. */
    RANG("rang", "Rang", Groupe.ACTION, TypeColonne.NOMBRE, true),
    DATE_ACTION("dateAction", "Date action", Groupe.ACTION, TypeColonne.DATE, true,
            Chemin.date("docDate")),
    DATE_ALERTE("dateAlerte", "Date alerte", Groupe.ACTION, TypeColonne.DATE, true,
            Chemin.date("alertValidityDate")),
    /** {@code AdmAlertData.Active}. Page servie seulement. */
    ALERTE_ACTIVE("alerteActive", "Alerte active", Groupe.ACTION, TypeColonne.BOOLEEN, true),
    CATEGORIE("categorie", "Catégorie", Groupe.ACTION, TypeColonne.TEXTE, true,
            Chemin.groupable("comActionMessageCategory.description")),
    /** Sous-catégorie : « Rendez vous pris », « Hors budget »… */
    RESULTAT("resultat", "Résultat", Groupe.ACTION, TypeColonne.TEXTE, true,
            Chemin.groupable("comActionMessageSubCategory.description")),
    /** {@code AlertMessage} — la note saisie. {@code nvarchar(max)} : ni groupable ni agrégeable. */
    MESSAGE("message", "Message", Groupe.ACTION, TypeColonne.TEXTE, true),
    IMPAYE("impaye", "Impayé", Groupe.ACTION, TypeColonne.BOOLEEN, true,
            Chemin.groupable("isUnpaid")),
    /** Relances et événements générés, que l'écran OMRAN liste aussi. */
    SYSTEME("systeme", "Système", Groupe.ACTION, TypeColonne.BOOLEEN, false,
            Chemin.groupable("isSystem")),
    /** Rarement vrai : le type (« Téléphone entrant ») porte le sens bien plus souvent. */
    ENTRANTE("entrante", "Entrante", Groupe.ACTION, TypeColonne.BOOLEEN, false,
            Chemin.groupable("isIn")),

    // -- Les personnes.
    /**
     * {@code CfgEmployeeId} — l'employé concerné, pas l'auteur. Groupé sur {@code Description}, comme
     * la vue l'affiche, pas sur la priorité de {@code TierNomFormatter} : ligne et groupe même libellé.
     */
    COMMERCIAL("commercial", "Commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgEmployee.description")),
    /** Employé du compte, sinon le compte, sinon le commercial — règle de la vue. Page servie seulement. */
    PERSONNEL("personnel", "Personnel", Groupe.ACTEURS, TypeColonne.TEXTE, true),
    /**
     * Tiers de l'action, sinon fiche prospect de la prospection, sinon {@code TempDescription} — même
     * cascade que {@code SuiviActionService#tiersNom}, sur {@code Description} comme {@link #COMMERCIAL} :
     * sans elle, les actions de pré-prospection (sans {@code CfgTierId}) n'ont ni nom ni groupe.
     */
    TIERS_NOM("tiersNom", "Tiers", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.repli("cfgTier.description", "comProspection.cfgProspect.description",
                    "comProspection.tempDescription")),

    // -- Ce à quoi l'action est rattachée.
    PROSPECTION_CODE("prospectionCode", "Prospection", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("comProspection.code")),
    DOSSIER_CODE("dossierCode", "N° dossier", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("comFolder.code")),
    /** Statut du dossier (ou du dossier né de la prospection), sinon la catégorie. Page servie seulement. */
    ETAT("etat", "État", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true),
    ARTICLE_CODE("articleCode", "Article", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("stkItem.code")),
    ARTICLE_LIBELLE("articleLibelle", "Libellé article", Groupe.RATTACHEMENT, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.description")),
    /** Tranche résolue comme la vue OMRAN. */
    TRANCHE_CODE("trancheCode", "Tranche", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("trancheResolue.code")),
    TRANCHE_LIBELLE("trancheLibelle", "Libellé tranche", Groupe.RATTACHEMENT, TypeColonne.TEXTE, false,
            Chemin.groupable("trancheResolue.description")),
    PROGRAMME("programme", "Programme", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("trancheResolue.cfgProject.description")),
    ENTITE("entite", "Entité", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("trancheResolue.cfgProject.cfgCompany.description")),

    // -- Identifiants : pour ouvrir la fiche au double-clic, comme OMRAN.
    ID("id", "Identifiant", Groupe.IDENTIFIANTS, TypeColonne.TEXTE, true,
            Chemin.groupable("id")),
    TYPE_ID("typeId", "Id action", Groupe.IDENTIFIANTS, TypeColonne.TEXTE, false,
            Chemin.groupable("comActionMessageType.id")),
    CATEGORIE_ID("categorieId", "Id catégorie", Groupe.IDENTIFIANTS, TypeColonne.TEXTE, false,
            Chemin.groupable("comActionMessageCategory.id")),
    TIERS_ID("tiersId", "Id tiers", Groupe.IDENTIFIANTS, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgTier.id")),
    PROSPECTION_ID("prospectionId", "Id prospection", Groupe.IDENTIFIANTS, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.id")),
    DOSSIER_ID("dossierId", "Id dossier", Groupe.IDENTIFIANTS, TypeColonne.TEXTE, false,
            Chemin.groupable("comFolder.id")),
    TRANCHE_ID("trancheId", "Id tranche", Groupe.IDENTIFIANTS, TypeColonne.TEXTE, false,
            Chemin.groupable("trancheResolue.id")),

    // -- Piste d'audit.
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userCreate")),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userCreateDateTime")),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userUpdate")),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userUpdateDateTime"));

    /** Regroupement d'affichage du sélecteur. */
    public enum Groupe { ACTION, ACTEURS, RATTACHEMENT, IDENTIFIANTS, AUDIT }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson qui porte la sélection de colonnes du suivi des actions. */
    public static final String FILTRE = "colonnesSuiviAction";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;
    private final Chemin chemin;

    /** Colonne ni groupable ni agrégeable — cf. {@link Colonne#chemin()}. */
    ColonneSuiviAction(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut) {
        this(nom, libelle, groupe, type, parDefaut, null);
    }

    ColonneSuiviAction(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut,
                       Chemin chemin) {
        this.nom = nom;
        this.libelle = libelle;
        this.groupe = groupe;
        this.type = type;
        this.parDefaut = parDefaut;
        this.chemin = chemin;
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

    @Override
    public Chemin chemin() {
        return chemin;
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneSuiviAction resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneSuiviAction.class, demandee);
    }

    /** Aucune colonne agrégeable : l'effectif du groupe est la mesure, une demande d'agrégat rend 400. */
    public static List<AgregatDemande> resoudreAgregats(List<String> demandees) {
        return Colonnes.resoudreAgregats(ColonneSuiviAction.class, demandees);
    }

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneSuiviAction.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneSuiviAction> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneSuiviAction.class, demandees, ID);
    }
}
