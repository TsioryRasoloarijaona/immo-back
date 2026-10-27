package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.domain.foncier.SurfacesTerrain;
import com.tsix.immocrm.dto.patrimoine.FiltresTerrains;
import com.tsix.immocrm.dto.patrimoine.TerrainDetailDto;
import com.tsix.immocrm.dto.patrimoine.TerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TerrainListeDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.lnd.LndVocation;
import com.tsix.immocrm.entities.stk.StkCostCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.stk.StkExpenseRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.FiltresArticle;
import com.tsix.immocrm.services.patrimoine.StkItemSpecifications;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.ArticleService.VOCATION_PROJET;
import static com.tsix.immocrm.services.patrimoine.terrain.TerrainsVisibles.NATURE_TERRAIN;

/** Fiche et listes des terrains, lotissements (FEATURE-patrimoine-terrains.md §1, §4). */
@Service
@Transactional(readOnly = true)
public class TerrainService {

    private static final String ID = "id";

    private final StkItemRepository items;
    private final StkExpenseRepository depenses;
    private final TerrainsVisibles terrains;
    private final EcritureTerrain ecriture;
    private final SuppressionTerrain suppression;

    TerrainService(StkItemRepository items, StkExpenseRepository depenses, TerrainsVisibles terrains,
                   EcritureTerrain ecriture, SuppressionTerrain suppression) {
        this.items = items;
        this.depenses = depenses;
        this.terrains = terrains;
        this.ecriture = ecriture;
        this.suppression = suppression;
    }

    /** {@code lotissements} : terrains d'une tranche, vocation autre que PROJET ({@code LotLandList.cs:168-189}). */
    public Page<TerrainListeDto> lister(FiltresTerrains f, boolean lotissements, Pageable pageable) {
        Specification<StkItem> terrainsVisibles = (root, query, cb) -> {
            Join<StkItem, CfgTranche> tranche = root.join("stkHierarchy", JoinType.LEFT);
            Join<StkItem, CfgCompany> societe = root.join("cfgCompany", JoinType.LEFT);
            List<Predicate> p = new ArrayList<>(List.of(
                    cb.equal(root.join("stkKind").get("code"), NATURE_TERRAIN),
                    VisibiliteEntites.depuisProjet(tranche.join("cfgProject", JoinType.LEFT), cb),
                    // Même règle que StkItemRepository.SOCIETE_DIRECTE : la société directe ne compte que sans tranche.
                    cb.or(cb.isNotNull(tranche.get(ID)), cb.equal(societe.get("isActive"), Boolean.TRUE),
                            cb.isNull(societe.get(ID)))));
            if (lotissements) {
                p.add(cb.isNotNull(tranche.get(ID)));
                p.add(cb.notEqual(root.join("stkVocation").get("code"), VOCATION_PROJET));
                Subquery<UUID> entiteArticle = query.subquery(UUID.class);
                Root<StrEntity> entite = entiteArticle.from(StrEntity.class);
                p.add(root.get("strEntityId").in(entiteArticle.select(entite.get(ID))
                        .where(cb.equal(entite.get("code"), LotissementsVisibles.ENTITE_ARTICLE))));
            }
            egal(p, cb, tranche.get(ID), f.trancheId());
            egal(p, cb, societe.get(ID), f.entiteId());
            egal(p, cb, root.get("strStatus").get(ID), f.statutId());
            egal(p, cb, root.get("stkVocation").get(ID), f.vocationId());
            return cb.and(p.toArray(new Predicate[0]));
        };
        Specification<StkItem> recherche = StkItemSpecifications.build(new FiltresArticle(null, null, false, null, null,
                null, f.search(), false, null, null, null, null, null, null, null, null, false, null, null));
        return items.findAll(terrainsVisibles.and(recherche), pageable).map(TerrainService::ligne);
    }

    public TerrainDetailDto lire(UUID id) {
        return detail(terrains.exiger(id));
    }

    @Transactional
    public TerrainDetailDto creer(TerrainEcritureDto saisie) {
        return detail(ecriture.creer(saisie));
    }

    @Transactional
    public TerrainDetailDto modifier(UUID id, TerrainEcritureDto saisie) {
        StkItem terrain = terrains.exiger(id);
        ecriture.modifier(terrain, saisie);
        return detail(terrain);
    }

    @Transactional
    public void supprimer(UUID id) {
        suppression.supprimer(terrains.exiger(id));
    }

    @Transactional
    public TerrainDetailDto creerSousTerrain(UUID parentId, TerrainEcritureDto saisie) {
        return detail(ecriture.creerSousTerrain(terrains.exiger(parentId), saisie));
    }

    private static void egal(List<Predicate> p, CriteriaBuilder cb, Path<Object> champ, UUID valeur) {
        if (valeur != null) {
            p.add(cb.equal(champ, valeur));
        }
    }

    /** {@code LandProjectItem.cs:3442-3467} : calculés à la lecture, jamais stockés. */
    private TerrainDetailDto detail(StkItem t) {
        BigDecimal coutTotal = depenses.coutTotal(t.getId());
        BigDecimal surface = t.getArea();
        BigDecimal coutUnitaire = surface == null || surface.signum() == 0
                ? BigDecimal.ZERO : coutTotal.divide(surface, 6, RoundingMode.HALF_UP);
        return new TerrainDetailDto(t.getId(), t.getCode(), t.getDescription(),
                ref(t.getCfgCompany(), CfgCompany::getId, CfgCompany::getCode, CfgCompany::getDescription),
                ref(t.getAdmCountry(), AdmCountry::getId, AdmCountry::getCode, AdmCountry::getDescription),
                ref(t.getAdmCity(), AdmCity::getId, AdmCity::getCode, AdmCity::getDescription),
                ref(t.getStkKind(), StkKind::getId, StkKind::getCode, StkKind::getDescription),
                ref(t.getStkVocation(), StkVocation::getId, StkVocation::getCode, StkVocation::getDescription),
                ref(t.getStkItemCategory(), StkItemCategory::getId, StkItemCategory::getCode,
                        StkItemCategory::getDescription),
                ref(t.getStkItemType(), StkItemType::getId, StkItemType::getCode, StkItemType::getDescription),
                ref(t.getStrStatus(), StrStatus::getId, StrStatus::getCode, StrStatus::getDescription),
                ref(t.getLndLevel(), LndLevel::getId, LndLevel::getCode, LndLevel::getDescription),
                ref(t.getLndVocation(), LndVocation::getId, LndVocation::getCode, LndVocation::getDescription),
                ref(t.getStkOrientation(), StkOrientation::getId, StkOrientation::getCode, StkOrientation::getDescription),
                ref(t.getStkCostCategory(), StkCostCategory::getId, StkCostCategory::getCode,
                        StkCostCategory::getDescription),
                ref(t.getCfgSupplier(), CfgTier::getId, CfgTier::getCode, CfgTier::getDescription),
                ref(t.getParent(), StkItem::getId, StkItem::getCode, StkItem::getDescription),
                t.getIsFinished(), TerrainsVisibles.sousTerrainsAutorises(t), ArticleService.estOccupe(t),
                t.getLndLandId(),
                t.getAreaFloor(), t.getComSaleUnitCostFloorHT(), t.getComSaleUnitCostFloor(), t.getSalePriceFloorHT(),
                t.getSalePriceFloor(),
                t.getArea(), t.getComSaleUnitCostHT(), t.getComSaleUnitCost(), t.getSalePriceHT(), t.getSalePrice(),
                t.getVat(), t.getPurchaseUnitCost(), t.getPurchaseCost(), t.getPurchaseDate(),
                t.getBuildingPermissionDate(), coutTotal, coutUnitaire,
                t.getTitleNumber(), t.getTitleName(), t.getTitleDate(), t.getAddress(), t.getGmapLink(), t.getNotes(),
                t.getAnomalies(), items.findByParentId(t.getId()).stream().map(TerrainService::ligne).toList(),
                t.getUserCreate(), t.getUserCreateDateTime(), t.getUserUpdate(), t.getUserUpdateDateTime(),
                superficiesFoncieres(t), t.getProspectionPriceProposed());
    }

    /** Valeurs stockées (copiées de la prospection d'origine), sans recalcul ; la nette n'est pas une colonne de StkItem. */
    private static SurfacesTerrain superficiesFoncieres(StkItem t) {
        BigDecimal nette = Stream.of(t.getEquipementsArea(), t.getAreaEquipmentTransferred(), t.getAreaRegularized(),
                t.getAreaCeded()).filter(Objects::nonNull).reduce(Objects.requireNonNullElse(t.getArea(), BigDecimal.ZERO),
                BigDecimal::subtract);
        return new SurfacesTerrain(t.getArea(), t.getEquipementsArea(), t.getAreaEquipmentTransferred(),
                t.getAreaRegularized(), t.getAreaCeded(), t.getEquipementsCoef(), nette, t.getCos(), t.getAreaCos(),
                t.getCuf(), t.getAreaCuf(), t.getCoefSaleAbleArea(), t.getSalableArea(), t.getCommerceCoef(),
                t.getCommerceArea(), t.getOfficeCoef(), t.getOfficeArea(), t.getHabitatArea(), t.getUndergroundParkingCoef(),
                t.getUndergroundParkingArea(), t.getProspectionUnitCost(), t.getProspectionPrice());
    }

    private static TerrainListeDto ligne(StkItem t) {
        return new TerrainListeDto(t.getId(), t.getCode(), t.getDescription(),
                ref(t.getCfgCompany(), CfgCompany::getId, CfgCompany::getCode, CfgCompany::getDescription),
                ref(t.getAdmCity(), AdmCity::getId, AdmCity::getCode, AdmCity::getDescription),
                ref(t.getStrStatus(), StrStatus::getId, StrStatus::getCode, StrStatus::getDescription),
                ref(t.getStkVocation(), StkVocation::getId, StkVocation::getCode, StkVocation::getDescription),
                ref(t.getStkHierarchy(), CfgTranche::getId, CfgTranche::getCode, CfgTranche::getDescription),
                t.getArea(), t.getSalePrice(), t.getTitleNumber(), ArticleService.estOccupe(t));
    }

    private static <T> ReferentielItemDto ref(T cible, Function<T, UUID> id, Function<T, String> code,
                                              Function<T, String> description) {
        return cible == null ? null : new ReferentielItemDto(id.apply(cible), code.apply(cible), description.apply(cible), null);
    }
}
