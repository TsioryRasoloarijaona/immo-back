package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/tranches} : le tableau « Liste » de la page TRANCHES de l'aide OMRAN,
 * libellés de {@code CfgTrancheView} (FEATURE-patrimoine-tranche-fiche.md §4.4). Défauts = les colonnes servies avant
 * le catalogue, plus Standing, Entité et Visible vente. Aucune colonne groupable dans ce lot.
 */
public enum ColonneTranche implements Colonne {

    // -- Général
    ID("id", "Identifiant", Groupe.GENERAL, TypeColonne.TEXTE, true),
    CODE("code", "Code", Groupe.GENERAL, TypeColonne.TEXTE, true),
    DESCRIPTION("description", "Description", Groupe.GENERAL, TypeColonne.TEXTE, true),
    DESCRIPTION_LONGUE("descriptionLongue", "Description détaillée", Groupe.GENERAL, TypeColonne.TEXTE, false),
    PROJET_ID("projetId", "Id programme", Groupe.GENERAL, TypeColonne.TEXTE, true),
    PROJET_CODE("projetCode", "Code programme", Groupe.GENERAL, TypeColonne.TEXTE, true),
    PROJET_LIBELLE("projetLibelle", "Programme", Groupe.GENERAL, TypeColonne.TEXTE, false),
    ENTITE_LIBELLE("entiteLibelle", "Entité", Groupe.GENERAL, TypeColonne.TEXTE, true),
    STANDING("standingLibelle", "Standing", Groupe.GENERAL, TypeColonne.TEXTE, true),
    NIVEAU_TERRAIN("niveauTerrainLibelle", "Nbr. étage", Groupe.GENERAL, TypeColonne.TEXTE, false),
    PLANCHER_COUVERT("plancherCouvert", "Plancher couvert (m²)", Groupe.GENERAL, TypeColonne.NOMBRE, false),
    RESPONSABLE_SAV("responsableSavLibelle", "Responsable S.A.V", Groupe.GENERAL, TypeColonne.TEXTE, false),
    EST_EN_VENTE("estEnVente", "Visible dans le module Vente", Groupe.VISIBILITE, TypeColonne.BOOLEEN, true),
    EST_EN_LOCATION("estEnLocation", "Visible dans le module Location", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),
    VISIBLE_PROJET("visibleProjet", "Visible dans le module Projet", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),
    VISIBLE_ACHAT("visibleAchat", "Visible dans le module Achat", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),
    VISIBLE_SAV("visibleSav", "Visible dans le module service après vente", Groupe.VISIBILITE,
            TypeColonne.BOOLEEN, false),
    VISIBLE_PRM("visiblePrm", "Visible dans le module gestion immobilière", Groupe.VISIBILITE,
            TypeColonne.BOOLEEN, false),
    VISIBLE_ROM("visibleRom", "Visible dans le module Location-Vente", Groupe.VISIBILITE, TypeColonne.BOOLEEN, false),

    // -- Délais
    DATE_DEBUT_TRAVAUX("dateDebutTravaux", "Date lancement des travaux", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_FIN_TRAVAUX("dateFinTravaux", "Date fin des travaux", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_OCTROI_TF("dateOctroiTitresFonciers", "Date octroi des T.F", Groupe.DELAIS, TypeColonne.DATE, true),
    DATE_PERMIS_HABITER("datePermisHabiter", "Date permis d'habiter", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_PREMIERE_RELANCE("datePremiereRelance", "Date de la 1ère relance", Groupe.DELAIS, TypeColonne.DATE, false),
    DATE_DEBUT_COMMERCIALISATION("dateDebutCommercialisation", "Date début de commercialisation", Groupe.DELAIS,
            TypeColonne.DATE, false),
    DATE_FIN_CONCRETISATION("dateFinConcretisation", "Date fin des concrétisations", Groupe.DELAIS,
            TypeColonne.DATE, false),
    DATE_DEBUT_FINALISATION("dateDebutFinalisation", "Date début des finalisations", Groupe.DELAIS,
            TypeColonne.DATE, false),
    DATE_FIN_COMMERCIALISATION("dateFinCommercialisation", "Date fin de commercialisation", Groupe.DELAIS,
            TypeColonne.DATE, false),
    DATE_RECEPTION("dateReception", "Date réception", Groupe.DELAIS, TypeColonne.DATE, false),

    // -- Coordonnées (courrier et représentants : ceux du programme et de l'entité, comme CfgTrancheView)
    ADRESSE_COMPLETE("adresseComplete", "Adresse", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    VILLE("ville", "Ville", Groupe.COORDONNEES, TypeColonne.TEXTE, true),
    PAYS("paysLibelle", "Pays", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    TELEPHONE("telephone", "Tél.", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    MOBILE("mobile", "Mobile", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    FAX("fax", "Fax", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    EMAIL("email", "Email", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    SITE_WEB("siteWeb", "Site web", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    ADRESSE_SAV("adresseSav", "Adresse SAV", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    VILLE_SIGNATURE("villeSignature", "Courrier fait à", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    REPRESENTANT_LEGAL("representantLegal", "Représentant légal", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    SIGNATAIRE("signataireLibelle", "Courrier signataire", Groupe.COORDONNEES, TypeColonne.TEXTE, false),
    RESPONSABLE_TECHNIQUE("responsableTechniqueLibelle", "Responsable technique", Groupe.COORDONNEES,
            TypeColonne.TEXTE, false),
    RESPONSABLE_COMMERCIAL("responsableCommercialLibelle", "Responsable commercial", Groupe.COORDONNEES,
            TypeColonne.TEXTE, false),

    // -- Règles de gestion 1
    PRE_RESERVATION("montantPreReservation", "Pré-réservation", Groupe.REGLES_1, TypeColonne.MONTANT, false),
    AVANCE_SUR_MONTANT("avanceSurMontant", "Avance basée sur le montant", Groupe.REGLES_1, TypeColonne.BOOLEEN, false),
    CONCRETISATION("advancePercent", "Concrétisation", Groupe.REGLES_1, TypeColonne.TAUX, false),
    AVANCE("montantAvance", "Avance", Groupe.REGLES_1, TypeColonne.MONTANT, false),
    FRAIS_DOSSIER("fraisDossier", "Frais de dossier", Groupe.REGLES_1, TypeColonne.MONTANT, false),
    PENALITE_DESISTEMENT("tauxPenaliteAnnulation", "Pénalité de désistement", Groupe.REGLES_1, TypeColonne.TAUX,
            false),
    PENALITE_SUR_PRIX("penaliteAnnulationSurPrixArticle",
            "Pénalité de désistement basée sur le prix de vente / encaissement", Groupe.REGLES_1,
            TypeColonne.BOOLEEN, false),
    PENALITE_TRANSFERT_PRODUIT("penaliteTransfertArticle", "Pénalité de transfert produit", Groupe.REGLES_1,
            TypeColonne.MONTANT, false),
    PENALITE_TRANSFERT_TRANCHE("penaliteTransfertTranche", "Pénalité de transfert tranche", Groupe.REGLES_1,
            TypeColonne.MONTANT, false),
    PENALITE_TRANSFERT_CLIENT("penaliteTransfertClient", "Pénalité de transfert client", Groupe.REGLES_1,
            TypeColonne.MONTANT, false),
    AUTRES_FRAIS("autresFrais", "Autre frais", Groupe.REGLES_1, TypeColonne.MONTANT, false),
    FRAIS_SYNDIC("montantSyndic", "Frais de syndic", Groupe.REGLES_1, TypeColonne.MONTANT, false),
    PERIODE_SYNDIC("periodeSyndic", "Période de syndic", Groupe.REGLES_1, TypeColonne.NOMBRE, false),

    // -- Règles de gestion 2
    DELAI_PREMIER_ENGAGEMENT("delaiPremierEngagement", "Délai d'attente après première notification T.F. (en jours)",
            Groupe.REGLES_2, TypeColonne.NOMBRE, false),
    DELAI_DEUXIEME_ENGAGEMENT("delaiDeuxiemeEngagement",
            "Délai d'attente après deuxième notification T.F. (en jours)", Groupe.REGLES_2, TypeColonne.NOMBRE, false),
    PENALITE_NON_FINALISATION("tauxPenalitePremierEngagement", "Pénalité de non finalisation de la vente",
            Groupe.REGLES_2, TypeColonne.TAUX, false),
    DEDUCTION_APRES_CLOTURE("tauxPenaliteDeuxiemeEngagement", "Déduction après clôture du dossier de vente",
            Groupe.REGLES_2, TypeColonne.TAUX, false),
    DELAI_SOLDE("delaiSoldePaiement", "Délai de mise à disposition du solde de versements (en jours)",
            Groupe.REGLES_2, TypeColonne.NOMBRE, false),
    DUREE_SAV("delaiGarantie", "Durée de service après vente (en mois)", Groupe.REGLES_2, TypeColonne.NOMBRE, false),
    DUREE_SAV_SUPP("delaiGarantieSupplement", "Durée de service après vente supp. (en mois)", Groupe.REGLES_2,
            TypeColonne.NOMBRE, false),
    ARTICLE_RESILIATION("codeDroitRetractation", "Article légal pour la résiliation", Groupe.REGLES_2,
            TypeColonne.TEXTE, false),
    TRIBUNAL("tribunalCommerce", "Tribunal de commerce", Groupe.REGLES_2, TypeColonne.TEXTE, false),
    DELAI_VISITE("delaiMinimumIntervention", "Délai minimum pour la programmation d'une visite technique",
            Groupe.REGLES_2, TypeColonne.NOMBRE, false),
    VISITES_MAX("maxVisitNumber", "Nombre maximum de visite par jours", Groupe.REGLES_2, TypeColonne.NOMBRE, false),
    DELAI_RETRACTATION("delaiRetractation", "Délai de rétractation (heures)", Groupe.REGLES_2, TypeColonne.NOMBRE,
            false),
    DELAI_MAX_ECHEANCE("delaiMaxEcheance", "Délai max des échéances", Groupe.REGLES_2, TypeColonne.NOMBRE, false),
    VERIF_DELAI_MAX("verifierDelaiMaxEcheance", "Avec vérification du délai max des échéances", Groupe.REGLES_2,
            TypeColonne.BOOLEEN, false),
    CREDIT("creditPercent", "Crédit", Groupe.REGLES_2, TypeColonne.TAUX, false),
    VERIF_CREDIT("isWithVerifCredit", "Avec vérification du crédit", Groupe.REGLES_2, TypeColonne.BOOLEEN, false),
    CADEAU_PARRAIN("cadeauParrain", "Avec octroi d'un cadeau pour le parrain", Groupe.REGLES_2,
            TypeColonne.BOOLEEN, false),
    CADEAU_ANCIEN_CLIENT("cadeauAncienClient", "Avec octroi d'un cadeau pour les anciens clients", Groupe.REGLES_2,
            TypeColonne.BOOLEEN, false),
    CADEAU_SOURCE_CONTACT("cadeauOrigineProspection", "Avec octroi d'un cadeau pour une source de contact",
            Groupe.REGLES_2, TypeColonne.BOOLEEN, false),
    CADEAUX_CUMULES("cadeauxCumules", "Avec possibilité de cumul des cadeaux", Groupe.REGLES_2, TypeColonne.BOOLEEN,
            false),

    // -- Remises anciens clients
    REMISE_PRE_RESERVATION("montantPreReservationRemise", "Remise sur la pré-réservation", Groupe.REMISES,
            TypeColonne.MONTANT, false),
    SEUIL_CONCRETISATION("advancePercentRemise", "Seuil de versement lors de la concrétisation", Groupe.REMISES,
            TypeColonne.TAUX, false),
    REMISE_ACTIVE("advancePercentRemiseActive", "Active", Groupe.REMISES, TypeColonne.BOOLEEN, false),
    REMISE_FRAIS_DOSSIER("fraisDossierRemise", "Remise sur les frais de dossier", Groupe.REMISES,
            TypeColonne.MONTANT, false),
    REMISE_DESISTEMENT("tauxPenaliteAnnulationRemise", "Remise sur les pénalités de désistement", Groupe.REMISES,
            TypeColonne.TAUX, false),
    REMISE_TRANSFERT_PRODUIT("penaliteTransfertArticleRemise", "Remise sur les pénalités de transfert produit",
            Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_TRANSFERT_TRANCHE("penaliteTransfertTrancheRemise", "Remise sur les pénalités de transfert tranche",
            Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_TRANSFERT_CLIENT("penaliteTransfertClientRemise", "Remise sur les pénalités de transfert client",
            Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_AUTRES_FRAIS("autresFraisRemise", "Remise sur autre frais", Groupe.REMISES, TypeColonne.MONTANT, false),
    REMISE_SYNDIC("montantSyndicRemise", "Remise sur les frais de syndic", Groupe.REMISES, TypeColonne.MONTANT, false),

    // -- Autres
    DELAI_NOTAIRE("delaiNotaireMinutes", "Délai entre la date d'envoi au notaire et la date minute", Groupe.AUTRES,
            TypeColonne.NOMBRE, false),
    DELAI_TOUT_SOLDE("delaiToutSoldeMinutes", "Délai entre la date minute et l'encaissement des restes à régler",
            Groupe.AUTRES, TypeColonne.NOMBRE, false),
    ECART_TYPE_CONCRETISATION("ecartTypeConcretisation", "Écart-type concrétisation", Groupe.AUTRES,
            TypeColonne.NOMBRE, false),
    ECART_TYPE_FINALISATION("ecartTypeFinalisation", "Écart-type finalisation", Groupe.AUTRES, TypeColonne.NOMBRE,
            false),
    TIERS_AUTRUI("tiersAutruiLibelle", "Tiers autrui", Groupe.AUTRES, TypeColonne.TEXTE, false),
    PROJET_FACTURATION("projetFacturationLibelle", "Projet de facturation", Groupe.AUTRES, TypeColonne.TEXTE, false),
    PLAN_COMPTABLE("planComptableLibelle", "Plan comptable", Groupe.AUTRES, TypeColonne.TEXTE, false),
    RUBRIQUE("rubriqueLibelle", "Rubrique", Groupe.AUTRES, TypeColonne.TEXTE, false),

    // -- Identification foncière
    DATE_MAIN_LEVEE_AFH("dateMainLeveeAfh", "Date main levée AFH", Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_CERTIFICAT_1("dateCertificatDefinitif1", "Date certificat définitif 1", Groupe.FONCIER, TypeColonne.DATE,
            false),
    DATE_MAIN_LEVEE_BANQUE("dateMainLeveeBancaire", "Date main levée bancaire", Groupe.FONCIER, TypeColonne.DATE,
            false),
    DATE_CERTIFICAT_2("dateCertificatDefinitif2", "Date certificat définitif 2", Groupe.FONCIER, TypeColonne.DATE,
            false),
    DATE_DEPOT_MUNICIPALITE("dateDepotLotissementMunicipalite",
            "Date de dépôt du dossier de lotissement à la municipalité", Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_RETRAIT_MUNICIPALITE("dateRetraitLotissementMunicipalite",
            "Date de retrait du dossier de lotissement de la municipalité", Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_DEPOT_OTC_NON_APPROUVE("dateDepotLotissementNonApprouveOtc",
            "Date de dépôt du dossier de lotissement non approuvé à l'OTC", Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_REGLEMENT_OTC("dateReglementFraisOtc", "Date de règlement des frais de l'OTC", Groupe.FONCIER,
            TypeColonne.DATE, false),
    DATE_DEPOT_OTC_APPROUVE("dateDepotLotissementApprouveOtc",
            "Date de dépôt du dossier de lotissement approuvé à l'OTC", Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_RETRAIT_MUTATION_OTC("dateRetraitPlansMutationOtc", "Date de retrait des plans de mutation de l'OTC",
            Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_ACHEVEMENT_PARTAGES("dateAchevementPartages", "Date d'achèvement des partages", Groupe.FONCIER,
            TypeColonne.DATE, false),
    DATE_DEPOT_PARTAGE_CPF("dateDepotPartageCpf", "Date de dépôt du dossier de partage à la CPF", Groupe.FONCIER,
            TypeColonne.DATE, false),
    DATE_PAIEMENT_PARTAGE_CPF("datePaiementFraisPartageCpf", "Date de paiement des frais de partage CPF",
            Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_RETRAIT_PARTAGE_CPF("dateRetraitPartageCpf", "Date de retrait du dossier de partage à la CPF",
            Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_RETRAIT_CERTIFICATS("dateRetraitCertificatsIndividuels",
            "Date de retrait des certificats de propriété individuels", Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_VERIFICATION_CERTIFICATS("dateVerificationCertificats", "Date de vérification des certificats",
            Groupe.FONCIER, TypeColonne.DATE, false),
    DATE_REPARTITION_CHARGES("dateRepartitionCharges", "Date de répartition des charges", Groupe.FONCIER,
            TypeColonne.DATE, false),

    // -- Piste d'audit
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false);

    /** Sections du sélecteur, calquées sur les onglets de la fiche OMRAN. */
    public enum Groupe { GENERAL, VISIBILITE, DELAIS, COORDONNEES, REGLES_1, REGLES_2, REMISES, AUTRES, FONCIER, AUDIT }

    /** {@code TAUX} : fraction (0.2 = 20 %), comme la fiche. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, TAUX, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson de {@code TrancheListItemDto}. */
    public static final String FILTRE = "colonnesTranche";

    /** Colonnes lues à travers le programme, l'entité ou une clé étrangère : calculées seulement si demandées. */
    public static final Set<ColonneTranche> COUTEUSES = Set.of(PROJET_LIBELLE, ENTITE_LIBELLE, STANDING,
            NIVEAU_TERRAIN, RESPONSABLE_SAV, PAYS, ADRESSE_COMPLETE, VILLE_SIGNATURE, REPRESENTANT_LEGAL, SIGNATAIRE,
            RESPONSABLE_TECHNIQUE, RESPONSABLE_COMMERCIAL, TIERS_AUTRUI, PROJET_FACTURATION, PLAN_COMPTABLE, RUBRIQUE);

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

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneTranche.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneTranche> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneTranche.class, demandees, ID);
    }
}
