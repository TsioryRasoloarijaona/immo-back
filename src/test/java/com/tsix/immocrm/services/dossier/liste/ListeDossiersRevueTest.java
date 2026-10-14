package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot D (FEATURE-vente-liste.md) : défauts relevés sur données réelles. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ListeDossiersRevueTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private ColonnesCalculeesDossier colonnesCalculees;
    @Autowired
    private EntityManager em;

    private CfgTranche tranche;
    private String prefixe;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("TR-LRV-" + UUID.randomUUID().toString().substring(0, 6), true);
        prefixe = "LRV" + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * yesRemsProd : ARS0322 (ARS 2026/00378, RESERVATION) et ARS0501 (ARS 2026/00379, CONCRETISATION) ont
     * StkItem.ComFolderId vide mais sont l'article principal d'un dossier en cours : listés deux fois.
     */
    @Test
    void articlePrincipalDUnDossierEnCours_sansLienInverse_nEstPasUnLotLibre() throws Exception {
        ComFolder f = fixture.dossier(prefixe, tranche, StatutDossier.CONCRETISATION);
        f.getStkItem().setComFolderId(null);
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("lignes", "LIBRES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /** L'export accepte 20 000 lignes, mais la requête native lie un paramètre par id (SQL Server : 2 100 max). */
    @Test
    void colonnesCalculees_surPlusDe2100Dossiers_neCassentPasLExport() {
        List<UUID> ids = Stream.generate(UUID::randomUUID).limit(2_101).toList();

        assertThatCode(() -> colonnesCalculees.pour(ids, null)).doesNotThrowAnyException();
    }
}
