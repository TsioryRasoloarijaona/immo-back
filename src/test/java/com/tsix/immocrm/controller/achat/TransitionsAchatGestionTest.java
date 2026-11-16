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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
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
 * Gestion d'une transition d'achat au PUT — FEATURE-restes-projet-achat.md §5 et « Vérification » §5 : renvoyer {@code gestionId}
 * la conserve, l'omettre la fait passer à « Sans gestion » (comportement documenté), en changer la déplace ; couple déjà présent
 * dans la gestion visée → 409. Relu par GET /transitions ({@code gestionCode}, {@code gestionLibelle}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionsAchatGestionTest {

    private static final String TRANSITION = TRANSITIONS + "/{id}";
    private static final String GESTION_CODE = "gestionCode";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID enCours;
    private UUID valide;
    private UUID g1;
    private UUID transition;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1G");
        admin = statuts.administrateur(jeton);
        enCours = config.statut(Statut.simple("EC-" + jeton, 1));
        valide = config.statut(Statut.simple("VA-" + jeton, 2));
        g1 = config.gestion("G1-" + jeton, "Gestion 1 " + jeton);
        transition = config.transition(g1, 1, "Gérée " + jeton, enCours, valide);
    }

    private Map<String, Object> corps(UUID gestion) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ordre", 1);
        c.put("description", "Gérée modifiée " + jeton);
        c.put("statutDepartId", enCours.toString());
        c.put("statutArriveeId", valide.toString());
        c.put("noteObligatoire", false);
        c.put("valideeParDeclencheur", false);
        c.put("roleIds", List.of());
        if (gestion != null) {
            c.put("gestionId", gestion.toString());
        }
        return c;
    }

    private MockHttpServletRequestBuilder modification(Map<String, Object> corps) {
        return put(TRANSITION, transition).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps));
    }

    private Map<String, Object> relire() throws Exception {
        Map<String, Object> l = ligne(lire(mockMvc, TRANSITIONS, admin), transition);
        return l == null ? null : new HashMap<>(l);
    }

    @Test
    void s5_put_avecSaGestionId_gestionConservee() throws Exception {
        mockMvc.perform(modification(corps(g1)).with(admin)).andExpect(status().isOk());

        assertThat(relire()).isNotNull().containsEntry(GESTION_CODE, "G1-" + jeton);
    }

    /** Comportement documenté : un PUT sans {@code gestionId} range la transition « Sans gestion ». */
    @Test
    void s5_put_sansGestionId_passeASansGestion() throws Exception {
        mockMvc.perform(modification(corps(null)).with(admin)).andExpect(status().isOk());

        assertThat(relire()).isNotNull().containsEntry(GESTION_CODE, null).containsEntry("gestionLibelle", "Sans gestion");
    }

    @Test
    void s5_put_autreGestion_gestionMiseAJour() throws Exception {
        UUID g2 = config.gestion("G2-" + jeton, "Gestion 2 " + jeton);

        mockMvc.perform(modification(corps(g2)).with(admin)).andExpect(status().isOk());

        assertThat(relire()).isNotNull().containsEntry(GESTION_CODE, "G2-" + jeton);
    }

    /** Couple départ-arrivée déjà présent dans la gestion visée → 409, la transition garde sa gestion. */
    @Test
    void s5_put_versGestionAyantDejaLeCouple_409_inchangee() throws Exception {
        UUID g2 = config.gestion("G2-" + jeton, "Gestion 2 " + jeton);
        config.transition(g2, 1, "Occupante " + jeton, enCours, valide);

        mockMvc.perform(modification(corps(g2)).with(admin)).andExpect(status().isConflict());

        assertThat(relire()).isNotNull().containsEntry(GESTION_CODE, "G1-" + jeton);
    }
}
