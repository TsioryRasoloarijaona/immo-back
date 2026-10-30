package com.tsix.immocrm.config;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureRoles;
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
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODES;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureRoles.DROITS;
import static com.tsix.immocrm.fixtures.FixtureRoles.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRoles.RACINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 15 de FEATURE-admin-roles.md : routage réel ({@link DataSourceConfig}) sur deux pools de la base de test. Aucune
 * instruction d'une transaction d'écriture sur la copie, chaque écriture relue, tables déclarées parent d'abord.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresRolesTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesRoles {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieRoles(Environment env) {
            return pool(env, "roles-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceRoles(Environment env) {
            return pool(env, "roles-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieRoles(env), LECTURE),
                    new Journalisee(poolSourceRoles(env), ECRITURE), p);
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
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        admin = fixture.compte();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private void aucuneEcritureSurLaCopie() {
        assertThat(List.copyOf(JOURNAL)).contains(ECRITURE + EN_ECRITURE).doesNotContain(LECTURE + EN_ECRITURE);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    @Test
    void r15_creation_partSurLaSource_ligneRelue() throws Exception {
        String code = "RCR-" + jeton;
        JOURNAL.clear();

        mockMvc.perform(json(post(RACINE), corps().avec(CODE, code).json()).with(connecte(admin)))
                .andExpect(status().isCreated());

        assertAll(this::aucuneEcritureSurLaCopie, () -> assertThat(fixture.nombreRoles(code)).isEqualTo(1));
    }

    @Test
    void r15_modification_partSurLaSource_ligneRelue() throws Exception {
        UUID role = fixture.role("RMO-" + jeton);
        JOURNAL.clear();

        mockMvc.perform(json(patch(FICHE, role), corps().avec(DESCRIPTION, "Route " + jeton).json()).with(connecte(admin)))
                .andExpect(status().isOk());

        assertAll(this::aucuneEcritureSurLaCopie,
                () -> assertThat(fixture.ligne(role)).containsEntry("Description", "Route " + jeton));
    }

    @Test
    void r15_droits_partSurLaSource_lignesRelues() throws Exception {
        UUID role = fixture.role("RDR-" + jeton);
        String retire = "RET-" + jeton;
        String ajoute = "AJO-" + jeton;
        fixture.noeud(retire);
        fixture.noeud(ajoute);
        fixture.droit(role, retire);
        JOURNAL.clear();

        mockMvc.perform(json(put(DROITS, role), corps().avec(CODES, List.of(ajoute)).json()).with(connecte(admin)))
                .andExpect(status().isOk());

        assertAll(this::aucuneEcritureSurLaCopie,
                () -> assertThat(fixture.codesDroits(role)).containsExactly(ajoute));
    }

    @Test
    void r15_suppression_partSurLaSource_ligneAbsente() throws Exception {
        UUID role = fixture.role("RSU-" + jeton);
        fixture.droit(role, "HORS-" + jeton);
        JOURNAL.clear();

        mockMvc.perform(delete(FICHE, role).with(connecte(admin))).andExpect(status().isNoContent());

        assertAll(this::aucuneEcritureSurLaCopie, () -> assertThat(fixture.rolesPresents(role)).isZero());
    }

    /** Sans déclaration, la ligne écrite sur la source ne remonte pas vers la copie, ou y arrive avant son parent. */
    @Test
    void r15_tablesDeclarees_roleAvantSesDroits() {
        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).containsSubsequence("AdmRole", "AdmRight");
    }
}
