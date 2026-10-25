package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import com.tsix.immocrm.fixtures.NettoyageProspectionsTerrain;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * R5 — FEATURE-patrimoine-import-export.md §4.1 (« une transaction », écart volontaire avec OMRAN §0.3) et §5 : l'écriture
 * de l'alerte du 2e dossier échoue → rollback complet, 0 date et 0 alerte sur tous les dossiers. Sans {@code @Transactional}
 * (une transaction de test masquerait le rollback) : fixtures validées puis effacées. L'échec est forcé sur
 * {@code ComActionMessageRepository} (chemin de l'alerte système, {@code AlertesStatutDossier.alerteExpiration} factorisée).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PremieresRelancesAtomiciteTest {

    private static final String RELANCES = "/api/tranches/{id}/premieres-relances";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePremieresRelances fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private ComActionMessageRepository messages;

    private NettoyageProspectionsTerrain nettoyage;
    private AdmUser admin;
    private Jeu jeu;
    private final List<ComFolder> dossiers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        nettoyage = new NettoyageProspectionsTerrain(dataSource, transactionManager);
        nettoyage.noterConfigurationAbsente();
        admin = nettoyage.valider(patrimoine::administrateur);
        nettoyage.suivre("AdmUser", admin.getId());
        jeu = nettoyage.commeAdmin(admin, fixture::jeu);
        nettoyage.suivre("CfgCompany", jeu.projet().getCfgCompany().getId());
        nettoyage.suivre("CfgProject", jeu.projet().getId());
        nettoyage.suivre("CfgTranche", jeu.tranche().getId());
        nettoyage.suivre("StkItemCategory", jeu.categorie().getId());
        nettoyage.suivre("StkItemType", jeu.type().getId());
        StrStatus statut = nettoyage.commeAdmin(admin, () -> fixture.enCours(jeu, "RES"));
        nettoyage.suivre("StrStatus", statut.getId());
        for (String suffixe : List.of("-A", "-B", "-C")) {
            ComFolder d = nettoyage.commeAdmin(admin, () -> fixture.dossier(jeu, jeu.jeton() + suffixe, statut, true));
            nettoyage.suivre("CfgTier", d.getCfgCustomer().getId());
            nettoyage.suivre("CfgTier", d.getCfgCommercial().getId());
            nettoyage.suivre("ComProspection", d.getComProspection().getId());
            nettoyage.suivre("StkItem", d.getStkItem().getId());
            nettoyage.suivre("ComFolder", d.getId());
            dossiers.add(d);
        }
    }

    @AfterEach
    void nettoyer() {
        Mockito.reset(messages);
        String tranche = jeu.tranche().getId().toString();
        for (ComFolder d : dossiers) {
            String id = d.getId().toString();
            nettoyage.jdbc().update("DELETE FROM AdmAlertData WHERE RelatedKey IN "
                    + "(SELECT PKey FROM ComActionMessage WHERE ComFolderId = ?)", id);
            nettoyage.jdbc().update("DELETE FROM ComActionMessage WHERE ComFolderId = ?", id);
        }
        nettoyage.jdbc().update("UPDATE StkItem SET ComFolderId = NULL WHERE StkHierarchyId = ?", tranche);
        nettoyage.nettoyer();
    }

    /** La 2e alerte écrite lève une exception (save unitaire ou saveAll groupé). */
    private void fairEchouerLaDeuxiemeAlerte() {
        AtomicInteger ecrites = new AtomicInteger();
        Mockito.doAnswer(appel -> {
            if (ecrites.incrementAndGet() >= 2) {
                throw new IllegalStateException("Echec simule de l'alerte du 2e dossier");
            }
            return appel.callRealMethod();
        }).when(messages).save(ArgumentMatchers.any());
        Mockito.doAnswer(appel -> {
            Iterable<?> lot = appel.getArgument(0);
            lot.forEach(m -> ecrites.incrementAndGet());
            if (ecrites.get() >= 2) {
                throw new IllegalStateException("Echec simule des alertes groupees");
            }
            return appel.callRealMethod();
        }).when(messages).saveAll(ArgumentMatchers.any());
    }

    @Test
    void r5_echecSurLeDeuxiemeDossier_rollbackComplet_aucuneDateAucuneAlerte() throws Exception {
        fairEchouerLaDeuxiemeAlerte();

        int statut = mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin)))
                .andReturn().getResponse().getStatus();

        List<String> ids = dossiers.stream().map(d -> d.getId().toString()).toList();
        String place = String.join(",", ids.stream().map(i -> "?").toList());
        Integer dates = nettoyage.jdbc().queryForObject("SELECT COUNT(*) FROM ComFolder WHERE PKey IN (" + place
                + ") AND (Revival1 IS NOT NULL OR Revival1ValidityDate IS NOT NULL)", Integer.class, ids.toArray());
        Integer alertes = nettoyage.jdbc().queryForObject("SELECT COUNT(*) FROM ComActionMessage WHERE ComFolderId IN ("
                + place + ")", Integer.class, ids.toArray());
        assertAll(
                () -> assertThat(statut).as("échec propagé").isGreaterThanOrEqualTo(400).isNotEqualTo(404),
                () -> assertThat(dates).as("dates de relance écrites").isZero(),
                () -> assertThat(alertes).as("alertes écrites").isZero());
    }
}
