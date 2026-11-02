package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSessions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-admin-sessions.md : verrou de fermeture sous concurrence réelle et saisies illisibles. Non transactionnel. */
@SpringBootTest
@AutoConfigureMockMvc
class SessionsRevueTest {

    private static final String URL = "/api/administration/sessions";
    private static final int APPELS = 6;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions fixture;
    @Autowired private PlatformTransactionManager transactions;

    private AdmUser admin;
    private UUID session;
    private final String adresse = "revue/" + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        session = new TransactionTemplate(transactions).execute(t -> {
            fixture.assurerTables();
            return fixture.session(admin, LocalDateTime.of(2026, 3, 10, 9, 15, 30))
                    .avec(FixtureSessions.ADRESSE_IP, adresse).inserer();
        });
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmWebSessions WHERE PKey = ?", session.toString());
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    @Test
    void fermeturesSimultanees_uneSeuleReussit_lesAutres409() throws Exception {
        CountDownLatch depart = new CountDownLatch(1);
        Callable<Integer> appel = () -> {
            depart.await();
            return mockMvc.perform(post(URL + "/{id}/fermer", session).with(connecte(admin)))
                    .andReturn().getResponse().getStatus();
        };
        List<Integer> statuts = new ArrayList<>();
        try (ExecutorService fils = Executors.newFixedThreadPool(APPELS)) {
            List<Future<Integer>> reponses = new ArrayList<>();
            for (int i = 0; i < APPELS; i++) {
                reponses.add(fils.submit(appel));
            }
            depart.countDown();
            for (Future<Integer> reponse : reponses) {
                statuts.add(reponse.get());
            }
        }

        assertThat(statuts).containsOnly(200, 409).filteredOn(s -> s == 200).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {URL + "?dateDebut=pas-une-date", URL + "?dateFin=31/03/2026", URL + "?ouverte=peut-etre"})
    void administrateur_parametreIllisible_400(String url) throws Exception {
        mockMvc.perform(get(url).with(connecte(admin))).andExpect(status().isBadRequest());
    }

    @Test
    void administrateur_idNonUuid_400() throws Exception {
        mockMvc.perform(post(URL + "/pas-un-uuid/fermer").with(connecte(admin))).andExpect(status().isBadRequest());
    }

    @Test
    void utilisateurBlanc_pasDeFiltre() throws Exception {
        mockMvc.perform(get(URL).with(connecte(admin)).param("utilisateur", "   ").param("ip", adresse))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void pageTresGrande_pasDe500() throws Exception {
        int statut = mockMvc.perform(get(URL).with(connecte(admin)).param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("size", "200"))
                .andReturn().getResponse().getStatus();

        assertThat(statut).isLessThan(500);
    }
}
