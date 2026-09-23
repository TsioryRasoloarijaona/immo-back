package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes sélectionnables de {@code GET /api/suivi-actions} — l'écran OMRAN
 * « Suivi des actions ».
 *
 * <p>Liste blanche de {@code ?colonnes=}, publiée par
 * {@code GET /api/referentiels/colonnes/suivi-actions} pour le sélecteur côté front, et porteuse
 * des {@link Chemin} qui rendent {@code GET /api/suivi-actions/groupes} possible.
 *
 * <h2>Comment il a été établi</h2>
 *
 * <p>Les colonnes sont celles de la vue {@code ComActionMessageView} que lit l'écran (trace
 * d'audit du 23/09) et que liste sa documentation officielle, <b>filtrées par la mesure</b> sur les
 * 48 213 actions réelles : désistement, transfert client, transfert produit (0 ligne), SAV (2),
 * location, offre et syndic (0) ne sont pas au catalogue. Voir
 * {@code instructions/FEATURE-suivi-actions.md} §2.
 *
 * <p><b>Le défaut</b> reproduit la grille OMRAN ; identifiants et piste d'audit sont hors défaut.
 *
 * <h2>Les colonnes sans chemin</h2>
 *
 * <ul>
 *   <li>{@link #RANG}, {@link #ALERTE_ACTIVE}, {@link #ETAT}, {@link #PERSONNEL} : calculées pour
 *       la <em>page servie</em> seulement. La vue les calcule sur toutes les lignes, et c'est ce
 *       qui la rend inutilisable (6,6 s la page) ;</li>
 *   <li>{@link #MESSAGE} : {@code AlertMessage} est un {@code nvarchar(max)}, que le
 *       {@code COUNT(DISTINCT)} du moteur de groupement refuse.</li>
 * </ul>
 *
 * <p>Les libellés de tiers et de commercial sont groupés sur {@code Description}, la colonne que
 * la vue affiche — pas sur la priorité de {@code TierNomFormatter} : la ligne et son groupe doivent
 * porter le même libellé.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
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
    /** 39 363 / 48 213. */
    DATE_ALERTE("dateAlerte", "Date alerte", Groupe.ACTION, TypeColonne.DATE, true,
            Chemin.date("alertValidityDate")),
    /** {@code AdmAlertData.Active}. Page servie seulement. */
    ALERTE_ACTIVE("alerteActive", "Alerte active", Groupe.ACTION, TypeColonne.BOOLEEN, true),
    CATEGORIE("categorie", "Catégorie", Groupe.ACTION, TypeColonne.TEXTE, true,
            Chemin.groupable("comActionMessageCategory.description")),
    /** Sous-catégorie : « Rendez vous pris », « Hors budget »… — 44 650 / 48 213. */
    RESULTAT("resultat", "Résultat", Groupe.ACTION, TypeColonne.TEXTE, true,
            Chemin.groupable("comActionMessageSubCategory.description")),
    /** {@code AlertMessage} — la note saisie. {@code nvarchar(max)} : ni groupable ni agrégeable. */
    MESSAGE("message", "Message", Groupe.ACTION, TypeColonne.TEXTE, true),
    /** 930 à vrai. */
    IMPAYE("impaye", "Impayé", Groupe.ACTION, TypeColonne.BOOLEEN, true,
            Chemin.groupable("isUnpaid")),
    /** 4 482 à vrai : relances et événements générés, que l'écran OMRAN liste aussi. */
    SYSTEME("systeme", "Système", Groupe.ACTION, TypeColonne.BOOLEEN, false,
            Chemin.groupable("isSystem")),
    /** 13 à vrai sur 48 213 : le type (« Téléphone entrant ») porte le sens bien plus souvent. */
    ENTRANTE("entrante", "Entrante", Groupe.ACTION, TypeColonne.BOOLEEN, false,
            Chemin.groupable("isIn")),

    // -- Les personnes.
    /** {@code CfgEmployeeId} — l'employé concerné, pas l'auteur. 37 219 / 48 213 (77 %). */
    COMMERCIAL("commercial", "Commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgEmployee.description")),
    /** Employé du compte, sinon le compte, sinon le commercial — règle de la vue. Page servie seulement. */
    PERSONNEL("personnel", "Personnel", Groupe.ACTEURS, TypeColonne.TEXTE, true),
    TIERS_NOM("tiersNom", "Tiers", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgTier.description")),

    // -- Ce à quoi l'action est rattachée.
    PROSPECTION_CODE("prospectionCode", "Prospection", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("comProspection.code")),
    /** 1 507 / 48 213. */
    DOSSIER_CODE("dossierCode", "N° dossier", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("comFolder.code")),
    /** Statut du dossier (ou du dossier né de la prospection), sinon la catégorie. Page servie seulement. */
    ETAT("etat", "État", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true),
    /** 4 170 / 48 213. */
    ARTICLE_CODE("articleCode", "Article", Groupe.RATTACHEMENT, TypeColonne.TEXTE, true,
            Chemin.groupable("stkItem.code")),
    ARTICLE_LIBELLE("articleLibelle", "Libellé article", Groupe.RATTACHEMENT, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.description")),
    /** Tranche résolue comme la vue OMRAN — 46 559 / 48 213, 14 tranches. */
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

    /**
     * Résout {@code ?agregats=}. Aucune colonne de ce catalogue n'est agrégeable : l'effectif de
     * chaque groupe est la mesure — le nombre d'actions. Une demande d'agrégat rend donc 400.
     */
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
