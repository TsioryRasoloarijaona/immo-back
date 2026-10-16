package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue FEATURE-vente-fiche.md : chaque nouvelle route sous /api/dossiers/{id}/… rend 404 hors périmètre. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FicheDossierPerimetreRevueTest {

    private static final String COMMERCIAL = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;

    private ComFolder dansPerimetre;
    private ComFolder horsPerimetre;
    private RequestPostProcessor commercial;

    @BeforeEach
    void setUp() {
        CfgTranche maTranche = fixture.tranche("PERIM-A", true);
        CfgTranche autreTranche = fixture.tranche("PERIM-B", true);
        dansPerimetre = fixture.dossier("DOS-PERIM-A", maTranche, StatutDossier.RESERVATION);
        horsPerimetre = fixture.dossier("DOS-PERIM-B", autreTranche, StatutDossier.RESERVATION);

        AdmUser compte = fixture.compte("COM-PERIM", COMMERCIAL);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(maTranche.getCfgProject());
        habilitationRepository.save(habilitation);
        commercial = connecte(compte, COMMERCIAL);
    }

    @Test
    void dossierDuPerimetre_ficheLisible() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/fiche", dansPerimetre.getId()).with(commercial))
                .andExpect(status().isOk());
    }

    @Test
    void dossierHorsPerimetre_toutesLesNouvellesRoutes_404() throws Exception {
        UUID id = horsPerimetre.getId();
        String json = MediaType.APPLICATION_JSON_VALUE;
        List<MockHttpServletRequestBuilder> requetes = List.of(
                get("/api/dossiers/{id}/fiche", id),
                get("/api/dossiers/{id}/actions", id),
                post("/api/dossiers/{id}/actions", id).contentType(json).content("{\"note\":\"x\"}"),
                patch("/api/dossiers/{id}/instructions/{i}/lue", id, UUID.randomUUID()),
                get("/api/dossiers/{id}/compteurs", id),
                patch("/api/dossiers/{id}/compteurs", id).contentType(json).content("{\"compteurEau\":\"X\"}"),
                get("/api/dossiers/{id}/visites-techniques", id),
                post("/api/dossiers/{id}/visites-techniques", id).contentType(json)
                        .content("{\"debut\":\"2030-01-01T09:00:00\",\"fin\":\"2030-01-01T10:00:00\"}"));
        for (MockHttpServletRequestBuilder requete : requetes) {
            mockMvc.perform(requete.with(commercial)).andExpect(status().isNotFound());
        }
    }
}
