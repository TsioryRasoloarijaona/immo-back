package com.tsix.immocrm.controller.desistement;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDesistements;
import com.tsix.immocrm.fixtures.FixtureDesistements.Parametres;
import com.tsix.immocrm.fixtures.FixtureDesistements.Scene;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Compléments à DesistementControllerTest : fiche, R9/R10, plafond relu, motifs (FEATURE-desistements.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DesistementCompletementTest {

    private static final String ADMIN = "Administrateur";
    private static final String CREATION = "{\"dateDemande\":\"2026-10-01T00:00:00\",\"observations\":\"Test\"}";
    private static final String VALIDATION =
            "{\"dateValidation\":\"2026-10-05T00:00:00\",\"dateLegalisation\":\"2026-10-06T00:00:00\"}";
    private static final String FICHE = "/api/desistements/{id}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDesistements desistements;
    @Autowired private FixtureDossierVente fixture;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.compte("TST-DES-CPL", ADMIN);
    }

    @Test
    void fiche_idInconnu_404() throws Exception {
        mockMvc.perform(get(FICHE, UUID.randomUUID()).with(commeAdmin())).andExpect(status().isNotFound());
    }

    @Test
    void brouillon_penaliteSaisie_remiseRecalculee() throws Exception {
        UUID id = creer(desistements.scene("CPL-PEN", Parametres.standard()));

        executer(patch(FICHE, id), "{\"penaliteApresRemise\":75000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remise", closeTo(0.25, 1e-9)))
                .andExpect(jsonPath("$.penaliteApresRemise", closeTo(75000, 1e-6)))
                .andExpect(jsonPath("$.aRembourser", closeTo(205000, 1e-6)));
    }

    @Test
    void valideNonLegalise_remise409_observationsAcceptees() throws Exception {
        UUID id = creer(desistements.scene("CPL-R9", Parametres.standard().avecTaux(BigDecimal.ZERO)));
        executer(post(FICHE + "/validation", id), "{\"dateValidation\":\"2026-10-05T00:00:00\"}")
                .andExpect(status().isOk());

        executer(patch(FICHE, id), "{\"remise\":0.5}").andExpect(status().isConflict());
        executer(patch(FICHE, id), "{\"observations\":\"Virement prévu\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observations").value("Virement prévu"));
    }

    @Test
    void legalise_plusRienNEstModifiable_409() throws Exception {
        UUID id = creer(desistements.scene("CPL-LEG", Parametres.standard()));
        executer(post(FICHE + "/validation", id), VALIDATION).andExpect(status().isOk());

        executer(patch(FICHE, id), "{\"observations\":\"Après légalisation\"}").andExpect(status().isConflict());
    }

    @Test
    void remboursementSurBrouillon_409() throws Exception {
        Scene scene = desistements.scene("CPL-BRO", Parametres.standard());
        UUID id = creer(scene);

        executer(post(FICHE + "/remboursement", id), remboursement(scene, "1000")).andExpect(status().isConflict());
    }

    @Test
    void fiche_resteARembourser_suitLesDecaissements() throws Exception {
        Scene scene = desistements.scene("CPL-RST", Parametres.standard());
        UUID id = creer(scene);
        executer(post(FICHE + "/validation", id), VALIDATION).andExpect(status().isOk());
        executer(post(FICHE + "/remboursement", id), remboursement(scene, "30000")).andExpect(status().isCreated());

        mockMvc.perform(get(FICHE, id).with(commeAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etat").value("LEGALISE"))
                .andExpect(jsonPath("$.resteARembourser", closeTo(150000, 1e-6)));
    }

    @Test
    void motifs_listeLeMotifCree() throws Exception {
        UUID motif = desistements.motif("TST-DES-MOTIF");

        mockMvc.perform(get("/api/referentiels/motifs-desistement").with(commeAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(motif.toString())));
    }

    private UUID creer(Scene scene) throws Exception {
        String reponse = executer(post("/api/dossiers/{id}/desistement", scene.dossierId()), CREATION)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    private static String remboursement(Scene scene, String montant) {
        return "{\"montant\":" + montant + ",\"dateEcheance\":\"2026-10-10T00:00:00\",\"typeId\":\""
                + scene.typeReglementId() + "\"}";
    }

    private ResultActions executer(MockHttpServletRequestBuilder requete, String corps) throws Exception {
        return mockMvc.perform(requete.with(commeAdmin()).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private RequestPostProcessor commeAdmin() {
        return FixtureDossierVente.connecte(admin, ADMIN);
    }
}
