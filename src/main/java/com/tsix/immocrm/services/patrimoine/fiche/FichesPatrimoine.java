package com.tsix.immocrm.services.patrimoine.fiche;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.entite.EntiteJuridiqueDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.LigneEtageDto;
import com.tsix.immocrm.dto.patrimoine.LigneTerrainDto;
import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.TerrainDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainDetailDto;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Champ;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Fiche;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Section;
import com.tsix.immocrm.web.export.LignesAplaties;
import com.tsix.immocrm.web.export.MiseEnPagePdf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contenu des six fiches PDF du patrimoine : sections de la fiche Arpent, valeurs lues par nom sur le DTO du détail
 * mis à plat ({@link LignesAplaties}). Un nom préfixé {@code %} est une fraction imprimée en pourcentage.
 */
public final class FichesPatrimoine {

    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String LIBELLE_CODE = "Code";
    private static final String LIBELLE_DESCRIPTION = "Libellé";
    private static final String IDENTIFICATION = "Identification";
    private static final String COORDONNEES_TITRE = "Coordonnées";
    private static final String SUPERFICIES = "Superficies / valorisation";
    private static final String FONCIER = "Foncier";
    private static final String AUTRES = "Autres";
    private static final String DELAIS = "Délais";
    private static final String DESCRIPTION_LONGUE = "descriptionLongue";
    private static final String DESCRIPTION_DETAILLEE = "Description détaillée";
    private static final String SUPERFICIE = "Superficie";

    private static final String[] COORDONNEES = {"adresse", "Adresse", "adresse2", "Adresse 2", "adresse3", "Adresse 3",
            "codePostal", "Code postal", "ville", "Ville", "telephone", "Tél.", "mobile", "Mobile", "fax", "Fax",
            "email", "Email", "siteWeb", "Site web"};
    private static final String[] AUDIT = {"creePar", "Créé par", "creeLe", "Créé le", "modifiePar", "Modifié par",
            "modifieLe", "Modifié le"};
    private static final String[] DATES_PROGRAMME = {"dateDebutTravaux", "Date lancement des travaux",
            "dateFinTravaux", "Date fin des travaux", "dateDebutCommercialisation", "Début de commercialisation",
            "dateFinCommercialisation", "Fin de commercialisation"};
    private static final String[] ENREGISTREMENT = {"dateEnregistrement", "Date d'enregistrement",
            "numeroEnregistrement", "Numéro d'enregistrement", "lieuEnregistrement", "Lieu d'enregistrement",
            "cpfImmatriculation", "C.P.F. immatriculation", "dateImmatriculation", "Date d'immatriculation"};
    private static final String[] RESPONSABLES = {"signataireLibelle", "Courrier signataire",
            "responsableCommercialLibelle", "Responsable commercial", "responsableTechniqueLibelle",
            "Responsable technique"};

    private FichesPatrimoine() {
    }

    public static Fiche projet(ProjetDetailDto d) {
        Map<String, Object> v = LignesAplaties.de(d);
        return new Fiche("fiche-programme_" + d.code(), "Fiche programme " + d.code(), d.entiteId(), List.of(
                section(v, IDENTIFICATION, CODE, LIBELLE_CODE, DESCRIPTION, LIBELLE_DESCRIPTION, "entiteLibelle", "Entité",
                        "paysLibelle", "Pays", DESCRIPTION_LONGUE, DESCRIPTION_DETAILLEE),
                section(v, COORDONNEES_TITRE, COORDONNEES),
                section(v, DELAIS, DATES_PROGRAMME),
                section(v, "Programme", concat(new String[]{"villeSignature", "Courrier fait à"}, RESPONSABLES)),
                section(v, "Audit", AUDIT)));
    }

    public static Fiche tranche(TrancheDetailDto d, List<LigneTerrainDto> terrains, List<LigneEtageDto> etages) {
        Map<String, Object> v = LignesAplaties.de(d);
        return new Fiche("fiche-tranche_" + d.code(), "Fiche tranche " + d.code(), d.entiteId(), List.of(
                section(v, IDENTIFICATION, CODE, LIBELLE_CODE, DESCRIPTION, LIBELLE_DESCRIPTION, "projetCode", "Programme",
                        "entiteCode", "Entité", "standingLibelle", "Standing", "niveauTerrainLibelle", "Nbr. étage",
                        "estEnVente", "En vente", "estEnLocation", "En location", "plancherCouvert", "Plancher couvert",
                        DESCRIPTION_LONGUE, DESCRIPTION_DETAILLEE),
                section(v, COORDONNEES_TITRE, COORDONNEES),
                section(v, DELAIS, concat(DATES_PROGRAMME, new String[]{
                        "dateOctroiTitresFonciers", "Octroi des titres fonciers", "datePermisHabiter", "Permis d'habiter",
                        "dateReception", "Réception", "datePremiereRelance", "Date de la 1re relance",
                        "dateFinConcretisation", "Fin de concrétisation", "dateDebutFinalisation", "Début de finalisation",
                        "delaiGarantie", "Délai de garantie", "delaiPremierEngagement", "Délai 1er engagement",
                        "delaiDeuxiemeEngagement", "Délai 2e engagement", "delaiSoldePaiement", "Délai solde paiement",
                        "delaiRetractation", "Délai de rétractation (h)", "delaiMaxEcheance", "Délai max. échéance"})),
                section(v, "Règles", "%tauxPenaliteAnnulation", "Pénalité d'annulation",
                        "penaliteAnnulationSurPrixArticle", "Pénalité sur le prix de l'article", "%advancePercent",
                        "Concrétisation", "avanceSurMontant", "Avance en montant", "montantAvance", "Montant de l'avance",
                        "montantPreReservation", "Montant pré-réservation", "fraisDossier", "Frais de dossier",
                        "autresFrais", "Autres frais", "montantSyndic", "Montant syndic", "periodeSyndic",
                        "Période syndic (mois)", "maxVisitNumber", "Nombre max. de visites", "isWithVerifCredit",
                        "Vérifier le crédit", "%creditPercent", "Taux de crédit", "verifierDelaiMaxEcheance",
                        "Vérifier le délai max. d'échéance", "tribunalCommerce", "Tribunal de commerce",
                        "codeDroitRetractation", "Code droit de rétractation"),
                Section.tableau("Terrains", List.of(LIBELLE_CODE, LIBELLE_DESCRIPTION, SUPERFICIE, "Ratio"), terrains,
                        t -> new Object[]{t.terrainCode(), t.terrainDescription(), t.terrainSurface(), pourcentage(t.ratio())}),
                Section.tableau("Étages", List.of(LIBELLE_CODE, LIBELLE_DESCRIPTION), etages,
                        e -> new Object[]{e.niveauCode(), e.niveauDescription()}),
                section(v, "Audit", AUDIT)));
    }

    /** @param societeId entité de la tranche de l'article (en-tête société) */
    public static Fiche article(ArticleDetailDto d, UUID societeId) {
        Map<String, Object> v = LignesAplaties.de(d);
        return new Fiche("fiche-article_" + d.code(), "Fiche article " + d.code(), societeId, List.of(
                section(v, IDENTIFICATION, CODE, LIBELLE_CODE, DESCRIPTION, LIBELLE_DESCRIPTION, "trancheCode", "Tranche",
                        "lotParentCode", "Article parent", "immeubleLibelle", "Immeuble", "etageLibelle", "Étage",
                        "natureLibelle", "Nature", "vocationLibelle", "Vocation", "categorieLibelle", "Catégorie",
                        "typeLibelle", "Type", "presentationTypeLibelle", "Type de présentation", "statut",
                        "Disponibilité", "dossierCode", "Dossier", "clientNom", "Client"),
                section(v, SUPERFICIES, "surfaceProvisoire", "Superficie provisoire", "area", "Superficie définitive",
                        "superficieSousArticles", "Superficie des sous-articles", "superficieNette", "Superficie nette",
                        "superficieTitree", "Superficie titrée", "surfaceInterieure", "Surface intérieure",
                        "surfaceMurs", "Surface des murs", "surfacePartiesCommunes", "Surface parties communes",
                        "%tauxTVA", "TVA", "prixM2ProvisoireTTC", "Prix m² provisoire TTC", "prixM2DefinitifTTC",
                        "Prix m² définitif TTC", "prixArticleDefinitifTTC", "Prix de l'article TTC",
                        "prixVenteProvisoireTTC", "Prix de vente provisoire TTC", "prixVenteDefinitifTTC",
                        "Prix de vente définitif TTC", "prixSuggere", "Prix de référence TTC", "%remiseMax1",
                        "Remise max. 1", "%remiseMax2", "Remise max. 2", "avanceMinimum", "Avance minimum"),
                section(v, FONCIER, concat(new String[]{"titreFoncier", "Titre foncier", "estTitre", "Titré",
                        "titreFoncierMere", "Titre foncier mère", "intituleTitreFoncier", "Intitulé du titre",
                        "dateTitreFoncier", "Date du titre", "proprieteDite", "Propriété dite", "fractionDivise",
                        "Fraction divise", "partiesDivises", "Parties divises", "partiesIndivises", "Parties indivises"},
                        ENREGISTREMENT)),
                section(v, AUTRES, "adresse", "Adresse", "contenanceLibre", "Contenance", "toujoursAffecte",
                        "Toujours affecté", "loyerMensuelHT", "Loyer mensuel HT", "nombreCautions", "Nombre de cautions",
                        "notes", "Notes", "anomalies", "Anomalies"),
                Section.tableau("Sous-articles", List.of(LIBELLE_CODE, LIBELLE_DESCRIPTION, SUPERFICIE,
                                "Prix de vente TTC", "Disponibilité"), d.sousLots(),
                        s -> new Object[]{s.code(), s.description(), s.area(), s.comSalePrice(), s.statut()}),
                section(v, "Audit", AUDIT)));
    }

    public static Fiche terrain(TerrainDetailDto d) {
        Map<String, Object> v = LignesAplaties.de(d);
        UUID societeId = d.entite() == null ? null : d.entite().id();
        return new Fiche("fiche-terrain_" + d.code(), "Fiche terrain " + d.code(), societeId, List.of(
                section(v, IDENTIFICATION, CODE, LIBELLE_CODE, DESCRIPTION, LIBELLE_DESCRIPTION, "entite", "Société",
                        "nature", "Nature", "vocation", "Vocation", "categorie", "Catégorie", "type", "Type", "statut",
                        "Statut", "niveau", "Niveau", "vocationTerrain", "Vocation foncière", "orientation", "Orientation",
                        "parent", "Terrain parent", "fournisseur", "Propriétaire", "estTermine", "Terminé", "occupe",
                        "Occupé"),
                section(v, SUPERFICIES, "surfaceProvisoire", "Surface provisoire", "prixM2ProvisoireTTC",
                        "Prix m² provisoire TTC", "prixProvisoireTTC", "Prix provisoire TTC", "surfaceDefinitive",
                        "Surface définitive", "prixM2DefinitifTTC", "Prix m² définitif TTC", "prixDefinitifTTC",
                        "Prix définitif TTC", "%tauxTva", "TVA", "prixAchatM2", "Prix d'achat du m²", "prixAchat",
                        "Prix d'achat", "dateAchat", "Date d'achat", "coutTotal", "Coût total", "coutUnitaireFinal",
                        "Coût unitaire final", "prixPropose", "Prix proposé"),
                section(v, FONCIER, "titreFoncier", "Titre foncier", "intituleTitreFoncier", "Intitulé du titre",
                        "dateTitreFoncier", "Date du titre", "datePermisConstruire", "Permis de construire", "adresse",
                        "Adresse", "lienGoogle", "Lien Google Maps"),
                section(v, AUTRES, "notes", "Notes", "anomalies", "Anomalies"),
                Section.tableau("Sous-terrains", List.of(LIBELLE_CODE, LIBELLE_DESCRIPTION, "Surface définitive",
                                "Prix définitif TTC"), d.sousTerrains(),
                        t -> new Object[]{t.code(), t.description(), t.surfaceDefinitive(), t.prixDefinitifTTC()}),
                section(v, "Audit", "userCreate", "Créé par", "userCreateDateTime", "Créé le", "userUpdate",
                        "Modifié par", "userUpdateDateTime", "Modifié le")));
    }

    public static Fiche prospectionTerrain(ProspectionTerrainDetailDto d) {
        Map<String, Object> v = LignesAplaties.de(d);
        String code = d.ligne().code();
        return new Fiche("fiche-prospection-terrain_" + code, "Fiche prospection terrain " + code, null, List.of(
                section(v, IDENTIFICATION, CODE, LIBELLE_CODE, "date", "Date", DESCRIPTION, LIBELLE_DESCRIPTION,
                        "categorie", "Catégorie", "type", "Type", "niveau", "Niveau", "vocation", "Vocation", "statut",
                        "Statut", "dateValiditeStatut", "Validité du statut", "courtier", "Courtier", "notaire",
                        "Notaire", "estArchivee", "Archivée", "estTransformee", "Transformée", "terrainCode",
                        "Terrain issu"),
                section(v, "Situation", "pays", "Pays", "ville", "Ville", "adresse", "Adresse", "orientation",
                        "Orientation", "lienGoogle", "Lien Google Maps", "proprietaires", "Propriétaires"),
                section(v, SUPERFICIES, "superficie", SUPERFICIE, "superficieNette", "Superficie nette",
                        "superficieCos", "Emprise au sol", "superficieCuf", "Plancher couvert", "superficieVendable",
                        "Plancher vendable", "prixM2", "Prix du m² du propriétaire", "prix", "Prix total du propriétaire",
                        "prixPropose", "Prix proposé"),
                section(v, FONCIER, concat(ENREGISTREMENT, new String[]{"arreteAlignement", "Arrêté d'alignement",
                        "hauteurMax", "Hauteur maximale"})),
                section(v, AUTRES, "notes", "Notes", "anomalies", "Anomalies"),
                section(v, "Audit", AUDIT)));
    }

    public static Fiche entite(EntiteJuridiqueDto d) {
        Map<String, Object> v = LignesAplaties.de(d);
        return new Fiche("fiche-entite_" + d.code(), "Fiche entité " + d.code(), d.id(), List.of(
                section(v, IDENTIFICATION, CODE, LIBELLE_CODE, DESCRIPTION, LIBELLE_DESCRIPTION, "isActive", "Active",
                        "formeJuridique", "Forme juridique", "identifiantFiscal", "Matricule fiscale", "registreCommerce",
                        "Registre de commerce", "dateCreation", "Date de création", "capital", "Capital", "cnss", "CNSS",
                        "patente", "Patente", "ice", "Identifiant commun de l'entreprise", "deviseCode", "Devise",
                        "dateLiquidation", "Date de liquidation", "partMaison", "Part maison", DESCRIPTION_LONGUE,
                        DESCRIPTION_DETAILLEE),
                section(v, COORDONNEES_TITRE, concat(COORDONNEES, new String[]{"paysLibelle", "Pays", "villeSignature",
                        "Courrier fait à"})),
                section(v, "Représentants", concat(new String[]{"representantLegalLibelle", "Représentant légal"},
                        RESPONSABLES)),
                section(v, "Actionnaires", "groupeActionnairesLibelle", "Groupe d'actionnaires", "nombreActionnaires",
                        "Nombre d'actionnaires", "nombreParts", "Nombre de parts (actions)"),
                Section.tableau("Répartition", List.of(LIBELLE_CODE, "Actionnaire", "Nombre de parts"),
                        d.repartition(),
                        r -> new Object[]{r.actionnaireCode(), r.actionnaireLibelle(), r.nombreParts()}),
                section(v, "Audit", AUDIT)));
    }

    /** {@code nomsEtLibelles} : paires (nom de la valeur, libellé imprimé). */
    private static Section section(Map<String, Object> valeurs, String titre, String... nomsEtLibelles) {
        List<Champ> champs = new ArrayList<>();
        for (int i = 0; i + 1 < nomsEtLibelles.length; i += 2) {
            String nom = nomsEtLibelles[i];
            Object valeur = nom.startsWith("%") ? pourcentage(valeurs.get(nom.substring(1))) : valeurs.get(nom);
            champs.add(FichePdf.champ(nomsEtLibelles[i + 1], valeur));
        }
        return Section.champs(titre, champs.toArray(Champ[]::new));
    }

    private static String pourcentage(Object fraction) {
        return fraction instanceof BigDecimal b ? MiseEnPagePdf.texte(b.movePointRight(2), TypeExport.NOMBRE) + " %" : null;
    }

    private static String[] concat(String[] a, String[] b) {
        String[] tout = new String[a.length + b.length];
        System.arraycopy(a, 0, tout, 0, a.length);
        System.arraycopy(b, 0, tout, a.length, b.length);
        return tout;
    }
}
