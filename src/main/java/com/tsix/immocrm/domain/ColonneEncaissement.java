package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes sélectionnables de {@code GET /api/encaissements}.
 *
 * <p>C'est la <strong>liste blanche</strong> du paramètre {@code ?colonnes=} et, exposée par
 * {@code GET /api/referentiels/colonnes/encaissements}, la source du sélecteur de colonnes côté
 * front — qui n'a donc aucune liste à coder en dur. Porte aussi les {@link Chemin} qui rendent
 * {@code GET /api/encaissements/groupes} possible sans une ligne de plus dans
 * {@code GroupementService}.
 *
 * <h2>Comment ce catalogue a été établi</h2>
 *
 * <p>L'écran légataire {@code Yes.Com.Front.Credit.SettlementList} émet un
 * {@code SELECT StlSettlementView.*} — <strong>toute la vue, 107 colonnes</strong>, sans en nommer
 * aucune. Et sa disposition de grille n'est pas en base : aucune table {@code *Layout*} /
 * {@code *Grid*} / {@code *Column*} ne la porte. La trace d'audit ne peut donc pas dire quelles
 * colonnes l'écran affiche.
 *
 * <p>Le critère retenu est la <strong>mesure</strong>, sur exactement la population que
 * l'endpoint sert (Vente + {@code IsCredit = 1}, 643 règlements dans {@code yesRemsProd}) —
 * valeurs <em>distinctes</em> et, pour les numériques, <em>non-zéro</em>. Jamais
 * {@code COUNT(colonne)}, qui compte les non-nuls et a déjà trompé trois fois ce dépôt. Le détail
 * est dans {@code instructions/FEATURE-colonnes-groupement-encaissements.md}.
 *
 * <h2>Trois choses à savoir avant d'en ajouter</h2>
 *
 * <p><strong>1. Le défaut est intouchable.</strong> {@code parDefaut = true} reproduit exactement
 * les 27 champs servis avant ce lot. Y ajouter quoi que ce soit rompt le contrat des clients
 * existants, qui verraient apparaître des clés qu'ils n'ont pas demandées.
 *
 * <p><strong>2. Une colonne pleine n'est pas une colonne utile.</strong> {@code Category},
 * {@code ItemNumber} et {@code ItemDescription} sont renseignées sur les 643 lignes et ne portent
 * <em>qu'une seule valeur distincte</em> ({@code None}, {@code Non définie}, {@code Non définie}) :
 * des placeholders. {@code Group1Label} / {@code Group2Label} / {@code Group3Label} — les colonnes
 * de groupement du légataire lui-même — sont à <strong>0 sur 643</strong>. Aucune n'est au
 * catalogue, et c'est mesuré, pas supposé.
 *
 * <p><strong>3. Un {@code nvarchar(max)} ne peut pas être groupable.</strong> {@link #NOTES} n'a
 * pas de chemin pour cette raison technique : {@code GroupementService#compterGroupes} fait un
 * {@code COUNT(DISTINCT clé)}, que SQL Server refuse sur {@code nvarchar(max)}. {@link #DESCRIPTION}
 * est en {@code nvarchar(255)} et ne pose pas ce problème.
 *
 * <h2>Ce qui n'est pas groupable, et pourquoi c'est structurel</h2>
 *
 * <p>{@link #DOSSIER_ID} et {@link #DOSSIER_CODE} <strong>n'ont pas de chemin</strong>. Un
 * règlement n'a pas de dossier : il a des <em>lignes</em>, chacune rattachée à une échéance donc à
 * un dossier. Mesuré : 643 règlements, <strong>654</strong> couples (règlement, dossier),
 * <strong>8 règlements portant sur plusieurs dossiers</strong>. Grouper par ce chemin traverserait
 * une association <em>to-many</em> et compterait ces 8 plusieurs fois — la somme des effectifs
 * rendrait 654 là où la liste annonce 643, brisant l'invariant testé par
 * {@code DossierGroupementTest#sommeDesEffectifs_egaleLeTotalDeLaListe}. Même raisonnement pour
 * une tranche ou un projet, qui se résolvent par le même chemin. Les deux colonnes restent
 * <em>sélectionnables</em> ; elles ne sont pas groupables.
 *
 * <p>{@link #MONTANT_BRUT_RESTANT} et {@link #MONTANT_NET} l'étaient aussi, et ne le sont plus :
 * ce sont les deux colonnes que la bande « Total : » du légataire totalise, donc les laisser hors
 * d'atteinte revenait à ne pas reproduire l'écran. Ils sont désormais portés par la base — une
 * {@code @Formula} pour l'un, une pré-agrégation joignable pour l'autre — et leur javadoc explique
 * pourquoi les deux mécanismes diffèrent.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 */
public enum ColonneEncaissement implements Colonne {

    // ------------------------------------------------------------------
    // Colonnes historiques : la sélection par défaut, à ne pas modifier.
    //
    // Les `*_ID` sont groupables au même titre que leur jumelle `*_NOM`/`*_CODE` : même partition,
    // libellé différent, et c'est au client de choisir laquelle il affiche (décision du lot H).
    // ------------------------------------------------------------------
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("id")),
    REFERENCE("reference", "Référence", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("code")),
    ETAT("etat", "État", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("status")),

    /**
     * Sélectionnable mais <strong>pas groupable</strong> — voir l'entête : le dossier se résout par
     * les lignes du règlement, une association to-many.
     */
    DOSSIER_ID("dossierId", "Id dossier", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    /** Même raison que {@link #DOSSIER_ID}. */
    DOSSIER_CODE("dossierCode", "Dossier", Groupe.IDENTITE, TypeColonne.TEXTE, true),

    DATE_DOCUMENT("dateDocument", "Date pièce", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("docDate")),
    DATE_ECHEANCE("dateEcheance", "Date échéance", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("echeanceDate")),
    /** Mesurée à <strong>10 lignes sur 643</strong> (1,6 %) : presque toujours vide. */
    DATE_REGLEMENT("dateReglement", "Date de règlement", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("settlementDate")),

    TIER_ID("tierId", "Id tiers", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("tier.id")),
    TIER_NOM("tierNom", "Tiers", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("tier")),

    MONTANT_BRUT("montantBrut", "Montant brut", Groupe.FINANCIER, TypeColonne.MONTANT, true,
            Chemin.groupableEtAgregeable("amount")),
    /**
     * Brut moins la part déjà affectée à des échéances.
     *
     * <p><strong>Sommable, pas groupable.</strong> La part affectée est pré-agrégée dans
     * {@code StlSettlementAffectation} et retranchée par jointure — une {@code @Formula} à
     * sous-requête corrélée aurait donné la valeur ligne à ligne mais aurait rendu tout
     * {@code SUM()} impossible (SQL Server, erreur 130). Voir {@link Chemin#difference}.
     *
     * <p>Ni {@code MIN} ni {@code MAX} : le moteur agrège les deux termes séparément, ce qui est
     * exact pour un total et une moyenne, faux pour un extremum. Mesuré à <strong>zéro sur les 643
     * règlements réels</strong> — tous sont intégralement affectés, ce que l'écran légataire
     * affiche d'ailleurs aussi (« 0.00 DH » sur chaque ligne).
     */
    MONTANT_BRUT_RESTANT("montantBrutRestant", "Montant brut restant", Groupe.FINANCIER,
            TypeColonne.MONTANT, true,
            Chemin.difference("amount", "affectation.montantAffecte")),
    /**
     * Brut moins retenue à la source et droit de timbre.
     *
     * <p>Porté par une {@code @Formula} sur {@code StlSettlement}, donc une propriété ordinaire :
     * groupable <em>et</em> agrégeable sans une ligne dans le moteur. Mesuré <strong>égal au
     * montant brut sur les 643 lignes</strong>, les deux retenues valant zéro partout.
     */
    MONTANT_NET("montantNet", "Montant net", Groupe.FINANCIER, TypeColonne.MONTANT, true,
            Chemin.groupableEtAgregeable("montantNetCalcule")),

    COMPTABILISE("comptabilise", "Comptabilisé", Groupe.SUIVI, TypeColonne.BOOLEEN, true,
            Chemin.groupable("isAccounted")),

    TYPE_ID("typeId", "Id type", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("stlSettlementType.id")),
    TYPE_CODE("typeCode", "Code type", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("stlSettlementType.code")),
    TYPE_LIBELLE("typeLibelle", "Mode de règlement", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("stlSettlementType.description")),

    ENTITE_ID("entiteId", "Id entité", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgCompany.id")),
    ENTITE_CODE("entiteCode", "Code entité", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgCompany.code")),
    ENTITE_LIBELLE("entiteLibelle", "Entité", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgCompany.description")),

    /** Mesurée à <strong>12 lignes sur 643</strong> (1,9 %). */
    COMPTE_ID("compteId", "Id compte", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlAccount.id")),
    /** Même mesure que {@link #COMPTE_ID}. */
    COMPTE_CODE("compteCode", "Compte", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlAccount.code")),

    /**
     * Mesurée à <strong>0 ligne sur 643</strong> : toujours vide sur le périmètre Vente. Conservée
     * au catalogue parce que le contrat la publie déjà, mais elle ne doit pas être proposée comme
     * un filtre utile.
     */
    DEPOT_ID("depotId", "Id dépôt", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlDeposit.id")),
    /** Même mesure que {@link #DEPOT_ID} : 0 sur 643. */
    DEPOT_CODE("depotCode", "Dépôt", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlDeposit.code")),

    BANQUE_ID("banqueId", "Id banque", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgBank.id")),
    BANQUE_NOM("banqueNom", "Banque", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("cfgBank")),

    /**
     * Sans chemin, et pour une raison <strong>technique</strong>, pas de cardinalité :
     * {@code StlSettlement.Notes} est un {@code nvarchar(max)}, que le {@code COUNT(DISTINCT)} du
     * comptage de groupes refuse. Mesurée à 3 lignes sur 643 par ailleurs.
     */
    NOTES("notes", "Notes", Groupe.SUIVI, TypeColonne.TEXTE, true),

    // ------------------------------------------------------------------
    // Ajouts de ce lot. Tous hors défaut, tous scalaires ou associations déjà mappées :
    // le coût d'une colonne non demandée est nul.
    // ------------------------------------------------------------------

    /**
     * Libellé du règlement — <strong>643 / 643, 530 valeurs distinctes</strong>. Le manque le plus
     * net du DTO d'origine : le légataire le sert jusque dans sa grille de détail des affectations.
     */
    DESCRIPTION("description", "Libellé", Groupe.IDENTITE, TypeColonne.TEXTE, false,
            Chemin.groupable("description")),

    /** {@code AmountBase} — 643 / 643, tous non nuls et tous non-zéro, 200 valeurs distinctes. */
    MONTANT_BASE("montantBase", "Montant base", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("amountBase")),

    /**
     * Tiers <em>payeur</em>, distinct du tiers du règlement — 643 / 643, mais ils ne divergent que
     * sur <strong>7 lignes</strong>. Servi parce que le légataire le distingue ; à ne pas présenter
     * comme un axe d'analyse à part entière.
     */
    TIER_PAYEUR_ID("tierPayeurId", "Id tiers payeur", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.groupable("tierPayer.id")),
    /** Voir {@link #TIER_PAYEUR_ID}. */
    TIER_PAYEUR_NOM("tierPayeurNom", "Tiers payeur", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("tierPayer")),

    /** {@code IsDcoumentDeliveredToCenter} — 643 / 643, 2 valeurs. La faute de frappe est celle du schéma légataire. */
    REMIS_AU_CENTRE("remisAuCentre", "Remis au centre", Groupe.SUIVI, TypeColonne.BOOLEEN, false,
            Chemin.groupable("isDcoumentDeliveredToCenter")),

    /** Mesurée à 19 lignes sur 643 (3 %). */
    RECU_CODE("recuCode", "N° de reçu", Groupe.SUIVI, TypeColonne.TEXTE, false,
            Chemin.groupable("receiptCode")),
    /** Mesurée à 4 lignes sur 643. */
    DATE_REMISE_CENTRE("dateRemiseAuCentre", "Date de remise au centre", Groupe.SUIVI,
            TypeColonne.DATE, false,
            Chemin.date("deliveredToCenterDate")),
    /** Mesurée à 3 lignes sur 643. */
    BORDEREAU_CODE("bordereauCode", "Bordereau", Groupe.SUIVI, TypeColonne.TEXTE, false,
            Chemin.groupable("slipCode")),
    /** Mesurée à 4 lignes sur 643. */
    DATE_BORDEREAU("dateBordereau", "Date bordereau", Groupe.SUIVI, TypeColonne.DATE, false,
            Chemin.date("slipDate")),

    // -- Piste d'audit. Scalaires, 643 / 643 — même traitement que ColonneProspect.
    //
    // `creePar` porte 17 valeurs distinctes sur 643 lignes : c'est un bon axe de groupement
    // (« qui a saisi le règlement »), pas seulement une colonne d'affichage.
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userCreate")),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userCreateDateTime")),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userUpdate")),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userUpdateDateTime"));

    /** Regroupement d'affichage du sélecteur. */
    public enum Groupe { IDENTITE, ACTEURS, FINANCIER, SUIVI, AUDIT }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson qui porte la sélection de colonnes des encaissements. */
    public static final String FILTRE = "colonnesEncaissement";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;
    private final Chemin chemin;

    /** Colonne ni groupable ni agrégeable — cf. {@link Colonne#chemin()}. */
    ColonneEncaissement(String nom, String libelle, Groupe groupe, TypeColonne type,
                        boolean parDefaut) {
        this(nom, libelle, groupe, type, parDefaut, null);
    }

    ColonneEncaissement(String nom, String libelle, Groupe groupe, TypeColonne type,
                        boolean parDefaut, Chemin chemin) {
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
    public static List<ColonneEncaissement> groupables() {
        return Colonnes.groupables(ColonneEncaissement.class);
    }

    /** Colonnes acceptées par {@code ?agregats=}, dans l'ordre de déclaration. */
    public static List<ColonneEncaissement> agregeables() {
        return Colonnes.agregeables(ColonneEncaissement.class);
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneEncaissement resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneEncaissement.class, demandee);
    }

    /**
     * Résout {@code ?agregats=}, forme {@code colonne} (somme) ou {@code colonne:FONCTION}.
     *
     * @throws ColonneInconnueException         si un nom n'est pas une colonne de ce catalogue
     * @throws FonctionAgregatInconnueException si la colonne n'autorise pas cette fonction
     */
    public static List<AgregatDemande> resoudreAgregats(List<String> demandees) {
        return Colonnes.resoudreAgregats(ColonneEncaissement.class, demandees);
    }

    public static Set<ColonneEncaissement> parDefautSet() {
        return Colonnes.parDefaut(ColonneEncaissement.class);
    }

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneEncaissement.class);
    }

    public static List<String> nomsValides() {
        return Colonnes.nomsValides(ColonneEncaissement.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneEncaissement> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneEncaissement.class, demandees, ID);
    }
}
