package com.tsix.immocrm.config;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers.ListeTiers;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_PAYS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.PAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Décisions 3 et 4 de FEATURE-admin-referentiels-tiers.md : tables écrites déclarées (parent avant enfant), routage réel
 * ({@link DataSourceConfig}) sur deux pools de la base de test sans écriture sur la copie, audit {@code AdmWebHistorics}
 * produit par le moteur. Sans {@code @Transactional} : lignes validées, effacées après chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresReferentielsTiersTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();
    /** Les 15 tables écrites (Types de tiers en lecture seule). */
    private static final List<String> TABLES = FixtureReferentielsTiers.modifiables().map(ListeTiers::table).toList();

    @TestConfiguration
    static class DeuxBasesReferentielsTiers {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieReferentielsTiers(Environment env) {
            return pool(env, "referentiels-tiers-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceReferentielsTiers(Environment env) {
            return pool(env, "referentiels-tiers-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieReferentielsTiers(env), LECTURE),
                    new Journalisee(poolSourceReferentielsTiers(env), ECRITURE), p);
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
    private final List<UUID> pays = new ArrayList<>();
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        JOURNAL.clear();
    }

    @AfterEach
    void nettoyer() {
        pays.forEach(id -> {
            jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id.toString());
            jdbc.update("DELETE FROM AdmCountry WHERE PKey = ?", id.toString());
        });
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

    /** Validé avant la requête : préparation hors du journal observé. */
    private UUID paysValide(String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmCountry (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code,
                "Libelle " + code);
        pays.add(id);
        JOURNAL.clear();
        return id;
    }

    private UUID creer(String code) throws Exception {
        String json = mockMvc.perform(post(TABLE, PAYS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(L_PAYS.saisie(code).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        pays.add(id);
        return id;
    }

    private int paysDeCode(String code) {
        return compter("SELECT COUNT(*) FROM AdmCountry WHERE Code = ?", code);
    }

    // ------------------------------------------------------------------ décision 4 : tables déclarées

    /** Sans déclaration, la ligne écrite sur la source ne remonte jamais vers la copie. */
    @Test
    void decision4_tablesEcritesDeclarees() {
        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).containsAll(TABLES);
    }

    /** Parent avant enfant : {@code AdmCity → AdmCountry}, {@code AdmTierTitle → AdmSexType}. */
    @Test
    void decision4_parentsAvantEnfants() {
        assertAll(
                () -> assertThat(CollecteurLignesEcrites.rang("AdmCity"))
                        .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                        .isGreaterThan(CollecteurLignesEcrites.rang("AdmCountry")),
                () -> assertThat(CollecteurLignesEcrites.rang("AdmTierTitle"))
                        .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                        .isGreaterThan(CollecteurLignesEcrites.rang("AdmSexType")));
    }

    // ------------------------------------------------------------------ routage réel

    @Test
    void decision4_creation_partSurLaSource_ligneRelue() throws Exception {
        String code = "RCR-" + jeton;

        creer(code);

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(paysDeCode(code)).isEqualTo(1));
    }

    @Test
    void decision4_modification_partSurLaSource_ligneRelue() throws Exception {
        UUID id = paysValide("RMO-" + jeton);
        String description = "Route " + jeton;

        mockMvc.perform(patch(LIGNE, PAYS, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, description).json()))
                .andExpect(status().isOk());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmCountry WHERE PKey = ? AND Description = ?",
                        id.toString(), description)).isEqualTo(1));
    }

    @Test
    void decision4_suppression_partSurLaSource_ligneAbsente() throws Exception {
        UUID id = paysValide("RSU-" + jeton);

        mockMvc.perform(delete(LIGNE, PAYS, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmCountry WHERE PKey = ?", id.toString())).isZero());
    }

    // ------------------------------------------------------------------ décision 3 : audit par le moteur

    @Test
    void decision3_creationTracee_dansLAudit() throws Exception {
        UUID id = creer("RAU-" + jeton);

        assertThat(compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ?", id.toString())).isPositive();
    }
}
