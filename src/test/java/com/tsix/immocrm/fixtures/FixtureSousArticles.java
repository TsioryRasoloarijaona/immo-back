package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lignes du lot P4 (FEATURE-patrimoine-sous-articles.md) : une tranche complète par test et des StkItem écrits comme
 * OMRAN (sous-article = 10 m² à 5 000 HT, TVA 0,20 → 60 000 TTC / 50 000 HT). Références créées d'abord (FK de la CI).
 */
@Component
public class FixtureSousArticles {

    public static final BigDecimal PRIX_SOUS_ARTICLE_TTC = new BigDecimal("60000");
    public static final BigDecimal PRIX_SOUS_ARTICLE_HT = new BigDecimal("50000");

    private final FixturePatrimoine fixture;
    private final StkItemRepository itemRepository;
    private final StkVocationRepository vocationRepository;
    private final EntityManager entityManager;

    public FixtureSousArticles(FixturePatrimoine fixture, StkItemRepository itemRepository,
                               StkVocationRepository vocationRepository, EntityManager entityManager) {
        this.fixture = fixture;
        this.itemRepository = itemRepository;
        this.vocationRepository = vocationRepository;
        this.entityManager = entityManager;
    }

    /** Références d'une tranche de test ; {@code jeton} suffixe tous les codes (yesRemsTest est partagée). */
    public record Contexte(String jeton, CfgCompany entite, CfgProject projet, CfgTranche tranche, StkBuilding immeuble,
                           StkFloor etage, StkItemCategory categorie, StkItemType type, StkItemCategory categorieSous,
                           StkItemType typeSous) {
    }

    public Contexte contexte(String prefixe) {
        fixture.referentielArticle();
        String jeton = prefixe + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgCompany entite = fixture.entite("ENT-" + jeton, true);
        CfgProject projet = fixture.projet("PRJ-" + jeton, entite);
        return new Contexte(jeton, entite, projet, fixture.tranche("TR-" + jeton, projet),
                fixture.immeuble("IMM-" + jeton), fixture.etage("ETG-" + jeton),
                fixture.categorie("CAT-" + jeton, fixture.natureBat(), false),
                fixture.type("TYP-" + jeton, fixture.natureBat(), false),
                fixture.categorie("CATS-" + jeton, fixture.natureBat(), true),
                fixture.type("TYPS-" + jeton, fixture.natureBat(), true));
    }

    public CfgTranche autreTranche(Contexte c, String suffixe) {
        return fixture.tranche("TR-" + c.jeton() + "-" + suffixe, c.projet());
    }

    /** Vocation de test ni VENTE ni PROJET : permise pour un sous-article libre (recalage R4-3). */
    public StkVocation vocation(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode(code);
        vocation.setDescription("Vocation " + code);
        return vocationRepository.saveAndFlush(vocation);
    }

    /** Article principal (IsUnderItem = 0), 63 m², 819 000 TTC. */
    public StkItem principal(Contexte c, String code, CfgTranche tranche) {
        StkItem item = base(c, code, tranche);
        item.setStkItemCategory(c.categorie());
        item.setStkItemType(c.type());
        item.setIsUnderItem(false);
        item.setIsUnderItemOrigin(false);
        item.setIsTitled(true);
        item.setArea(new BigDecimal("63"));
        item.setAreaFloor(new BigDecimal("63"));
        prix(item, new BigDecimal("819000"), new BigDecimal("682500"));
        return itemRepository.saveAndFlush(item);
    }

    /** Sous-article d'origine (IsUnderItem = IsUnderItemOrigin = 1) ; {@code parent} nul = libre. */
    public StkItem sousArticle(Contexte c, String code, StkItem parent) {
        StkItem item = base(c, code, c.tranche());
        item.setStkItemCategory(c.categorieSous());
        item.setStkItemType(c.typeSous());
        item.setIsUnderItem(true);
        item.setIsUnderItemOrigin(true);
        item.setIsTitled(false);
        item.setArea(BigDecimal.TEN);
        item.setAreaFloor(BigDecimal.TEN);
        item.setComSaleUnitCostHT(new BigDecimal("5000"));
        item.setComSaleUnitCostFloorHT(new BigDecimal("5000"));
        item.setComSaleUnitCost(new BigDecimal("6000"));
        item.setComSaleUnitCostFloor(new BigDecimal("6000"));
        prix(item, PRIX_SOUS_ARTICLE_TTC, PRIX_SOUS_ARTICLE_HT);
        item.setParent(parent);
        return itemRepository.saveAndFlush(item);
    }

    public StkItem enregistrer(StkItem item) {
        return itemRepository.saveAndFlush(item);
    }

    public StkItem recharger(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return itemRepository.findById(id).orElseThrow();
    }

    /** PKey de l'article de ce code dans la tranche (création par l'API sans lire la réponse). */
    @SuppressWarnings("unchecked")
    public List<String> idsParCode(String code, UUID trancheId) {
        entityManager.flush();
        List<Object> ids = entityManager.createNativeQuery(
                        "SELECT PKey FROM StkItem WHERE Code = :code AND StkHierarchyId = :tranche")
                .setParameter("code", code).setParameter("tranche", trancheId).getResultList();
        return ids.stream().map(FixturePatrimoine::guid).toList();
    }

    private StkItem base(Contexte c, String code, CfgTranche tranche) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkHierarchy(tranche);
        item.setStkVocation(fixture.vocationVente());
        item.setStkKind(fixture.natureBat());
        item.setStkBuilding(c.immeuble());
        item.setStkFloor(c.etage());
        item.setAlwaysAffected(false);
        item.setVat(new BigDecimal("0.2"));
        return item;
    }

    private static void prix(StkItem item, BigDecimal ttc, BigDecimal ht) {
        item.setSalePrice(ttc);
        item.setSalePriceFloor(ttc);
        item.setComSalePrice(ttc);
        item.setComSalePriceFloor(ttc);
        item.setSalePriceHT(ht);
        item.setSalePriceFloorHT(ht);
        item.setComSalePriceHT(ht);
        item.setComSalePriceFloorHT(ht);
    }
}
