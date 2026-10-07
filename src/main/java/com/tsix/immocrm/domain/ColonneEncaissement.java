package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/encaissements} : liste blanche de {@code ?colonnes=}.
 * Colonnes retenues ou écartées sur mesure : FEATURE-colonnes-groupement-encaissements.md §3–§5.
 */
public enum ColonneEncaissement implements Colonne {

    // -- Colonnes historiques : la sélection par défaut, à ne pas modifier (contrat publié).
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("id")),
    REFERENCE("reference", "Référence", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("code")),
    ETAT("etat", "État", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("status")),

    /**
     * Pas groupable : le dossier se résout par les lignes du règlement (to-many), un règlement
     * compterait dans plusieurs groupes — FEATURE-colonnes-groupement-encaissements.md §5.2.
     */
    DOSSIER_ID("dossierId", "Id dossier", Groupe.IDENTITE, TypeColonne.TEXTE, true),
    /** Même raison que {@link #DOSSIER_ID}. */
    DOSSIER_CODE("dossierCode", "Dossier", Groupe.IDENTITE, TypeColonne.TEXTE, true),

    DATE_DOCUMENT("dateDocument", "Date pièce", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("docDate")),
    DATE_ECHEANCE("dateEcheance", "Date échéance", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("echeanceDate")),
    DATE_REGLEMENT("dateReglement", "Date de règlement", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("settlementDate")),

    TIER_ID("tierId", "Id tiers", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("tier.id")),
    TIER_NOM("tierNom", "Tiers", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("tier")),

    MONTANT_BRUT("montantBrut", "Montant brut", Groupe.FINANCIER, TypeColonne.MONTANT, true,
            Chemin.groupableEtAgregeable("amount")),
    /**
     * Brut moins la part déjà affectée aux échéances, pré-agrégée et retranchée par jointure
     * (voir {@link Chemin#difference}) : sommable, ni groupable ni MIN/MAX.
     */
    MONTANT_BRUT_RESTANT("montantBrutRestant", "Montant brut restant", Groupe.FINANCIER,
            TypeColonne.MONTANT, true,
            Chemin.difference("amount", "affectation.montantAffecte")),
    /** Brut moins retenue à la source et droit de timbre ; {@code @Formula}, donc une propriété ordinaire. */
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

    COMPTE_ID("compteId", "Id compte", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlAccount.id")),
    COMPTE_CODE("compteCode", "Compte", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlAccount.code")),

    /** Toujours vide sur le périmètre Vente ; gardée parce que le contrat la publie. */
    DEPOT_ID("depotId", "Id dépôt", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlDeposit.id")),
    /** Voir {@link #DEPOT_ID}. */
    DEPOT_CODE("depotCode", "Dépôt", Groupe.SUIVI, TypeColonne.TEXTE, true,
            Chemin.groupable("stlDeposit.code")),

    BANQUE_ID("banqueId", "Id banque", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgBank.id")),
    BANQUE_NOM("banqueNom", "Banque", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("cfgBank")),

    /** Sans chemin : {@code nvarchar(max)}, que le {@code COUNT(DISTINCT)} du comptage de groupes refuse. */
    NOTES("notes", "Notes", Groupe.SUIVI, TypeColonne.TEXTE, true),

    // -- Hors défaut, scalaires ou associations déjà mappées : coût nul si non demandées.

    /** Libellé du règlement ({@code nvarchar(255)}, donc groupable contrairement à {@link #NOTES}). */
    DESCRIPTION("description", "Libellé", Groupe.IDENTITE, TypeColonne.TEXTE, false,
            Chemin.groupable("description")),

    MONTANT_BASE("montantBase", "Montant base", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("amountBase")),

    /** Tiers <em>payeur</em> : diverge rarement du tiers du règlement, servi parce que le légataire le distingue. */
    TIER_PAYEUR_ID("tierPayeurId", "Id tiers payeur", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.groupable("tierPayer.id")),
    /** Voir {@link #TIER_PAYEUR_ID}. */
    TIER_PAYEUR_NOM("tierPayeurNom", "Tiers payeur", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("tierPayer")),

    /** {@code IsDcoumentDeliveredToCenter} : la faute de frappe est celle du schéma légataire. */
    REMIS_AU_CENTRE("remisAuCentre", "Remis au centre", Groupe.SUIVI, TypeColonne.BOOLEEN, false,
            Chemin.groupable("isDcoumentDeliveredToCenter")),

    RECU_CODE("recuCode", "N° de reçu", Groupe.SUIVI, TypeColonne.TEXTE, false,
            Chemin.groupable("receiptCode")),
    DATE_REMISE_CENTRE("dateRemiseAuCentre", "Date de remise au centre", Groupe.SUIVI,
            TypeColonne.DATE, false,
            Chemin.date("deliveredToCenterDate")),
    BORDEREAU_CODE("bordereauCode", "Bordereau", Groupe.SUIVI, TypeColonne.TEXTE, false,
            Chemin.groupable("slipCode")),
    DATE_BORDEREAU("dateBordereau", "Date bordereau", Groupe.SUIVI, TypeColonne.DATE, false,
            Chemin.date("slipDate")),

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

    public static List<ColonneEncaissement> groupables() {
        return Colonnes.groupables(ColonneEncaissement.class);
    }

    public static List<ColonneEncaissement> agregeables() {
        return Colonnes.agregeables(ColonneEncaissement.class);
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneEncaissement resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneEncaissement.class, demandee);
    }

    /** Forme {@code colonne} (somme) ou {@code colonne:FONCTION}. */
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
