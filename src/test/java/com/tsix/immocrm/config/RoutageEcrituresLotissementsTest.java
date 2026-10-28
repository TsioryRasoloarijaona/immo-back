package com.tsix.immocrm.config;

import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_DEFI;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TYPE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.VOCATION;
import static com.tsix.immocrm.fixtures.FixtureLotissements.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENTS;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TYPE_IMPORT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.corpsLot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écritures d'un lot en mode deux bases — FEATURE-patrimoine-lotissements.md « Piège readOnly » et réponse 5 du manager
 * (27/10) : avec open-in-view, un appel {@code readOnly} avant l'écriture fixe la connexion de la copie pour toute la
 * requête. Routage réel ({@link DataSourceConfig}) sur deux pools de la base de test ; aucune instruction d'une
 * transaction d'écriture ne doit passer par la copie, et chaque écriture se relit ensuite en base.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresLotissementsTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final String EN_TETE_DESCRIPTION = "Description";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesLotissements {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieLotissements(Environment env) {
            return pool(env, "lotissements-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceLotissements(Environment env) {
            return pool(env, "lotissements-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieLotissements(env), LECTURE),
                    new Journalisee(poolSourceLotissements(env), ECRITURE), p);
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
    @Autowired private FixtureLotissements fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CR");
        JOURNAL.clear();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    private void aucuneEcritureSurLaCopie() {
        assertThat(List.copyOf(JOURNAL)).contains(ECRITURE + EN_ECRITURE).doesNotContain(LECTURE + EN_ECRITURE);
    }

    @Test
    void creation_partSurLaSource_lotRelu() throws Exception {
        String code = j.code("RCR");

        mockMvc.perform(post(LOTISSEMENTS).with(connecte(j.admin()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpsLot(j, code).json()))
                .andExpect(status().isCreated());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(fixture.ids(j.tranche(), code)).hasSize(1));
    }

    @Test
    void modification_partSurLaSource_descriptionRelue() throws Exception {
        UUID lot = fixture.lot(j, j.code("RMO"));
        JOURNAL.clear();

        mockMvc.perform(patch(LOTISSEMENT, lot).with(connecte(j.admin())).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Lot route " + j.jeton()).json()))
                .andExpect(status().isOk());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(fixture.ligne(lot)).containsEntry("Description", "Lot route " + j.jeton()));
    }

    @Test
    void suppression_partSurLaSource_lotAbsent() throws Exception {
        UUID lot = fixture.lot(j, j.code("RSU"));
        JOURNAL.clear();

        mockMvc.perform(delete(LOTISSEMENT, lot).with(connecte(j.admin()))).andExpect(status().isNoContent());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(fixture.existe(lot)).isFalse());
    }

    /** Le chemin d'import appelle exigerVisible (tranche) avant transaction.execute. */
    @Test
    void importReel_partSurLaSource_lotRelu() throws Exception {
        String code = j.code("RIM");
        byte[] classeur = fichier(List.of(colonnes(PIVOT, code, CODE, code,
                EN_TETE_DESCRIPTION, FixtureLotissements.description(code),
                VOCATION, j.codeVocation(), CATEGORIE, j.ter().codeCategorie(), TYPE, j.ter().codeType(),
                SURFACE_PROV, new BigDecimal("1000"), SURFACE_DEFI, new BigDecimal("1000"))));

        mockMvc.perform(requete(TYPE_IMPORT, j.tranche(), false, classeur, connecte(j.admin())))
                .andExpect(status().isOk());

        assertAll(
                this::aucuneEcritureSurLaCopie,
                () -> assertThat(fixture.ids(j.tranche(), code)).hasSize(1));
    }
}
