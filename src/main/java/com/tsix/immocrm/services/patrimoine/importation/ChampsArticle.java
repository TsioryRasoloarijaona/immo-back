package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkCostCategory;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.BOOLEEN;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.BOOLEEN_INVERSE;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.DATE;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.ENTIER;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.NOMBRE;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.TEXTE;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.TVA;

/**
 * En-têtes OMRAN des imports d'articles et de sous-articles (omran-src/Yes.Rems/Yes.Stk.Front.Import/ImportItems.cs,
 * ImportUnderItems.cs) → champ d'{@code ArticleEcritureDto} ; {@code valeur} pré-remplit le modèle.
 */
final class ChampsArticle {

    static final String PIVOT = "Pivot";
    static final String TRANCHE = "Tranche";
    static final String NATURE = "Nature";
    static final String VOCATION = "Vocation";
    static final String ARTICLE_PARENT = "Article parent";
    static final int SOUS_ARTICLES_PAR_LIGNE = 3;

    enum Portee { TOUS, ARTICLES, SOUS_ARTICLES }

    record Reference(Class<? extends CodeLibelleOmran> classe, boolean parLibelle) {
    }

    /**
     * {@code champ} nul = colonne de contrôle (pivot, tranche, nature, parent) ; {@code rang} 0 = l'objet de la ligne,
     * n = son sous-article n (colonnes « Sous article n … »).
     */
    record ChampImport(String entete, String champ, TypeCellule type, Reference reference,
                       Function<StkItem, Object> valeur, Portee portee, int rang) {

        boolean pour(boolean sousArticles) {
            return portee == Portee.TOUS || (portee == Portee.SOUS_ARTICLES) == sousArticles;
        }
    }

    static final List<ChampImport> CHAMPS = champs();

    private ChampsArticle() {
    }

    static List<ChampImport> du(boolean sousArticles) {
        return CHAMPS.stream().filter(c -> c.pour(sousArticles)).toList();
    }

    private static List<ChampImport> champs() {
        List<ChampImport> c = new ArrayList<>(List.of(
                controle(PIVOT, StkItem::getCode, Portee.TOUS),
                controle(TRANCHE, i -> Champs.de(i.getStkHierarchy(), CfgTranche::getCode), Portee.TOUS),
                controle(NATURE, StkItem::getStkKind, Portee.ARTICLES),
                code(VOCATION, "vocationId", StkVocation.class, false, StkItem::getStkVocation),
                champ("Code", "code", TEXTE, StkItem::getCode),
                champ("Description", "description", TEXTE, StkItem::getDescription),
                code("Immeuble", "immeubleId", StkBuilding.class, true, StkItem::getStkBuilding),
                code("Etage", "etageId", StkFloor.class, true, StkItem::getStkFloor),
                code("Catégorie", "categorieId", StkItemCategory.class, true, StkItem::getStkItemCategory),
                code("Type", "typeId", StkItemType.class, false, StkItem::getStkItemType),
                new ChampImport("Type présentation", "presentationId", TypeCellule.REFERENCE,
                        new Reference(StkPresentationType.class, false), StkItem::getStkPresentationType, Portee.ARTICLES, 0),
                code("Orientation", "orientationId", StkOrientation.class, false, StkItem::getStkOrientation),
                code("Catégorie de charge", "categorieChargeId", StkCostCategory.class, true, StkItem::getStkCostCategory),
                code("Entité de gestion du syndic", "entiteSyndicId", CfgCompany.class, false, StkItem::getCfgCompanySyndic),
                controle(ARTICLE_PARENT, i -> Champs.de(i.getParent(), StkItem::getCode), Portee.SOUS_ARTICLES),
                new ChampImport("Toujours affecté", "toujoursAffecte", BOOLEEN, null, StkItem::getAlwaysAffected,
                        Portee.SOUS_ARTICLES, 0),
                champ("Adresse", "adresse", TEXTE, StkItem::getAddress),
                champ("Notes", "notes", TEXTE, StkItem::getNotes),
                champ("Prix m² Prov. HT", "prixM2ProvisoireHT", NOMBRE, StkItem::getComSaleUnitCostFloorHT),
                champ("Prix m² Defi. HT", "prixM2DefinitifHT", NOMBRE, StkItem::getComSaleUnitCostHT),
                champ("Superficie Prov. (m²)", "surfaceProvisoire", NOMBRE, StkItem::getAreaFloor),
                champ("Superficie Defi. (m²)", "surfaceDefinitive", NOMBRE, StkItem::getArea),
                champ("TVA", "tauxTva", TVA, StkItem::getVat),
                champ("Niveau de remise 1", "remiseMax1", NOMBRE, StkItem::getDiscountMax),
                champ("Niveau de remise 2", "remiseMax2", NOMBRE, StkItem::getDiscountMax2),
                champ("Avance min.", "avanceMinimum", NOMBRE, StkItem::getComSaleMinAdvance),
                champ("Loyer mensuel HT", "loyerMensuelHT", NOMBRE, StkItem::getAmountHT),
                champ("Nombre de caution", "nombreCautions", ENTIER, StkItem::getCautionNumber),
                champ("Titré", "estTitre", BOOLEEN, StkItem::getIsTitled),
                champ("Date enregistrement", "dateEnregistrement", DATE, StkItem::getRegistryDate),
                champ("N° enregistrement", "numeroEnregistrement", TEXTE, StkItem::getRegistryNumber),
                champ("Lieu enregistrement", "lieuEnregistrement", TEXTE, StkItem::getRegistryPlace),
                champ("C.P.F. Immatriculation", "cpfImmatriculation", TEXTE, StkItem::getImmatriculationCPF),
                champ("Date Immatriculation", "dateImmatriculation", DATE, StkItem::getImmatriculationDate),
                champ("N° Titre foncier mère", "titreFoncierMere", TEXTE, StkItem::getTitleNumberMother),
                champ("N° Titre foncier", "titreFoncier", TEXTE, StkItem::getTitleNumber),
                champ("Intitulé du Titre foncier", "intituleTitreFoncier", TEXTE, StkItem::getTitleName),
                champ("Date du Titre foncier", "dateTitreFoncier", DATE, StkItem::getTitleDate),
                champ("Proprieté dite", "proprieteDite", TEXTE, StkItem::getPropertyCalled),
                champ("fraction divise N°", "fractionDivise", TEXTE, StkItem::getFractionDivided),
                champ("fraction divise terrain", "fractionDiviseTerrain", TEXTE, StkItem::getFractionDividedLand),
                champ("Parties divise", "partiesDivises", NOMBRE, StkItem::getDividedPart),
                champ("Parties indivise", "partiesIndivises", NOMBRE, StkItem::getUndividedPart),
                champ("Quota terrain groupe", "quotaTerrainGroupe", TEXTE, StkItem::getGroupLandQuota),
                champ("Notes Titre foncier", "notesTitreFoncier", TEXTE, StkItem::getTitleNotes),
                champ("Référence électricité", "referenceElectricite", TEXTE, StkItem::getElectricityRef),
                champ("N° police électricité", "policeElectricite", TEXTE, StkItem::getElectricityMeterPolice),
                champ("N° Compteur électricité", "compteurElectricite", TEXTE, StkItem::getElectricityMeter),
                champ("Arrêté du compteur électricité", "releveElectricite", TEXTE, StkItem::getElectricityMeterIndex),
                champ("Référence eau", "referenceEau", TEXTE, StkItem::getWaterRef),
                champ("N° police eau", "policeEau", TEXTE, StkItem::getWaterMeterPolice),
                champ("N° Compteur eau", "compteurEau", TEXTE, StkItem::getWaterMeter),
                champ("Arrêté du compteur eau", "releveEau", TEXTE, StkItem::getWaterMeterIndex),
                champ("Référence gaz", "referenceGaz", TEXTE, StkItem::getGasRef),
                champ("N° police gaz", "policeGaz", TEXTE, StkItem::getGasMeterPolice),
                champ("N° Compteur gaz", "compteurGaz", TEXTE, StkItem::getGasMeter),
                champ("Arrêté du compteur gaz", "releveGaz", TEXTE, StkItem::getGasMeterIndex),
                champ("Indisponible pour la vente sur le web", "disponibleVenteWeb", BOOLEEN_INVERSE,
                        StkItem::getIsAvailableForSaleInWebSite),
                champ("Indisponible pour la location sur le web", "disponibleLocationWeb", BOOLEEN_INVERSE,
                        StkItem::getIsAvailableForRentInWebSite),
                champ("Superficie intérieure (m²)", "surfaceInterieure", NOMBRE, StkItem::getInteriorArea),
                champ("Superficie murs (m²)", "surfaceMurs", NOMBRE, StkItem::getWallsArea),
                champ("Superficie parties communes (m²)", "surfacePartiesCommunes", NOMBRE, StkItem::getCommonPartsArea),
                new ChampImport("Contenance", "contenanceLibre", TEXTE, null, StkItem::getContenance, Portee.ARTICLES, 0),
                champ("Anomalies", "anomalies", TEXTE, StkItem::getAnomalies)));
        for (int n = 1; n <= SOUS_ARTICLES_PAR_LIGNE; n++) {
            c.addAll(sousArticle(n));
        }
        return List.copyOf(c);
    }

    /** OMRAN : catégorie et type du sous-article par code seul ; surfaces et prix HT, TVA, « affecté par défaut ». */
    private static List<ChampImport> sousArticle(int n) {
        String p = "Sous article " + n + " ";
        return List.of(
                new ChampImport(p + "code", "code", TEXTE, null, StkItem::getCode, Portee.ARTICLES, n),
                new ChampImport(p + "catégorie code", "categorieId", TypeCellule.REFERENCE,
                        new Reference(StkItemCategory.class, false), StkItem::getStkItemCategory, Portee.ARTICLES, n),
                new ChampImport(p + "type code", "typeId", TypeCellule.REFERENCE, new Reference(StkItemType.class, false),
                        StkItem::getStkItemType, Portee.ARTICLES, n),
                new ChampImport(p + "prix m² Prov. HT", "prixM2ProvisoireHT", NOMBRE, null,
                        StkItem::getComSaleUnitCostFloorHT, Portee.ARTICLES, n),
                new ChampImport(p + "prix m² Defi. HT", "prixM2DefinitifHT", NOMBRE, null, StkItem::getComSaleUnitCostHT,
                        Portee.ARTICLES, n),
                new ChampImport(p + "Superficie Prov. (m²)", "surfaceProvisoire", NOMBRE, null, StkItem::getAreaFloor,
                        Portee.ARTICLES, n),
                new ChampImport(p + "Superficie Defi. (m²)", "surfaceDefinitive", NOMBRE, null, StkItem::getArea,
                        Portee.ARTICLES, n),
                new ChampImport(p + "TVA", "tauxTva", TVA, null, StkItem::getVat, Portee.ARTICLES, n),
                new ChampImport(p + "affecté par défaut", "toujoursAffecte", BOOLEEN, null, StkItem::getAlwaysAffected,
                        Portee.ARTICLES, n));
    }

    private static ChampImport champ(String entete, String champ, TypeCellule type, Function<StkItem, Object> valeur) {
        return new ChampImport(entete, champ, type, null, valeur, Portee.TOUS, 0);
    }

    private static ChampImport code(String entete, String champ, Class<? extends CodeLibelleOmran> classe, boolean parLibelle,
                                    Function<StkItem, Object> valeur) {
        return new ChampImport(entete, champ, TypeCellule.REFERENCE, new Reference(classe, parLibelle), valeur, Portee.TOUS, 0);
    }

    private static ChampImport controle(String entete, Function<StkItem, Object> valeur, Portee portee) {
        return new ChampImport(entete, null, TEXTE, null, valeur, portee, 0);
    }

    /** Valeur écrite dans le modèle : code d'une référence, case « Indisponible » inversée. */
    static Object pourModele(ChampImport c, StkItem item) {
        Object v = c.valeur().apply(item);
        if (v instanceof CodeLibelleOmran ref) {
            return ref.getCode();
        }
        if (c.type() == BOOLEEN_INVERSE && v instanceof Boolean b) {
            return !b;
        }
        return v;
    }
}
