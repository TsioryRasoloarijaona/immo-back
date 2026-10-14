package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.services.dossier.liste.StatutsVisiblesListe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Point d'extension des statuts visibles par rôle (FEATURE-vente-liste.md §9) : chemins de refus. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeStatutsVisiblesTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private FixtureStatutsDossier statuts;
    @Autowired
    private EntityManager em;
    @MockitoBean
    private StatutsVisiblesListe statutsVisibles;

    private String prefixe;
    private ComFolder concretise;

    @BeforeEach
    void setUp() {
        CfgTranche tranche = fixture.tranche("TR-LSV-" + UUID.randomUUID().toString().substring(0, 6), true);
        prefixe = "LSTV" + UUID.randomUUID().toString().substring(0, 6);
        concretise = fixture.dossier(prefixe + "A", tranche, null);
        concretise.setStrStatus(statuts.statut(StatutDossier.CONCRETISATION));
        ComFolder reserve = fixture.dossier(prefixe + "B", tranche, null);
        reserve.setStrStatus(statuts.statut(StatutDossier.RESERVATION));
        em.flush();
        em.clear();
    }

    @Test
    void restriction_neSertQueLesStatutsVisibles() throws Exception {
        when(statutsVisibles.pourUtilisateurCourant())
                .thenReturn(Optional.of(Set.of(statuts.statut(StatutDossier.CONCRETISATION).getId())));

        mockMvc.perform(get("/api/dossiers").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(concretise.getId().toString()));
        mockMvc.perform(get("/api/dossiers/statuts"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CONCRETISATION"));
    }

    @Test
    void ensembleVide_neSertAucunDossier() throws Exception {
        when(statutsVisibles.pourUtilisateurCourant()).thenReturn(Optional.of(Set.of()));

        mockMvc.perform(get("/api/dossiers").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
