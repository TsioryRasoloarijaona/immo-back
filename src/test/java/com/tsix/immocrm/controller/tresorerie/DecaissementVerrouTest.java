package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * FEATURE-n3bis-b1-reglements.md §5 V-a : la création d'un décaissement verrouille la pièce affectée avant d'en lire le
 * reste à régler. Transactions réelles : le test tient la pièce ({@code UPDLOCK}), y affecte 900 sur 1 000 et commite ;
 * le POST concurrent doit avoir attendu, puis juger sur le reste à jour (100).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DecaissementVerrouTest {

    private static final Logger log = LoggerFactory.getLogger(DecaissementVerrouTest.class);

    private static final String DEVISE = "DefaultCurrency";
    private static final String TENIR_LA_PIECE = "SELECT PKey FROM BuyDocument WITH (UPDLOCK, ROWLOCK) WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private List<Map<String, Object>> deviseSauvee;
    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private AdmUser compte;
    private RequestPostProcessor qui;
    private UUID pa;
    private boolean attendu;

    /** Issue du POST concurrent : a-t-il attendu le verrou, et son statut HTTP. */
    private record Issue(boolean aAttendu, int statut, String corps) {
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        appels = new AppelsSaisieDecaissement(mockMvc);
        deviseSauvee = jdbc.queryForList("SELECT CodeValue, PKey FROM AdmParamsGuid WHERE Code = ?", DEVISE);
        transactions.executeWithoutResult(statut -> {
            s = fixture.saisie("DVA");
            compte = fixture.compte(s, "DVA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU);
            pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
        });
        qui = connecte(compte, FixtureTresorerie.ROLE);
    }

    @AfterEach
    void nettoyer() {
        if (s == null) {
            restaurerDevise();
            return;
        }
        String societe = s.c().societe().toString();
        String reglements = "SELECT PKey FROM StlSettlement WHERE CfgCompanyId = ?";
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (" + reglements + ") OR ParentObjectId IN ("
                + reglements + ")", societe, societe);
        effacer("DELETE FROM StlSettlementHistory WHERE StlSettlementId IN (" + reglements + ")", societe);
        effacer("DELETE FROM StlSettlementLine WHERE StlSettlementId IN (" + reglements + ")", societe);
        effacer("DELETE FROM StlSettlement WHERE CfgCompanyId = ?", societe);
        String piece = pa.toString();
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", piece, piece);
        for (String enfant : List.of("BuyDocumentStatusHistory", "BuyVentilationCategory", "BuyDocumentLine")) {
            effacer("DELETE FROM " + enfant + " WHERE BuyDocumentId = ?", piece);
        }
        effacer("DELETE FROM BuyDocument WHERE PKey = ?", piece);
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", compte.getId().toString());
        restaurerDevise();
    }

    private void restaurerDevise() {
        effacer("DELETE FROM AdmParamsGuid WHERE Code = ?", DEVISE);
        for (Map<String, Object> ligne : deviseSauvee) {
            effacer("INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, ?)", DEVISE,
                    ligne.get("CodeValue"), ligne.get("PKey"));
        }
    }

    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    /**
     * Le test tient la pièce et lance le POST ; s'il n'est pas revenu en 3 s, il attend le verrou. Toujours sous verrou, le
     * test affecte 900 à la pièce et commite ; on lit ensuite l'issue du POST.
     */
    private Issue posterPendantQueLaPieceEstTenue(String montant) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Reponse> post = transactions.execute(statut -> {
                jdbc.queryForList(TENIR_LA_PIECE, pa.toString());
                Future<Reponse> f = pool.submit(() -> appels.creer(qui,
                        corps(s, s.code("-R" + montant), montant, List.of(ligne(pa, montant)))));
                attendre3s(f);
                UUID r = tresorerie.reglement(Reglement.debit(s.code("-R900"), s.c(), "900"));
                tresorerie.ligne(r, pa, "900");
                return f;
            });
            boolean aAttendu = attendu;
            Reponse reponse = post.get(60, TimeUnit.SECONDS);
            return new Issue(aAttendu, reponse.statut(), reponse.corps());
        } finally {
            pool.shutdownNow();
        }
    }

    private void attendre3s(Future<Reponse> f) {
        try {
            f.get(3, TimeUnit.SECONDS);
            attendu = false;
        } catch (TimeoutException e) {
            attendu = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private BigDecimal affecteSurLaPiece() {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(l.Amount), 0) FROM StlSettlementLine l
                JOIN StlSettlement r ON r.PKey = l.StlSettlementId
                WHERE l.BuyDocumentId = ? AND (r.Status IS NULL OR r.Status <> ?)""", BigDecimal.class,
                pa.toString(), FixtureTresorerie.ANNULE);
    }

    // V-a : 150 demandés alors qu'un règlement concurrent laisse 100 → le POST attend, puis 400 ; la pièce reste ≤ 1 000.
    @Test
    void vA_postConcurrent_attendLeVerrou_puisRefuseLeDepassement() throws Exception {
        Issue issue = posterPendantQueLaPieceEstTenue("150");

        assertAll(
                () -> assertThat(issue.aAttendu()).as("le POST a attendu le verrou de la pièce").isTrue(),
                () -> assertThat(issue.statut()).as(issue.corps()).isEqualTo(400),
                () -> assertThat(affecteSurLaPiece()).as("Σ affecté sur la pièce").isEqualByComparingTo("900"));
    }

    // V-a témoin : 100 demandés = le reste exact après le règlement concurrent → le POST attend, puis 201.
    @Test
    void vA_temoin_postConcurrentDansLeReste_attendPuisCree() throws Exception {
        Issue issue = posterPendantQueLaPieceEstTenue("100");

        assertAll(
                () -> assertThat(issue.aAttendu()).as("le POST a attendu le verrou de la pièce").isTrue(),
                () -> assertThat(issue.statut()).as(issue.corps()).isEqualTo(201),
                () -> assertThat(affecteSurLaPiece()).as("Σ affecté sur la pièce").isEqualByComparingTo("1000"));
    }
}
