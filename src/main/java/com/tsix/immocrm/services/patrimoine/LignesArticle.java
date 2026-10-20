package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.LigneArticleDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.ArticleService.estOccupe;

/**
 * Lignes {@link LigneArticleDto} d'une page : champs d'{@link ArticleService#toListItem} plus le catalogue
 * {@link ColonneArticle} ; les sommes, codes des sous-articles et le parent ne sont lus que si leur colonne est demandée.
 */
@Component
record LignesArticle(StkItemRepository items, ComFolderRepository folders) {

    private static final Set<ColonneArticle> COLONNES_PARENT =
            Set.of(ColonneArticle.PARENT_CODE, ColonneArticle.PARENT_DESCRIPTION, ColonneArticle.PARENT_OCCUPE);

    /** Lot de calculs d'une page, chargé une fois. */
    private record Lot(Map<UUID, ComFolder> dossiers, Map<UUID, BigDecimal> superficies, Map<UUID, Object[]> sommes,
                       Map<UUID, String> codes, boolean parent, boolean sousArticles) {
    }

    /** Une requête pour toute la page, pas une par article. */
    Map<UUID, ComFolder> dossiers(List<StkItem> articles) {
        List<UUID> folderIds = articles.stream().map(StkItem::getComFolderId).filter(Objects::nonNull).distinct().toList();
        if (folderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return folders.findWithStatusAndCustomerByIdIn(folderIds).stream()
                .collect(Collectors.toMap(ComFolder::getId, Function.identity()));
    }

    /** Par id d'article parent ; absent = aucun sous-article, {@code toListItem} rend alors zéro. */
    Map<UUID, BigDecimal> superficies(List<StkItem> articles) {
        List<UUID> ids = articles.stream().map(StkItem::getId).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return items.sumAreaGroupedByParentIdIn(ids).stream()
                .collect(Collectors.toMap(r -> (UUID) r[0], r -> (BigDecimal) r[1]));
    }

    Page<LigneArticleDto> de(Page<StkItem> page, Set<ColonneArticle> colonnes) {
        List<UUID> ids = page.getContent().stream().map(StkItem::getId).toList();
        boolean sommes = !ids.isEmpty() && colonnes.stream().anyMatch(ColonneArticle.SOMMES_SOUS_ARTICLES::contains);
        boolean codes = !ids.isEmpty() && colonnes.contains(ColonneArticle.SOUS_ARTICLES);
        Lot lot = new Lot(dossiers(page.getContent()), superficies(page.getContent()),
                sommes ? items.sumPrixGroupedByParentIdIn(ids).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], Function.identity())) : Collections.emptyMap(),
                codes ? items.codesByParentIdIn(ids).stream().collect(Collectors.groupingBy(r -> (UUID) r[0],
                        Collectors.mapping(r -> (String) r[1], Collectors.joining(", ")))) : Collections.emptyMap(),
                colonnes.stream().anyMatch(COLONNES_PARENT::contains), codes);
        return page.map(a -> ligne(a, lot));
    }

    private static LigneArticleDto ligne(StkItem a, Lot lot) {
        ArticleListItemDto l = ArticleService.toListItem(a, lot.dossiers(), lot.superficies());
        CfgTranche tranche = a.getStkHierarchy();
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgCompany entite = Champs.de(projet, CfgProject::getCfgCompany);
        StkItem parent = lot.parent() ? a.getParent() : null;
        Object[] sommes = lot.sommes().get(a.getId());
        return new LigneArticleDto(
                l.id(), l.code(), l.description(),
                code(tranche), libelle(tranche), code(projet), libelle(projet),
                code(entite), libelle(entite),
                l.lotParentId(), Champs.de(parent, StkItem::getCode), Champs.de(parent, StkItem::getDescription),
                l.sousArticle(), a.getIsUnderItem(), a.getAlwaysAffected(),
                lot.sousArticles() ? lot.codes().getOrDefault(a.getId(), "") : null,
                l.immeubleId(), l.immeubleCode(), l.immeubleLibelle(), l.etageId(), l.etageCode(), l.etageLibelle(),
                code(a.getStkVocation()), libelle(a.getStkVocation()), l.natureId(), l.natureCode(), l.natureLibelle(),
                l.categorieId(), l.categorieCode(), l.categorieLibelle(), l.typeId(), l.typeCode(), l.typeLibelle(),
                l.presentationTypeId(), l.presentationTypeCode(), l.presentationTypeLibelle(),
                code(a.getStkOrientation()), libelle(a.getStkOrientation()),
                code(a.getStkCostCategory()), libelle(a.getStkCostCategory()),
                a.getAddress(), a.getNotes(), a.getAnomalies(), a.getContenance(),
                l.statut(), etat(a, lot.dossiers()), parent == null ? null : estOccupe(parent),
                l.dossierId(), l.dossierCode(), l.dossierStatut(), l.clientId(), l.clientNom(),
                l.area(), a.getAreaFloor(), somme(l.area(), l.superficieSousArticles()), l.superficieSousArticles(),
                l.tauxTVA(), a.getDiscountMax(), a.getDiscountMax2(), a.getComSaleMinAdvance(), a.getAmountHT(),
                loyerTTC(a), a.getCautionNumber(),
                a.getComSaleUnitCostHT(), a.getComSaleUnitCost(), a.getComSaleUnitCostFloorHT(),
                a.getComSaleUnitCostFloor(), a.getSalePriceHT(), l.salePrice(), a.getSalePriceFloorHT(),
                l.salePriceFloor(), somme(sommes, 1), somme(sommes, 2), somme(sommes, 3), somme(sommes, 4),
                a.getComSalePriceHT(), l.comSalePrice(), a.getComSalePriceFloorHT(), l.comSalePriceFloor(),
                l.prixSuggere(), l.prixSuggereEstPlancher(),
                l.titreFoncier(), a.getRegistryDate(), a.getRegistryNumber(), a.getRegistryPlace(),
                a.getImmatriculationCPF(), a.getImmatriculationDate(), a.getTitleNumberMother(), a.getTitleName(),
                a.getTitleDate(), a.getTitleNotes(), a.getPropertyCalled(), a.getFractionDivided(),
                a.getFractionDividedLand(), a.getDividedPart(), a.getUndividedPart(),
                a.getElectricityRef(), a.getElectricityMeterPolice(), a.getElectricityMeter(),
                a.getElectricityMeterIndex(), a.getWaterRef(), a.getWaterMeterPolice(), a.getWaterMeter(),
                a.getWaterMeterIndex(), a.getGasRef(), a.getGasMeterPolice(), a.getGasMeter(), a.getGasMeterIndex(),
                a.getUserCreate(), a.getUserCreateDateTime(), a.getUserUpdate(), a.getUserUpdateDateTime());
    }

    /** Vue {@code StkItemView} : statut du dossier de vente, sinon de location, sinon « Libre ». */
    private static String etat(StkItem a, Map<UUID, ComFolder> dossiers) {
        if (a.getComFolderId() != null) {
            String statut = Champs.de(dossiers.get(a.getComFolderId()), ComFolder::getStrStatus, StrStatus::getDescription);
            return statut != null ? statut : "Vendu";
        }
        return a.getRntFolderId() != null ? "Loué" : "Libre";
    }

    private static BigDecimal loyerTTC(StkItem a) {
        BigDecimal tva = a.getVat() != null ? a.getVat() : BigDecimal.ZERO;
        return a.getAmountHT() == null ? null : a.getAmountHT().multiply(BigDecimal.ONE.add(tva));
    }

    private static BigDecimal somme(BigDecimal surface, BigDecimal sousArticles) {
        return surface == null ? sousArticles : surface.add(sousArticles);
    }

    /** Sans sous-article (ou colonne non demandée, alors masquée par le filtre) : zéro. */
    private static BigDecimal somme(Object[] sommes, int rang) {
        return sommes == null ? BigDecimal.ZERO : (BigDecimal) sommes[rang];
    }

    private static String code(CodeLibelleOmran reference) {
        return Champs.de(reference, CodeLibelleOmran::getCode);
    }

    private static String libelle(CodeLibelleOmran reference) {
        return Champs.de(reference, CodeLibelleOmran::getDescription);
    }
}
