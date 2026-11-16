package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsLot;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CATALOGUE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUT;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUTS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.TRANSITION;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.TRANSITIONS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.corps;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.creation;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.modification;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.suppression;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.transition;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.TYPE_INTERMEDIAIRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue F1 (FEATURE-restes-projet-achat.md §6) : un statut ou une transition d'un genre qui n'est pas un des 5 genres de lot
 * est invisible de l'éditeur — 404 en écriture, rien de modifié ; un genre hors lot au POST → 400 ; catalogue → 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsLotHorsGenreRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsLot fixture;
    @Autowired private FixtureStatutsAchat comptes;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID horsLot;
    private UUID statut;
    private UUID autre;
    private UUID transitionHorsLot;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1R");
        fixture.genres();
        admin = comptes.administrateur(jeton);
        horsLot = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)",
                horsLot.toString(), "HorsLot" + jeton, "Hors lot " + jeton);
        statut = fixture.statut(horsLot, "HL1-" + jeton, 1);
        autre = fixture.statut(horsLot, "HL2-" + jeton, 2);
        transitionHorsLot = fixture.transition(horsLot, "HL " + jeton, statut, autre);
    }

    @Test
    void statutHorsLot_put_404_inchange() throws Exception {
        mockMvc.perform(modification(json, STATUT, statut, corps(horsLot, "PIRATE-" + jeton, TYPE_INTERMEDIAIRE))
                .with(admin)).andExpect(status().isNotFound());

        assertThat(fixture.ligne(statut)).isNotNull().containsEntry("Code", "HL1-" + jeton);
    }

    @Test
    void statutHorsLot_delete_404_conserve() throws Exception {
        jdbc.update("DELETE FROM StrWorkFlow WHERE PKey = ?", transitionHorsLot.toString());

        mockMvc.perform(suppression(STATUT, autre).with(admin)).andExpect(status().isNotFound());

        assertThat(fixture.statutsParId(autre)).isEqualTo(1);
    }

    @Test
    void transitionHorsLot_putEtDelete_404_conservee() throws Exception {
        mockMvc.perform(modification(json, TRANSITION, transitionHorsLot,
                transition(horsLot, autre, statut, "Pirate " + jeton)).with(admin)).andExpect(status().isNotFound());
        mockMvc.perform(suppression(TRANSITION, transitionHorsLot).with(admin)).andExpect(status().isNotFound());

        assertThat(fixture.transitionsParId(transitionHorsLot)).isEqualTo(1);
        assertThat(fixture.transitionsDecrites("Pirate " + jeton)).isZero();
    }

    @Test
    void creation_genreHorsLot_400() throws Exception {
        mockMvc.perform(creation(json, STATUTS, corps(horsLot, "HL3-" + jeton, TYPE_INTERMEDIAIRE)).with(admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(creation(json, TRANSITIONS, transition(horsLot, autre, statut, "Retour " + jeton)).with(admin))
                .andExpect(status().isBadRequest());

        assertThat(fixture.statutsDuCode(horsLot, "HL3-" + jeton)).isZero();
        assertThat(fixture.transitionsDecrites("Retour " + jeton)).isZero();
    }

    @Test
    void catalogue_genreHorsLot_404() throws Exception {
        mockMvc.perform(get(CATALOGUE).param("genreId", horsLot.toString()).with(admin))
                .andExpect(status().isNotFound());
    }
}
