package com.tsix.immocrm.config;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
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
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTES_DEPOT;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTE_IDS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.FICHE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.HABILITATIONS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.INACTIF;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.NOEUDS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.RACINE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.REFERENCES;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLES;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLE_IDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R19 de FEATURE-admin-utilisateurs.md : routage réel ({@link DataSourceConfig}) sur deux pools de la base de test. Aucune instruction
 * d'une transaction d'écriture sur la copie, chaque écriture relue, la fiche relue sur la source, tables déclarées parent d'abord.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresUtilisateursTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesUtilisateurs {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieUtilisateurs(Environment env) {
            return pool(env, "utilisateurs-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceUtilisateurs(Environment env) {
            return pool(env, "utilisateurs-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieUtilisateurs(env), LECTURE),
                    new Journalisee(poolSourceUtilisateurs(env), ECRITURE), p);
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
    @Autowired private FixtureUtilisateurs fixture;

    private String jeton;
    private AdmUser admin;
    private UUID cible;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        admin = fixture.compte();
        cible = fixture.compte().getId();
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

    private void ecrit(MockHttpServletRequestBuilder requete, String corps) throws Exception {
        JOURNAL.clear();
        mockMvc.perform(json(requete, corps).with(connecte(admin))).andExpect(status().is2xxSuccessful());
    }

    @Test
    void r19_creation_partSurLaSource_ligneRelue() throws Exception {
        String code = "Route." + jeton;

        ecrit(post(RACINE), corps().avec(CODE, code).avec(DESCRIPTION, "Route").json());

        assertAll(this::aucuneEcritureSurLaCopie, () -> assertThat(fixture.nombreComptes(code)).isEqualTo(1));
    }

    @Test
    void r19_modification_partSurLaSource_ligneRelue() throws Exception {
        ecrit(patch(FICHE, cible), corps().avec(DESCRIPTION, "Route " + jeton).json());

        assertAll(this::aucuneEcritureSurLaCopie,
                () -> assertThat(fixture.ligne(cible)).containsEntry("Description", "Route " + jeton));
    }

    /** {@code TokenVersion} incrémenté dans la même transaction : il doit atteindre la source. */
    @Test
    void r19_desactivation_partSurLaSource_tokenVersionRelu() throws Exception {
        fixture.credential(cible, 1);

        ecrit(patch(FICHE, cible), corps().avec(INACTIF, true).json());

        assertAll(this::aucuneEcritureSurLaCopie, () -> assertThat(fixture.tokenVersion(cible)).isEqualTo(2));
    }

    @Test
    void r19_roles_partSurLaSource_lignesRelues() throws Exception {
        UUID role = fixture.role("RRO-" + jeton);

        ecrit(put(ROLES, cible), corps().avec(ROLE_IDS, List.of(role)).json());

        assertAll(this::aucuneEcritureSurLaCopie, () -> assertThat(fixture.rolesDe(cible)).containsExactly(role));
    }

    @Test
    void r19_comptesDepot_partSurLaSource_lignesRelues() throws Exception {
        UUID depot = fixture.depot("RDE-" + jeton);

        ecrit(put(COMPTES_DEPOT, cible), corps().avec(COMPTE_IDS, List.of(depot)).json());

        assertAll(this::aucuneEcritureSurLaCopie, () -> assertThat(fixture.depotsDe(cible)).containsExactly(depot));
    }

    @Test
    void r19_habilitations_retrait_partSurLaSource_ligneAbsente() throws Exception {
        UUID projet = fixture.projet("RHA-" + jeton, fixture.societe("RSO-" + jeton, true));
        fixture.habiliter(cible, null, projet, null, null);

        ecrit(put(HABILITATIONS, cible), corps().avec(NOEUDS, List.of()).json());

        assertAll(this::aucuneEcritureSurLaCopie, () -> assertThat(fixture.habilitations(cible)).isEmpty());
    }

    private void litLaSource(String route) throws Exception {
        JOURNAL.clear();

        mockMvc.perform(get(route, cible).with(connecte(admin))).andExpect(status().isOk());

        assertThat(List.copyOf(JOURNAL)).isNotEmpty().allMatch(ligne -> ligne.startsWith(ECRITURE));
    }

    @Test
    void r19_fiche_litLaSource() throws Exception {
        litLaSource(FICHE);
    }

    @Test
    void r19_habilitationsLues_litLaSource() throws Exception {
        litLaSource(HABILITATIONS);
    }

    @Test
    void r19_references_litLaSource() throws Exception {
        litLaSource(REFERENCES);
    }

    /** Sans déclaration, la ligne écrite sur la source ne remonte pas vers la copie, ou y arrive avant son parent. */
    @Test
    void r19_tablesDeclarees_parentsDAbord() {
        assertAll(
                () -> assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).containsSubsequence("AdmUser", "AdmUserRole"),
                () -> assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).containsSubsequence("AdmRole", "AdmUserRole"),
                () -> assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).containsSubsequence("AdmUser", "AdmUserHabilitation"),
                () -> assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).containsSubsequence("AdmUser", "StlDepositUser"));
    }
}
