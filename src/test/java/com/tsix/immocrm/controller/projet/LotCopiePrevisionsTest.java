package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R1, R2 — FEATURE-lc1-creation-lot.md (D2, D3) : la copie d'un lot recopie ses prévisions ({@code PrjMarketPrevision},
 * {@code PrjMarketUserPrevision}) et ses échéances de retenue de garantie ({@code PrjMarketWarrantyLine}, date d'échéance NULL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotCopiePrevisionsTest {

    private static final String PREVISION = "PrjMarketPrevision";
    private static final String PREVISION_UTILISATEUR = "PrjMarketUserPrevision";
    private static final String GARANTIE = "PrjMarketWarrantyLine";
    private static final String PKEY = "PKey";

    // Absentes de yesRemsTest (définitions de yesRemsProd) ; créées dans la transaction du test, annulées avec elle.
    private static final String COLONNES_POINTS = "(PKey uniqueidentifier NOT NULL PRIMARY KEY, PointTime decimal(26, 16) NULL, "
            + "PointDate smalldatetime NULL, PointAchivement decimal(26, 16) NULL, PointPayment decimal(26, 6) NULL, "
            + "PointAmount decimal(26, 6) NULL, PrjMarketId uniqueidentifier NULL)";
    private static final String TABLE_GARANTIE = """
            IF OBJECT_ID('dbo.PrjMarketWarrantyLine', 'U') IS NULL
            CREATE TABLE dbo.PrjMarketWarrantyLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                Description nvarchar(255) NULL, Delay int NULL, Ratio decimal(26, 6) NULL, EcheanceDate datetime NULL,
                PrjMarketId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)""";

    private static final String INSERT_POINT = "INSERT INTO %s (PKey, PointTime, PointDate, PointAchivement, PointPayment, "
            + "PointAmount, PrjMarketId) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_GARANTIE = "INSERT INTO PrjMarketWarrantyLine (PKey, Sorting, Description, Delay, Ratio, "
            + "EcheanceDate, PrjMarketId) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String POINTS_DU_LOT = "SELECT * FROM %s WHERE PrjMarketId = ? ORDER BY PointTime ASC";
    private static final String GARANTIES_DU_LOT = "SELECT * FROM PrjMarketWarrantyLine WHERE PrjMarketId = ? ORDER BY Sorting ASC";

    private static final LocalDate JOUR_1 = LocalDate.of(2026, 4, 15);
    private static final LocalDate JOUR_2 = LocalDate.of(2026, 9, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private LotFicheSupport appels;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        String jeton = FixtureProjetLots.jeton();
        maj(tablePoints(PREVISION));
        maj(tablePoints(PREVISION_UTILISATEUR));
        maj(TABLE_GARANTIE);
        parite.genresDeLot();
        parite.statutsDepart(jeton);
        parite.sequenceMarche(ThreadLocalRandom.current().nextInt(50_000, 90_000));
        UUID projet = parite.projet("CVP-" + jeton, parite.entiteJuridique("CVE-" + jeton));
        UUID statut = parite.statut(ENTREPRENEURS, "CVS-" + jeton);
        lot = parite.lot(projet, "CVL-" + jeton, ENTREPRENEURS, statut);
        admin = connecte(parite.administrateur("lots.cpv." + jeton, projet), "Administrateur");
    }

    // ------------------------------------------------------------------ R1 : prévisions

    @Test
    void r1_copie_recopiePrjMarketPrevision_memesValeurs_nouvellesCles() throws Exception {
        point(PREVISION, "1", JOUR_1, "0.25", "15000", "60000");
        point(PREVISION, "2", JOUR_2, "0.75", "45000", "60000");

        UUID copie = copier();

        verifierPointsRecopies(PREVISION, copie);
    }

    @Test
    void r1_copie_recopiePrjMarketUserPrevision_memesValeurs_nouvellesCles() throws Exception {
        point(PREVISION_UTILISATEUR, "3", JOUR_1, "0.4", "12000.5", "30000.25");
        point(PREVISION_UTILISATEUR, "5", JOUR_2, "1", "18000", "30000.25");

        UUID copie = copier();

        verifierPointsRecopies(PREVISION_UTILISATEUR, copie);
    }

    @Test
    void r1_sourceSansPrevision_copieSansPrevision() throws Exception {
        UUID copie = copier();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(points(PREVISION, copie)).as(PREVISION).isEmpty();
            s.assertThat(points(PREVISION_UTILISATEUR, copie)).as(PREVISION_UTILISATEUR).isEmpty();
            s.assertThat(garanties(copie)).as(GARANTIE).isEmpty();
        });
    }

    @Test
    void r1_r2_sourceIntacte_apresCopie() throws Exception {
        point(PREVISION, "1", JOUR_1, "0.25", "15000", "60000");
        point(PREVISION_UTILISATEUR, "2", JOUR_2, "0.5", "20000", "40000");
        garantie(1, "Réception provisoire", 12, "0.05", JOUR_1);
        List<List<Object>> prevAvant = valeursPoints(points(PREVISION, lot));
        List<List<Object>> utilAvant = valeursPoints(points(PREVISION_UTILISATEUR, lot));
        List<List<Object>> garAvant = valeursGaranties(garanties(lot), true);

        copier();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(valeursPoints(points(PREVISION, lot))).as(PREVISION).isEqualTo(prevAvant);
            s.assertThat(valeursPoints(points(PREVISION_UTILISATEUR, lot))).as(PREVISION_UTILISATEUR).isEqualTo(utilAvant);
            s.assertThat(valeursGaranties(garanties(lot), true)).as(GARANTIE + " (date d'échéance comprise)").isEqualTo(garAvant);
        });
    }

    // ------------------------------------------------------------------ R2 : échéances de retenue de garantie

    @Test
    void r2_copie_recopieEcheancesRG_memesValeurs_nouvellesCles_echeanceNulle() throws Exception {
        garantie(1, "Réception provisoire", 12, "0.05", JOUR_1);
        garantie(2, "Réception définitive", 24, "0.05", JOUR_2);
        List<Map<String, Object>> source = garanties(lot);

        UUID copie = copier();

        List<Map<String, Object>> copies = garanties(copie);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(valeursGaranties(copies, false)).as("Sorting, Description, Delay, Ratio")
                    .isEqualTo(valeursGaranties(source, false));
            s.assertThat(copies).isNotEmpty().allSatisfy(g -> s.assertThat(g.get("EcheanceDate")).as("EcheanceDate").isNull());
            s.assertThat(copies).isNotEmpty().allSatisfy(g -> s.assertThat(guid(g.get(PKEY))).isNotIn(cles(source)));
        });
    }

    // ------------------------------------------------------------------ outils

    private UUID copier() throws Exception {
        Creation c = appels.copier(admin, lot);
        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        return c.id();
    }

    private void verifierPointsRecopies(String table, UUID copie) {
        List<Map<String, Object>> source = points(table, lot);
        List<Map<String, Object>> copies = points(table, copie);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(valeursPoints(copies)).as(table + " : PointTime, PointDate, PointAchivement, PointPayment, PointAmount")
                    .hasSize(2).isEqualTo(valeursPoints(source));
            s.assertThat(copies).isNotEmpty().allSatisfy(p -> s.assertThat(guid(p.get(PKEY))).as("nouvelle clé").isNotIn(cles(source)));
        });
    }

    private void point(String table, String temps, LocalDate jour, String avancement, String paiement, String montant) {
        maj(INSERT_POINT.formatted(table), UUID.randomUUID().toString(), new BigDecimal(temps), jour, new BigDecimal(avancement),
                new BigDecimal(paiement), new BigDecimal(montant), lot.toString());
    }

    private void garantie(int ordre, String description, int delai, String taux, LocalDate echeance) {
        maj(INSERT_GARANTIE, UUID.randomUUID().toString(), ordre, description, delai, new BigDecimal(taux), echeance, lot.toString());
    }

    private List<Map<String, Object>> points(String table, UUID marche) {
        return fiche.lignes(POINTS_DU_LOT.formatted(table), marche.toString());
    }

    private List<Map<String, Object>> garanties(UUID marche) {
        return fiche.lignes(GARANTIES_DU_LOT, marche.toString());
    }

    private static List<List<Object>> valeursPoints(List<Map<String, Object>> lignes) {
        return lignes.stream().map(p -> Arrays.<Object>asList(decimal(p.get("PointTime")), jour(p.get("PointDate")),
                decimal(p.get("PointAchivement")), decimal(p.get("PointPayment")), decimal(p.get("PointAmount")))).toList();
    }

    private static List<List<Object>> valeursGaranties(List<Map<String, Object>> lignes, boolean avecEcheance) {
        return lignes.stream().map(g -> Arrays.<Object>asList(g.get("Sorting"), g.get("Description"), g.get("Delay"),
                decimal(g.get("Ratio")), avecEcheance ? jour(g.get("EcheanceDate")) : null)).toList();
    }

    private static List<String> cles(List<Map<String, Object>> lignes) {
        return lignes.stream().map(l -> guid(l.get(PKEY))).toList();
    }

    private static BigDecimal decimal(Object valeur) {
        BigDecimal n = nombre(valeur);
        return n == null ? null : n.stripTrailingZeros();
    }

    private static LocalDate jour(Object valeur) {
        return valeur == null ? null : ((Timestamp) valeur).toLocalDateTime().toLocalDate();
    }

    private static String tablePoints(String table) {
        return "IF OBJECT_ID('dbo.%s', 'U') IS NULL CREATE TABLE dbo.%s %s".formatted(table, table, COLONNES_POINTS);
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }
}
