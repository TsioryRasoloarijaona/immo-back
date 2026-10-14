package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sous-articles : ajout, retrait, taux de remise recalculé, montant inchangé (FEATURE-vente-echeancier.md §2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class DossierSousArticlesTest {

    private static final String SOUS_ARTICLES = "/api/dossiers/{id}/sous-articles";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureReferentielStk referentielStk;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComFolderItemRepository folderItemRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository typeRepository;
    @Autowired private StkItemCategoryRepository categorieRepository;
    @Autowired private StrFolderStatusHistoryRepository historiqueRepository;

    private CfgTranche tranche;
    private ComFolder dossier;
    private StkItem terrasse;

    /** Article 1 000 000 HT (TVA 20 %), remise 100 000 HT soit 10 % ; une terrasse libre de 50 000 HT dans la tranche. */
    @BeforeEach
    void dossierRemise() {
        tranche = fixture.tranche("TR-SOUS-ART", true);
        dossier = fixture.dossier("DOS-SOUS-ART", tranche, StatutDossier.RESERVATION);
        dossier.setItemPriceHT(new BigDecimal("1000000"));
        dossier.setItemPrice(new BigDecimal("1200000"));
        dossier.setPriceBeforeDiscountHT(new BigDecimal("1000000"));
        dossier.setDiscountAmount(new BigDecimal("100000"));
        dossier.setDiscount(new BigDecimal("0.1"));
        dossier.setPriceAfterDiscountHT(new BigDecimal("900000"));
        dossier.setPriceAfterDiscount(new BigDecimal("1080000"));
        dossier = folderRepository.save(dossier);
        terrasse = stock("TER-SOUS-ART", new BigDecimal("50000"), false);
    }

    @Test
    void ajout_tauxRecalcule_montantDeRemiseInchange_articleRattache() throws Exception {
        mockMvc.perform(post(SOUS_ARTICLES, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":\"%s\"}".formatted(terrasse.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].code").value("TER-SOUS-ART"))
                .andExpect(jsonPath("$.remiseMontantHT").value(100000));

        ComFolder f = relire();
        StkItem stock = stkItemRepository.findById(terrasse.getId()).orElseThrow();
        assertAll(
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("100000"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.095238"),
                () -> assertThat(f.getPriceBeforeDiscountHT()).isEqualByComparingTo("1050000"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("950000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1140000"),
                () -> assertThat(stock.getComFolderId()).isEqualTo(dossier.getId()),
                () -> assertThat(stock.getIsUnderItem()).isTrue(),
                () -> assertThat(historique(HistoriqueLibelle.AFFECTATION)).hasSize(1));
    }

    @Test
    void retrait_tauxRevient_articleLibere_annulationAffectation() throws Exception {
        String json = mockMvc.perform(post(SOUS_ARTICLES, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":\"%s\"}".formatted(terrasse.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String ligneId = com.jayway.jsonpath.JsonPath.read(json, "$.lignes[0].id");

        mockMvc.perform(delete(SOUS_ARTICLES + "/{ligneId}", dossier.getId(), ligneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes").isEmpty());

        ComFolder f = relire();
        StkItem stock = stkItemRepository.findById(terrasse.getId()).orElseThrow();
        assertAll(
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("100000"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.1"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("900000"),
                () -> assertThat(stock.getComFolderId()).isNull(),
                () -> assertThat(stock.getParent()).isNull(),
                () -> assertThat(historique(HistoriqueLibelle.ANNULATION)).hasSize(1));
    }

    @Test
    void retrait_sousArticleAffecteParDefaut_409() throws Exception {
        ComFolderItem ligne = new ComFolderItem();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(dossier);
        ligne.setIsUnderItem(true);
        ligne.setAlwaysAffected(true);
        folderItemRepository.save(ligne);

        mockMvc.perform(delete(SOUS_ARTICLES + "/{ligneId}", dossier.getId(), ligne.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("affecté par défaut")));
    }

    @Test
    void ajout_articleDejaPris_409() throws Exception {
        StkItem pris = stock("TER-PRIS", new BigDecimal("1000"), false);
        pris.setComFolderId(fixture.dossier("DOS-AUTRE-SA", tranche, StatutDossier.RESERVATION).getId());
        stkItemRepository.save(pris);

        mockMvc.perform(post(SOUS_ARTICLES, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":\"%s\"}".formatted(pris.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "Commercial")
    void ajout_horsAdministrateur_403() throws Exception {
        mockMvc.perform(post(SOUS_ARTICLES, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":\"%s\"}".formatted(terrasse.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void disponibles_seulementLesSousArticlesLibresDeLaTranche() throws Exception {
        stock("TER-TOUJOURS", new BigDecimal("1000"), true);

        mockMvc.perform(get(SOUS_ARTICLES + "/disponibles", dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("TER-SOUS-ART"));
    }

    @Test
    void retrait_ligneInconnue_404() throws Exception {
        mockMvc.perform(delete(SOUS_ARTICLES + "/{ligneId}", dossier.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private enum HistoriqueLibelle {
        AFFECTATION("AFFECTATION"), ANNULATION("ANNULATION AFFECTATION");

        private final String texte;

        HistoriqueLibelle(String texte) {
            this.texte = texte;
        }
    }

    private List<StrFolderStatusHistory> historique(HistoriqueLibelle libelle) {
        return historiqueRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId()).stream()
                .filter(h -> libelle.texte.equals(h.getDescription()))
                .toList();
    }

    private ComFolder relire() {
        entityManager.flush();
        entityManager.clear();
        return folderRepository.findById(dossier.getId()).orElseThrow();
    }

    private StkItem stock(String code, BigDecimal prixHT, boolean toujoursAffecte) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-" + code);
        type.setStkKind(referentielStk.natureParDefaut());
        typeRepository.save(type);
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-" + code);
        categorie.setStkKind(referentielStk.natureParDefaut());
        categorieRepository.save(categorie);
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setStkItemType(type);
        item.setStkItemCategory(categorie);
        item.setStkVocation(dossier.getStkVocation());
        item.setStkKind(referentielStk.natureParDefaut());
        item.setStkHierarchy(tranche);
        item.setIsUnderItem(true);
        item.setIsUnderItemOrigin(true);
        item.setAlwaysAffected(toujoursAffecte);
        item.setArea(new BigDecimal("20"));
        item.setVat(new BigDecimal("0.2"));
        item.setSalePriceHT(prixHT);
        return stkItemRepository.save(item);
    }
}
