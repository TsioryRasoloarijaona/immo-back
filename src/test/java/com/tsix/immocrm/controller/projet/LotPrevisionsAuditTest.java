package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.FIN_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.inchange;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.saisie;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.LIGNE_PERIMEE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.TYPE_STATUT_NOUVEAU;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-lot-previsions.md R10 (une ligne {@code AdmWebHistorics} par PUT) et R8 / réponse du manager 1 (recalcul dans la
 * même transaction : son échec rend 500 et annule tout le PUT). Sans {@code @Transactional} : l'audit n'est écrit qu'à la
 * validation et le rollback ne se voit que hors transaction de test ; tout ce qui est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotPrevisionsAuditTest {

    private static final Logger log = LoggerFactory.getLogger(LotPrevisionsAuditTest.class);
    private static final String TYPE_AUDIT = "Yes.Prj.Market.MarketMonitoring";
    private static final String PAR_LOT = " WHERE PrjMarketId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;

    private final String jeton = FixtureProjetLots.jeton();
    private final List<Runnable> nettoyages = new ArrayList<>();

    private JdbcTemplate jdbc;
    private PrevisionsLotSupport appels;
    private RequestPostProcessor admin;
    private UUID projet;
    private UUID lot;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        appels = new PrevisionsLotSupport(mockMvc);
        UUID genre = GENRES.get(ENTREPRENEURS);
        siAbsent("StrEntity", genre, "INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'PrjMarket', N'Lot Entrepreneurs')");
        siAbsent("StrStatusType", TYPE_STATUT_NOUVEAU,
                "INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (?, N'Nouveau', N'Nouveau', 0)");
        UUID statut = inserer("StrStatus", """
                INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId)
                VALUES (?, ?, ?, 1, ?, ?)""", "PVU-" + jeton, "PVU-" + jeton, genre.toString(), TYPE_STATUT_NOUVEAU.toString());
        projet = inserer("PrjProject", "INSERT INTO PrjProject (PKey, Code, Description) VALUES (?, ?, ?)",
                "PVP-" + jeton, "PVP-" + jeton);
        lot = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO PrjMarket (PKey, Code, Description, PrjProjectId, StrEntityId, StrStatusId, IsMultiSupplier,
                    StartDate, Delay, EndDate, StartDateManual, DelayManual, EndDateManual, PaymentDelay,
                    EstimatedPriceHTManual, EstimatedVatAmountManual, EstimatedPriceTTCManual, DefaultVat,
                    AdvanceRatio, WarrantyRatio, WarrantyInMonths, ProportionRatio, InsuranceDECRatio, InsuranceTRCRatio,
                    BlokWarrantyRestitution)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, 0, 1000, 200, 1200, 0.2, 0, 0.1, 1, 0, 0, 0, 0)""",
                lot.toString(), "PVL-" + jeton, "Lot prévisions " + jeton, projet.toString(), genre.toString(),
                statut.toString(), jour(DEBUT_2026), DUREE_90, jour(FIN_2026), jour(DEBUT_2026), DUREE_90, jour(FIN_2026));
        nettoyages.add(0, this::effacerLot);
        jdbc.update("""
                INSERT INTO PrjMarketPaymentPrevision (PKey, PaymentDate, Amount, Description, PrjMarketId, Achievement)
                VALUES (?, ?, 999, ?, ?, 0)""", UUID.randomUUID().toString(), jour(LocalDate.of(2020, 1, 1)),
                LIGNE_PERIMEE, lot.toString());
        admin = connecte(administrateur());
    }

    private static Date jour(LocalDate date) {
        return Date.valueOf(date);
    }

    private UUID inserer(String table, String sql, Object... autres) {
        UUID id = UUID.randomUUID();
        Object[] params = new Object[autres.length + 1];
        params[0] = id.toString();
        System.arraycopy(autres, 0, params, 1, autres.length);
        jdbc.update(sql, params);
        nettoyages.add(0, () -> effacer("DELETE FROM " + table + " WHERE PKey = ?", id));
        return id;
    }

    private void siAbsent(String table, UUID id, String insertion) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE PKey = ?", Integer.class, id.toString());
        if (n != null && n == 0) {
            jdbc.update(insertion, id.toString());
            nettoyages.add(0, () -> effacer("DELETE FROM " + table + " WHERE PKey = ?", id));
        }
    }

    private AdmUser administrateur() {
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode("lots.pvu." + jeton);
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, 0)",
                compte.getId().toString(), compte.getCode(), "Compte audit prévisions lot");
        nettoyages.add(0, () -> effacer("DELETE FROM AdmUser WHERE PKey = ?", compte.getId()));
        UUID role = inserer("AdmRole", """
                INSERT INTO AdmRole (PKey, Code, Description, WithSequenceFreeRestriction) VALUES (?, N'Administrateur', ?, 0)""",
                "Administrateur audit prévisions " + jeton);
        inserer("AdmUserRole", "INSERT INTO AdmUserRole (PKey, AdmUserId, AdmRoleId) VALUES (?, ?, ?)",
                compte.getId().toString(), role.toString());
        inserer("AdmUserHabilitation", "INSERT INTO AdmUserHabilitation (PKey, AdmUserId, PrjProjectId) VALUES (?, ?, ?)",
                compte.getId().toString(), projet.toString());
        return compte;
    }

    // enfants (FK de la CI sur PrjMarketId) avant le lot
    private void effacerLot() {
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", lot, lot);
        effacer("DELETE FROM StrHistory WHERE ObjectId = ?", lot);
        effacer("DELETE FROM PrjMarketPaymentPrevision" + PAR_LOT, lot);
        effacer("DELETE FROM PrjMarketPrevision" + PAR_LOT, lot);
        effacer("DELETE FROM PrjMarketUserPrevision" + PAR_LOT, lot);
        effacer("DELETE FROM PrjMarket WHERE PKey = ?", lot);
    }

    @AfterEach
    void nettoyer() {
        nettoyages.forEach(Runnable::run);
    }

    /** Nettoyage au mieux : une table annexe (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        Object[] textes = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            textes[i] = parametres[i].toString();
        }
        try {
            jdbc.update(sql, textes);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private int compter(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class, lot.toString());
        return n == null ? 0 : n;
    }

    private int audits() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND ObjectType = ?",
                Integer.class, lot.toString(), TYPE_AUDIT);
        return n == null ? 0 : n;
    }

    @Test
    void r10_put_uneLigneDAuditDuLot() throws Exception {
        Reponse r = appels.enregistrer(admin, lot, inchange());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(audits()).isEqualTo(1);
    }

    /**
     * Réponse du manager 1 : le calcul échoue (garantie de 120 000 mois → remboursement de RG daté de l'an 12026, hors des
     * bornes d'une date SQL Server) → 500, et rien du PUT n'est validé : planning, points, ancien calcul, audit.
     */
    @Test
    void r8_echecDuCalcul_500_toutLePutAnnule() throws Exception {
        jdbc.update("UPDATE PrjMarket SET WarrantyInMonths = 120000 WHERE PKey = ?", lot.toString());

        Reponse r = appels.enregistrer(admin, lot, saisie(LocalDate.of(2026, 2, 1), 60, DEBUT_2026, DUREE_90, "1000",
                List.of(PrevisionsLotSupport.point(LocalDate.of(2026, 2, 1), "0"),
                        PrevisionsLotSupport.point(LocalDate.of(2026, 4, 2), "1")),
                List.of()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(500);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jdbc.queryForObject("SELECT CONVERT(date, StartDate) FROM PrjMarket WHERE PKey = ?",
                    LocalDate.class, lot.toString())).as("planning inchangé").isEqualTo(DEBUT_2026);
            s.assertThat(compter("SELECT COUNT(*) FROM PrjMarketPrevision" + PAR_LOT)).as("points non écrits").isZero();
            s.assertThat(jdbc.queryForList("SELECT Description FROM PrjMarketPaymentPrevision" + PAR_LOT, String.class,
                    lot.toString())).as("ancien calcul conservé").containsExactly(LIGNE_PERIMEE);
            s.assertThat(audits()).as("aucun audit").isZero();
        });
    }
}
