package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes sélectionnables de {@code GET /api/dossiers}.
 *
 * <p>C'est la <strong>liste blanche</strong> du paramètre {@code ?colonnes=} et, exposée par
 * {@code GET /api/referentiels/colonnes/dossiers}, la source du sélecteur de colonnes côté front.
 *
 * <h2>Pourquoi ce catalogue existe</h2>
 *
 * <p>La liste des dossiers servait <strong>13 colonnes</strong> quand la fiche en sert une
 * quarantaine, et le logiciel de bureau davantage encore : {@code ComSaleFolderView} porte 217
 * colonnes. Voir {@code instructions/FEATURE-selecteur-colonnes-prospect-dossier.md}.
 *
 * <h2>Ce qui n'y figure pas, et pourquoi</h2>
 *
 * <p><strong>Les articles.</strong> Ils exigent une requête <em>par dossier</em>
 * ({@code StkItemRepository.findByComFolderId}) : les servir en liste ferait un N+1 de 20
 * requêtes par page. Ils restent sur la fiche.
 *
 * <p><strong>La ventilation financière du légataire.</strong> Mesurée sur les 358 dossiers réels,
 * 5 postes seulement sur 12 sont vivants, et le bloc {@code Agreement*} diverge du bloc principal
 * sur 20 % des lignes sans qu'on sache lequel fait foi — décision métier ouverte. Un catalogue
 * n'est pas le bon endroit pour trancher ça.
 *
 * <p><strong>{@code notes}.</strong> {@code ComFolder.Notes} est mesurée à <strong>0 ligne sur
 * 358</strong> : la servir en liste ajouterait une colonne toujours vide au sélecteur.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 */
public enum ColonneDossier implements Colonne {

    // -- Colonnes historiques : la sélection par défaut, à ne pas modifier.
    //
    // Les `*_ID` sont groupables au meme titre que le reste : leur jumelle `*_NOM` donne la
    // meme partition avec un libelle lisible, mais c'est au client de choisir laquelle il
    // affiche. Les refuser revenait a decider a sa place.
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
    //
    // Tous agrégeables, aucun groupable : `Price` porte 190 valeurs distinctes sur 358 dossiers.
    // `REMISE` et `PROBABILITE_FINALISATION` sont des RATIOS stockés en fraction (1.00 = 100 %) —
    // leur somme n'a pas de sens métier, mais c'est au client de choisir ce qu'il additionne, et
    // les exclure ferait une exception à expliquer pour un gain nul.
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

    // -- Contexte patrimonial. Chaîne article → tranche → projet → société : jointures paresseuses,
    //    payées seulement si demandées.
    //
    // Les chemins traversent `stkItem` (l'article principal), qui est NULLABLE. Le moteur de
    // groupement doit donc les joindre en LEFT explicite : une navigation implicite produirait
    // des INNER JOIN et ferait disparaître des groupes entiers — le piège déjà rencontré sur le
    // filtre `trancheId` de DossierService (voir les commentaires de buildSpec).
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

    // -- Origine, depuis la prospection. 99,2 % des dossiers en ont une.
    // `etapeProspection` lit bien ComProspectionKind : c'est ce que fait déjà le mappeur de liste.
    ETAPE_PROSPECTION("etapeProspection", "Étape prospection", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comProspectionKind.description")),
    ORIGINE_PROSPECTION("origineProspection", "Origine", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comProspectionOrigin.description")),
    DATE_PROSPECTION("dateProspection", "Date prospection", Groupe.ORIGINE, TypeColonne.DATE, false,
            Chemin.date("comProspection.docDate")),

    // -- Piste d'audit. Scalaires, 100 % en base réelle.
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userCreate")),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userCreateDateTime")),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userUpdate")),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userUpdateDateTime")),

    // ------------------------------------------------------------------
    // L'ARTICLE PRINCIPAL. Trouvé par la trace d'audit du logiciel de bureau (sys.fn_get_audit_file
    // sur C:\backup\*.sqlaudit) : sa liste des dossiers trie par
    // `ORDER BY ArticleCode, H1Code, H2Code, H3Code`, et nous ne servions AUCUNE de ces colonnes
    // d'article. H1/H2/H3 sont en revanche déjà servis — ce sont la tranche, le projet et la
    // société, sous d'autres noms.
    //
    // Chaîne `stkItem` → bâtiment / étage / catégorie / type / présentation : une jointure
    // paresseuse par ligne, payée seulement si demandée (voir exigeLArticle).
    // ------------------------------------------------------------------
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

    // ------------------------------------------------------------------
    // LE RESPONSABLE DE VENTE — acteur distinct du commercial, renseigné sur 221 des 358 dossiers
    // (61 %), et que nous n'exposions nulle part. `CfgSaleManager2Id` n'est PAS repris : mesuré à
    // 0 ligne sur 358.
    // ------------------------------------------------------------------
    RESPONSABLE_VENTE_ID("responsableVenteId", "Id responsable de vente", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgSaleManager.id")),
    RESPONSABLE_VENTE_NOM("responsableVenteNom", "Responsable de vente", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("cfgSaleManager")),

    // ------------------------------------------------------------------
    // JALONS ET DÉLAIS DU CYCLE DE VENTE. Les deux dates sont les entrées des délais : les servir
    // sans elles rendrait un nombre de jours invérifiable à l'écran.
    //
    // Les délais sont des @Formula sur l'entité, donc des propriétés ordinaires : groupables et
    // sommables comme n'importe quelle colonne. `DelayCredit` de la vue est écarté — il dépend de
    // CreditPossessionDate, mesurée à 0 ligne sur 358.
    // ------------------------------------------------------------------
    DATE_COMPROMIS("dateCompromis", "Compromis", Groupe.IDENTITE, TypeColonne.DATE, false,
            Chemin.date("agreementDate")),
    DATE_LIVRAISON_REELLE("dateLivraisonReelle", "Livraison réelle", Groupe.IDENTITE, TypeColonne.DATE, false,
            Chemin.date("deliveryDateReal")),
    /**
     * Sommable mais <strong>non groupable</strong>, et ce n'est pas un choix : sa formule contient
     * une sous-requête corrélée (la date de la prospection d'origine), et SQL Server refuse
     * « un agrégat ou une sous-requête dans une expression utilisée dans la liste GROUP BY ».
     * Le mettre groupable produisait une SQLGrammarException en 500.
     *
     * <p>Les deux autres délais n'ont pas ce problème : ils n'arithmétisent que des colonnes de
     * {@code ComFolder}, donc ils sont groupables normalement.
     */
    DELAI_CONCRETISATION("delaiConcretisation", "Délai de concrétisation (j)", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            Chemin.agregeable("delaiConcretisation")),
    DELAI_NOTAIRE("delaiNotaire", "Délai notaire (j)", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("delaiNotaire")),
    DELAI_LIVRAISON("delaiLivraison", "Délai de livraison (j)", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("delaiLivraison"));

    /**
     * Regroupement d'affichage du sélecteur.
     *
     * <p>{@code ARTICLE} et {@code DELAIS} sont ajoutés au lot « colonnes de l'écran légataire ».
     * {@code ARTICLE} est distinct de {@code PATRIMOINE} pour une raison concrète et non
     * cosmétique : {@link #exigeLePatrimoine} déclenche la résolution de la chaîne tranche →
     * projet → société, dont les colonnes d'article n'ont pas besoin. Les confondre ferait payer
     * trois jointures à qui ne demande qu'un code d'article.
     */
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

    /** Colonnes acceptées par {@code ?groupBy=}, dans l'ordre de déclaration. */
    public static List<ColonneDossier> groupables() {
        return Colonnes.groupables(ColonneDossier.class);
    }

    /** Colonnes acceptées par {@code ?agregats=}, dans l'ordre de déclaration. */
    public static List<ColonneDossier> agregeables() {
        return Colonnes.agregeables(ColonneDossier.class);
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneDossier resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneDossier.class, demandee);
    }

    /**
     * Résout {@code ?agregats=}, forme {@code colonne} (somme) ou {@code colonne:FONCTION}.
     *
     * @throws ColonneInconnueException         si un nom n'est pas une colonne de ce catalogue
     * @throws FonctionAgregatInconnueException si la colonne n'autorise pas cette fonction
     */
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

    /**
     * Vrai si la sélection oblige à traverser la chaîne article → tranche → projet → société.
     *
     * <p>Sans ce test, une liste par défaut paierait trois jointures paresseuses par ligne pour des
     * colonnes que personne n'a demandées.
     */
    public static boolean exigeLePatrimoine(Set<ColonneDossier> selection) {
        return selection.stream().anyMatch(c -> c.groupe == Groupe.PATRIMOINE);
    }

    /** Vrai si la sélection oblige à déréférencer la prospection d'origine. */
    public static boolean exigeLaProspection(Set<ColonneDossier> selection) {
        return selection.contains(ETAPE_PROSPECTION)
                || selection.contains(ORIGINE_PROSPECTION)
                || selection.contains(DATE_PROSPECTION);
    }

    /**
     * Vrai si la sélection oblige à déréférencer l'article principal.
     *
     * <p>Même discipline que les deux gardes ci-dessus : l'article et ses référentiels (bâtiment,
     * étage, catégorie, type, présentation) sont jusqu'à cinq jointures paresseuses par ligne,
     * soit cent requêtes inutiles sur une page de vingt quand personne ne les réclame.
     */
    public static boolean exigeLArticle(Set<ColonneDossier> selection) {
        return selection.stream().anyMatch(c -> c.groupe == Groupe.ARTICLE);
    }
}
