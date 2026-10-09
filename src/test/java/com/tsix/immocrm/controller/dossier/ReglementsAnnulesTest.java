package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-lot4 §2 : une ligne d'un règlement CANCELED ne compte dans aucun total. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementsAnnulesTest {

    private static final String MONTANTS = "/api/dossiers/{id}/montants";
    private static final String REGLE_ECHEANCE = "$.parCategorie[0].regle";
    private static final String RESTE_ECHEANCE = "$.parCategorie[0].reste";
    private static final String ANNULE = "CANCELED";
    private static final String EN_COURS = "IN_WORK";
    private static final String MONTANT_ECHEANCE = "100000";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private StkVocationRepository stkVocationRepository;
    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private EntityManager entityManager;

    private CfgTier newTier(String prefixe) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(prefixe + "-" + UUID.randomUUID());
        tier.setDescription("Tiers " + prefixe);
        return tierRepository.save(tier);
    }

    private ComFolder newFolder(String code, CfgTier client) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-ANN-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        folder.setCfgCustomer(client);
        return folderRepository.save(folder);
    }

    private ComDocument newEcheance(ComFolder folder, String montant) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new BigDecimal(montant));
        return documentRepository.save(doc);
    }

    /** Règlement saisi directement en base, au statut donné (NULL = ligne légataire ancienne). */
    private StlSettlement newReglement(ComDocument echeance, String statut, String montant) {
        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("REG-ANN-" + UUID.randomUUID());
        s.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        s.setAmount(new BigDecimal(montant));
        s.setStatus(statut);
        s.setIsCredit(true);
        s = settlementRepository.save(s);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(s);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal(montant));
        settlementLineRepository.save(ligne);
        return s;
    }

    private void relireDepuisLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void annulerUnReglement_faitRemonterLeResteAPayerDeSonMontantExact() throws Exception {
        ComFolder folder = newFolder("DOS-ANN-1", null);
        ComDocument echeance = newEcheance(folder, MONTANT_ECHEANCE);
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIR-ANN-" + UUID.randomUUID());
        type.setIsReferenceMandatory(false);
        type.setIsBankMandatory(false);
        settlementTypeRepository.save(type);
        String corps = "{\"montant\":60000.00,\"dateEcheance\":\"2026-09-01T10:00:00\",\"typeId\":\""
                + type.getId() + "\",\"payeurId\":\"" + newTier("PAYEUR").getId() + "\"}";
        String json = mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID reglementId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));

        mockMvc.perform(get(MONTANTS, folder.getId()))
                .andExpect(jsonPath(REGLE_ECHEANCE).value(60000.0))
                .andExpect(jsonPath(RESTE_ECHEANCE).value(40000.0));

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(MONTANTS, folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(REGLE_ECHEANCE).value(0.0))
                .andExpect(jsonPath(RESTE_ECHEANCE).value(100000.0));
        mockMvc.perform(get("/api/dossiers/{id}/echeancier", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].montantRegle").value(0.0))
                .andExpect(jsonPath("$.content[0].reste").value(100000.0));
    }

    @Test
    void reglementsInWorkEtStatutNul_comptent_annuleNeComptePas() throws Exception {
        ComFolder folder = newFolder("DOS-ANN-2", null);
        ComDocument echeance = newEcheance(folder, MONTANT_ECHEANCE);
        newReglement(echeance, EN_COURS,"10000");
        newReglement(echeance, null, "20000");
        newReglement(echeance, ANNULE,"50000");

        mockMvc.perform(get(MONTANTS, folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(REGLE_ECHEANCE).value(30000.0))
                .andExpect(jsonPath(RESTE_ECHEANCE).value(70000.0));
        mockMvc.perform(get("/api/dossiers/{id}/echeancier", folder.getId()))
                .andExpect(jsonPath("$.content[0].montantRegle").value(30000.0));
    }

    @Test
    void annule_restePourtantVisibleDansLaListeDesReglementsDeLEcheance() throws Exception {
        ComFolder folder = newFolder("DOS-ANN-3", null);
        ComDocument echeance = newEcheance(folder, MONTANT_ECHEANCE);
        newReglement(echeance, ANNULE,"50000");

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value(ANNULE));
    }

    @Test
    void totauxClient_ignorentLeReglementAnnule() {
        CfgTier client = newTier("CLI-ANN");
        ComDocument echeance = newEcheance(newFolder("DOS-ANN-4", client), MONTANT_ECHEANCE);
        newReglement(echeance, "POINTED", "15000");
        newReglement(echeance, ANNULE,"40000");

        List<Object[]> lignes = settlementLineRepository.sumAmountGroupedByClientId(List.of(client.getId()));

        assertThat(lignes).hasSize(1);
        assertThat((BigDecimal) lignes.get(0)[1]).isEqualByComparingTo("15000");
    }

    @Test
    void montantRestantDUnEncaissementAnnule_estSonMontantBrut() throws Exception {
        ComDocument echeance = newEcheance(newFolder("DOS-ANN-5", null), MONTANT_ECHEANCE);
        StlSettlement annule = newReglement(echeance, ANNULE,"50000");
        StlSettlement actif = newReglement(echeance, EN_COURS,"30000");
        relireDepuisLaBase();

        mockMvc.perform(get("/api/encaissements/" + annule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantBrutRestant").value(50000.0))
                .andExpect(jsonPath("$.etat").value(ANNULE));
        mockMvc.perform(get("/api/encaissements/" + actif.getId()))
                .andExpect(jsonPath("$.montantBrutRestant").value(0.0));
    }

    @Test
    void sommeParEcheance_etParReglement_excluentLAnnule() {
        ComDocument echeance = newEcheance(newFolder("DOS-ANN-6", null), MONTANT_ECHEANCE);
        StlSettlement annule = newReglement(echeance, ANNULE,"50000");
        newReglement(echeance, EN_COURS,"5000");

        assertThat(settlementLineRepository.sumAmountByComDocumentId(echeance.getId())).isEqualByComparingTo("5000");
        assertThat(settlementLineRepository.sumAmountByStlSettlementId(annule.getId())).isEqualByComparingTo("0");
    }

    @Test
    void predicatJava_seulCanceledEstExclu() {
        assertThat(StatutReglement.compteDansLesTotaux((String) null)).isTrue();
        assertThat(StatutReglement.compteDansLesTotaux(EN_COURS)).isTrue();
        assertThat(StatutReglement.compteDansLesTotaux("POINTED")).isTrue();
        assertThat(StatutReglement.compteDansLesTotaux("canceled")).isFalse();
    }
}
