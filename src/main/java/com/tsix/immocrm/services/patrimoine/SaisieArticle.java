package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.utils.Champs;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_COURTE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_LONGUE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_MOYENNE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.fraction;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.positifOuNul;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.si;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.texte;

/** Valeurs initiales et report des saisies d'un article (FEATURE-patrimoine-ecriture.md §5.2, §5.3, §2.4). */
final class SaisieArticle {

    private SaisieArticle() {
    }

    /** Les entrées du calcul de prix : on ne recalcule (et ne réécrit) les prix stockés que si l'une d'elles change. */
    record EntreesPrix(BigDecimal surfaceProvisoire, BigDecimal surfaceDefinitive, BigDecimal m2ProvisoireHT,
                       BigDecimal m2DefinitifHT, BigDecimal tva) {

        static EntreesPrix de(StkItem item) {
            return new EntreesPrix(item.getAreaFloor(), item.getArea(), item.getComSaleUnitCostFloorHT(),
                    item.getComSaleUnitCostHT(), item.getVat());
        }

        boolean differeDe(EntreesPrix autre) {
            return !(memeValeur(surfaceProvisoire, autre.surfaceProvisoire)
                    && memeValeur(surfaceDefinitive, autre.surfaceDefinitive)
                    && memeValeur(m2ProvisoireHT, autre.m2ProvisoireHT)
                    && memeValeur(m2DefinitifHT, autre.m2DefinitifHT)
                    && memeValeur(tva, autre.tva));
        }
    }

    /**
     * Tout ce qu'OMRAN écrit non NULL (ligne modèle ART344) : nombres à 0, booléens à faux, textes vides. Restent NULL
     * les colonnes tardives (§2.4), les dates, les clés facultatives, {@code Owner} et {@code GmapLink}.
     */
    static void initialiserDefauts(StkItem i) {
        initialiserMontants(i);
        initialiserMesures(i);
        initialiserTextes(i);
        initialiserCompteurs(i);
        i.setIsUnderItem(false);
        i.setIsUnderItemOrigin(false);
        i.setAlwaysAffected(false);
        i.setIsAvailableForSaleInWebSite(false);
        i.setIsAvailableForRentInWebSite(false);
        i.setIsTitled(true);
        i.setIsReversmentBasedOnPayment(false);
        i.setAreaFloor(BigDecimal.ONE);
    }

    private static void initialiserMontants(StkItem i) {
        i.setComSaleUnitCost(BigDecimal.ZERO);
        i.setComSaleUnitCostHT(BigDecimal.ZERO);
        i.setComSaleUnitCostFloor(BigDecimal.ZERO);
        i.setComSaleUnitCostFloorHT(BigDecimal.ZERO);
        i.setSalePrice(BigDecimal.ZERO);
        i.setSalePriceHT(BigDecimal.ZERO);
        i.setSalePriceFloor(BigDecimal.ZERO);
        i.setSalePriceFloorHT(BigDecimal.ZERO);
        i.setComSalePrice(BigDecimal.ZERO);
        i.setComSalePriceHT(BigDecimal.ZERO);
        i.setComSalePriceFloor(BigDecimal.ZERO);
        i.setComSalePriceFloorHT(BigDecimal.ZERO);
        i.setComSaleMinAdvance(BigDecimal.ZERO);
        i.setVat(BigDecimal.ZERO);
        i.setDiscountMax(BigDecimal.ZERO);
        i.setDiscountMax2(BigDecimal.ZERO);
        i.setAmountHT(BigDecimal.ZERO);
        i.setPurchaseUnitCost(BigDecimal.ZERO);
        i.setPurchaseCost(BigDecimal.ZERO);
        i.setProspectionUnitCost(BigDecimal.ZERO);
        i.setProspectionPrice(BigDecimal.ZERO);
        i.setProspectionPriceProposed(BigDecimal.ZERO);
        i.setUnitCostNonBuiltPrice(BigDecimal.ZERO);
        i.setCommissionRate(BigDecimal.ZERO);
    }

    private static void initialiserMesures(StkItem i) {
        i.setInteriorArea(BigDecimal.ZERO);
        i.setWallsArea(BigDecimal.ZERO);
        i.setCommonPartsArea(BigDecimal.ZERO);
        i.setAreaNetOld(BigDecimal.ZERO);
        i.setCos(BigDecimal.ZERO);
        i.setCuf(BigDecimal.ZERO);
        i.setAreaCos(BigDecimal.ZERO);
        i.setAreaCuf(BigDecimal.ZERO);
        i.setDividedPart(BigDecimal.ZERO);
        i.setUndividedPart(BigDecimal.ZERO);
        i.setEquipementsCoef(BigDecimal.ZERO);
        i.setEquipementsArea(BigDecimal.ZERO);
        i.setSalableArea(BigDecimal.ZERO);
        i.setCoefSaleAbleArea(BigDecimal.ZERO);
        i.setCommerceArea(BigDecimal.ZERO);
        i.setHabitatArea(BigDecimal.ZERO);
        i.setMapLatitude(BigDecimal.ZERO);
        i.setMapLongitude(BigDecimal.ZERO);
        i.setAlinmentStop(0);
        i.setNeighborStop(0);
        i.setMaxHeight(0);
        i.setCautionNumber(0);
    }

    private static void initialiserTextes(StkItem i) {
        i.setAddress("");
        i.setNotes("");
        i.setContenance("");
        i.setAnomalies("");
        i.setTitleNumber("");
        i.setTitleNumberMother("");
        i.setTitleName("");
        i.setTitleNotes("");
        i.setPropertyCalled("");
        i.setFractionDivided("");
        i.setFractionDividedLand("");
        i.setGroupLandQuota("");
        i.setRegistryNumber("");
        i.setRegistryPlace("");
        i.setImmatriculationCPF("");
    }

    private static void initialiserCompteurs(StkItem i) {
        i.setElectricityRef("");
        i.setElectricityMeter("");
        i.setElectricityMeterIndex("");
        i.setElectricityMeterPolice("");
        i.setWaterRef("");
        i.setWaterMeter("");
        i.setWaterMeterIndex("");
        i.setWaterMeterPolice("");
        i.setGasRef("");
        i.setGasMeter("");
        i.setGasMeterIndex("");
        i.setGasMeterPolice("");
    }

    /** Saisies facultatives (surfaces, prix, TVA, remises, textes) : absent = inchangé, contrôlées avant d'être posées. */
    static void appliquer(StkItem i, ArticleEcritureDto d) {
        si(d.surfaceProvisoire(), i::setAreaFloor);
        si(d.surfaceDefinitive(), i::setArea);
        si(positifOuNul(d.prixM2ProvisoireHT(), "prixM2ProvisoireHT"), i::setComSaleUnitCostFloorHT);
        si(positifOuNul(d.prixM2DefinitifHT(), "prixM2DefinitifHT"), i::setComSaleUnitCostHT);
        si(fraction(d.tauxTva(), "tauxTva"), i::setVat);
        si(fraction(d.remiseMax1(), "remiseMax1"), i::setDiscountMax);
        si(fraction(d.remiseMax2(), "remiseMax2"), i::setDiscountMax2);
        si(d.estTitre(), i::setIsTitled);
        texte(d.titreFoncier(), LONGUEUR_COURTE, "titreFoncier", i::setTitleNumber);
        texte(d.adresse(), LONGUEUR_MOYENNE, "adresse", i::setAddress);
        texte(d.notes(), LONGUEUR_LONGUE, "notes", i::setNotes);
        si(positifOuNul(d.surfaceInterieure(), "surfaceInterieure"), i::setInteriorArea);
        si(positifOuNul(d.surfaceMurs(), "surfaceMurs"), i::setWallsArea);
        si(positifOuNul(d.surfacePartiesCommunes(), "surfacePartiesCommunes"), i::setCommonPartsArea);
    }

    /** Messages d'OMRAN ({@code Item.AfterVerifMadatories}). */
    static void verifierSurfaces(StkItem i) {
        if (i.getAreaFloor() == null || i.getAreaFloor().signum() <= 0) {
            throw new ChampInvalideException("surfaceProvisoire",
                    "La valeur de la supérficie provisoire doit être strictement supérieure à 0 !");
        }
        if (i.getArea() == null || i.getArea().signum() <= 0) {
            throw new ChampInvalideException("surfaceDefinitive",
                    "La valeur de la supérficie définitive doit être strictement supérieure à 0 !");
        }
    }

    /** Premier champ protégé (§5.4.4) que la demande change réellement ; vide si seuls des champs libres changent. */
    static Optional<String> champProtegeModifie(StkItem i, ArticleEcritureDto d) {
        Map<String, Boolean> ecarts = new LinkedHashMap<>();
        ecarts.put("code", d.code() != null && !d.code().trim().equals(i.getCode()));
        ecarts.put("vocationId", differe(d.vocationId(), Champs.de(i.getStkVocation(), StkVocation::getId)));
        ecarts.put("categorieId", differe(d.categorieId(), Champs.de(i.getStkItemCategory(), StkItemCategory::getId)));
        ecarts.put("typeId", differe(d.typeId(), Champs.de(i.getStkItemType(), StkItemType::getId)));
        ecarts.put("surfaceProvisoire", differe(d.surfaceProvisoire(), i.getAreaFloor()));
        ecarts.put("surfaceDefinitive", differe(d.surfaceDefinitive(), i.getArea()));
        ecarts.put("prixM2ProvisoireHT", differe(d.prixM2ProvisoireHT(), i.getComSaleUnitCostFloorHT()));
        ecarts.put("prixM2DefinitifHT", differe(d.prixM2DefinitifHT(), i.getComSaleUnitCostHT()));
        ecarts.put("tauxTva", differe(d.tauxTva(), i.getVat()));
        ecarts.put("remiseMax1", differe(d.remiseMax1(), i.getDiscountMax()));
        ecarts.put("remiseMax2", differe(d.remiseMax2(), i.getDiscountMax2()));
        return ecarts.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).findFirst();
    }

    private static boolean differe(UUID demande, UUID actuel) {
        return demande != null && !demande.equals(actuel);
    }

    /** Comparaison par valeur ({@code 63} = {@code 63.000000}) ; une demande absente ne change rien. */
    private static boolean differe(BigDecimal demande, BigDecimal actuel) {
        return demande != null && !memeValeur(demande, actuel);
    }

    private static boolean memeValeur(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
    }
}
