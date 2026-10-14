package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/prospections} : liste blanche de {@code ?colonnes=}.
 * Filtrée à la sérialisation, pas en SELECT dynamique : voir FEATURE-selection-colonnes-listes.md §2.
 */
public enum ColonneProspection implements Colonne {

    // -- Identité. Colonnes historiques, servies depuis ComProspection.
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("id")),
    CODE("code", "Code", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("code")),
    DATE_DOCUMENT("dateDocument", "Date", Groupe.IDENTITE, TypeColonne.DATE, true, Source.ENTITE,
            Chemin.date("docDate")),
    TEMP_DESCRIPTION("tempDescription", "Description saisie", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("tempDescription")),
    PROSPECT_ID("prospectId", "Id prospect", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("cfgProspect.id")),
    PROSPECT_NOM("prospectNom", "Prospect", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.tier("cfgProspect")),
    COMMERCIAL_ID("commercialId", "Id commercial", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("cfgCommercial.id")),
    COMMERCIAL_NOM("commercialNom", "Commercial", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.tier("cfgCommercial")),
    // `code` et non `description` : c'est ce que sert le mappeur de liste et ce qu'attendent les
    // filtres ?origine= et ?etape= ; grouper sur le libellé donnerait des valeurs que le filtre ignore.
    ORIGINE("origine", "Origine", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("comProspectionOrigin.code")),
    ETAPE("etape", "Étape", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("comProspectionKind.code")),
    FERMEE("fermee", "Fermée", Groupe.IDENTITE, TypeColonne.BOOLEEN, true, Source.ENTITE,
            Chemin.groupable("isClosed")),

    // -- Contact et relance.
    // Repli fiche > saisie, identique au mappeur de liste — voir Chemin.repli.
    TELEPHONE("telephone", "Téléphone", Groupe.CONTACT, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.repli("cfgProspect.mobile", "cfgProspect.phone", "tempTel")),
    EMAIL("email", "Email", Groupe.CONTACT, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.repli("cfgProspect.eMail", "tempEMail")),
    DERNIERE_ALERTE_DATE("derniereAlerteDate", "Relance prévue", Groupe.CONTACT, TypeColonne.DATE, true, Source.ENTITE),
    DERNIERE_ALERTE_MESSAGE("derniereAlerteMessage", "Note de relance", Groupe.CONTACT, TypeColonne.TEXTE, true, Source.ENTITE),

    // -- Contexte et financier.
    TRANCHE_ID("trancheId", "Id tranche", Groupe.CONTEXTE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("stkHierarchy.id")),
    TRANCHE_CODE("trancheCode", "Tranche", Groupe.CONTEXTE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("stkHierarchy.code")),
    TRANCHE_DESCRIPTION("trancheDescription", "Libellé tranche", Groupe.CONTEXTE, TypeColonne.TEXTE, true, Source.ENTITE,
            Chemin.groupable("stkHierarchy.description")),
    // -- Qualification budgétaire, morte (0 partout, jamais NULL) : HORS défaut, sinon « 0,00 DH »
    //    sur tout le portefeuille. Agrégeable quand même : sommer rend 0, ce qui est exact.
    //    Voir instructions/INVENTAIRE-colonnes-prospection.md §1.
    PRIX("prix", "Budget", Groupe.FINANCIER, TypeColonne.MONTANT, false, Source.ENTITE,
            Chemin.groupableEtAgregeable("price")),
    SURFACE("surface", "Surface", Groupe.FINANCIER, TypeColonne.NOMBRE, false, Source.ENTITE,
            Chemin.groupableEtAgregeable("area")),

    // -- Présentes dans la vue légataire, servies par les entités : aucune requête de vue.
    //    Voir instructions/INVENTAIRE-colonnes-prospection.md §2.

    // Scalaires ou associations déjà déréférencées par la liste : coût nul.
    PROSPECT_CODE("prospectCode", "Code prospect", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("cfgProspect.code")),
    MOBILE_PROSPECT("mobileProspect", "Mobile (fiche)", Groupe.CONTACT, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("cfgProspect.mobile")),
    INDICATIF_MOBILE("indicatifMobile", "Indicatif", Groupe.CONTACT, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("cfgProspect.mobileIndicatif")),
    EMAIL_FICHE("emailFiche", "Email (fiche)", Groupe.CONTACT, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("cfgProspect.eMail")),
    COMMERCIAL_CODE("commercialCode", "Code commercial", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("cfgCommercial.code")),
    ANCIEN_CLIENT("ancienClient", "Ancien client", Groupe.IDENTITE, TypeColonne.BOOLEEN, false, Source.ENTITE,
            Chemin.groupable("isOldCustomer")),
    DATE_FERMETURE("dateFermeture", "Date de clôture", Groupe.CLOTURE, TypeColonne.DATE, false, Source.ENTITE,
            Chemin.date("closingDate")),
    CREE_PAR("creePar", "Créée par", Groupe.AUDIT, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("userCreate")),
    CREE_LE("creeLe", "Créée le", Groupe.AUDIT, TypeColonne.DATE, false, Source.ENTITE,
            Chemin.date("userCreateDateTime")),
    MODIFIE_PAR("modifiePar", "Modifiée par", Groupe.AUDIT, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("userUpdate")),
    MODIFIE_LE("modifieLe", "Modifiée le", Groupe.AUDIT, TypeColonne.DATE, false, Source.ENTITE,
            Chemin.date("userUpdateDateTime")),

    // Associations supplémentaires : une jointure paresseuse de plus par ligne, seulement si demandée.
    TYPE_PROSPECT("typeProspect", "Type de prospect", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("cfgProspect.cfgTierType.code")),
    RAISON_FERMETURE("raisonFermeture", "Motif de clôture", Groupe.CLOTURE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("comProspectionClosingReason.description")),
    ORIGINE_SUPPORT("origineSupport", "Support d'origine", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("comProspectionOriginSupport.description")),
    // `comAction`, pas `comActionMessageType` : c'est ce que déréférence le mappeur de liste.
    ACTION_COMMERCIALE("actionCommerciale", "Action commerciale", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("comAction.description")),
    PAYS("pays", "Pays", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("admCountry.description")),

    // Deux jointures de plus (tranche → projet → société), même règle.
    SOCIETE_ID("societeId", "Id société", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("stkHierarchy.cfgProject.cfgCompany.id")),
    SOCIETE_CODE("societeCode", "Société", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("stkHierarchy.cfgProject.cfgCompany.code")),
    SOCIETE_LIBELLE("societeLibelle", "Libellé société", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("stkHierarchy.cfgProject.cfgCompany.description")),

    // -- Type de document du légataire (StrEntity) : vente, pré-prospection ou location.
    //    Source.ENTITE et non VUE (ComProspection.StrEntityId) : ne déclenche pas la requête de vue,
    //    mais la résolution du référentiel — voir exigeLeTypeProspection(), FEATURE-strentity-prospection.md.
    TYPE_PROSPECTION_ID("typeProspectionId", "Id type", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE,
            Chemin.groupable("strEntityId")),
    TYPE_PROSPECTION_CODE("typeProspectionCode", "Code type", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE),
    TYPE_PROSPECTION("typeProspection", "Type de prospection", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.ENTITE),

    // -- Activité, calculée par la base légataire. Hors sélection par défaut : ajouter un
    //    de ces champs déclenche la requête de vue, absente sinon.
    JOURS_INACTIVITE("joursInactivite", "Jours d'inactivité", Groupe.ACTIVITE, TypeColonne.NOMBRE, false, Source.VUE),
    NB_INTERACTIONS("nbInteractions", "Interactions", Groupe.ACTIVITE, TypeColonne.NOMBRE, false, Source.VUE),
    DERNIERE_ACTION_DATE("derniereActionDate", "Dernière action", Groupe.ACTIVITE, TypeColonne.DATE, false, Source.VUE),
    DERNIERE_COMMUNICATION_DATE("derniereCommunicationDate", "Dernier échange", Groupe.ACTIVITE, TypeColonne.DATE, false, Source.VUE),
    DERNIERE_COMMUNICATION_RESULTAT("derniereCommunicationResultat", "Résultat du dernier échange", Groupe.ACTIVITE, TypeColonne.TEXTE, false, Source.VUE),
    NB_VISITES("nbVisites", "Visites", Groupe.ACTIVITE, TypeColonne.NOMBRE, false, Source.VUE,
            Chemin.groupableEtAgregeable("visites.nbrVisit")),
    DERNIERE_VISITE_DATE("derniereVisiteDate", "Dernière visite", Groupe.ACTIVITE, TypeColonne.DATE, false, Source.VUE,
            Chemin.date("visites.lastVisitDate")),

    PROJET_ID("projetId", "Id projet", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.VUE,
            Chemin.groupable("stkHierarchy.cfgProject.id")),
    PROJET_CODE("projetCode", "Projet", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.VUE,
            Chemin.groupable("stkHierarchy.cfgProject.code")),
    PROJET_LIBELLE("projetLibelle", "Libellé projet", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.VUE,
            Chemin.groupable("stkHierarchy.cfgProject.description")),
    CIN("cin", "CIN", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.VUE,
            Chemin.groupable("cfgProspect.iDNumber")),

    // -- Conversion (dossier issu de la prospection).
    DOSSIER_ID("dossierId", "Id dossier", Groupe.CONVERSION, TypeColonne.TEXTE, false, Source.VUE),
    DOSSIER_CODE("dossierCode", "Dossier", Groupe.CONVERSION, TypeColonne.TEXTE, false, Source.VUE),
    STATUT_DOSSIER("statutDossier", "Statut dossier", Groupe.CONVERSION, TypeColonne.TEXTE, false, Source.VUE);

    /** Regroupement d'affichage du sélecteur. */
    public enum Groupe { IDENTITE, CONTACT, CONTEXTE, FINANCIER, ACTIVITE, CONVERSION, CLOTURE, AUDIT }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE, BOOLEEN }

    /** D'où vient la valeur — décide si la requête de vue est nécessaire. */
    public enum Source { ENTITE, VUE }

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;
    private final Source source;
    private final Chemin chemin;

    /** Colonne ni groupable ni agrégeable — le cas par défaut, cf. {@link Colonne#chemin()}. */
    ColonneProspection(String nom, String libelle, Groupe groupe, TypeColonne type,
                       boolean parDefaut, Source source) {
        this(nom, libelle, groupe, type, parDefaut, source, null);
    }

    ColonneProspection(String nom, String libelle, Groupe groupe, TypeColonne type,
                       boolean parDefaut, Source source, Chemin chemin) {
        this.nom = nom;
        this.libelle = libelle;
        this.groupe = groupe;
        this.type = type;
        this.parDefaut = parDefaut;
        this.source = source;
        this.chemin = chemin;
    }

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

    public boolean parDefaut() {
        return parDefaut;
    }

    public Source source() {
        return source;
    }

    /**
     * Les colonnes de vue sans chemin n'existent pas dans le graphe de {@code ComProspection} ;
     * {@code TYPE_PROSPECTION_CODE}/{@code TYPE_PROSPECTION} : {@code strEntityId} est un UUID brut,
     * rien à joindre. Voir FEATURE-groupement-toutes-colonnes.md §4.
     */
    @Override
    public Chemin chemin() {
        return chemin;
    }

    public static List<ColonneProspection> groupables() {
        return Colonnes.groupables(ColonneProspection.class);
    }

    public static List<ColonneProspection> agregeables() {
        return Colonnes.agregeables(ColonneProspection.class);
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneProspection resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneProspection.class, demandee);
    }

    /** Forme {@code colonne} (somme) ou {@code colonne:FONCTION}. */
    public static List<AgregatDemande> resoudreAgregats(List<String> demandees) {
        return Colonnes.resoudreAgregats(ColonneProspection.class, demandees);
    }

    /** Sélection sans {@code ?colonnes=} — voir {@link Colonnes#parDefaut}. */
    public static Set<ColonneProspection> parDefautSet() {
        return Colonnes.parDefaut(ColonneProspection.class);
    }

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneProspection.class);
    }

    public static List<String> nomsValides() {
        return Colonnes.nomsValides(ColonneProspection.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneProspection> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneProspection.class, demandees, ID);
    }

    /** Vrai si la sélection oblige à interroger {@code ComSaleProspectionView}. */
    public static boolean exigeLaVue(Set<ColonneProspection> selection) {
        return selection.stream().anyMatch(c -> c.source == Source.VUE);
    }

    /** Distinct de {@link #exigeLaVue(Set)} : ces colonnes viennent des entités, pas de la vue. */
    public static boolean exigeLeTypeProspection(Set<ColonneProspection> selection) {
        return selection.contains(TYPE_PROSPECTION_ID)
                || selection.contains(TYPE_PROSPECTION_CODE)
                || selection.contains(TYPE_PROSPECTION);
    }
}
