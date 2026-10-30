package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code /api/audit} n'exige aucun rôle : le journal d'un rendez-vous reste réservé à l'Administrateur de l'écran. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RendezVousAuditAccesTest {

    private static final String AUDIT = "/api/audit";
    private static final String OBJET = "objetId";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches taches;
    @Autowired private FixtureRendezVous fixture;

    @Test
    void journalDUnRendezVous_lisibleParLAdministrateur_404PourUnAutreRole() throws Exception {
        taches.assurer();
        AdmUser admin = patrimoine.administrateur();
        UUID id = fixture.rendezVous("ACC-" + FixtureReferentielsAdmin.jeton(), JOUR);

        mockMvc.perform(get(AUDIT).param(OBJET, id.toString()).with(connecte(admin))).andExpect(status().isOk());
        mockMvc.perform(get(AUDIT).param(OBJET, id.toString())
                        .with(FixtureFaisabiliteWorkflow.avecRole(admin, "Commercial")))
                .andExpect(status().isNotFound());
    }

    @Test
    void journalDUneTache_404MemePourLAdministrateur() throws Exception {
        taches.assurer();
        AdmUser admin = patrimoine.administrateur();
        UUID id = taches.tache("ACT-" + FixtureReferentielsAdmin.jeton());

        mockMvc.perform(get(AUDIT).param(OBJET, id.toString()).with(connecte(admin))).andExpect(status().isNotFound());
    }
}
