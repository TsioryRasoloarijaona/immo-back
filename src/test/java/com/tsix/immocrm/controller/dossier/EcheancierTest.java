package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.ReferentielSeme;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EcheancierTest {

    private static final String ECHEANCIER = "/api/dossiers/{id}/echeancier";
    private static final String ECHEANCE = "/api/dossiers/{id}/echeancier/{echeanceId}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private ComSettlementCategoryRepository categoryRepository;

    @Autowired
    private FixturePatrimoine fixturePatrimoine;

    @Autowired
    private EntityManager entityManager;

    private FixturePatrimoine.Chaine chaine;

    private ComFolder newFolder(String code) {
        chaine = fixturePatrimoine.chaine(code);
        StkItem article = fixturePatrimoine.article(chaine, "LOT-" + code);
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(chaine.vocation());
        folder.setStkItem(article);
        return folderRepository.save(folder);
    }

    private ComSettlementCategory avance() {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("AVANCE");
        categorie.setDescription("Avance");
        categorie.setStkVocation(chaine.vocation());
        categorie.setIsCredit(true);
        categorie.setExcludeFromPrice(false);
        categorie.setVatRatio(new BigDecimal("0.2"));
        return categoryRepository.save(categorie);
    }

    private ComDocument relire(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return documentRepository.findById(id).orElseThrow();
    }

    @Test
    void findEcheances_returnsExistingLine() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-1");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setDescription("Avance");
        documentRepository.save(doc);

        mockMvc.perform(get(ECHEANCIER, folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + doc.getId() + "')]").exists());
    }

    @Test
    void findEcheances_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get(ECHEANCIER, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addEcheance_happyPath_returns201() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-2");
        String body = """
                {"montant":150000.00,"description":"Premiere echeance","categorieId":"%s"}"""
                .formatted(avance().getId());

        mockMvc.perform(post(ECHEANCIER, folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Premiere echeance"))
                .andExpect(jsonPath("$.montant").value(150000.00));
    }

    /** {@code FEATURE-interop-omran-ecritures.md} §4 : chaque colonne qu'OMRAN écrit, relue en base. */
    @Test
    void addEcheance_ecritLesColonnesOmran() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-OMR");
        String body = """
                {"montant":100000,"dateEcheance":"2026-11-15T14:30:00","categorieId":"%s"}"""
                .formatted(avance().getId());

        String json = mockMvc.perform(post(ECHEANCIER, folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ComDocument ligne = relire(UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id")));

        assertAll(
                () -> assertThat(ligne.getCfgCompany().getId()).isEqualTo(chaine.societe().getId()),
                () -> assertThat(ligne.getAdmModuleId()).isEqualTo(ReferentielSeme.ADM_MODULE_COM),
                () -> assertThat(ligne.getDocDate()).isEqualTo(LocalDateTime.of(2026, 11, 15, 0, 0)),
                () -> assertThat(ligne.getVATRatio()).isEqualByComparingTo("0.2"),
                () -> assertThat(ligne.getAmountTTC()).isEqualByComparingTo("100000"),
                () -> assertThat(ligne.getAmountHT()).isEqualByComparingTo("83333.333333"),
                () -> assertThat(ligne.getDiscountRatio()).isEqualByComparingTo("0"),
                () -> assertThat(ligne.getExcludeFromPrice()).isFalse(),
                () -> assertThat(ligne.getDescription()).isEqualTo("Avance"),
                () -> assertThat(ligne.getCode()).isEmpty(),
                () -> assertThat(ligne.getNotes()).isEmpty(),
                () -> assertThat(ligne.getQuittanceCode()).isEmpty(),
                () -> assertThat(ligne.getUserCreate()).isEqualTo(ligne.getUserUpdate()),
                () -> assertThat(ligne.getUserCreateDateTime()).isNotNull().isEqualTo(ligne.getUserUpdateDateTime()));
    }

    @Test
    void addEcheance_sansCategorie_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-NOCAT");

        mockMvc.perform(post(ECHEANCIER, folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isBadRequest());

        entityManager.flush();
        assertThat(documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId())).isEmpty();
    }

    @Test
    void addEcheance_unknownDossier_returns404() throws Exception {
        String body = """
                {"montant":1000}""";

        mockMvc.perform(post(ECHEANCIER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEcheance_happyPath_appliesPartialChanges() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-3");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setDescription("Ancienne description");
        doc.setComSettlementCategory(avance());
        documentRepository.save(doc);

        String body = """
                {"description":"Nouvelle description","montant":60000}""";

        mockMvc.perform(patch(ECHEANCE, folder.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nouvelle description"));

        ComDocument ligne = relire(doc.getId());
        assertAll(
                () -> assertThat(ligne.getAmountTTC()).isEqualByComparingTo("60000"),
                () -> assertThat(ligne.getAmountHT()).isEqualByComparingTo("50000"),
                () -> assertThat(ligne.getCfgCompany().getId()).isEqualTo(chaine.societe().getId()),
                () -> assertThat(ligne.getUserUpdateDateTime()).isNotNull(),
                () -> assertThat(ligne.getUserCreateDateTime()).as("la modification ne signe pas la création").isNull());
    }

    @Test
    void updateEcheance_ligneSansCategorie_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-3B");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        documentRepository.save(doc);

        mockMvc.perform(patch(ECHEANCE, folder.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEcheance_belongsToDifferentDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-ECH-4A");
        ComFolder folder2 = newFolder("DOS-ECH-4B");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder1);
        documentRepository.save(doc);

        mockMvc.perform(patch(ECHEANCE, folder2.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEcheance_unknownEcheance_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-5");

        mockMvc.perform(patch(ECHEANCE, folder.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}
