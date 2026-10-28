package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureImpressionsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CONCRETISATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lot P9f, réponse 1 du manager au test-writer (27/10) : tous les dossiers retenus écartés (reste ≤ 0) → PDF 409, aperçu 200. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpressionsTrancheEcartesTest {

    private static final String LETTRES = "/api/tranches/{id}/relances-2.pdf";
    private static final String APERCU = "/api/tranches/{id}/relances-2/apercu";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureImpressionsTranche fixture;

    private AdmUser admin;
    private Jeu jeu;
    private ComFolder solde;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeu = fixture.jeu();
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        solde = fixture.prix(fixture.dossier(jeu, j + "-SOL", statut), "200000");
        fixture.echeance(solde, Nature.AVANCE, "200000", "200000", null);
        ComFolder tropPercu = fixture.prix(fixture.dossier(jeu, j + "-TP", statut), "100000");
        fixture.echeance(tropPercu, Nature.AVANCE, "100000", "100000.01", null);
        fixture.relire();
    }

    @Test
    void tousEcartes_pdf409_aucunReste() throws Exception {
        mockMvc.perform(get(LETTRES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Aucun dossier n'a de reste du prix à relancer."));
    }

    @Test
    void tousEcartes_apercu200_deuxDossiersZeroLettre() throws Exception {
        mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossiers").value(2))
                .andExpect(jsonPath("$.lettres").value(0))
                .andExpect(jsonPath("$.ecartes.length()").value(2))
                .andExpect(jsonPath("$.parStatut[0].dossiers").value(2))
                .andExpect(jsonPath("$.parStatut[0].lettres").value(0))
                .andExpect(jsonPath("$.ecartes[0].dossierId").value(solde.getId().toString()));
    }
}
