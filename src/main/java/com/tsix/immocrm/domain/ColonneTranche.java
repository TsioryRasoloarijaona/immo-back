package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/tranches} : tableau Liste de la page TRANCHES de l'aide OMRAN, sous les
 * noms de {@code TrancheDetailDto} (FEATURE-patrimoine-tranche-fiche.md §4.4). « Statut finalisation 1/2 » omis :
 * colonnes commentées dans {@code CfgTrancheView}. Les libellés de référence ne sont lus que si la colonne est demandée.
 */
public enum ColonneTranche implements Colonne {

    // -- Sélection par défaut : les colonnes historiques de la liste, plus standing, entité et vente.
    ID("id", "Identifiant", Groupe.GENERAL, TypeColonne.TEXTE, true),
    PROJET_ID("projetId", "Id programme", Groupe.GENERAL, TypeColonne.TEXTE, true),
    CODE("code", "Code", Groupe.GENERAL, TypeColonne.TEXTE, true),
    DESCRIPTION("description", "Description", Groupe.GENERAL, TypeColonne.TEXTE, true),
    PROJET_CODE("projetCode", "Programme", Groupe.GENERAL, TypeColonne.TEXTE, true),
    VILLE("ville", "Ville", Groupe.COORDONNEES, TypeColonne.TEXTE, true),
    DATE_OCTROI_TF("dateOctroiTitresFonciers", "Date octroi des T.F", Groupe.DELAIS, TypeColonne.DATE, true),
    STANDING("standingLibelle", "Standing", Groupe.GENERAL, TypeColonne.TEXTE, true),
    ENTITE("entiteCode", "Entité", Groupe.GENERAL, TypeColonne.TEXTE, true),
    EST_EN_VENTE("estEnVente", "Visible dans le module Vente", Groupe.VISIBILITE, TypeColonne.BOOLEEN, true),

    // -- Général.
    DESCRIPTION_LONGUE("descriptionLongue", "Description détaillée", Groupe.GENERAL, TypeColonne.TEXTE, false),
    NIVEAU_TERRAIN("niveauTerrainLibelle", "Nbr. étage", Groupe.GENERAL, TypeColonne.TEXTE, false),
    RUBRIQUE("rubriqueLibelle", "Rubrique", Groupe.GENERAL, TypeColonne.TEXTE, false),
    PLANCHER_COUVERT("plancherCouvert", "Plancher couvert (m²)", Groupe.GENERAL, TypeColonne.NOMBRE, false),
    RESPONSABLE_SAV("responsableSavLibelle", "Responsable S.A.V", Groupe.GENERAL, TypeColonne.TEXTE, false),

    // -- Programme et entité de la tranche (CfgTrancheView).
    VILLE_SIGNATURE("villeSignature", "Courrier fait à", Groupe.PROGRAMME, TypeColonne.TEXTE, false),
    SIGNATAIRE("signataireLibelle", "Courrier signataire", Groupe.PROGRAMME, TypeColonne.TEXTE, false),
    REPRESENTANT_LEGAL("representantLegalLibelle", "Représentant légal", Groupe.PROGRAMME, TypeColonne.TEXTE, false),
    RESPONSABLE_COMMERCIAL("responsableCommercialLibelle", "Responsable commercial", Groupe.PROGRAMME,
            TypeColonne.TEXTE, false),
    RESPONSABLE_TECHNIQUE("responsableTechniqueLibelle", "Responsable technique", Groupe.PROGRAMME,
            TypeColonne.TEXTE, false),

    // -- Délais.
    DATE_DEBUT_TRAVAUX("dateDebutTravaux", "Date lancement des travaux", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_FIN_TRAVAUX("dateFinTravaux", "Date fin des travaux", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_PERMIS_HABITER("datePermisHabiter", "Date permis d'habiter", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_PREMIERE_RELANCE("datePremiereRelance", "Date de la 1ère relance", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_DEBUT_COMMERCIALISATION("dateDebutCommercialisation", "Date début de commercialisation", Groupe.DELAIS,
            TypeColonne.DATE, false),
    DATE_FIN_CONCRETISATION("dateFinConcretisation", "Date fin de concrétisations", Groupe.DELAIS, TypeColonne.DATE,
            false),
    DATE_DEBUT_FINALISATION("dateDebutFinalisation", "Date début de finalisations", Groupe.DELAIS, TypeColonne.DATE,
            false),
    DATE_FIN_COMMERCIALISATION("dateFinCommercialisation", "Date fin de commercialisation", Groupe.DELAIS,
            TypeColonne.DATE, false),

    // -- Coordonnées.
    ADRESSE("adresse", "Adresse", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    ADRESSE_SAV("adresseSav", "Adresse SAV", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    PAYS("paysLibelle", "Pays", Groupe.COORDONNEES, TypeColonne.TEXTE, false),

    // -- Règles de gestion 1.
    MONTANT_PRE_RESERVATION("montantPreReservation", "Pré-réservation", Groupe.REGLES, TypeColonne.MONTANT, false),
    CONCRETISATION("advancePercent", "Concrétisation", Groupe.REGLES, TypeColonne.POURCENTAGE, false),
    FRAIS_DOSSIER("fraisDossier", "Frais de dossier", Groupe.REGLES, TypeColonne.MONTANT, false),
    PENALITE_DESISTEMENT("tauxPenaliteAnnulation", "Pénalité de désistement", Groupe.REGLES, TypeColonne.POURCENTAGE,
            false),
    PENALITE_SUR_PRIX("penaliteAnnulationSurPrixArticle",
            "Pénalité de désistement basée sur le prix de vente / encaissement", Groupe.REGLES, TypeColonne.BOOLEEN,
            false),
    PENALITE_TRANSFERT_PRODUIT("penaliteTransfertArticle", "Pénalité de transfert produit", Groupe.REGLES,
            TypeColonne.MONTANT, false),
    PENALITE_TRANSFERT_TRANCHE("penaliteTransfertTranche", "Pénalité de transfert tranche", Groupe.REGLES,
            TypeColonne.MONTANT, false),
    PENALITE_TRANSFERT_CLIENT("penaliteTransfertClient", "Pénalité de transfert client", Groupe.REGLES,
            TypeColonne.MONTANT, false),
    AUTRES_FRAIS("autresFrais", "Autre frais", Groupe.REGLES, TypeColonne.MONTANT, false),
    FRAIS_SYNDIC("montantSyndic", "Frais de syndic", Groupe.REGLES, TypeColonne.MONTANT, false),
    PERIODE_SYNDIC("periodeSyndic", "Période de syndic", Groupe.REGLES, TypeColonne.NOMBRE, false),

    // -- Règles de gestion 2.
    DELAI_PREMIER_ENGAGEMENT("delaiPremierEngagement", "Délai d'attente après première notification T.F. (en jours)",
            Groupe.REGLES, TypeColonne.NOMBRE, false),
    DELAI_DEUXIEME_ENGAGEMENT("delaiDeuxiemeEngagement",
            "Délai d'attente après deuxième notification T.F. (en jours)", Groupe.REGLES, TypeColonne.NOMBRE, false),
    PENALITE_NON_FINALISATION("tauxPenalitePremierEngagement", "Pénalité de non finalisation de la vente",
            Groupe.REGLES, TypeColonne.POURCENTAGE, false),
    DEDUCTION_APRES_CLOTURE("tauxPenaliteDeuxiemeEngagement", "Déduction après clôture du dossier de vente",
            Groupe.REGLES, TypeColonne.POURCENTAGE, false),
    DELAI_SOLDE("delaiSoldePaiement", "Délai de mise à disposition du solde de versements (en jours)", Groupe.REGLES,
            TypeColonne.NOMBRE, false),
    DUREE_SAV("delaiGarantie", "Durée de service après vente (en mois)", Groupe.REGLES, TypeColonne.NOMBRE, false),
    DUREE_SAV_SUPP("delaiGarantieSupplement", "Durée de service après vente supp. (en mois)", Groupe.REGLES,
            TypeColonne.NOMBRE, false),
    ARTICLE_RESILIATION("codeDroitRetractation", "Article légal pour la résiliation", Groupe.REGLES, TypeColonne.TEXTE,
            false),
    TRIBUNAL("tribunalCommerce", "Tribunal de commerce", Groupe.REGLES, TypeColonne.TEXTE, false),
    DELAI_VISITE("delaiMinimumIntervention", "Délai minimum pour la programmation d'une visite technique",
            Groupe.REGLES, TypeColonne.NOMBRE, false),
    MAX_VISITES("maxVisitNumber", "Nombre maximum de visite par jours", Groupe.REGLES, TypeColonne.NOMBRE, false),
    CADEAU_PARRAIN("cadeauParrain", "Avec octroi d'un cadeau pour le parrain", Groupe.REGLES, TypeColonne.BOOLEEN,
            false),
    CADEAU_ANCIEN_CLIENT("cadeauAncienClient", "Avec octroi d'un cadeau pour les anciens clients", Groupe.REGLES,
            TypeColonne.BOOLEEN, false),
    CADEAU_SOURCE("cadeauOrigineProspection", "Avec octroi d'un cadeau pour une source de contact", Groupe.REGLES,
            TypeColonne.BOOLEEN, false),
    CADEAUX_CUMULES("cadeauxCumules", "Avec possibilité de cumul des cadeaux", Groupe.REGLES, TypeColonne.BOOLEEN,
            false),

    // -- Remises pour les anciens clients.
    REMISE_PRE_RESERVATION("montantPreReservationRemise", "Remise sur la pré-réservation", Groupe.REMISES,
            TypeColonne.MONTANT, false),
    SEUIL_CONCRETISATION("advancePercentRemise", "Seuil de versement lors de la concrétisation", Groupe.REMISES,
            TypeColonne.POURCENTAGE, false),
    REMISE_ACTIVE("advancePercentRemiseActive", "Active", Groupe.REMISES, TypeColonne.BOOLEEN, false),
    REMISE_FRAIS_DOSSIER("fraisDossierRemise", "Remise sur les frais de dossier", Groupe.REMISES, TypeColonne.MONTANT,
            false),
    REMISE_DESISTEMENT("tauxPenaliteAnnulationRemise", "Remise sur les pénalités de désistement", Groupe.REMISES,
            TypeColonne.POURCENTAGE, false),
    REMISE_TRANSFERT_PRODUIT("penaliteTransfertArticleRemise", "Remise sur les pénalités de transfert produit",
            Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_TRANSFERT_TRANCHE("penaliteTransfertTrancheRemise", "Remise sur les pénalités de transfert tranche",
            Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_TRANSFERT_CLIENT("penaliteTransfertClientRemise", "Remise sur les pénalités de transfert client",
            Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_SYNDIC("montantSyndicRemise", "Remise sur les frais de syndic", Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_AUTRES_FRAIS("autresFraisRemise", "Remise sur autre frais", Groupe.REMISES, TypeColonne.MONTANT, false),

    // -- Autres.
    DELAI_NOTAIRE("delaiNotaireMinutes", "Délai entre la date d'envoi au notaire et la date minute", Groupe.AUTRES,
            TypeColonne.NOMBRE, false),
    DELAI_TOUT_SOLDE("delaiToutSoldeMinutes", "Délai entre la date minute et l'encaissement des restes à régler",
            Groupe.AUTRES, TypeColonne.NOMBRE, false),
    ECART_CONCRETISATION("ecartTypeConcretisation", "Écart-type concrétisation", Groupe.AUTRES, TypeColonne.NOMBRE,
            false),
    ECART_FINALISATION("ecartTypeFinalisation", "Écart-type finalisation", Groupe.AUTRES, TypeColonne.NOMBRE, false),
    TIERS_AUTRUI("tiersAutruiLibelle", "Tiers autrui", Groupe.AUTRES, TypeColonne.TEXTE, false),
    PROJET_FACTURATION("projetFacturationLibelle", "Projet de facturation pour les dépenses", Groupe.AUTRES,
            TypeColonne.TEXTE, false),
    PLAN_COMPTABLE("planComptableLibelle", "Plan compt.", Groupe.AUTRES, TypeColonne.TEXTE, false),

    // -- Visibilité par module.
    EST_EN_LOCATION("estEnLocation", "Visible dans le module Location", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),
    VISIBLE_PROJET("visibleProjet", "Visible dans le module Projet", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),
    VISIBLE_ACHAT("visibleAchat", "Visible dans le module Achat", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),
    VISIBLE_SAV("visibleSav", "Visible dans le module service après vente", Groupe.VISIBILITE, TypeColonne.BOOLEEN,
            false),
    VISIBLE_ROM("visibleRom", "Visible dans le module ROM", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),
    VISIBLE_PRM("visiblePrm", "Visible dans le module PRM", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),

    // -- Piste d'audit.
    CREE_PAR("creePar", "Crée par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    CREE_LE("creeLe", "Crée le", Groupe.AUDIT, TypeColonne.DATE, false),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false);

    /** Regroupement du sélecteur, calqué sur les onglets de la fiche OMRAN. */
    public enum Groupe { GENERAL, PROGRAMME, DELAIS, COORDONNEES, REGLES, REMISES, AUTRES, VISIBILITE, AUDIT }

    /** Type d'affichage ; POURCENTAGE = fraction (0,2 = 20 %), exportée en nombre. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, POURCENTAGE, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson de {@code TrancheListItemDto}. */
    public static final String FILTRE = "colonnesTranche";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;

    ColonneTranche(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut) {
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

    @Override
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
        return type == TypeColonne.POURCENTAGE ? TypeExport.NOMBRE : TypeExport.valueOf(type.name());
    }

    @Override
    public boolean parDefaut() {
        return parDefaut;
    }

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneTranche.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneTranche> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneTranche.class, demandees, ID);
    }
}
