package com.tsix.immocrm.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/articles} et {@code GET /api/sous-articles} : tableau Liste de la page
 * ARTICLES de l'aide OMRAN plus 8 colonnes du grid (FEATURE-patrimoine-sous-articles.md R3, réponse 2). Défauts =
 * champs d'{@code ArticleListItemDto}. Libellés {@code Id …} : identifiants techniques, hors aide.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ColonneArticle implements Colonne {

    // -- Identification.
    ID("id", "Identifiant", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    CODE("code", "Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    DESCRIPTION("description", "Description", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    TRANCHE_CODE("trancheCode", "Tranche Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    TRANCHE("trancheDescription", "Tranche", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    PROGRAMME_CODE("programmeCode", "Programme Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    PROGRAMME("programmeLibelle", "Programme", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    ENTITE_CODE("entiteCode", "Entité Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    ENTITE("entiteLibelle", "Entité", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    PARENT_ID("lotParentId", "Id article parent", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    PARENT_CODE("parentCode", "Article parent", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    PARENT_DESCRIPTION("parentDescription", "Article parent description", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    RATTACHE("sousArticle", "Rattaché à un article", Groupe.IDENTIFICATION, TypeColonne.BOOLEEN, true),
    EST_SOUS_ARTICLE("estSousArticle", "Est un sous article", Groupe.IDENTIFICATION, TypeColonne.BOOLEEN),
    TOUJOURS_AFFECTE("toujoursAffecte", "Affecté par défaut", Groupe.IDENTIFICATION, TypeColonne.BOOLEEN),
    SOUS_ARTICLES("sousArticlesCodes", "Sous articles", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    IMMEUBLE_ID("immeubleId", "Id immeuble", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    IMMEUBLE_CODE("immeubleCode", "Immeuble Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    IMMEUBLE("immeubleLibelle", "Immeuble", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    ETAGE_ID("etageId", "Id étage", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    ETAGE_CODE("etageCode", "Étage Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    ETAGE("etageLibelle", "Étage", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    VOCATION_CODE("vocationCode", "Vocation Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    VOCATION("vocationLibelle", "Vocation", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    NATURE_ID("natureId", "Id nature", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    NATURE_CODE("natureCode", "Nature Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    NATURE("natureLibelle", "Nature", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    CATEGORIE_ID("categorieId", "Id catégorie", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    CATEGORIE_CODE("categorieCode", "Catégorie Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    CATEGORIE("categorieLibelle", "Catégorie", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    TYPE_ID("typeId", "Id type", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    TYPE_CODE("typeCode", "Type Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    TYPE("typeLibelle", "Type", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    PRESENTATION_ID("presentationTypeId", "Id type de présentation", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    PRESENTATION_CODE("presentationTypeCode", "Type Présentation Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE,
            true),
    PRESENTATION("presentationTypeLibelle", "Type Présentation", Groupe.IDENTIFICATION, TypeColonne.TEXTE, true),
    ORIENTATION_CODE("orientationCode", "Orientation Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    ORIENTATION("orientationLibelle", "Orientation", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    CHARGE_CODE("categorieChargeCode", "Catégorie de Charge Code", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    CHARGE("categorieChargeLibelle", "Catégorie de Charge", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    ADRESSE("adresse", "Adresse", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    NOTES("notes", "Notes", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    ANOMALIES("anomalies", "Anomalies", Groupe.IDENTIFICATION, TypeColonne.TEXTE),
    CONTENANCE("contenanceLibre", "Contenance", Groupe.IDENTIFICATION, TypeColonne.TEXTE),

    // -- État et dossier.
    STATUT("statut", "Disponibilité", Groupe.ETAT, TypeColonne.TEXTE, true),
    ETAT("etat", "État", Groupe.ETAT, TypeColonne.TEXTE),
    PARENT_OCCUPE("parentOccupe", "Parent occupé", Groupe.ETAT, TypeColonne.BOOLEEN),
    DOSSIER_ID("dossierId", "Id dossier", Groupe.ETAT, TypeColonne.TEXTE, true),
    DOSSIER_CODE("dossierCode", "N° Dossier", Groupe.ETAT, TypeColonne.TEXTE, true),
    DOSSIER_STATUT("dossierStatut", "Code statut du dossier", Groupe.ETAT, TypeColonne.TEXTE, true),
    CLIENT_ID("clientId", "Id client", Groupe.ETAT, TypeColonne.TEXTE, true),
    CLIENT("clientNom", "Client", Groupe.ETAT, TypeColonne.TEXTE, true),

    // -- Superficies, taux, avance, loyer.
    SURFACE("area", "Superficie (m²)", Groupe.SUPERFICIES, TypeColonne.NOMBRE, true),
    SURFACE_PROVISOIRE("surfaceProvisoire", "Superficie Prov. (m²)", Groupe.SUPERFICIES, TypeColonne.NOMBRE),
    SURFACE_NETTE("superficieNette", "Superficie Nette (m²)", Groupe.SUPERFICIES, TypeColonne.NOMBRE),
    SURFACE_SOUS_ARTICLES("superficieSousArticles", "Superficie Sous Article (m²)", Groupe.SUPERFICIES,
            TypeColonne.NOMBRE, true),
    TVA("tauxTVA", "TVA", Groupe.SUPERFICIES, TypeColonne.POURCENTAGE, true),
    REMISE_MAX("remiseMax1", "Remise max", Groupe.SUPERFICIES, TypeColonne.POURCENTAGE),
    REMISE_MAX_2("remiseMax2", "Niveau de remise 2", Groupe.SUPERFICIES, TypeColonne.POURCENTAGE),
    AVANCE("avanceMinimum", "Avance", Groupe.SUPERFICIES, TypeColonne.MONTANT),
    LOYER_HT("loyerMensuelHT", "Loyer mensuel HT", Groupe.SUPERFICIES, TypeColonne.MONTANT),
    LOYER_TTC("loyerMensuelTTC", "Loyer mensuel TTC", Groupe.SUPERFICIES, TypeColonne.MONTANT),
    CAUTIONS("nombreCautions", "Nombre de caution", Groupe.SUPERFICIES, TypeColonne.NOMBRE),

    // -- Prix.
    PRIX_M2_HT("prixM2DefinitifHT", "Prix du m² HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_M2_TTC("prixM2DefinitifTTC", "Prix du m² TTC", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_M2_PROV_HT("prixM2ProvisoireHT", "Prix du m² Prov.HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_M2_PROV_TTC("prixM2ProvisoireTTC", "Prix du m² Prov.TTC", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_ARTICLE_HT("prixArticleDefinitifHT", "Prix Article HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_ARTICLE_TTC("salePrice", "Prix Article TTC", Groupe.PRIX, TypeColonne.MONTANT, true),
    PRIX_ARTICLE_PROV_HT("prixArticleProvisoireHT", "Prix Article Prov.HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_ARTICLE_PROV_TTC("salePriceFloor", "Prix Article Prov.TTC", Groupe.PRIX, TypeColonne.MONTANT, true),
    PRIX_SOUS_HT("prixSousArticlesHT", "Prix Sous Article HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_SOUS_TTC("prixSousArticlesTTC", "Prix Sous Article TTC", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_SOUS_PROV_HT("prixSousArticlesProvisoireHT", "Prix Sous Article Prov.HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_SOUS_PROV_TTC("prixSousArticlesProvisoireTTC", "Prix Sous Article Prov.TTC", Groupe.PRIX,
            TypeColonne.MONTANT),
    PRIX_VENTE_HT("prixVenteDefinitifHT", "Prix de Ventes HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_VENTE_TTC("comSalePrice", "Prix de Ventes TTC", Groupe.PRIX, TypeColonne.MONTANT, true),
    PRIX_VENTE_PROV_HT("prixVenteProvisoireHT", "Prix de Ventes Prov.HT", Groupe.PRIX, TypeColonne.MONTANT),
    PRIX_VENTE_PROV_TTC("comSalePriceFloor", "Prix de Ventes Prov.TTC", Groupe.PRIX, TypeColonne.MONTANT, true),
    PRIX_REFERENCE("prixSuggere", "Prix de référence TTC", Groupe.PRIX, TypeColonne.MONTANT, true),
    PRIX_PLANCHER("prixSuggereEstPlancher", "Prix de référence plancher", Groupe.PRIX, TypeColonne.BOOLEEN, true),

    // -- Identification foncière.
    TITRE_FONCIER("titreFoncier", "N° Titre foncier", Groupe.FONCIER, TypeColonne.TEXTE, true),
    DATE_ENREGISTREMENT("dateEnregistrement", "Date enregistrement", Groupe.FONCIER, TypeColonne.DATE),
    NUMERO_ENREGISTREMENT("numeroEnregistrement", "N° enregistrement", Groupe.FONCIER, TypeColonne.TEXTE),
    LIEU_ENREGISTREMENT("lieuEnregistrement", "Lieu d'enregistrement", Groupe.FONCIER, TypeColonne.TEXTE),
    CPF("cpfImmatriculation", "CPF Immatriculation", Groupe.FONCIER, TypeColonne.TEXTE),
    DATE_IMMATRICULATION("dateImmatriculation", "Date Immatriculation", Groupe.FONCIER, TypeColonne.DATE),
    TITRE_MERE("titreFoncierMere", "N° Titre foncier mère", Groupe.FONCIER, TypeColonne.TEXTE),
    INTITULE_TITRE("intituleTitreFoncier", "Intitulé du Titre foncier", Groupe.FONCIER, TypeColonne.TEXTE),
    DATE_TITRE("dateTitreFoncier", "Date du titre foncier", Groupe.FONCIER, TypeColonne.DATE),
    NOTES_TITRE("notesTitreFoncier", "Notes du titre foncier", Groupe.FONCIER, TypeColonne.TEXTE),
    PROPRIETE_DITE("proprieteDite", "Propriété dite", Groupe.FONCIER, TypeColonne.TEXTE),
    FRACTION("fractionDivise", "Fraction Divise N°", Groupe.FONCIER, TypeColonne.TEXTE),
    FRACTION_TERRAIN("fractionDiviseTerrain", "Fraction Divise Terrain", Groupe.FONCIER, TypeColonne.TEXTE),
    PARTIES_DIVISES("partiesDivises", "Parties divise", Groupe.FONCIER, TypeColonne.NOMBRE),
    PARTIES_INDIVISES("partiesIndivises", "Parties indivise", Groupe.FONCIER, TypeColonne.NOMBRE),

    // -- Compteurs.
    REF_ELEC("referenceElectricite", "Ref.compt.électricité", Groupe.COMPTEURS, TypeColonne.TEXTE),
    POLICE_ELEC("policeElectricite", "Ref.police compt.électricité", Groupe.COMPTEURS, TypeColonne.TEXTE),
    COMPTEUR_ELEC("compteurElectricite", "N° compt.électricité", Groupe.COMPTEURS, TypeColonne.TEXTE),
    INDEX_ELEC("releveElectricite", "Index compt.électricité", Groupe.COMPTEURS, TypeColonne.TEXTE),
    REF_EAU("referenceEau", "Ref.compt.eau", Groupe.COMPTEURS, TypeColonne.TEXTE),
    POLICE_EAU("policeEau", "Ref.police compt.eau", Groupe.COMPTEURS, TypeColonne.TEXTE),
    COMPTEUR_EAU("compteurEau", "N° compt.eau", Groupe.COMPTEURS, TypeColonne.TEXTE),
    INDEX_EAU("releveEau", "Index compt.eau", Groupe.COMPTEURS, TypeColonne.TEXTE),
    REF_GAZ("referenceGaz", "Ref.compt.gaz", Groupe.COMPTEURS, TypeColonne.TEXTE),
    POLICE_GAZ("policeGaz", "Ref.police compt.gaz", Groupe.COMPTEURS, TypeColonne.TEXTE),
    COMPTEUR_GAZ("compteurGaz", "N° compt.gaz", Groupe.COMPTEURS, TypeColonne.TEXTE),
    INDEX_GAZ("releveGaz", "Index compt.gaz", Groupe.COMPTEURS, TypeColonne.TEXTE),

    // -- Piste d'audit.
    CREE_PAR("creePar", "Crée par", Groupe.AUDIT, TypeColonne.TEXTE),
    CREE_LE("creeLe", "Crée le", Groupe.AUDIT, TypeColonne.DATE),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE);

    /** Regroupement du sélecteur de colonnes. */
    public enum Groupe { IDENTIFICATION, ETAT, SUPERFICIES, PRIX, FONCIER, COMPTEURS, AUDIT }

    /** Type d'affichage ; POURCENTAGE = fraction (0,2 = 20 %), exportée en nombre. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, POURCENTAGE, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson de {@code LigneArticleDto}. */
    public static final String FILTRE = "colonnesArticle";

    /** Colonnes calculées par lot (sommes des sous-articles) : la requête ne part que si l'une est demandée. */
    public static final Set<ColonneArticle> SOMMES_SOUS_ARTICLES =
            Set.of(PRIX_SOUS_HT, PRIX_SOUS_TTC, PRIX_SOUS_PROV_HT, PRIX_SOUS_PROV_TTC);

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;

    ColonneArticle(String nom, String libelle, Groupe groupe, TypeColonne type) {
        this(nom, libelle, groupe, type, false);
    }

    @Override
    public TypeExport typeExport() {
        return type == TypeColonne.POURCENTAGE ? TypeExport.NOMBRE : TypeExport.valueOf(type.name());
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue (400 {@code colonnesValides}). */
    public static Set<ColonneArticle> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneArticle.class, demandees, ID);
    }
}
