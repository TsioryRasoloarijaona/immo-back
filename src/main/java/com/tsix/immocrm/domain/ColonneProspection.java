package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes sélectionnables de {@code GET /api/prospections}.
 *
 * <p>C'est la <strong>liste blanche</strong> du paramètre {@code ?colonnes=} et, exposée par
 * {@code GET /api/referentiels/colonnes/prospections}, la source du sélecteur de colonnes côté
 * front — qui n'a donc aucune liste à coder en dur.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 *
 * <h2>Ce que la sélection apporte, et ce qu'elle n'apporte pas</h2>
 *
 * <p>Elle réduit la <strong>charge utile</strong>, pas le temps de requête. Mesuré sur la base
 * réelle : charger les colonnes d'activité d'une page de 20 lignes coûte <strong>7 ms</strong>,
 * qu'on en demande 4 ou 20, parce que la vue est interrogée par lot d'identifiants
 * ({@code WHERE PKey IN (…)}) et jamais parcourue. Le filtrage est donc appliqué à la
 * sérialisation, et non traduit en {@code SELECT} dynamique : cela évite d'assembler du SQL à
 * partir d'une entrée client pour un gain nul.
 *
 * <p>En revanche, demander <em>zéro</em> colonne d'activité évite entièrement la requête de vue —
 * c'est ce que fait le comportement par défaut, qui reste identique à l'existant.
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
    // `code` et non `description` : c'est ce que sert le mappeur de liste, et c'est aussi ce
    // qu'attendent les filtres ?origine= et ?etape=. Grouper sur une autre colonne afficherait
    // des libellés que le filtre correspondant ne reconnaîtrait pas.
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
    // -- Qualification budgétaire : mesurée morte, donc HORS sélection par défaut.
    //    `Price` est renseigné sur 4 lignes sur 14 114 et vaut 0 partout ailleurs — jamais NULL,
    //    donc non masquable par un test de nullité côté front. `Area` : 0 ligne. Les laisser par
    //    défaut faisait afficher « 0,00 DH » de budget sur tout le portefeuille : de
    //    l'information fausse, plus nuisible que l'absence. Elles restent demandables via
    //    ?colonnes= pour les rares lignes qui portent une valeur.
    //    Voir instructions/INVENTAIRE-colonnes-prospection.md §1.
    //    Agrégeables malgré tout : sommer une colonne morte rend 0, ce qui est exact. C'est
    //    l'AFFICHER par défaut qui était le problème, pas la calculer sur demande explicite.
    PRIX("prix", "Budget", Groupe.FINANCIER, TypeColonne.MONTANT, false, Source.ENTITE,
            Chemin.groupableEtAgregeable("price")),
    SURFACE("surface", "Surface", Groupe.FINANCIER, TypeColonne.NOMBRE, false, Source.ENTITE,
            Chemin.groupableEtAgregeable("area")),

    // ------------------------------------------------------------------
    // Colonnes ajoutées au lot B — présentes dans la vue légataire (donc affichables par le
    // logiciel de bureau) mais que nos GET ne renvoyaient pas. Toutes servies par les entités :
    // aucune requête de vue, aucun coût quand elles ne sont pas demandées.
    // Voir instructions/INVENTAIRE-colonnes-prospection.md §2.
    // ------------------------------------------------------------------

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

    // Associations supplémentaires : une jointure paresseuse de plus par ligne, payée seulement
    // si la colonne est demandée — le mappeur ne les déréférence pas autrement.
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

    // ------------------------------------------------------------------
    // Type de document du légataire (StrEntity) — le discriminant réel entre prospection de
    // vente, pré-prospection et prospection de location. Peuplé à 100 % sur la vue, et la
    // seule colonne dans ce cas que nos GET ne renvoyaient pas.
    //
    // Source.ENTITE et non VUE : la valeur vient de ComProspection.StrEntityId résolu contre
    // StrEntity, donc demander ces colonnes ne doit PAS déclencher la requête de vue. Elle
    // déclenche en revanche la résolution du référentiel — voir exigeLeTypeProspection().
    // Voir instructions/FEATURE-strentity-prospection.md.
    // ------------------------------------------------------------------
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

    // -- Conversion. Renseignées sur 2 % des lignes — c'est le taux de conversion réel.
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

    public boolean parDefaut() {
        return parDefaut;
    }

    public Source source() {
        return source;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Aucune colonne {@link Source#VUE} n'a de chemin</strong>, et c'est structurel :
     * leur valeur vient de {@code ComSaleProspectionView}, chargée par une requête distincte par
     * lot d'identifiants. Elle n'existe pas dans le graphe d'entités de {@code ComProspection},
     * donc elle ne peut pas figurer dans un {@code GROUP BY} sur cette racine. Grouper par
     * {@code joursInactivite} ou {@code statutDossier} demanderait d'agréger la vue elle-même —
     * un autre lot, avec sa propre mesure de coût.
     *
     * <p>{@code TYPE_PROSPECTION*} est exclue pour une raison différente : {@code strEntityId} est
     * un {@code UUID} brut sur l'entité, pas une association — il n'y a rien à joindre vers
     * {@code StrEntity} pour en tirer un libellé.
     */
    @Override
    public Chemin chemin() {
        return chemin;
    }

    /** Colonnes acceptées par {@code ?groupBy=}, dans l'ordre de déclaration. */
    public static List<ColonneProspection> groupables() {
        return Colonnes.groupables(ColonneProspection.class);
    }

    /** Colonnes acceptées par {@code ?agregats=}, dans l'ordre de déclaration. */
    public static List<ColonneProspection> agregeables() {
        return Colonnes.agregeables(ColonneProspection.class);
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneProspection resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneProspection.class, demandee);
    }

    /**
     * Résout {@code ?agregats=}, forme {@code colonne} (somme) ou {@code colonne:FONCTION}.
     *
     * @throws ColonneInconnueException         si un nom n'est pas une colonne de ce catalogue
     * @throws FonctionAgregatInconnueException si la colonne n'autorise pas cette fonction
     */
    public static List<AgregatDemande> resoudreAgregats(List<String> demandees) {
        return Colonnes.resoudreAgregats(ColonneProspection.class, demandees);
    }

    /**
     * Sélection appliquée quand {@code ?colonnes=} est absent : exactement les colonnes servies
     * avant l'introduction du paramètre. C'est ce qui rend le lot additif — une intégration qui
     * ignore {@code colonnes} ne voit aucun changement.
     */
    public static Set<ColonneProspection> parDefautSet() {
        return Colonnes.parDefaut(ColonneProspection.class);
    }

    /** Noms par défaut, forme attendue par le filtre Jackson. */
    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneProspection.class);
    }

    /** Tous les noms valides, dans l'ordre de déclaration — pour le message d'erreur du 400. */
    public static List<String> nomsValides() {
        return Colonnes.nomsValides(ColonneProspection.class);
    }

    /**
     * Résout une sélection reçue du client — voir {@link Colonnes#resoudre}.
     *
     * @throws ColonneInconnueException si un nom ne figure pas au catalogue.
     */
    public static Set<ColonneProspection> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneProspection.class, demandees, ID);
    }

    /** Vrai si la sélection oblige à interroger {@code ComSaleProspectionView}. */
    public static boolean exigeLaVue(Set<ColonneProspection> selection) {
        return selection.stream().anyMatch(c -> c.source == Source.VUE);
    }

    /**
     * Vrai si la sélection oblige à résoudre le référentiel {@code StrEntity}.
     *
     * <p>Distinct de {@link #exigeLaVue(Set)} : ces colonnes viennent des entités, pas de la vue.
     * Sans ce test, une liste par défaut paierait une requête de référentiel pour trois champs que
     * personne n'a demandés — le même travers que la sélection par défaut évite pour la vue.
     */
    public static boolean exigeLeTypeProspection(Set<ColonneProspection> selection) {
        return selection.contains(TYPE_PROSPECTION_ID)
                || selection.contains(TYPE_PROSPECTION_CODE)
                || selection.contains(TYPE_PROSPECTION);
    }
}
