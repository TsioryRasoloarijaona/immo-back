package com.tsix.immocrm.controller.client;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/clients[/...]} — a "client" is any {@code CfgTier} referenced by at least one
 * {@code ComFolder.CfgCustomerId}, regardless of type/kind. See {@code ClientService}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    private StkVocation newVocation(String suffix) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-CLIENT-" + suffix);
        return vocationRepository.save(vocation);
    }

    @Test
    void getClients_onlyReturnsTiersWithAtLeastOneDossier_withAggregatedTotals() throws Exception {
        CfgTier avecDossier = new CfgTier();
        avecDossier.setId(UUID.randomUUID());
        avecDossier.setCode("CLI-WITH-DOSSIER");
        avecDossier.setFirstName("Nor");
        avecDossier.setLastName("CHEMAOU-TEST");
        tierRepository.save(avecDossier);

        CfgTier sansDossier = new CfgTier();
        sansDossier.setId(UUID.randomUUID());
        sansDossier.setCode("CLI-NO-DOSSIER");
        sansDossier.setFirstName("Sans");
        sansDossier.setLastName("Dossier");
        tierRepository.save(sansDossier);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-CLIENT-TEST");
        folder.setStkVocation(newVocation("1"));
        folder.setCfgCustomer(avecDossier);
        folderRepository.save(folder);

        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(folder);
        echeance.setAmount(new BigDecimal("100000.00"));
        documentRepository.save(echeance);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setAmount(new BigDecimal("40000.00"));
        settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(new BigDecimal("40000.00"));
        settlementLineRepository.save(line);

        String json = mockMvc.perform(get("/api/clients").param("search", "CHEMAOU-TEST"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer total = JsonPath.read(json, "$.totalElements");
        assertThat(total).isEqualTo(1);
        assertThat((String) JsonPath.read(json, "$.content[0].nomAffiche")).contains("CHEMAOU-TEST");
        assertThat(((Number) JsonPath.read(json, "$.content[0].nbDossiers")).intValue()).isEqualTo(1);
        assertThat(((Number) JsonPath.read(json, "$.content[0].totalAPayer")).doubleValue()).isEqualTo(100000.0);
        assertThat(((Number) JsonPath.read(json, "$.content[0].totalRegle")).doubleValue()).isEqualTo(40000.0);
        assertThat(((Number) JsonPath.read(json, "$.content[0].totalReste")).doubleValue()).isEqualTo(60000.0);

        // The tier with no dossier at all must never appear, however it's searched for.
        mockMvc.perform(get("/api/clients").param("search", "Sans"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String jsonSans = mockMvc.perform(get("/api/clients").param("search", "Sans"))
                .andReturn().getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(jsonSans, "$.totalElements")).isZero();
    }

    @Test
    void getClientById_reusesProspectFiche() throws Exception {
        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-DETAIL-TEST");
        client.setFirstName("Karim");
        client.setLastName("MAZZI-TEST");
        tierRepository.save(client);

        mockMvc.perform(get("/api/clients/{id}", client.getId()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.nom").value("MAZZI-TEST"));
    }

    @Test
    void getClientById_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/clients/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getClientDossiers_returnsOnlyThisClientsDossiers() throws Exception {
        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-DOSSIERS-TEST");
        tierRepository.save(client);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-CLIENT-DOSSIERS-TEST");
        folder.setStkVocation(newVocation("2"));
        folder.setCfgCustomer(client);
        folderRepository.save(folder);

        mockMvc.perform(get("/api/clients/{id}/dossiers", client.getId()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.content[0].code").value("DOS-CLIENT-DOSSIERS-TEST"));
    }
}
