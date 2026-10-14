package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-vente-echeancier.md §2 (sous-articles) : l'ajout change le prix de vente ; le net à payer du dossier
 * ({@code TotalToPay} = prix + modifications + frais + pénalités, {@code Sale.cs:TotalToPay}) doit le suivre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class DossierSousArticlesRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureReferentielStk referentielStk;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository typeRepository;
    @Autowired private StkItemCategoryRepository categorieRepository;

    @Test
    void ajout_netAPayerSuitLeNouveauPrix() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-SA-REV", true);
        ComFolder dossier = fixture.dossier("DOS-SA-REV", tranche, StatutDossier.RESERVATION);
        dossier.setItemPriceHT(new BigDecimal("1000000"));
        dossier.setItemPrice(new BigDecimal("1200000"));
        dossier.setVATRatio(new BigDecimal("0.2"));
        dossier.setPriceBeforeDiscountHT(new BigDecimal("1000000"));
        dossier.setDiscountAmount(new BigDecimal("100000"));
        dossier.setDiscount(new BigDecimal("0.1"));
        dossier.setPriceAfterDiscountHT(new BigDecimal("900000"));
        dossier.setPriceAfterDiscount(new BigDecimal("1080000"));
        dossier.setPrice(new BigDecimal("1080000"));
        dossier.setTotalToPay(new BigDecimal("1080000"));
        folderRepository.save(dossier);
        StkItem terrasse = sousArticleLibre(tranche, dossier, new BigDecimal("50000"));

        mockMvc.perform(post("/api/dossiers/{id}/sous-articles", dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":\"%s\"}".formatted(terrasse.getId())))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        ComFolder relu = folderRepository.findById(dossier.getId()).orElseThrow();
        assertThat(relu.getTotalToPay()).isEqualByComparingTo(relu.getPrice());
    }

    private StkItem sousArticleLibre(CfgTranche tranche, ComFolder dossier, BigDecimal prixHT) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-SA-REV");
        type.setStkKind(referentielStk.natureParDefaut());
        typeRepository.save(type);
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-SA-REV");
        categorie.setStkKind(referentielStk.natureParDefaut());
        categorieRepository.save(categorie);
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode("TER-SA-REV");
        item.setStkItemType(type);
        item.setStkItemCategory(categorie);
        item.setStkVocation(dossier.getStkVocation());
        item.setStkKind(referentielStk.natureParDefaut());
        item.setStkHierarchy(tranche);
        item.setIsUnderItem(true);
        item.setIsUnderItemOrigin(true);
        item.setAlwaysAffected(false);
        item.setArea(new BigDecimal("20"));
        item.setVat(new BigDecimal("0.2"));
        item.setSalePriceHT(prixHT);
        return stkItemRepository.save(item);
    }
}
