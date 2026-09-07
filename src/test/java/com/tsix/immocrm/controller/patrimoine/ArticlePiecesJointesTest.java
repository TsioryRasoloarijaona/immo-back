package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.storage.FixturePieceJointe;
import com.tsix.immocrm.storage.StockageMemoire;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/articles/{id}/pieces-jointes[/...]} — same {@code AdmAttachedFile.CategoryId}
 * polymorphic-parent linkage as Dossier ({@code CategoryId = StkItem.PKey} here), confirmed
 * against real data: the bulk of real Article attachments are house/floor plans
 * ({@code AdmAttachedFileType.Code} PLAN/PLAN_ARCHITECTE/PLAN_ETAGE, {@code Category} literally
 * "Yes.Stk.Building.Item").
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ArticlePiecesJointesTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private StockageMemoire stockageMemoire;

    @Autowired
    private FixturePieceJointe fixturePieceJointe;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StkVocationRepository vocationRepository;

    @Autowired
    private StkItemTypeRepository itemTypeRepository;

    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;

    @Autowired
    private StkItemRepository itemRepository;

    @Autowired
    private AdmAttachedFileRepository attachedFileRepository;

    @Autowired
    private AdmAttachedFileTypeRepository attachedFileTypeRepository;

    private StkItem newArticle(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-APJ-" + UUID.randomUUID());
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-APJ-" + UUID.randomUUID());
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-APJ-" + UUID.randomUUID());
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(category);

        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setStkItemType(type);
        item.setStkItemCategory(category);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return itemRepository.save(item);
    }

    private AdmAttachedFile newArticleAttachment(UUID articleId, byte[] bytes, String extension, String description) {
        AdmAttachedFile file = new AdmAttachedFile();
        file.setId(UUID.randomUUID());
        file.setCategoryId(articleId);
        file.setCategory("Yes.Stk.Building.Item");
        file.setDescription(description);
        file.setExtension(extension);
        return fixturePieceJointe.deposer(file, bytes);
    }

    @Test
    void getPiecesJointes_returnsOnlyThisArticlesAttachments() throws Exception {
        StkItem article = newArticle("ART-APJ-1");
        StkItem otherArticle = newArticle("ART-APJ-2");

        AdmAttachedFile matching = newArticleAttachment(article.getId(), "PDF-CONTENT".getBytes(StandardCharsets.UTF_8),
                ".pdf", "PLAN RDC");
        newArticleAttachment(otherArticle.getId(), "OTHER".getBytes(StandardCharsets.UTF_8), ".pdf", "Autre article");

        mockMvc.perform(get("/api/articles/{id}/pieces-jointes", article.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + matching.getId() + "')]").exists())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPiecesJointes_includesPlanDocumentType() throws Exception {
        AdmAttachedFileType planType = new AdmAttachedFileType();
        planType.setId(UUID.randomUUID());
        planType.setCode("PLAN");
        planType.setDescription("PLAN");
        attachedFileTypeRepository.save(planType);

        StkItem article = newArticle("ART-APJ-PLAN");
        AdmAttachedFile plan = new AdmAttachedFile();
        plan.setId(UUID.randomUUID());
        plan.setCategoryId(article.getId());
        plan.setCategory("Yes.Stk.Building.Item");
        plan.setDescription("Plan de l'appartement");
        plan.setExtension(".pdf");
        plan.setAdmAttachedFileTypeId(planType.getId());
        fixturePieceJointe.deposer(plan, "PLAN-BYTES".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/articles/{id}/pieces-jointes", article.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PLAN"))
                .andExpect(jsonPath("$[0].description").value("Plan de l'appartement"));
    }

    @Test
    void getPiecesJointes_unknownArticle_returns404() throws Exception {
        mockMvc.perform(get("/api/articles/{id}/pieces-jointes", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFichier_redirigeVersUrlPresigneeDuBonObjet() throws Exception {
        StkItem article = newArticle("ART-APJ-3");
        byte[] contenu = "PLAN-PDF-CONTENT".getBytes(StandardCharsets.UTF_8);
        AdmAttachedFile file = newArticleAttachment(article.getId(), contenu, ".pdf", "Plan étage 2");

        String cle = fixturePieceJointe.cleAttendue(file);

        mockMvc.perform(get("/api/articles/{id}/pieces-jointes/{pieceId}/fichier", article.getId(), file.getId()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(cle)));

        org.assertj.core.api.Assertions.assertThat(stockageMemoire.contenu(cle)).isEqualTo(contenu);
    }

    @Test
    void getFichier_wrongArticle_returns404() throws Exception {
        StkItem article = newArticle("ART-APJ-4");
        StkItem otherArticle = newArticle("ART-APJ-5");
        AdmAttachedFile file = newArticleAttachment(article.getId(), "X".getBytes(StandardCharsets.UTF_8), ".pdf", "Test");

        mockMvc.perform(get("/api/articles/{id}/pieces-jointes/{pieceId}/fichier", otherArticle.getId(), file.getId()))
                .andExpect(status().isNotFound());
    }
}
