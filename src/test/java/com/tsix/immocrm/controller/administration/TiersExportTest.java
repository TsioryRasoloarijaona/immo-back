package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.EXPORT;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.lignesXlsx;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Impression et export des listes de tiers — FEATURE-admin-onglets-tiers.md, réponse R4 et décision 8 : Exporter = xlsx,
 * Imprimer = pdf de {@code ExportListe}, sur les cinq familles, mêmes filtres que la liste ({@code recherche}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersExportTest {

    private static final String FORMAT = "format";
    private static final String RECHERCHE = "recherche";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    private MockHttpServletResponse exporter(TypeTiers type, String format, String recherche) throws Exception {
        return executer(mockMvc, get(EXPORT, type.famille()).with(connecte(j.admin()))
                .param(FORMAT, format).param(RECHERCHE, recherche)).getResponse();
    }

    @ParameterizedTest
    @EnumSource(TypeTiers.class)
    void reponseR4_exporter_xlsx_lignesDeLaListeFiltree(TypeTiers type) throws Exception {
        j.tiers(type, "A").inserer();
        j.tiers(type, "B").inserer();

        MockHttpServletResponse reponse = exporter(type, "xlsx", j.code("A"));

        assertThat(reponse.getStatus()).isEqualTo(200);
        List<String> cellules = lignesXlsx(reponse.getContentAsByteArray()).stream().flatMap(List::stream)
                .map(Objects::toString).toList();
        assertThat(cellules).contains(j.code("A")).doesNotContain(j.code("B"));
    }

    @ParameterizedTest
    @EnumSource(TypeTiers.class)
    void reponseR4_imprimer_pdf_lignesDeLaListeFiltree(TypeTiers type) throws Exception {
        j.tiers(type, "A").inserer();
        j.tiers(type, "B").inserer();

        MockHttpServletResponse reponse = exporter(type, "pdf", j.code("A"));

        assertThat(reponse.getStatus()).isEqualTo(200);
        byte[] contenu = reponse.getContentAsByteArray();
        assertThat(estPdf(contenu)).isTrue();
        assertThat(compact(textePdf(contenu))).contains(j.code("A")).doesNotContain(j.code("B"));
    }
}
