package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-reglement-multi-echeances.md : bornes d'arrondi des montants de ligne (§2.1 règle 6). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementMultiEcheancesRevueTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private ComSettlementCategoryRepository categoryRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired
    private CfgTierRepository tierRepository;

    private ComFolder dossier;
    private ComDocument echeance;
    private StlSettlementType type;
    private CfgTier payeur;

    @BeforeEach
    void setUp() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-RMR-" + UUID.randomUUID());
        vocationRepository.save(vocation);

        dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOS-RMR");
        dossier.setStkVocation(vocation);
        folderRepository.save(dossier);

        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("AVANCE-RMR");
        categorie.setDescription("Catégorie AVANCE-RMR");
        categoryRepository.save(categorie);

        echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setComSettlementCategory(categorie);
        echeance.setDocDate(LocalDateTime.of(2026, 1, 10, 0, 0));
        echeance.setAmount(new BigDecimal("5000.00"));
        documentRepository.save(echeance);

        type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIREMENT-RMR");
        type.setDescription("VIREMENT RMR");
        type.setIsReferenceMandatory(false);
        type.setIsBankMandatory(false);
        settlementTypeRepository.save(type);

        payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-RMR");
        payeur.setDescription("Karim ALAMI");
        tierRepository.save(payeur);
    }

    /**
     * Règle 6 : une ligne à 0 est refusée. 0,0000001 passe {@code signum() > 0} mais s'écrit à 0,000000 dans
     * la colonne decimal(26,6) : une ligne à 0 en base.
     */
    @Test
    void creer_ligneSousLaPrecisionDeLaColonne_400SansLigneAZero() throws Exception {
        long lignesAvant = settlementLineRepository.count();
        String corps = "{\"montant\":1000.00,\"dateEcheance\":\"2026-09-01T10:00:00\""
                + ",\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + payeur.getId() + "\""
                + ",\"affectations\":[{\"echeanceId\":\"" + echeance.getId() + "\",\"montant\":0.0000001}]}";

        mockMvc.perform(post("/api/dossiers/{id}/reglements", dossier.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest());

        assertThat(settlementLineRepository.count()).isEqualTo(lignesAvant);
    }
}
