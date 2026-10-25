package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.FixtureImportAutres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.Autres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.EnTete;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.ANNEE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.MOIS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.NOTES;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.QTE_CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.QTE_FINALISATION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Revue P9b : règle I5 (une erreur → bilan complet, {@code ecrit = false}) pour une ligne de prévision que la base
 * refuse (Notes au-delà des 1 000 caractères de {@code CfgPrevision.Notes}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportPrevisionsRevueTest {

    private static final LocalDateTime DEBUT = LocalDateTime.of(2027, 1, 1, 0, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2027, 3, 1, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportAutres fixture;

    private Autres a;

    @BeforeEach
    void setUp() {
        a = fixture.preparer("P9BR");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(a);
    }

    @Test
    void i5_notesTropLongues_bilanRendu_rienEcrit() throws Exception {
        UUID tranche = a.i().trancheId();
        fixture.enTete(tranche, new EnTete(1, new BigDecimal("1000"), new BigDecimal("0.4"), DEBUT, FIN, DEBUT, FIN,
                BigDecimal.ONE));

        MvcResult reponse = mockMvc.perform(requete(PREVISIONS, tranche, false, fichier("PREVISIONS", List.of(
                        colonnes(ANNEE, 2027, MOIS, 1, QTE_CONCRETISATION, 1, QTE_FINALISATION, 1, NOTES, "N".repeat(1001)))),
                connecte(a.i().admin()))).andReturn();

        assertThat(reponse.getResponse().getStatus()).as(reponse.getResponse().getContentAsString()).isEqualTo(200);
        String bilan = corps(reponse);
        assertAll(
                () -> assertThat(ecrit(bilan)).as(bilan).isFalse(),
                () -> assertThat(fixture.lignesPrevision(tranche)).isEmpty());
    }
}
