package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.AVENANTS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.RECEPTIONS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.avenant;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.echeance;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ligne;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.reception;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;

/**
 * R17 (FEATURE-lot-avenants-avances.md) : une ligne d'historique Arpent ({@code AdmWebHistorics}, {@code ObjectId} = le lot) par
 * enregistrement validé — avenant, grille des échéances, réception. Écrit au commit : sans {@code @Transactional}, tout ce qui
 * est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotAvenantsHistoriqueCommitTest {

    private static final Logger log = LoggerFactory.getLogger(LotAvenantsHistoriqueCommitTest.class);
    private static final String PAR_PKEY = " WHERE PKey = ?";
    private static final String HISTORIQUE_DU_LOT = "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ?";
    private static final String RECEPTIONS_DU_LOT = "(SELECT PKey FROM PrjMarketReception WHERE PrjMarketId = ?)";
    private static final LocalDate MARS = LocalDate.of(2026, 3, 5);

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;

    private final String jeton = FixtureProjetLots.jeton();
    private final List<Runnable> nettoyages = new ArrayList<>();

    private JdbcTemplate jdbc;
    private AvenantsLotSupport appels;
    private RequestPostProcessor admin;
    private UUID projet;
    private UUID lot;
    private UUID categorie;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        appels = new AvenantsLotSupport(mockMvc);
        jdbc.execute(FixtureLotAvenants.TABLES);
        siAbsent("AdmSequence", FixtureLotAvenants.SEQUENCE_AVENANT_ID, """
                INSERT INTO AdmSequence (PKey, Code, SeqValue, SeqYearNumber, SeqValueNumber, Separator, BasedOnSystemYear)
                VALUES (?, N'Avenant', 0, 2, 5, N'/', 1)""");
        UUID genre = GENRES.get(ENTREPRENEURS);
        siAbsent("StrEntity", genre, "INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'PrjMarket', N'Lot Entrepreneurs')");
        UUID statut = inserer("StrStatus", """
                INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId) VALUES (?, ?, ?, 1, ?)""",
                "AHS-" + jeton, "AHS-" + jeton, genre.toString());
        projet = inserer("PrjProject", "INSERT INTO PrjProject (PKey, Code, Description) VALUES (?, ?, ?)", "AHP-" + jeton, "AHP-" + jeton);
        categorie = inserer("StlCategory", "INSERT INTO StlCategory (PKey, Code, Description, IsCredit) VALUES (?, ?, ?, 0)",
                "AHC-" + jeton, "AHC-" + jeton);
        lot = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO PrjMarket (PKey, Code, Description, PrjProjectId, StrEntityId, StrStatusId, WarrantyRatio,
                    EstimatedPriceHTManual, EstimatedVatAmountManual, EstimatedPriceTTCManual, IsMultiSupplier)
                VALUES (?, ?, ?, ?, ?, ?, 0.1, 0, 0, 0, 0)""",
                lot.toString(), "AHL-" + jeton, "Lot avenants " + jeton, projet.toString(), genre.toString(), statut.toString());
        admin = connecte(administrateur());
    }

    private UUID inserer(String table, String sql, Object... autres) {
        UUID id = UUID.randomUUID();
        Object[] params = new Object[autres.length + 1];
        params[0] = id.toString();
        System.arraycopy(autres, 0, params, 1, autres.length);
        jdbc.update(sql, params);
        nettoyages.add(0, () -> effacer("DELETE FROM " + table + PAR_PKEY, id));
        return id;
    }

    private void siAbsent(String table, UUID id, String insertion) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + PAR_PKEY, Integer.class, id.toString());
        if (n != null && n == 0) {
            jdbc.update(insertion, id.toString());
            nettoyages.add(0, () -> effacer("DELETE FROM " + table + PAR_PKEY, id));
        }
    }

    private AdmUser administrateur() {
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode("lots.ah." + jeton);
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, 0)",
                compte.getId().toString(), compte.getCode(), "Compte avenants");
        nettoyages.add(0, () -> effacer("DELETE FROM AdmUser WHERE PKey = ?", compte.getId()));
        UUID role = inserer("AdmRole", """
                INSERT INTO AdmRole (PKey, Code, Description, WithSequenceFreeRestriction) VALUES (?, N'Administrateur', ?, 0)""",
                "Administrateur avenants " + jeton);
        inserer("AdmUserRole", "INSERT INTO AdmUserRole (PKey, AdmUserId, AdmRoleId) VALUES (?, ?, ?)",
                compte.getId().toString(), role.toString());
        inserer("AdmUserHabilitation", "INSERT INTO AdmUserHabilitation (PKey, AdmUserId, PrjProjectId) VALUES (?, ?, ?)",
                compte.getId().toString(), projet.toString());
        return compte;
    }

    @AfterEach
    void nettoyer() {
        String l = lot.toString();
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", l, l);
        effacer("DELETE FROM PrjClauseLine WHERE PrjClauseId IN (SELECT PKey FROM PrjClause WHERE PrjMarketId = ?)", l);
        effacer("DELETE FROM PrjClause WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketWarrantyLine WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketReceptionReserve WHERE PrjMarketReceptionId IN " + RECEPTIONS_DU_LOT, l);
        effacer("DELETE FROM PrjMarketReceptionCfgTier WHERE PrjMarketReceptionId IN " + RECEPTIONS_DU_LOT, l);
        effacer("DELETE FROM PrjMarketReception WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketPaymentPrevision WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM StrHistory WHERE ObjectId = ?", l);
        effacer("DELETE FROM PrjMarket WHERE PKey = ?", l);
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

    private int historique() {
        Integer n = jdbc.queryForObject(HISTORIQUE_DU_LOT, Integer.class, lot.toString());
        return n == null ? 0 : n;
    }

    @Test
    void r17_creationDAvenant_ligneDHistoriqueDuLot() throws Exception {
        Reponse r = appels.creer(admin, lot, AVENANTS, avenant(MARS, List.of(ligne(UUID.randomUUID(), 1, "1", "2", "100", "20", "0", categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(historique()).isPositive();
        });
    }

    @Test
    void r17_enregistrementDesEcheances_ligneDHistoriqueDuLot() throws Exception {
        Reponse r = appels.enregistrerEcheances(admin, lot, List.of(echeance(UUID.randomUUID(), 1, "Échéance 1", 30, null, "100")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(historique()).isPositive();
        });
    }

    @Test
    void r17_creationDeReception_ligneDHistoriqueDuLot() throws Exception {
        UUID type = inserer("PrjMarketReceptionType", "INSERT INTO PrjMarketReceptionType (PKey, sorting, Code, Description) VALUES (?, 1, ?, ?)",
                "AHT-" + jeton, "Type AHT-" + jeton);
        UUID genre = inserer("PrjMarketReceptionKind",
                "INSERT INTO PrjMarketReceptionKind (PKey, sorting, Code, Description, IsFinal) VALUES (?, 1, ?, ?, 1)",
                "AHK-" + jeton, "Genre AHK-" + jeton);

        Reponse r = appels.creer(admin, lot, RECEPTIONS, reception(1, "R1", MARS, type, genre, List.of(), List.of()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(historique()).isPositive();
        });
    }
}
