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
 * FEATURE-n3bis-b1-reglements.md §5 V-b : le dépassement du règlement de vente se juge au centime (arrondi à 2 décimales),
 * à la création ({@code POST /reglements}) comme à l'ajout d'affectations ({@code POST .../affectations}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementVenteCentimeTest {

    private static final String REGLEMENTS = "/api/dossiers/{id}/reglements";
    private static final String AFFECTATIONS = "/api/dossiers/{id}/reglements/{reglementId}/affectations";
    private static final String M_100 = "100.00";

    @Autowired private MockMvc mockMvc;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categoryRepository;
    @Autowired private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired private CfgTierRepository tierRepository;

    private ComFolder dossier;
    private ComDocument echeance;
    private StlSettlementType type;
    private CfgTier payeur;

    @BeforeEach
    void setUp() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-RVC-" + UUID.randomUUID());
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-RVC");
        folder.setStkVocation(vocation);
        dossier = folderRepository.save(folder);

        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("PRE-RES-RVC");
        categorie.setDescription("Catégorie PRE-RES-RVC");
        categoryRepository.save(categorie);

        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(dossier);
        doc.setComSettlementCategory(categorie);
        doc.setDocDate(LocalDateTime.of(2026, 1, 10, 0, 0));
        doc.setAmount(new BigDecimal("5000.00"));
        echeance = documentRepository.save(doc);

        type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIREMENT-RVC");
        type.setDescription("VIREMENT RVC");
        type.setIsReferenceMandatory(false);
        type.setIsBankMandatory(false);
        settlementTypeRepository.save(type);

        payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-RVC");
        payeur.setDescription("Karim ALAMI");
        tierRepository.save(payeur);
    }

    private String ligne(String montant) {
        return "{\"echeanceId\":\"" + echeance.getId() + "\",\"montant\":" + montant + "}";
    }

    private String corps(String montant, String... lignes) {
        return "{\"montant\":" + montant + ",\"dateEcheance\":\"2026-09-01T10:00:00\""
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

    /** Seconde échéance du dossier : une échéance ne s'affecte qu'une fois par règlement. */
    private String ligneSurSecondeEcheance(String montant) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(dossier);
        doc.setComSettlementCategory(echeance.getComSettlementCategory());
        doc.setDocDate(LocalDateTime.of(2026, 2, 10, 0, 0));
        doc.setAmount(new BigDecimal("5000.00"));
        ComDocument seconde = documentRepository.save(doc);
        return "{\"echeanceId\":\"" + seconde.getId() + "\",\"montant\":" + montant + "}";
    }

    /** Règlement de 100.00 déjà affecté de 60 : reste à affecter 40. */
    private UUID reglementAffecteDe60() throws Exception {
        String reponse = creer(corps(M_100, ligne("60"))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    // ------------------------------------------------------------------ V-b création

    @Test
    void vB_creation_depassementInferieurAuCentime_201() throws Exception {
        creer(corps(M_100, ligne("100.004"))).andExpect(status().isCreated());
    }

    @Test
    void vB_creation_depassementDUnCentime_400_messageInchange() throws Exception {
        creer(corps(M_100, ligne("100.01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le reste à affecter doit être >= 0 : les affectations (100.01)"
                        + " dépassent le montant du règlement (100.00)"));
    }

    // ------------------------------------------------------------------ V-b ajout d'affectations

    @Test
    void vB_affectations_depassementInferieurAuCentime_2xx() throws Exception {
        UUID reglement = reglementAffecteDe60();

        affecter(reglement, ligneSurSecondeEcheance("40.004")).andExpect(status().is2xxSuccessful());
    }

    @Test
    void vB_affectations_depassementDUnCentime_400() throws Exception {
        UUID reglement = reglementAffecteDe60();

        affecter(reglement, ligneSurSecondeEcheance("40.01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("dépassent le reste à affecter (")));
    }
}
