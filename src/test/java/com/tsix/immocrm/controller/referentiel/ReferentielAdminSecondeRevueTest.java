package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTrancheStatus;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_DOSSIER_VENTE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.OPERATEUR;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RATIO;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.REGLEMENT_AU_COMPTANT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SEUILS_VERSEMENT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TYPE_REGLEMENT_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Seconde revue P6 (FEATURE-patrimoine-referentiels.md §8.2) : l'unicité (tranche, entité, statut) des seuils de
 * versement doit tenir aussi pour deux créations concurrentes, puisque le contrôle de versement lit {@code findFirst()}.
 * Sans {@code @Transactional} : chaque création est validée ; tout ce qui est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReferentielAdminSecondeRevueTest {

    private static final Logger log = LoggerFactory.getLogger(ReferentielAdminSecondeRevueTest.class);
    private static final BigDecimal TRENTE_POUR_CENT = new BigDecimal("0.300000");
    private static final String PAR_STATUT = " WHERE StrStatusId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CfgTrancheStatusRepository seuils;

    private final List<String[]> parentsInseres = new ArrayList<>();
    private JdbcTemplate jdbc;
    private AdmUser admin;
    private UUID statut;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        admin = patrimoine.administrateur();
        parentSiAbsent("StrEntity", ENTITE_DOSSIER_VENTE, "ComFolder");
        parentSiAbsent("ComFolderStatusPaymentType", REGLEMENT_AU_COMPTANT, "Au comptant");
        statut = UUID.randomUUID();
        String code = "TST-SR-" + jeton();
        jdbc.update("INSERT INTO StrStatus (PKey, Code, Description, StrConfigEntityId) VALUES (?, ?, ?, ?)",
                statut.toString(), code, code, ENTITE_DOSSIER_VENTE.toString());
    }

    private void parentSiAbsent(String table, UUID id, String code) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE PKey = ?", Integer.class, id.toString());
        if (n != null && n == 0) {
            jdbc.update("INSERT INTO " + table + " (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, code);
            parentsInseres.add(new String[]{table, id.toString()});
        }
    }

    @AfterEach
    void nettoyer() {
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM CfgTrancheStatus" + PAR_STATUT + ")",
                statut.toString());
        effacer("DELETE FROM CfgTrancheStatus" + PAR_STATUT, statut.toString());
        effacer("DELETE FROM StrStatus WHERE PKey = ?", statut.toString());
        parentsInseres.forEach(p -> effacer("DELETE FROM " + p[0] + " WHERE PKey = ?", p[1]));
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        effacer("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    private void effacer(String sql, String id) {
        try {
            jdbc.update(sql, id);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private CorpsJson seuilGlobal(int operateur) {
        return corps().avec(STATUT_ID, statut).avec(TYPE_REGLEMENT_ID, REGLEMENT_AU_COMPTANT)
                .avec(OPERATEUR, operateur).avec(RATIO, TRENTE_POUR_CENT);
    }

    private int poster(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, SEUILS_VERSEMENT).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json())).andReturn().getResponse().getStatus();
    }

    private int lignesDuStatut() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM CfgTrancheStatus" + PAR_STATUT, Integer.class,
                statut.toString());
        return n != null ? n : -1;
    }

    /** Ce que l'écran écrit est exactement ce que lit {@code ControlesTransition.configuration} (seuil global). */
    @Test
    void seuilEcritParLEcran_luParLeControleDeVersement() throws Exception {
        assertThat(poster(seuilGlobal(2))).isEqualTo(201);

        // global() charge le type de règlement (JOIN FETCH) : lisible hors transaction.
        List<CfgTrancheStatus> lus = new TransactionTemplate(transactionManager).execute(t -> seuils.global(statut));
        assertThat(lus).singleElement().satisfies(s -> {
            assertThat(s.getThresholdOperator()).isEqualTo(2);
            assertThat(s.getThresholdRatio()).isEqualByComparingTo(TRENTE_POUR_CENT);
            assertThat(s.getComFolderStatusPaymentType().getId()).isEqualTo(REGLEMENT_AU_COMPTANT);
            assertThat(s.getStrFolderEntityId()).isEqualTo(ENTITE_DOSSIER_VENTE);
        });
    }

    /**
     * A crée le seuil global du statut sans encore valider ; B crée le même seuil et valide ; A valide. Le COUNT de
     * {@code unicite} ne voit pas l'écriture non validée de A et aucune contrainte n'existe en base : deux lignes.
     */
    @Test
    void deuxCreationsConcurrentes_duMemeSeuilGlobal_uneSeuleLigne() throws Exception {
        CountDownLatch aAEcrit = new CountDownLatch(1);
        CountDownLatch bATermine = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> tx.execute(t -> {
            int statutA = posterSansErreur(seuilGlobal(1));
            aAEcrit.countDown();
            patienter(bATermine);
            return statutA;
        }));
        attendre(aAEcrit);
        int statutB;
        try {
            statutB = poster(seuilGlobal(2));
        } finally {
            bATermine.countDown();
        }
        int statutA = a.get(30, TimeUnit.SECONDS);

        log.info("Création concurrente : A = {}, B = {}", statutA, statutB);
        assertThat(lignesDuStatut()).isEqualTo(1);
    }

    private int posterSansErreur(CorpsJson corps) {
        try {
            return poster(corps);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean patienter(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void attendre(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("délai dépassé");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
