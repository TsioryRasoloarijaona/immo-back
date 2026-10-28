package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
import com.tsix.immocrm.fixtures.FixturePlans;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import de plan d'une tranche de lots — FEATURE-patrimoine-lotissements.md §1.6, règle L15 : le formulaire
 * {@code ImportSvgPath} existant (P3) rapproche les tracés par code de tous les StkItem de la tranche, nature TER comprise.
 * Comportement déjà livré : ce test est vert avant le code du lot.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportPlanLotTest {

    private static final String IMPORT = "/api/tranches/{id}/plans/import";
    private static final String D = "m 10.5,20 h 30 v 40 z";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotissements fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CP");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    @Test
    void l15_traceAuCodeDUnLot_traceCreePourLeLot() throws Exception {
        UUID lot = fixture.lot(j, j.code("LPL"));
        UUID etage = fixture.etage(j);
        String svg = """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1312" height="928">
                <g><image width="1312.5979" height="928.15833" x="73.34" y="22.7" xlink:href="%s"/>
                <path id="%s" d="%s"/></g>
                </svg>""".formatted(FixturePlans.dataUriPng(), j.code("LPL"), D);

        mockMvc.perform(multipart(IMPORT, j.tranche())
                        .file(new MockMultipartFile("fichier", "plan.svg", "image/svg+xml",
                                svg.getBytes(StandardCharsets.UTF_8)))
                        .param("etageId", etage.toString())
                        .with(connecte(j.admin())))
                .andExpect(status().is2xxSuccessful());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkHierarchyPath WHERE StkItemId = ?", lot.toString()))
                .isEqualTo(1);
    }
}
