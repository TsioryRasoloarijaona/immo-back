package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Revue N4b : décimales au-delà de l'échelle 6 de la colonne (réponse du PUT vs GET, dépassement après arrondi). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsChantierRevueTest {

    private static final String ROUTE = FixtureProjetConfig.PROJET + "/previsions-chantier";
    private static final String SCHEMA = """
            IF OBJECT_ID(N'dbo.GrhWorkedDayType', N'U') IS NULL
                CREATE TABLE dbo.GrhWorkedDayType (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    Description nvarchar(255) NULL, Notes nvarchar(500) NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                    GroupDescription nvarchar(255) NULL, Unity nvarchar(255) NULL, UnitPriceHT decimal(26, 6) NULL,
                    VatRatio decimal(26, 6) NULL, Unity1 nvarchar(255) NULL, UnitPriceHT1 decimal(26, 6) NULL,
                    VatRatio1 decimal(26, 6) NULL);
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'GrhWorkedDayTypeId') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD GrhWorkedDayTypeId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'Quantity') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD Quantity decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UnitPriceHT') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UnitPriceHT decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'VatRatio') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD VatRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'Notes') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD Notes nvarchar(1000) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserCreate') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserCreate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserCreateDateTime') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserCreateDateTime datetime NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserUpdate') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserUpdate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserUpdateDateTime') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserUpdateDateTime datetime NULL;""";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private FixtureProjetConfig config;
    @Autowired private FixtureLotsParite parite;

    private String jeton;
    private UUID projet;
    private UUID type;
    private RequestPostProcessor admin;

    @BeforeAll
    static void schema(@Autowired DataSource dataSource) throws SQLException {
        try (Connection connexion = dataSource.getConnection(); Statement ordre = connexion.createStatement()) {
            connexion.setAutoCommit(true);
            ordre.execute(SCHEMA);
        }
    }

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        config.parametrage();
        UUID societe = parite.entiteJuridique("PCR-" + jeton);
        projet = config.projet("PCR-" + jeton, societe);
        config.liens(projet, config.nivellement("NIVR-" + jeton), null, null);
        admin = connecte(parite.administrateur("pcr.adm." + jeton, projet), "Administrateur");
        type = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO GrhWorkedDayType (PKey, Code, Description, Unity, UnitPriceHT, VatRatio) VALUES (?, ?, ?, ?, ?, ?)",
                type.toString(), "TTR-" + jeton, "Type " + jeton, "H", new BigDecimal("100"), new BigDecimal("0.2"));
        em.clear();
    }

    private Reponse executer(MockHttpServletRequestBuilder requete) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private Reponse enregistrer(String quantite, String tva) throws Exception {
        CorpsJson ligne = corps().avec("typeTravauxId", type).avec("quantite", new BigDecimal(quantite))
                .avec("prixUnitaireHT", new BigDecimal("100")).avec("tva", new BigDecimal(tva)).avec("notes", null);
        return executer(put(ROUTE, projet).with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("lignes", List.of(ligne)).json()));
    }

    private static BigDecimal nombre(Reponse r, String champ) {
        assertThat((List<?>) r.valeur("$.lignes")).as(r.corps()).hasSize(1);
        return new BigDecimal(String.valueOf(r.valeur("$.lignes[0]." + champ)));
    }

    /** Spec, réponse 9 : « Réponse du PUT 200 = même corps que le GET ». Une 7e décimale est arrondie par la colonne. */
    @Test
    void put_decimalesAuDelaDeLEchelle_reponseDuPutEgaleAuGetSuivant() throws Exception {
        Reponse ecrit = enregistrer("1.1234567", "20.1234567");
        assertThat(ecrit.statut()).as(ecrit.corps()).isEqualTo(200);
        em.clear();

        Reponse relu = executer(get(ROUTE, projet).with(admin));

        SoftAssertions s = new SoftAssertions();
        for (String champ : List.of("quantite", "tva", "totalHT", "totalTTC")) {
            s.assertThat(nombre(ecrit, champ)).as(champ).isEqualByComparingTo(nombre(relu, champ));
        }
        s.assertAll();
    }

    /** 20 chiffres entiers et une 7e décimale qui arrondit à 10^20 : dépasse decimal(26,6) après arrondi → 400 attendu. */
    @Test
    void put_valeurQuiDepasseApresArrondi_400() throws Exception {
        Reponse r = enregistrer("99999999999999999999.9999999", "20");

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }
}
