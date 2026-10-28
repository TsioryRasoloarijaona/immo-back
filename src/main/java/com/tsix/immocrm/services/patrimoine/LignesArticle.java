package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.LigneArticleDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.dossier.DossierSorti;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.dossier.liste.StatutsVisiblesListe;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.ArticleService.estOccupe;

/**
 * Lignes {@link LigneArticleDto} d'une page : champs d'{@link ArticleService#toListItem} plus le catalogue
 * {@link ColonneArticle} ; les sommes, codes des sous-articles et le parent ne sont lus que si leur colonne est demandée.
 */
@Component
record LignesArticle(StkItemRepository items, ComFolderRepository folders, ComFolderItemRepository lignesDossier,
                     MontantsParNatureDossier montants, PerimetreCommercialService perimetre,
                     StatutsVisiblesListe statutsVisibles) {

    private static final Set<ColonneArticle> COLONNES_PARENT =
            Set.of(ColonneArticle.PARENT_CODE, ColonneArticle.PARENT_DESCRIPTION, ColonneArticle.PARENT_OCCUPE);
    private static final String TERRASSE = "TER";
    private static final String PARKING = "PAR";
    private static final String SEPARATEUR = ", ";

    /** Lot de calculs d'une page, chargé une fois. */
    private record Lot(Map<UUID, ComFolder> dossiers, Map<UUID, BigDecimal> superficies, Map<UUID, Object[]> sommes,
                       Map<UUID, String> codes, boolean parent, boolean sousArticles, Omran omran) {
    }

    /**
     * Colonnes de FEATURE-articles-colonnes-omran.md §2.1, vides si non demandées : superficies [parentId, Σtitrés,
     * ΣTER] et vues, parkings par article ; restes par dossier.
     */
    private record Omran(Map<UUID, Object[]> superficies, Map<UUID, BigDecimal> restes, Map<UUID, String> parkings,
                         Map<UUID, String> vues) {
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
        Map<UUID, ComFolder> dossiers = dossiers(page.getContent());
        Lot lot = new Lot(dossiers, superficies(page.getContent()),
                sommes ? parId(items.sumPrixGroupedByParentIdIn(ids)) : Collections.emptyMap(),
                codes ? joindre(items.codesByParentIdIn(ids)) : Collections.emptyMap(),
                colonnes.stream().anyMatch(COLONNES_PARENT::contains), codes,
                ids.isEmpty() ? new Omran(Map.of(), Map.of(), Map.of(), Map.of())
                        : omran(page.getContent(), ids, colonnes, dossiers));
        return page.map(a -> ligne(a, lot));
    }

    private Omran omran(List<StkItem> articles, List<UUID> ids, Set<ColonneArticle> colonnes,
                        Map<UUID, ComFolder> dossiers) {
        return new Omran(
                colonnes.stream().anyMatch(ColonneArticle.SUPERFICIES_SOUS_ARTICLES::contains)
                        ? parId(items.sumAreaTitreeEtCategorieGroupedByParentIdIn(ids, TERRASSE)) : Map.of(),
                colonnes.contains(ColonneArticle.RESTE_A_REGLER) ? restes(dossiers) : Map.of(),
                colonnes.contains(ColonneArticle.PARKING_CHOISI) ? parkings(articles, ids, dossiers.keySet()) : Map.of(),
                colonnes.contains(ColonneArticle.VUES) ? joindre(items.vuesByStkItemIdIn(ids)) : Map.of());
    }

    /**
     * Règle 6 : {@code prixReste} de la liste des dossiers, pour les seuls dossiers que cette liste montrerait à
     * l'appelant (périmètre, statuts visibles) ; un dossier annulé ou désisté n'a pas de reste affiché.
     */
    private Map<UUID, BigDecimal> restes(Map<UUID, ComFolder> dossiers) {
        if (dossiers.isEmpty()) {
            return Map.of();
        }
        Specification<ComFolder> affiches = (r, q, cb) -> r.get("id").in(dossiers.keySet());
        Optional<Set<UUID>> statuts = statutsVisibles.pourUtilisateurCourant();
        Map<UUID, BigDecimal> restes = new HashMap<>();
        montants.pour(folders.findAll(affiches.and(perimetre.filtreDossiers())).stream()
                        .filter(f -> statuts.map(ids -> f.getStrStatus() != null
                                && ids.contains(f.getStrStatus().getId())).orElse(true))
                        .filter(Predicate.not(DossierSorti::estSorti)).toList())
                .forEach((id, m) -> restes.put(id, m.get(ColonneDossier.PRIX_RESTE)));
        return restes;
    }

    /** Parkings du dossier de l'article ({@code ComFolderItem}) et sous-articles PAR rattachés, sans doublon. */
    private Map<UUID, String> parkings(List<StkItem> articles, List<UUID> ids, Collection<UUID> dossierIds) {
        Map<UUID, Set<String>> duLot = grouper(items.codesByParentIdInAndCategorie(ids, PARKING));
        Map<UUID, Set<String>> duDossier = dossierIds.isEmpty() ? Map.of()
                : grouper(lignesDossier.codesByComFolderIdInAndCategorie(List.copyOf(dossierIds), PARKING));
        Map<UUID, String> parkings = new HashMap<>();
        for (StkItem a : articles) {
            Set<String> codes = new LinkedHashSet<>(duLot.getOrDefault(a.getId(), Set.of()));
            if (a.getComFolderId() != null) {
                codes.addAll(duDossier.getOrDefault(a.getComFolderId(), Set.of()));
            }
            if (!codes.isEmpty()) {
                parkings.put(a.getId(), String.join(SEPARATEUR, codes));
            }
        }
        return parkings;
    }

    private static Map<UUID, Object[]> parId(List<Object[]> lignes) {
        return lignes.stream().collect(Collectors.toMap(r -> (UUID) r[0], Function.identity()));
    }

    /** Tuples [id, texte] → textes distincts et non nuls par id, dans l'ordre de la requête. */
    private static Map<UUID, Set<String>> grouper(List<Object[]> lignes) {
        return lignes.stream().filter(r -> r[1] != null).collect(Collectors.groupingBy(r -> (UUID) r[0],
                Collectors.mapping(r -> (String) r[1], Collectors.toCollection(LinkedHashSet::new))));
    }

    private static Map<UUID, String> joindre(List<Object[]> lignes) {
        Map<UUID, String> joints = new HashMap<>();
        grouper(lignes).forEach((id, textes) -> joints.put(id, String.join(SEPARATEUR, textes)));
        return joints;
    }

    private static LigneArticleDto ligne(StkItem a, Lot lot) {
        ArticleListItemDto l = ArticleService.toListItem(a, lot.dossiers(), lot.superficies());
        CfgTranche tranche = a.getStkHierarchy();
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgCompany entite = Champs.de(projet, CfgProject::getCfgCompany);
        StkItem parent = lot.parent() ? a.getParent() : null;
        Object[] sommes = lot.sommes().get(a.getId());
        Object[] surfaces = lot.omran().superficies().get(a.getId());
        BigDecimal reste = a.getComFolderId() == null ? null : lot.omran().restes().get(a.getComFolderId());
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
                libelle(a.getStkFacade()), lot.omran().vues().get(a.getId()),
                code(a.getStkCostCategory()), libelle(a.getStkCostCategory()),
                a.getAddress(), a.getNotes(), a.getAnomalies(), a.getContenance(),
                l.statut(), etat(a, lot.dossiers()), parent == null ? null : estOccupe(parent),
                l.dossierId(), l.dossierCode(), l.dossierStatut(), l.clientId(), l.clientNom(),
                reste, lot.omran().parkings().get(a.getId()),
                l.area(), a.getAreaFloor(), somme(l.area(), l.superficieSousArticles()), l.superficieSousArticles(),
                SuperficiesArticle.titree(a, somme(surfaces, 1)), somme(surfaces, 2),
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
                a.getFractionDividedLand(), a.getDividedPart(), a.getUndividedPart(), a.getGroupLandQuota(),
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
