package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Réinitialiser depuis le stock — FEATURE-vente-fiche.md §B5. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierReinitialisationTest {

    private static final String APERCU = "/api/dossiers/{id}/reinitialiser/apercu";
    private static final String REINITIALISER = "/api/dossiers/{id}/reinitialiser";
    private static final String ADMIN = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComFolderItemRepository folderItemRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private EntityManager em;

    /** Dossier figé à 100 m² × 10 000 HT, remise 50 000 HT ; le stock passe à 110 m². */
    private ComFolder dossierAvecStockModifie(String code) {
        ComFolder dossier = fixture.dossier(code, fixture.tranche(code + "-TR", true), StatutDossier.CONCRETISATION);
        dossier.setDiscountAmount(new BigDecimal("50000"));
        dossier.setPriceBeforeDiscountHT(new BigDecimal("1000000"));
        dossier.setPriceAfterDiscountHT(new BigDecimal("950000"));
        dossier.setPriceAfterDiscount(new BigDecimal("1140000"));
        dossier.setDiscount(new BigDecimal("0.05"));
        folderRepository.save(dossier);
        StkItem article = dossier.getStkItem();
        article.setArea(new BigDecimal("110"));
        stkItemRepository.save(article);
        return dossier;
    }

    @Test
    @WithMockUser(roles = ADMIN)
    void apercu_montreAvantApres_sansRienEcrire() throws Exception {
        ComFolder dossier = dossierAvecStockModifie("DOS-REI1");

        mockMvc.perform(get(APERCU, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modifie").value(true))
                .andExpect(jsonPath("$.avant.superficie").value(100))
                .andExpect(jsonPath("$.apres.superficie").value(110))
                .andExpect(jsonPath("$.apres.totalHT").value(1100000))
                .andExpect(jsonPath("$.apres.remiseMontantHT").value(50000))
                .andExpect(jsonPath("$.apres.prixVenteHT").value(1050000))
                .andExpect(jsonPath("$.apres.prixVenteTTC").value(1260000));

        em.flush();
        em.clear();
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getArea()).isEqualByComparingTo("100");
    }

    @Test
    @WithMockUser(roles = ADMIN)
    void reinitialiser_rechargeArticleEtSousArticle_montantDeRemiseConserveTauxRecalcule() throws Exception {
        ComFolder dossier = dossierAvecStockModifie("DOS-REI2");
        StkItem cave = new StkItem();
        cave.setId(UUID.randomUUID());
        cave.setCode("CAVE-REI2");
        cave.setStkItemType(dossier.getStkItem().getStkItemType());
        cave.setStkItemCategory(dossier.getStkItem().getStkItemCategory());
        cave.setStkVocation(dossier.getStkVocation());
        cave.setStkKind(dossier.getStkItem().getStkKind());
        cave.setArea(new BigDecimal("10"));
        cave.setSalePriceHT(new BigDecimal("50000"));
        cave.setVat(new BigDecimal("0.2"));
        stkItemRepository.save(cave);
        ComFolderItem ligne = new ComFolderItem();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(dossier);
        ligne.setStkItem(cave);
        ligne.setIsUnderItem(true);
        ligne.setArea(new BigDecimal("8"));
        ligne.setVATRatio(new BigDecimal("0.2"));
        ligne.setPriceBeforeDiscountHT(new BigDecimal("40000"));
        folderItemRepository.save(ligne);

        mockMvc.perform(post(REINITIALISER, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apres.totalHT").value(1150000))
                .andExpect(jsonPath("$.apres.remiseMontantHT").value(50000));

        em.flush();
        em.clear();
        ComFolder relu = folderRepository.findById(dossier.getId()).orElseThrow();
        ComFolderItem ligneRelue = folderItemRepository.findById(ligne.getId()).orElseThrow();
        assertAll(
                () -> assertThat(relu.getArea()).isEqualByComparingTo("110"),
                () -> assertThat(relu.getDiscountAmount()).isEqualByComparingTo("50000"),
                () -> assertThat(relu.getPriceAfterDiscountHT()).isEqualByComparingTo("1100000"),
                () -> assertThat(relu.getDiscount()).isEqualByComparingTo("0.043478"),
                () -> assertThat(relu.getUserUpdateDateTime()).isNotNull(),
                () -> assertThat(ligneRelue.getArea()).isEqualByComparingTo("10"),
                () -> assertThat(ligneRelue.getPriceBeforeDiscountHT()).isEqualByComparingTo("50000"),
                () -> assertThat(ligneRelue.getCode()).isEqualTo("CAVE-REI2"));
    }

    private StkItem sousArticleDuStock(String code, ComFolder dossier, UUID affecteA) {
        StkItem sous = new StkItem();
        sous.setId(UUID.randomUUID());
        sous.setCode(code);
        sous.setParent(dossier.getStkItem());
        sous.setStkItemType(dossier.getStkItem().getStkItemType());
        sous.setStkItemCategory(dossier.getStkItem().getStkItemCategory());
        sous.setStkVocation(dossier.getStkVocation());
        sous.setStkKind(dossier.getStkItem().getStkKind());
        sous.setArea(new BigDecimal("12"));
        sous.setSalePriceHT(new BigDecimal("60000"));
        sous.setVat(new BigDecimal("0.2"));
        sous.setComFolderId(affecteA);
        return stkItemRepository.save(sous);
    }

    @Test
    @WithMockUser(roles = ADMIN)
    void reinitialiser_ajouteLeSousArticleLibreDuStock_maisPasCeluiDUnAutreDossier() throws Exception {
        ComFolder dossier = dossierAvecStockModifie("DOS-REI5");
        ComFolder autre = fixture.dossier("DOS-REI5B", fixture.tranche("REI5B-TR", true), StatutDossier.RESERVATION);
        StkItem parking = sousArticleDuStock("PARK-REI5", dossier, null);
        sousArticleDuStock("BOX-REI5", dossier, autre.getId());

        mockMvc.perform(get(APERCU, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sousArticlesAjoutes.length()").value(1))
                .andExpect(jsonPath("$.sousArticlesAjoutes[0]").value("PARK-REI5"))
                .andExpect(jsonPath("$.apres.prixSousArticlesHT").value(60000))
                .andExpect(jsonPath("$.apres.totalHT").value(1160000));
        em.flush();
        assertThat(folderItemRepository.findByComFolderId(dossier.getId())).isEmpty();

        mockMvc.perform(post(REINITIALISER, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sousArticlesAjoutes[0]").value("PARK-REI5"));

        em.flush();
        em.clear();
        assertThat(folderItemRepository.findByComFolderId(dossier.getId())).singleElement()
                .satisfies(l -> assertAll(
                        () -> assertThat(l.getStkItem().getId()).isEqualTo(parking.getId()),
                        () -> assertThat(l.getIsUnderItem()).isTrue(),
                        () -> assertThat(l.getParentId()).isEqualTo(dossier.getStkItem().getId()),
                        () -> assertThat(l.getCode()).isEqualTo("PARK-REI5"),
                        () -> assertThat(l.getArea()).isEqualByComparingTo("12"),
                        () -> assertThat(l.getPriceBeforeDiscountHT()).isEqualByComparingTo("60000"),
                        () -> assertThat(l.getPriceHT()).isNotNull(),
                        () -> assertThat(l.getUserCreateDateTime()).isNotNull()));
        assertAll(
                () -> assertThat(stkItemRepository.findById(parking.getId()).orElseThrow().getComFolderId())
                        .isEqualTo(dossier.getId()),
                () -> assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getUnderItemsPriceHT())
                        .isEqualByComparingTo("60000"));
    }

    @Test
    @WithMockUser
    void reinitialiser_sansRoleAdministrateur_403() throws Exception {
        ComFolder dossier = dossierAvecStockModifie("DOS-REI3");

        mockMvc.perform(post(REINITIALISER, dossier.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ADMIN)
    void reinitialiser_dossierAnnule_409() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-REI4", fixture.tranche("REI4-TR", true), StatutDossier.ANNULATION);

        mockMvc.perform(post(REINITIALISER, dossier.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = ADMIN)
    void apercu_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(APERCU, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
