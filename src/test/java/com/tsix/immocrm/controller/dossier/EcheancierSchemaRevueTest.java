package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-vente-echeancier.md §1.1 : la suppression d'une ligne d'échéancier est une écriture d'argent ; OMRAN
 * la réserve au droit {@code ComFolderDeleteLine} (Sale.cs:Sale_Load), que le rôle « Commercial » n'a pas en
 * production, et la règle commune réserve les écritures sensibles à l'Administrateur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcheancierSchemaRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categorieRepository;

    @Test
    @WithMockUser(roles = "Commercial")
    void suppression_parUnCommercialSimple_403EtLigneConservee() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-ECH-REV", fixture.tranche("TR-ECH-REV", true), StatutDossier.RESERVATION);
        ComSettlementCategory autofinancement = new ComSettlementCategory();
        autofinancement.setId(UUID.randomUUID());
        autofinancement.setCode("AUTOFIN-REV");
        autofinancement.setDescription("AUTOFIN-REV");
        autofinancement.setIsCredit(true);
        autofinancement.setIsSaleAutoFinancment(true);
        categorieRepository.save(autofinancement);
        ComDocument ligne = fixture.echeance(dossier, autofinancement, new BigDecimal("400000"),
                LocalDateTime.of(2027, 1, 1, 0, 0));

        mockMvc.perform(delete("/api/dossiers/{id}/echeancier/{echeanceId}", dossier.getId(), ligne.getId()))
                .andExpect(status().isForbidden());

        entityManager.flush();
        assertThat(documentRepository.existsById(ligne.getId())).isTrue();
    }
}
