package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
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
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-n3bis-b2a-retenue.md §6 E4 : chaque ligne d'affectation se compare au reste de son échéance au centime
 * ({@code CalculReglement.depasse}), à la création comme à l'ajout d'affectations ; message inchangé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementVenteAffectationCentimeTest {

    private static final String REGLEMENTS = "/api/dossiers/{id}/reglements";
    private static final String AFFECTATIONS = "/api/dossiers/{id}/reglements/{reglementId}/affectations";
    private static final String ECHEANCE_100 = "100.00";
    private static final String MESSAGE_ECHEANCE = "montant (100.01) dépasse le reste à payer de l'échéance";

    @Autowired private MockMvc mockMvc;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categoryRepository;
    @Autowired private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired private CfgTierRepository tierRepository;

    private ComFolder dossier;
    private ComSettlementCategory categorie;
    private ComDocument echeance;
    private StlSettlementType type;
    private CfgTier payeur;

    @BeforeEach
    void setUp() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-RVA-" + UUID.randomUUID());
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-RVA");
        folder.setStkVocation(vocation);
        dossier = folderRepository.save(folder);

        categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("PRE-RES-RVA");
        categorie.setDescription("Catégorie PRE-RES-RVA");
        categoryRepository.save(categorie);

        echeance = echeanceDe100(LocalDateTime.of(2026, 1, 10, 0, 0));

        type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIREMENT-RVA");
        type.setDescription("VIREMENT RVA");
        type.setIsReferenceMandatory(false);
        type.setIsBankMandatory(false);
        settlementTypeRepository.save(type);

        payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-RVA");
        payeur.setDescription("Karim ALAMI");
        tierRepository.save(payeur);
    }

    private ComDocument echeanceDe100(LocalDateTime date) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(dossier);
        doc.setComSettlementCategory(categorie);
        doc.setDocDate(date);
        doc.setAmount(new BigDecimal(ECHEANCE_100));
        return documentRepository.save(doc);
    }

    private static String ligne(ComDocument e, String montant) {
        return "{\"echeanceId\":\"" + e.getId() + "\",\"montant\":" + montant + "}";
    }

    // Règlement de 300 : seule l'échéance de 100.00 borne la ligne.
    private String corps(String... lignes) {
        return "{\"montant\":300.00,\"dateEcheance\":\"2026-09-01T10:00:00\""
                + ",\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + payeur.getId() + "\""
                + ",\"affectations\":[" + String.join(",", lignes) + "]}";
    }

    private ResultActions creer(String corps) throws Exception {
        return mockMvc.perform(post(REGLEMENTS, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ResultActions affecter(UUID reglementId, String... lignes) throws Exception {
        return mockMvc.perform(post(AFFECTATIONS, dossier.getId(), reglementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"affectations\":[" + String.join(",", lignes) + "]}"));
    }

    /** Règlement de 300 qui solde la première échéance (100) : reste 200 sur le règlement. */
    private UUID reglementSoldantLaPremiere() throws Exception {
        String reponse = creer(corps(ligne(echeance, ECHEANCE_100))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    // E4 création : 0.004 au-dessus du reste de l'échéance, arrondi au centime → accepté.
    @Test
    void e4_creation_ecartSousLeCentimeSurEcheance_201() throws Exception {
        creer(corps(ligne(echeance, "100.004"))).andExpect(status().isCreated());
    }

    // E4 création : 0.01 au-dessus → 400, message inchangé.
    @Test
    void e4_creation_ecartDUnCentimeSurEcheance_400() throws Exception {
        creer(corps(ligne(echeance, "100.01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_ECHEANCE)));
    }

    // E4 ajout d'affectations : 0.004 au-dessus du reste de la seconde échéance → accepté.
    @Test
    void e4_affectations_ecartSousLeCentimeSurEcheance_2xx() throws Exception {
        UUID reglement = reglementSoldantLaPremiere();
        ComDocument seconde = echeanceDe100(LocalDateTime.of(2026, 2, 10, 0, 0));

        affecter(reglement, ligne(seconde, "100.004")).andExpect(status().is2xxSuccessful());
    }

    // E4 ajout d'affectations : 0.01 au-dessus → 400, message inchangé.
    @Test
    void e4_affectations_ecartDUnCentimeSurEcheance_400() throws Exception {
        UUID reglement = reglementSoldantLaPremiere();
        ComDocument seconde = echeanceDe100(LocalDateTime.of(2026, 2, 10, 0, 0));

        affecter(reglement, ligne(seconde, "100.01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_ECHEANCE)));
    }
}
