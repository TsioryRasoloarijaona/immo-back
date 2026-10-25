package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.FixtureImportAutres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.Autres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PRESENTATIONS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue P9b : réponse 2 du manager au test-writer (même tranche + code deux fois → 2e ligne en erreur, rien écrit). */
@SpringBootTest
@AutoConfigureMockMvc
class ImportPresentationsRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportAutres fixture;

    private Autres a;

    @BeforeEach
    void setUp() {
        a = fixture.preparer("P9BD");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(a);
    }

    @Test
    void i8_memeTrancheEtCodeDeuxFois_secondeEnErreur_rienEcrit() throws Exception {
        String tranche = a.i().tranche().getCode();
        String code = "DBL-" + a.jeton();

        String bilan = corps(mockMvc.perform(requete(PRESENTATIONS, null, false, fichier(List.of(
                                colonnes(TRANCHE, tranche, CODE, code, DESCRIPTION, "Premiere"),
                                colonnes(TRANCHE, tranche, CODE, code.toLowerCase(java.util.Locale.ROOT), DESCRIPTION,
                                        "Seconde"))),
                        connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn());

        assertAll(
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.presentations(a.i().trancheId(), code)).isEmpty());
    }
}
