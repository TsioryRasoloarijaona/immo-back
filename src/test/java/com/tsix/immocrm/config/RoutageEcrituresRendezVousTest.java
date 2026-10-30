package com.tsix.immocrm.config;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureTaches;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.RACINE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.TIER_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_RENDEZ_VOUS;
import static com.tsix.immocrm.fixtures.FixtureTaches.SEMIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 12 de FEATURE-admin-rendez-vous.md : routage réel ({@link DataSourceConfig}) sur deux pools de la base de test,
 * aucune instruction d'une transaction d'écriture sur la copie, chaque écriture relue. {@code AdmTask} est déjà déclarée
 * par {@code EcrituresTaches} : ce test n'attend aucune table nouvelle.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresRendezVousTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesRendezVous {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieRendezVous(Environment env) {
            return pool(env, "rendez-vous-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceRendezVous(Environment env) {
            return pool(env, "rendez-vous-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieRendezVous(env), LECTURE),
                    new Journalisee(poolSourceRendezVous(env), ECRITURE), p);
        }

        private static HikariDataSource pool(Environment env, String nom) {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(env.getRequiredProperty("spring.datasource.url"));
            ds.setUsername(env.getRequiredProperty("spring.datasource.username"));
            ds.setPassword(env.getRequiredProperty("spring.datasource.password"));
            ds.setMaximumPoolSize(4);
            ds.setPoolName(nom);
            return ds;
        }
    }

    /** Note, pour chaque instruction préparée, la cible qui la reçoit et le mode de la transaction en cours. */
    static final class Journalisee extends DelegatingDataSource {

        private final String nom;

        Journalisee(DataSource cible, String nom) {
            super(cible);
            this.nom = nom;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection c = super.getConnection();
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, methode, args) -> {
                        if (methode.getName().startsWith("prepare") || "createStatement".equals(methode.getName())) {
                            JOURNAL.add(nom + (TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                                    ? " / lecture seule" : EN_ECRITURE));
                        }
                        try {
                            return methode.invoke(c, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixturePatrimoine patrimoine;

    private final String jeton = FixtureReferentielsAdmin.jeton();
    private final List<UUID> lignes = new ArrayList<>();
    private AdmUser admin;
    private UUID tier;

    @BeforeEach
    void setUp() {
        FixtureTachesAdmin.TABLES_REFERENTIELS.forEach(jdbc::execute);
        SEMIS.forEach(jdbc::execute);
        admin = patrimoine.administrateur();
        tier = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgTier (PKey, Code, Description) VALUES (?, ?, ?)", tier.toString(), "TIE-" + jeton, "Tier");
        JOURNAL.clear();
    }

    @AfterEach
    void nettoyer() {
        lignes.forEach(id -> jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id.toString()));
        FixtureTaches.NETTOYAGE.forEach(jdbc::execute);
        jdbc.update("DELETE FROM CfgTier WHERE PKey = ?", tier.toString());
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    private void aucuneEcritureSurLaCopie() {
        assertThat(List.copyOf(JOURNAL)).contains(ECRITURE + EN_ECRITURE).doesNotContain(LECTURE + EN_ECRITURE);
    }

    private int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    private int lignesDeLibelle(UUID id, String description) {
        return compter("SELECT COUNT(*) FROM AdmTask WHERE PKey = ? AND Description = ?", id.toString(), description);
    }

    /** Validé avant la requête : préparation hors du journal observé. */
    private UUID rendezVousValide(String code) {
        UUID id = UUID.randomUUID();
        Timestamp debut = Timestamp.valueOf(JOUR);
        jdbc.update("""
                        INSERT INTO AdmTask (PKey, Code, Description, StrEntityId, CfgProspectId, StartDatePrev, DueDatePrev)
                        VALUES (?, ?, ?, ?, ?, ?, ?)""",
                id.toString(), code, "Libelle " + code, ENTITE_RENDEZ_VOUS.toString(), tier.toString(), debut, debut);
        lignes.add(id);
        JOURNAL.clear();
        return id;
    }

    @Test
    void r12_creation_partSurLaSource_ligneRelue_sequenceConsommee() throws Exception {
        String description = "Creation " + jeton;

        String json = mockMvc.perform(post(RACINE).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, description).avec(TIER_ID, tier)
                                .avec(DATE_PREVUE, "2031-03-10T09:30:00").json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        lignes.add(id);
        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(lignesDeLibelle(id, description)).isEqualTo(1),
                () -> assertThat(compter("SELECT SeqValue FROM AdmSequence WHERE Code = ?", FixtureTaches.SEQUENCE))
                        .isEqualTo(FixtureTaches.VALEUR_SEQUENCE + 1));
    }

    @Test
    void r12_modification_partSurLaSource_ligneRelue() throws Exception {
        UUID id = rendezVousValide("RMO-" + jeton);
        String description = "Route " + jeton;

        mockMvc.perform(patch(FICHE, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, description).json()))
                .andExpect(status().isOk());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(lignesDeLibelle(id, description)).isEqualTo(1));
    }

    @Test
    void r12_suppression_partSurLaSource_ligneAbsente() throws Exception {
        UUID id = rendezVousValide("RSU-" + jeton);

        mockMvc.perform(delete(FICHE, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmTask WHERE PKey = ?", id.toString())).isZero());
    }
}
