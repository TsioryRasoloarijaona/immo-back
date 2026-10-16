package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-dossiers-liste-complete.md : une colonne de table ne change pas selon ses voisines. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeMontantsNatureRevueTest {

    private static final String PREMIER = "$.content[0].prixContractuel";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private EntityManager em;

    /** AgreementPrice NULL : la colonne vaut null seule, elle doit rester null à côté d'une colonne de montant. */
    @Test
    void prixContractuelNul_resteNulQuandUnMontantParNatureEstDemande() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-REV-" + UUID.randomUUID().toString().substring(0, 6), true);
        String prefixe = "REV" + UUID.randomUUID().toString().substring(0, 6);
        ComFolder f = fixture.dossier(prefixe, tranche, StatutDossier.CONCRETISATION);
        f.setPrice(new BigDecimal("1000000"));
        f.setAgreementPrice(null);
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes", "prixContractuel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER).doesNotExist());
        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes", "prixContractuel,avance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER).doesNotExist());
    }
}
