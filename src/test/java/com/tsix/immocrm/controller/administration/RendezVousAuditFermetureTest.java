package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FICHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.NOTES;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ouvrir {@code /api/audit} aux rendez-vous ne doit ouvrir ni {@code /api/notes}, ni le journal à un autre rôle. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RendezVousAuditFermetureTest {

    private static final String AUDIT = "/api/audit";
    private static final String NOTES_API = "/api/notes";
    private static final String OBJET = "objetId";
    private static final String PORTEUR = "porteur";
    private static final String ID = "id";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches taches;
    @Autowired private FixtureRendezVous fixture;

    private AdmUser admin;
    private UUID rendezVous;

    @BeforeEach
    void setUp() {
        taches.assurer();
        admin = patrimoine.administrateur();
        rendezVous = fixture.rendezVous("FER-" + FixtureReferentielsAdmin.jeton(), JOUR);
    }

    @Test
    void notes_porteurRendezVous_refuse400() throws Exception {
        mockMvc.perform(get(NOTES_API).param(PORTEUR, "rendez-vous").param(ID, rendezVous.toString()).with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"projet", "tranche", "article", "prospection-terrain", "etude"})
    void notes_idDeRendezVous_404SousToutPorteur(String porteur) throws Exception {
        mockMvc.perform(get(NOTES_API).param(PORTEUR, porteur).param(ID, rendezVous.toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void notes_ecritureSurUnRendezVous_404() throws Exception {
        mockMvc.perform(post(NOTES_API).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(PORTEUR, "article").avec(ID, rendezVous).avec("texte", "Note").json()))
                .andExpect(status().isNotFound());
    }

    @Test
    void audit_directionGenerale_404() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET, rendezVous.toString())
                        .with(FixtureFaisabiliteWorkflow.avecRole(admin, "Direction Générale")))
                .andExpect(status().isNotFound());
    }

    @Test
    void audit_sansConnexion_refuse() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET, rendezVous.toString())).andExpect(status().is4xxClientError());
    }

    @Test
    void audit_idInconnu_404PourLAdministrateur() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET, UUID.randomUUID().toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    /** La saisie vide efface côté serveur : ce que la fiche doit envoyer pour vider les notes. */
    @Test
    void notes_chaineVide_efface() throws Exception {
        taches.definir(rendezVous, "Notes", "A effacer");

        mockMvc.perform(patch(FICHE, rendezVous).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(NOTES, "").json()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value(""));
    }
}
