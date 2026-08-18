package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommercialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTierKindRepository tierKindRepository;

    private CfgTierKind newKind(String code) {
        CfgTierKind kind = new CfgTierKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        return tierKindRepository.save(kind);
    }

    private CfgTier newTier(String code, CfgTierKind kind) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setFirstName("Prenom-" + code);
        tier.setLastName("Nom-" + code);
        tier.setCfgTierKind(kind);
        return tierRepository.save(tier);
    }

    @Test
    void findAll_includesCommercialKindsOnly() throws Exception {
        CfgTierKind agc = newKind("AGC");
        CfgTierKind fou = newKind("FOU-TEST");

        CfgTier commercial = newTier("COMM-TEST-1", agc);
        CfgTier fournisseur = newTier("FOU-TEST-1", fou);

        mockMvc.perform(get("/api/commerciaux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + commercial.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + fournisseur.getId() + "')]").doesNotExist());
    }
}
