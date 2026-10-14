package com.tsix.immocrm.web;

import com.tsix.immocrm.config.SourceDeVeriteProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue R7 en mono-base (pas de {@code ConnexionSourceSuivie}) : une requête dont une première écriture est partie
 * (auto-commit) puis dont l'emprunt suivant échoue ne doit pas répondre « rien n'a été enregistré ». R6 : en mono-base
 * aucune 503 — 500 générique du gestionnaire (FEATURE-erreur-500-401.md).
 */
class LiaisonSourceMonoBaseRevueTest {

    @RestController
    static class DeuxEcritures {

        private final DataSource base;

        DeuxEcritures(DataSource base) {
            this.base = base;
        }

        @PostMapping("/deux-ecritures")
        void deuxEcritures() {
            JdbcTemplate jdbc = new JdbcTemplate(base);
            jdbc.update("UPDATE ComFolder SET Description = 'x' WHERE 1 = 0");
            jdbc.update("UPDATE ComFolder SET Description = 'y' WHERE 1 = 0");
        }
    }

    @Test
    void r7_monoBase_echecDeConnexionApresUneEcritureValidee_pasDe503() throws Exception {
        DataSource base = mock(DataSource.class);
        Connection con = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(con.createStatement()).thenReturn(st);
        when(st.executeUpdate(anyString())).thenReturn(1);
        when(base.getConnection())
                .thenReturn(con)
                .thenThrow(new SQLTransientConnectionException(
                        "HikariPool-1 - Connection is not available, request timed out after 30000ms.", "08001"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DeuxEcritures(base))
                .setControllerAdvice(new GlobalExceptionHandler(
                        new DefaultListableBeanFactory().getBeanProvider(SourceDeVeriteProperties.class)))
                .build();

        mockMvc.perform(post("/deux-ecritures"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").doesNotExist());
        verify(st).executeUpdate(anyString());
    }
}
