package com.tsix.immocrm.config;

import com.tsix.immocrm.web.LiaisonSource;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FEATURE-stabilite-connexion-source.md §1 : pool d'écriture construit sans base joignable (Hikari démarre paresseux). */
class ReglagesPoolSourceTest {

    private static SourceDeVeriteProperties proprietes() {
        SourceDeVeriteProperties p = new SourceDeVeriteProperties();
        p.setHost("197.0.2.10");
        p.setPort(1433);
        p.setNom("yesRems");
        p.setUsername("crm");
        p.setPassword("MotDePasse-Secret!42");
        p.setTimeoutConnexionMs(4200);
        p.setTaillePool(4);
        p.setKeepaliveMs(45_000);
        p.setDureeVieMaxMs(500_000);
        p.setValidationTimeoutMs(2_500);
        p.setTentativesReconnexion(2);
        p.setIntervalleReconnexionS(7);
        p.setTimeoutSocketMs(120_000);
        return p;
    }

    @Test
    void poolEcriture_appliqueLesReglages() {
        try (HikariDataSource ds = (HikariDataSource) new DataSourceConfig().ecriture(proprietes())) {
            assertThat(ds.getKeepaliveTime()).isEqualTo(45_000);
            assertThat(ds.getMaxLifetime()).isEqualTo(500_000);
            assertThat(ds.getValidationTimeout()).isEqualTo(2_500);
            assertThat(ds.getConnectionTimeout()).isEqualTo(4_200);
            assertThat(ds.getMaximumPoolSize()).isEqualTo(4);
            assertThat(ds.getMinimumIdle()).as("R2 : minimumIdle = taille du pool").isEqualTo(4);
            assertThat(ds.getPoolName()).isEqualTo("hikari-source-de-verite");
            assertThat(ds.getInitializationFailTimeout()).as("pas d'échec rapide : cas A reconnaissable").isEqualTo(-1);
            assertThat(ds.getJdbcUrl()).isEqualTo(proprietes().urlJdbc());
        }
    }

    @Test
    void url_porteLesParametresDeLiaison_jamaisLeMotDePasse() {
        String url = proprietes().urlJdbc();

        assertThat(url).startsWith("jdbc:sqlserver://197.0.2.10:1433;databaseName=yesRems;")
                .contains(";encrypt=true;trustServerCertificate=true")
                .contains(";connectRetryCount=2")
                .contains(";connectRetryInterval=7")
                .contains(";loginTimeout=5")
                .contains(";socketTimeout=120000")
                .doesNotContain("MotDePasse-Secret!42")
                .doesNotContainIgnoringCase("password");
        assertThat(proprietes().description()).isEqualTo("197.0.2.10:1433/yesRems");
    }

    @Test
    void url_socketTimeoutZero_absent() {
        SourceDeVeriteProperties p = proprietes();
        p.setTimeoutSocketMs(0);

        assertThat(p.urlJdbc()).doesNotContain("socketTimeout");
    }

    @Test
    void loginTimeout_arrondiAuSuperieur_minimumUneSeconde() {
        SourceDeVeriteProperties p = proprietes();
        p.setTimeoutConnexionMs(5000);
        assertThat(p.urlJdbc()).contains(";loginTimeout=5;");
        p.setTimeoutConnexionMs(5001);
        assertThat(p.urlJdbc()).contains(";loginTimeout=6;");
        p.setTimeoutConnexionMs(250);
        assertThat(p.urlJdbc()).contains(";loginTimeout=1;");
    }

    @Test
    void defauts_duSpec() {
        SourceDeVeriteProperties p = new SourceDeVeriteProperties();

        assertThat(p.getKeepaliveMs()).isEqualTo(60_000);
        assertThat(p.getDureeVieMaxMs()).isEqualTo(600_000);
        assertThat(p.getValidationTimeoutMs()).isEqualTo(3_000);
        assertThat(p.getTentativesReconnexion()).isEqualTo(3);
        assertThat(p.getIntervalleReconnexionS()).isEqualTo(5);
        assertThat(p.getTimeoutSocketMs()).isEqualTo(300_000);
        p.verifierReglagesPool();
    }

    @Test
    void r1_keepaliveSuperieurOuEgalADureeDeVie_refuse() {
        SourceDeVeriteProperties p = proprietes();
        p.setKeepaliveMs(500_000);

        assertThatThrownBy(() -> new DataSourceConfig().ecriture(p))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SOURCE_DB_KEEPALIVE_MS")
                .hasMessageContaining("SOURCE_DB_MAX_LIFETIME_MS");
    }

    @Test
    void r1_keepaliveSousLePlancherHikari_refuse() {
        SourceDeVeriteProperties p = proprietes();
        p.setKeepaliveMs(29_999);

        assertThatThrownBy(() -> new DataSourceConfig().ecriture(p))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SOURCE_DB_KEEPALIVE_MS")
                .hasMessageContaining("30000");
    }

    /** Vrai pool, port fermé : l'échec doit être celui que le gestionnaire classe en cas A. */
    @Test
    void sourceInjoignable_poolRendSQLTransientConnectionException_classeeCasA() {
        SourceDeVeriteProperties p = proprietes();
        p.setHost("127.0.0.1");
        p.setPort(1);
        p.setTimeoutConnexionMs(1500);
        try (HikariDataSource ds = (HikariDataSource) new DataSourceConfig().ecriture(p)) {
            long debut = System.nanoTime();
            Throwable erreur = org.assertj.core.api.Assertions.catchThrowable(
                    () -> new JdbcTemplate(ds).queryForObject("SELECT 1", Integer.class));
            long dureeMs = (System.nanoTime() - debut) / 1_000_000;

            assertThat(erreur).hasCauseInstanceOf(java.sql.SQLTransientConnectionException.class);
            assertThat(LiaisonSource.classer(erreur, false)).isEqualTo(LiaisonSource.SOURCE_INJOIGNABLE);
            assertThat(dureeMs).as("≈ timeoutConnexionMs").isBetween(1_000L, 10_000L);
        }
    }
}
