package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/dossiers} : liste blanche de {@code ?colonnes=}.
 * Exclusions (articles, ventilation financière, notes) : FEATURE-selecteur-colonnes-prospect-dossier.md §3.2.
 */
public enum ColonneDossier implements Colonne {

    // -- Colonnes historiques : la sélection par défaut, à ne pas modifier.
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("id")),
    CODE("code", "Code", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("code")),
    DATE_DOCUMENT("dateDocument", "Date", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("docDate")),
    STATUT("statut", "Statut", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("strStatus.code")),
    CLIENT_ID("clientId", "Id client", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgCustomer.id")),
    CLIENT_NOM("clientNom", "Client", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("cfgCustomer")),
    COMMERCIAL_ID("commercialId", "Id commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgCommercial.id")),
    COMMERCIAL_NOM("commercialNom", "Commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("cfgCommercial")),
    PRIX("prix", "Prix", Groupe.FINANCIER, TypeColonne.MONTANT, true,
            Chemin.groupableEtAgregeable("price")),
    TOTAL_A_PAYER("totalAPayer", "Total à payer", Groupe.FINANCIER, TypeColonne.MONTANT, true,
            Chemin.groupableEtAgregeable("totalToPay")),
    DATE_CONCRETISATION("dateConcretisation", "Concrétisation", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("advanceDate")),
    PROSPECTION_ID("prospectionId", "Id prospection", Groupe.ORIGINE, TypeColonne.TEXTE, true,
            Chemin.groupable("comProspection.id")),
    PROSPECTION_CODE("prospectionCode", "Prospection", Groupe.ORIGINE, TypeColonne.TEXTE, true,
            Chemin.groupable("comProspection.code")),

    // -- Financier, déjà servi par la fiche. Scalaires de ComFolder : coût nul.
    // `REMISE` et `PROBABILITE_FINALISATION` sont des ratios en fraction (1.00 = 100 %) : sommables
    // quand même, c'est au client de choisir ce qu'il additionne.
    PRIX_AVANT_REMISE("prixAvantRemise", "Prix avant remise", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("priceBeforeDiscount")),
    REMISE("remise", "Remise (%)", Groupe.FINANCIER, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("discount")),
    REMISE_MONTANT("remiseMontant", "Remise", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("discountAmount")),
    PRIX_APRES_REMISE("prixApresRemise", "Prix après remise", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("priceAfterDiscount")),
    MONTANT_AVANCE("montantAvance", "Avance", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("agreementAmount")),
    PROBABILITE_FINALISATION("probabiliteFinalisation", "Probabilité", Groupe.FINANCIER, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("probabilityOfFinalisation")),

    // -- Contexte patrimonial, via `stkItem` (article principal) qui est NULLABLE : le groupement
    //    doit le joindre en LEFT explicite, sinon INNER JOIN implicite et groupes perdus.
    PROJET_ID("projetId", "Id projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.id")),
    PROJET_CODE("projetCode", "Projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.code")),
    PROJET_LIBELLE("projetLibelle", "Libellé projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.description")),
    TRANCHE_ID("trancheId", "Id tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.id")),
    TRANCHE_CODE("trancheCode", "Tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.code")),
    TRANCHE_LIBELLE("trancheLibelle", "Libellé tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.description")),
    SOCIETE_ID("societeId", "Id société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.cfgCompany.id")),
    SOCIETE_CODE("societeCode", "Société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.cfgCompany.code")),
    SOCIETE_LIBELLE("societeLibelle", "Libellé société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.cfgCompany.description")),
    VOCATION_CODE("vocationCode", "Vocation", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkVocation.code")),
    VOCATION_LIBELLE("vocationLibelle", "Libellé vocation", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkVocation.description")),

    // -- Acteurs.
    AVOCAT_ID("avocatId", "Id notaire", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgLawyer.id")),
    AVOCAT_NOM("avocatNom", "Notaire", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("cfgLawyer")),
    MODE_PAIE("modePaie", "Mode de paiement", Groupe.FINANCIER, TypeColonne.TEXTE, false,
            Chemin.groupable("comPaymentMethod.code")),

    // -- Origine, depuis la prospection. `etapeProspection` lit ComProspectionKind, comme le mappeur de liste.
    ETAPE_PROSPECTION("etapeProspection", "Étape prospection", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comProspectionKind.description")),
    ORIGINE_PROSPECTION("origineProspection", "Origine", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comProspectionOrigin.description")),
    DATE_PROSPECTION("dateProspection", "Date prospection", Groupe.ORIGINE, TypeColonne.DATE, false,
            Chemin.date("comProspection.docDate")),

    // -- Piste d'audit.
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userCreate")),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userCreateDateTime")),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userUpdate")),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userUpdateDateTime")),

    // -- Article principal : l'écran légataire trie par `ArticleCode, H1Code, H2Code, H3Code`
    //    (H1/H2/H3 = tranche, projet, société). Voir FEATURE-colonnes-dossier-ecran-legataire.md §1.
    ARTICLE_ID("articleId", "Id article", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.id")),
    /** La clé de tri par défaut de l'écran légataire. */
    ARTICLE_CODE("articleCode", "Article", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.code")),
    ARTICLE_LIBELLE("articleLibelle", "Libellé article", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.description")),
    ARTICLE_TITRE_FONCIER("articleTitreFoncier", "Titre foncier", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.titleNumber")),
    BLOC("bloc", "Bloc", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkBuilding.description")),
    ETAGE("etage", "Étage", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkFloor.description")),
    CATEGORIE_ARTICLE("categorieArticle", "Catégorie", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkItemCategory.code")),
    TYPE_ARTICLE("typeArticle", "Type", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkItemType.code")),
    TYPE_PRESENTATION("typePresentation", "Présentation", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkPresentationType.code")),

    // -- Responsable de vente, distinct du commercial. `CfgSaleManager2Id` non repris (vide).
    RESPONSABLE_VENTE_ID("responsableVenteId", "Id responsable de vente", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgSaleManager.id")),
    RESPONSABLE_VENTE_NOM("responsableVenteNom", "Responsable de vente", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("cfgSaleManager")),

    // -- Jalons et délais. Les délais sont des @Formula (FEATURE-colonnes-dossier-ecran-legataire.md §3) ;
    //    les deux dates en sont les entrées, sans elles un nombre de jours est invérifiable à l'écran.
    DATE_COMPROMIS("dateCompromis", "Compromis", Groupe.IDENTITE, TypeColonne.DATE, false,
            Chemin.date("agreementDate")),
    DATE_LIVRAISON_REELLE("dateLivraisonReelle", "Livraison réelle", Groupe.IDENTITE, TypeColonne.DATE, false,
            Chemin.date("deliveryDateReal")),
    /**
     * Non groupable : sa formule contient une sous-requête corrélée, que SQL Server refuse dans un
     * GROUP BY (SQLGrammarException en 500). Les deux autres délais n'en ont pas.
     */
    DELAI_CONCRETISATION("delaiConcretisation", "Délai de concrétisation (j)", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            Chemin.agregeable("delaiConcretisation")),
    DELAI_NOTAIRE("delaiNotaire", "Délai notaire (j)", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("delaiNotaire")),
    DELAI_LIVRAISON("delaiLivraison", "Délai de livraison (j)", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("delaiLivraison"));

    /** {@code ARTICLE} ≠ {@code PATRIMOINE} : {@link #exigeLePatrimoine} ferait payer trois jointures pour un code d'article. */
    public enum Groupe { IDENTITE, ACTEURS, FINANCIER, PATRIMOINE, ARTICLE, DELAIS, ORIGINE, AUDIT }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson qui porte la sélection de colonnes des dossiers. */
    public static final String FILTRE = "colonnesDossier";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;
    private final Chemin chemin;

    /** Colonne ni groupable ni agrégeable — le cas par défaut, cf. {@link Colonne#chemin()}. */
    ColonneDossier(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut) {
        this(nom, libelle, groupe, type, parDefaut, null);
    }

    ColonneDossier(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut,
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

    public static List<ColonneDossier> groupables() {
        return Colonnes.groupables(ColonneDossier.class);
    }

    public static List<ColonneDossier> agregeables() {
        return Colonnes.agregeables(ColonneDossier.class);
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneDossier resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneDossier.class, demandee);
    }

    /** Forme {@code colonne} (somme) ou {@code colonne:FONCTION}. */
    public static List<AgregatDemande> resoudreAgregats(List<String> demandees) {
        return Colonnes.resoudreAgregats(ColonneDossier.class, demandees);
    }

    public static Set<ColonneDossier> parDefautSet() {
        return Colonnes.parDefaut(ColonneDossier.class);
    }

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneDossier.class);
    }

    public static List<String> nomsValides() {
        return Colonnes.nomsValides(ColonneDossier.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneDossier> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneDossier.class, demandees, ID);
    }

    /** Sans ce test, la liste par défaut paierait trois jointures paresseuses par ligne. */
    public static boolean exigeLePatrimoine(Set<ColonneDossier> selection) {
        return selection.stream().anyMatch(c -> c.groupe == Groupe.PATRIMOINE);
    }

    /** Vrai si la sélection oblige à déréférencer la prospection d'origine. */
    public static boolean exigeLaProspection(Set<ColonneDossier> selection) {
        return selection.contains(ETAPE_PROSPECTION)
                || selection.contains(ORIGINE_PROSPECTION)
                || selection.contains(DATE_PROSPECTION);
    }

    /** L'article et ses référentiels : jusqu'à cinq jointures paresseuses par ligne. */
    public static boolean exigeLArticle(Set<ColonneDossier> selection) {
        return selection.stream().anyMatch(c -> c.groupe == Groupe.ARTICLE);
    }
}
