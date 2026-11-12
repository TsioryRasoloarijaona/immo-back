package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.STATUT_IDS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E4 — FEATURE-lots-liste-parite.md : classeur xlsx des lignes de E1 aux mêmes filtres, ligne de total en pied. Les droits
 * (403 / témoin / Administrateur) sont dans {@code LotsDroitsTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsExportTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;

    private LotsPariteSupport appels;
    private String jeton;
    private RequestPostProcessor qui;
    private UUID projet;
    private UUID s1;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        projet = fixture.projet("XP-" + jeton, fixture.entiteJuridique("XE-" + jeton));
        s1 = fixture.statut(ENTREPRENEURS, "XS1-" + jeton);
        UUID s2 = fixture.statut(ENTREPRENEURS, "XS2-" + jeton);
        fixture.lot(projet, "XA-" + jeton, ENTREPRENEURS, s1);
        fixture.lot(projet, "XB-" + jeton, ENTREPRENEURS, s2);
        qui = connecte(fixture.administrateur("lots.exp." + jeton, projet), "Administrateur");
    }

    /** Lignes de la première feuille, chaque ligne = textes de ses cellules. */
    private static List<List<String>> lignes(byte[] contenu) throws IOException {
        List<List<String>> lignes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheetAt(0);
            for (Row ligne : feuille) {
                List<String> cellules = new ArrayList<>();
                for (Cell cellule : ligne) {
                    cellules.add(cellule.toString());
                }
                lignes.add(cellules);
            }
        }
        return lignes;
    }

    @Test
    void e4_export_xlsx_lignesDuFiltreEtTotalEnPied() throws Exception {
        MockHttpServletResponse r = appels.exporter(qui, PROJET_IDS, projet.toString(), STATUT_IDS, s1.toString());

        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(200);
        List<List<String>> lignes = lignes(r.getContentAsByteArray());
        assertThat(lignes).hasSizeGreaterThanOrEqualTo(3);
        String texte = lignes.toString();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(texte).contains("XA-" + jeton).doesNotContain("XB-" + jeton);
            s.assertThat(lignes.get(lignes.size() - 1)).as("ligne de total en pied").isNotEmpty()
                    .first().asString().startsWith("Total");
        });
    }
}
