package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dossier de vente complet pour l'acte de réservation (FEATURE-modeles-documents.md §8) : client renseigné, immeuble,
 * étage, sous-article, pénalités et syndic de tranche, avance et deux échéances de solde (dont une sans date).
 */
@Component
public class FixtureActeReservation {

    private final FixtureDossierVente fixture;
    private final Depots depots;

    /** Regroupe les dépôts écrits par la fixture (S107). */
    @Component
    public record Depots(CfgTrancheRepository tranches, CfgTierRepository tiers, ComFolderRepository dossiers,
                         StkItemRepository articles, StkBuildingRepository immeubles, StkFloorRepository etages,
                         StkItemCategoryRepository categories, StkKindRepository natures) {
    }

    public FixtureActeReservation(FixtureDossierVente fixture, Depots depots) {
        this.fixture = fixture;
        this.depots = depots;
    }

    public ComFolder dossier(String code, boolean terrain, boolean societeActive) {
        String suffixe = code + "-" + UUID.randomUUID().toString().substring(0, 6);
        CfgTranche tranche = fixture.tranche("TST-" + suffixe, societeActive);
        tranche.setCancellationPenalityPercent(new BigDecimal("15.00"));
        tranche.setArticleTransferPenality(new BigDecimal("10000"));
        tranche.setCustomerTransferPenality(new BigDecimal("2.5"));
        tranche.setWarrantyDelay(12);
        tranche.setSyndicAmount(new BigDecimal("700.00"));
        tranche.setSyndicPeriod(24);
        tranche.setCommerceTribunal("Rabat");
        depots.tranches().save(tranche);

        ComFolder dossier = fixture.dossier("TST-" + suffixe, tranche, null);
        dossier.setPrice(new BigDecimal("1200000.00"));
        depots.dossiers().save(dossier);

        CfgTier client = dossier.getCfgCustomer();
        client.setIDNumber("A742122");
        client.setAddress("20 BIS CADI BEN HAMMADI");
        client.setCity("Casablanca");
        client.setMobile("0612345678");
        depots.tiers().save(client);

        StkItem article = dossier.getStkItem();
        article.getStkItemCategory().setDescription("APPARTEMENT");
        depots.categories().save(article.getStkItemCategory());
        article.setStkBuilding(immeuble(suffixe));
        article.setStkFloor(etage());
        if (terrain) {
            article.setStkKind(natureTerrain());
        }
        depots.articles().save(article);

        sousArticle(dossier, article);

        ComSettlementCategory avance = fixture.categorieEcheance("AV-" + suffixe, true, false, false, null);
        ComSettlementCategory solde = fixture.categorieEcheance("SO-" + suffixe, false, true, false, null);
        fixture.echeance(dossier, avance, new BigDecimal("120000.00"), LocalDateTime.of(2026, 10, 1, 0, 0));
        fixture.echeance(dossier, solde, new BigDecimal("540000.00"), LocalDateTime.of(2027, 6, 30, 0, 0));
        fixture.echeance(dossier, solde, new BigDecimal("540000.00"), null);
        return dossier;
    }

    private void sousArticle(ComFolder dossier, StkItem article) {
        StkItemCategory terrasse = new StkItemCategory();
        terrasse.setId(UUID.randomUUID());
        terrasse.setCode("CAT-TERRASSE");
        terrasse.setDescription("TERRASSE");
        terrasse.setStkKind(article.getStkKind());
        depots.categories().save(terrasse);

        StkItem sous = new StkItem();
        sous.setId(UUID.randomUUID());
        sous.setCode(article.getCode() + "-T");
        sous.setArea(new BigDecimal("16.00"));
        sous.setStkItemType(article.getStkItemType());
        sous.setStkItemCategory(terrasse);
        sous.setStkVocation(article.getStkVocation());
        sous.setStkKind(article.getStkKind());
        sous.setComFolderId(dossier.getId());
        depots.articles().save(sous);
    }

    private StkBuilding immeuble(String suffixe) {
        StkBuilding immeuble = new StkBuilding();
        immeuble.setId(UUID.randomUUID());
        immeuble.setCode("IMM-" + suffixe);
        return depots.immeubles().save(immeuble);
    }

    private StkFloor etage() {
        StkFloor etage = new StkFloor();
        etage.setId(UUID.randomUUID());
        etage.setDescription("Rez de chaussée");
        return depots.etages().save(etage);
    }

    private StkKind natureTerrain() {
        StkKind nature = new StkKind();
        nature.setId(UUID.randomUUID());
        nature.setCode("TER");
        return depots.natures().save(nature);
    }
}
