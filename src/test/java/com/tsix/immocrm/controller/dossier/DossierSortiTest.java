package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.dossier.DossierSorti;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dossier sorti lu sur les drapeaux 002 / 003 du statut, pas sur son code (FEATURE-dossier-durcissement.md §6). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class DossierSortiTest {

    private static final String SOUS_ARTICLES = "/api/dossiers/{id}/sous-articles";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StrStatusRepository statusRepository;

    private CfgTranche tranche;

    @BeforeEach
    void tranche() {
        tranche = fixture.tranche("TR-SORTI", true);
    }

    private ComFolder dossierAuStatut(String code, Boolean annulation, Boolean resiliation) {
        StrStatus s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(code);
        s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        s.setAnnulation(annulation);
        s.setResiliation(resiliation);
        ComFolder dossier = fixture.dossier("DOS-SORTI-" + code, tranche, StatutDossier.RESERVATION);
        dossier.setStrStatus(statusRepository.save(s));
        return folderRepository.save(dossier);
    }

    @Test
    void statutResiliationAuCodeInattendu_drapeau003_sousArticlesRefuses() throws Exception {
        ComFolder dossier = dossierAuStatut("TST-RESILIATION", false, true);

        mockMvc.perform(post(SOUS_ARTICLES, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("ne se modifient plus")));
    }

    @Test
    void statutAnnulationAuCodeInattendu_drapeau002_estSorti() {
        assertThat(DossierSorti.estSorti(dossierAuStatut("TST-ANNULE", true, false))).isTrue();
    }

    @Test
    void codeAnnulationSansDrapeau_nEstPasSorti_commeLaVue() {
        assertThat(DossierSorti.estSorti(dossierAuStatut("ANNULATION", null, null))).isFalse();
    }

    @Test
    void dossierSansStatut_nEstPasSorti() {
        ComFolder dossier = fixture.dossier("DOS-SORTI-NUL", tranche, null);

        assertThat(DossierSorti.estSorti(dossier)).isFalse();
    }
}
