package com.tsix.immocrm.config;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin.TableTache;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.TABLE_PRIORITES;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.TABLE_TYPES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 7 de FEATURE-admin-type-priorite-tache.md : routage réel ({@link DataSourceConfig}) sur deux pools de la base
 * de test. Aucune instruction d'une transaction d'écriture sur la copie, chaque écriture relue, tables déclarées.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresTachesAdminTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesTaches {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieTaches(Environment env) {
            return pool(env, "taches-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceTaches(Environment env) {
            return pool(env, "taches-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieTaches(env), LECTURE),
                    new Journalisee(poolSourceTaches(env), ECRITURE), p);
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

    private final String jeton = jeton();
    private final List<UUID> lignes = new ArrayList<>();
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        FixtureTachesAdmin.TABLES_REFERENTIELS.forEach(jdbc::execute);
        admin = patrimoine.administrateur();
        JOURNAL.clear();
    }

    @AfterEach
    void nettoyer() {
        lignes.forEach(id -> {
            jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id.toString());
            tables().forEach(t -> jdbc.update("DELETE FROM " + t.table() + " WHERE PKey = ?", id.toString()));
        });
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    static Stream<TableTache> tables() {
        return FixtureTachesAdmin.tables();
    }

    private void aucuneEcritureSurLaCopie() {
        assertThat(List.copyOf(JOURNAL)).contains(ECRITURE + EN_ECRITURE).doesNotContain(LECTURE + EN_ECRITURE);
    }

    private int lignes(TableTache t, UUID id, String description) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + t.table() + " WHERE PKey = ? AND Description = ?",
                Integer.class, id.toString(), description);
        return n == null ? 0 : n;
    }

    /** Validée avant la requête : préparation hors du journal observé. */
    private UUID ligneValidee(TableTache t, String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO " + t.table() + (t.avecTri() ? " (PKey, Sorting, Code, Description) VALUES (?, 1, ?, ?)"
                : " (PKey, Code, Description) VALUES (?, ?, ?)"), id.toString(), code, "Libelle " + code);
        lignes.add(id);
        JOURNAL.clear();
        return id;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r7_creation_partSurLaSource_ligneRelue(TableTache t) throws Exception {
        String code = "RCR-" + jeton;

        String json = mockMvc.perform(post(TABLE, t.slug()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(t.saisie(code).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        lignes.add(id);
        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(lignes(t, id, "Libelle " + code)).isEqualTo(1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r7_modification_partSurLaSource_ligneRelue(TableTache t) throws Exception {
        UUID id = ligneValidee(t, "RMO-" + jeton);

        mockMvc.perform(patch(LIGNE, t.slug(), id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Route " + jeton).json()))
                .andExpect(status().isOk());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(lignes(t, id, "Route " + jeton)).isEqualTo(1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r7_suppression_partSurLaSource_ligneAbsente(TableTache t) throws Exception {
        String code = "RSU-" + jeton;
        UUID id = ligneValidee(t, code);

        mockMvc.perform(delete(LIGNE, t.slug(), id).with(connecte(admin))).andExpect(status().isNoContent());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(lignes(t, id, "Libelle " + code)).isZero());
    }

    /** Sans déclaration, la ligne écrite sur la source ne remonte jamais vers la copie. */
    @Test
    void r7_tablesDeclareesPourLeRoutage() {
        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).contains(TABLE_TYPES, TABLE_PRIORITES);
    }
}
