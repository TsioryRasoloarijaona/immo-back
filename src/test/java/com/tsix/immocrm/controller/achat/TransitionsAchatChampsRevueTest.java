package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue F1 (FEATURE-restes-projet-achat.md §5, question 3) : les 6 champs exposés d'une transition d'achat — absents du PUT,
 * ils restent ; envoyés, ils sont écrits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionsAchatChampsRevueTest {

    private static final String SIX_CHAMPS_NOTIF = "avecNotification";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID depart;
    private UUID arrivee;
    private UUID transition;
    private UUID gestion;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1C");
        admin = statuts.administrateur(jeton);
        depart = config.statut(Statut.simple("CD-" + jeton, 1));
        arrivee = config.statut(Statut.simple("CA-" + jeton, 2));
        gestion = config.gestion("GC-" + jeton, "Gestion champs " + jeton);
        transition = config.transition(gestion, 1, "Champs " + jeton, depart, arrivee);
    }

    private Map<String, Object> corps() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ordre", 1);
        c.put("description", "Champs " + jeton);
        c.put("statutDepartId", depart.toString());
        c.put("statutArriveeId", arrivee.toString());
        c.put("roleIds", List.of());
        c.put("gestionId", gestion.toString());
        return c;
    }

    private void envoyer(Map<String, Object> c) throws Exception {
        mockMvc.perform(put(TRANSITIONS + "/{id}", transition).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(c)).with(admin)).andExpect(status().isOk());
    }

    @Test
    void sixChamps_ecritsPuisConservesQuandAbsents() throws Exception {
        Map<String, Object> plein = corps();
        plein.put("champsObligatoires", List.of("MaturityDate"));
        plein.put("champsObligatoiresFournisseur", List.of("Address"));
        plein.put(SIX_CHAMPS_NOTIF, true);
        plein.put("envoiEmails", true);
        plein.put("notifierDeclencheur", true);
        plein.put("emailDeclencheur", true);
        envoyer(plein);

        envoyer(corps());

        Map<String, Object> l = ligne(lire(mockMvc, TRANSITIONS, admin), transition);
        assertThat(l).isNotNull()
                .containsEntry("champsObligatoires", List.of("MaturityDate"))
                .containsEntry("champsObligatoiresFournisseur", List.of("Address"))
                .containsEntry(SIX_CHAMPS_NOTIF, true).containsEntry("envoiEmails", true)
                .containsEntry("notifierDeclencheur", true).containsEntry("emailDeclencheur", true);
    }

    @Test
    void sixChamps_videsEnvoyes_effaces() throws Exception {
        Map<String, Object> plein = corps();
        plein.put("champsObligatoires", List.of("MaturityDate"));
        plein.put(SIX_CHAMPS_NOTIF, true);
        envoyer(plein);

        Map<String, Object> vide = corps();
        vide.put("champsObligatoires", List.of());
        vide.put(SIX_CHAMPS_NOTIF, false);
        envoyer(vide);

        Map<String, Object> l = ligne(lire(mockMvc, TRANSITIONS, admin), transition);
        assertThat(l).isNotNull().containsEntry("champsObligatoires", List.of()).containsEntry(SIX_CHAMPS_NOTIF, false);
    }
}
