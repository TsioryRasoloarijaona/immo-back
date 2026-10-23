package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.liste;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Lecture commune des rapports du stock (FEATURE-patrimoine-rapports.md §8). */
final class RapportsPatrimoineSupport {

    static final String PATRIMOINE = "patrimoine";
    static final String TRANCHE_ID = "trancheId";
    static final String FORMAT = "format";
    static final String XLSX = "xlsx";

    private RapportsPatrimoineSupport() {
    }

    static List<String> codesVisibles(AppelsRapports appels, RequestPostProcessor qui) throws Exception {
        return JsonPath.read(appels.corps(get(liste(PATRIMOINE)).with(qui)), "$[*].rapports[*].code");
    }

    static List<String> libellesVisibles(AppelsRapports appels, RequestPostProcessor qui) throws Exception {
        return JsonPath.read(appels.corps(get(liste(PATRIMOINE)).with(qui)), "$[*].rapports[*].libelle");
    }

    /** Nom ({@code nom}) de la colonne dont le libellé vaut {@code libelle} (colonnes dynamiques) ; null si absente. */
    static String colonneDeLibelle(Map<String, Object> section, String libelle) {
        return colonnes(section).stream().filter(c -> libelle.equals(c.get("libelle")))
                .map(c -> (String) c.get("nom")).findFirst().orElse(null);
    }

    static List<String> libellesColonnes(Map<String, Object> section) {
        return colonnes(section).stream().map(c -> (String) c.get("libelle")).filter(Objects::nonNull).toList();
    }

    /**
     * T9 : l'export a une feuille par section du résultat (mêmes paramètres), dans le même ordre ; une section avec
     * totaux finit par une ligne « Total ».
     */
    static void verifierExport(AppelsRapports appels, MockHttpServletRequestBuilder resultat,
                               MockHttpServletRequestBuilder export) throws Exception {
        List<Map<String, Object>> sections = appels.sections(resultat);
        MockHttpServletResponse reponse = appels.exporter(export).getResponse();
        assertThat(reponse.getStatus()).isEqualTo(200);
        assertThat(sections).isNotEmpty();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            assertThat(classeur.getNumberOfSheets()).isEqualTo(sections.size());
            SoftAssertions soft = new SoftAssertions();
            for (int i = 0; i < sections.size(); i++) {
                if (totaux(sections.get(i)) != null) {
                    Sheet feuille = classeur.getSheetAt(i);
                    Row derniere = feuille.getRow(feuille.getLastRowNum());
                    soft.assertThat(derniere.getCell(0).toString()).as("ligne Total de %s", sections.get(i).get("id"))
                            .isEqualTo("Total");
                }
            }
            soft.assertAll();
        }
    }
}
