package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureRoles;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureRoles.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureRoles.TEXTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Export de la liste filtrée — FEATURE-admin-roles.md, route {@code /export} ; le 403 est dans {@link RolesSecuriteTest}. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesExportTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private String retenu;
    private String ecarte;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        retenu = "RET-" + jeton;
        ecarte = "ECA-" + jeton;
        fixture.role(retenu, "Libelle retenu");
        fixture.role(ecarte, "Libelle ecarte");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private MockHttpServletResponse exporter(String texte) throws Exception {
        MvcResult resultat = mockMvc.perform(get(EXPORT).param(TEXTE, texte).with(connecte(fixture.compte()))).andReturn();
        if (resultat.getRequest().isAsyncStarted()) {
            resultat = mockMvc.perform(asyncDispatch(resultat)).andReturn();
        }
        status().isOk().match(resultat);
        return resultat.getResponse();
    }

    private static List<String> lignes(MockHttpServletResponse reponse) throws Exception {
        List<String> lignes = new ArrayList<>();
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            for (Row ligne : classeur.getSheetAt(0)) {
                lignes.add(ligne.toString() + cellules(ligne));
            }
        }
        return lignes;
    }

    private static String cellules(Row ligne) {
        StringBuilder texte = new StringBuilder();
        ligne.forEach(cellule -> texte.append(cellule).append('|'));
        return texte.toString();
    }

    @Test
    void export_classeurXlsx() throws Exception {
        assertThat(exporter(retenu).getContentType()).isEqualTo(XLSX);
    }

    @Test
    void export_suitLeFiltreDeLaListe() throws Exception {
        List<String> lignes = lignes(exporter(retenu));

        assertThat(lignes).hasSize(2).anyMatch(l -> l.contains(retenu) && l.contains("Libelle retenu"))
                .noneMatch(l -> l.contains(ecarte));
    }
}
