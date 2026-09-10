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
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    CODE("code", "Code", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    DATE_DOCUMENT("dateDocument", "Date", Groupe.IDENTITE, TypeColonne.DATE, true),
    STATUT("statut", "Statut", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    CLIENT_ID("clientId", "Id client", Groupe.ACTEURS, TypeColonne.TEXTE, true),
    CLIENT_NOM("clientNom", "Client", Groupe.ACTEURS, TypeColonne.TEXTE, true),
    COMMERCIAL_ID("commercialId", "Id commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true),
    COMMERCIAL_NOM("commercialNom", "Commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true),
    PRIX("prix", "Prix", Groupe.FINANCIER, TypeColonne.MONTANT, true),
    TOTAL_A_PAYER("totalAPayer", "Total à payer", Groupe.FINANCIER, TypeColonne.MONTANT, true),
    DATE_CONCRETISATION("dateConcretisation", "Concrétisation", Groupe.IDENTITE, TypeColonne.DATE, true),
    PROSPECTION_ID("prospectionId", "Id prospection", Groupe.ORIGINE, TypeColonne.TEXTE, true),
    PROSPECTION_CODE("prospectionCode", "Prospection", Groupe.ORIGINE, TypeColonne.TEXTE, true),

    // -- Financier, déjà servi par la fiche. Scalaires de ComFolder : coût nul.
    PRIX_AVANT_REMISE("prixAvantRemise", "Prix avant remise", Groupe.FINANCIER, TypeColonne.MONTANT, false),
    REMISE("remise", "Remise (%)", Groupe.FINANCIER, TypeColonne.NOMBRE, false),
    REMISE_MONTANT("remiseMontant", "Remise", Groupe.FINANCIER, TypeColonne.MONTANT, false),
    PRIX_APRES_REMISE("prixApresRemise", "Prix après remise", Groupe.FINANCIER, TypeColonne.MONTANT, false),
    MONTANT_AVANCE("montantAvance", "Avance", Groupe.FINANCIER, TypeColonne.MONTANT, false),
    PROBABILITE_FINALISATION("probabiliteFinalisation", "Probabilité", Groupe.FINANCIER, TypeColonne.NOMBRE, false),

    // -- Contexte patrimonial. Chaîne article → tranche → projet → société : jointures paresseuses,
    //    payées seulement si demandées.
    PROJET_ID("projetId", "Id projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    PROJET_CODE("projetCode", "Projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    PROJET_LIBELLE("projetLibelle", "Libellé projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    TRANCHE_ID("trancheId", "Id tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    TRANCHE_CODE("trancheCode", "Tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    TRANCHE_LIBELLE("trancheLibelle", "Libellé tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    SOCIETE_ID("societeId", "Id société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    SOCIETE_CODE("societeCode", "Société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    SOCIETE_LIBELLE("societeLibelle", "Libellé société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    VOCATION_CODE("vocationCode", "Vocation", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),
    VOCATION_LIBELLE("vocationLibelle", "Libellé vocation", Groupe.PATRIMOINE, TypeColonne.TEXTE, false),

    // -- Acteurs.
    AVOCAT_ID("avocatId", "Id notaire", Groupe.ACTEURS, TypeColonne.TEXTE, false),
    AVOCAT_NOM("avocatNom", "Notaire", Groupe.ACTEURS, TypeColonne.TEXTE, false),
    MODE_PAIE("modePaie", "Mode de paiement", Groupe.FINANCIER, TypeColonne.TEXTE, false),

    // -- Origine, depuis la prospection. 99,2 % des dossiers en ont une.
    ETAPE_PROSPECTION("etapeProspection", "Étape prospection", Groupe.ORIGINE, TypeColonne.TEXTE, false),
    ORIGINE_PROSPECTION("origineProspection", "Origine", Groupe.ORIGINE, TypeColonne.TEXTE, false),
    DATE_PROSPECTION("dateProspection", "Date prospection", Groupe.ORIGINE, TypeColonne.DATE, false),

    // -- Piste d'audit. Scalaires, 100 % en base réelle.
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false);

    /** Regroupement d'affichage du sélecteur. */
    public enum Groupe { IDENTITE, ACTEURS, FINANCIER, PATRIMOINE, ORIGINE, AUDIT }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson qui porte la sélection de colonnes des dossiers. */
    public static final String FILTRE = "colonnesDossier";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;

    ColonneDossier(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut) {
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
    public boolean parDefaut() {
        return parDefaut;
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
}
