package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import com.tsix.immocrm.web.export.LignesExport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.ENTETES;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_DEBUT;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_FIN;
import static com.tsix.immocrm.fixtures.FixtureTaches.GESTIONNAIRE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.TEXTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Export — FEATURE-admin-rendez-vous.md, règle 13 (le 403 est dans {@link RendezVousSecuriteTest}). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RendezVousExportTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SEPARATEUR = "|";
    private static final String LE_10 = "2031-03-10";
    private static final String LE_12 = "2031-03-12";
    private static final String RETENU = "RETENU";
    private static final String ECARTE = "ECARTE";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);
    private static final int PLAFOND = LignesExport.LIGNES_EXPORT_MAX;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches taches;
    @Autowired private FixtureRendezVous fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        taches.assurer();
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private MockHttpServletRequestBuilder requete(String... paires) {
        MockHttpServletRequestBuilder requete = get(EXPORT).with(connecte(admin));
        for (int i = 0; i < paires.length; i += 2) {
            requete.param(paires[i], paires[i + 1]);
        }
        return requete;
    }

    /** Réponse à 200, écrite d'un bloc ou en flux. */
    private MockHttpServletResponse exporter(String... paires) throws Exception {
        MvcResult resultat = mockMvc.perform(requete(paires)).andReturn();
        if (resultat.getRequest().isAsyncStarted()) {
            resultat = mockMvc.perform(asyncDispatch(resultat)).andReturn();
        }
        status().isOk().match(resultat);
        return resultat.getResponse();
    }

    /** Lignes de la première feuille, en-tête compris, cellules jointes par {@code |}. */
    private List<String> lignes(String... paires) throws Exception {
        byte[] contenu = exporter(paires).getContentAsByteArray();
        List<String> lignes = new ArrayList<>();
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            for (Row ligne : classeur.getSheetAt(0)) {
                StringJoiner cellules = new StringJoiner(SEPARATEUR);
                for (Cell cellule : ligne) {
                    cellules.add(cellule.toString());
                }
                lignes.add(cellules.toString());
            }
        }
        return lignes;
    }

    private String code(String suffixe) {
        return suffixe + "-" + jeton;
    }

    private void retenuSeul(List<String> lignes) {
        assertThat(lignes).isNotEmpty()
                .anyMatch(ligne -> ligne.contains(code(RETENU)))
                .noneMatch(ligne -> ligne.contains(code(ECARTE)));
    }

    @Test
    void r13_classeurXlsxEnPieceJointe() throws Exception {
        fixture.rendezVous(code(RETENU), JOUR);

        MockHttpServletResponse reponse = exporter(TEXTE, jeton);

        assertAll(
                () -> assertThat(reponse.getContentType()).startsWith(XLSX),
                () -> assertThat(reponse.getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment").contains(".xlsx"));
    }

    @Test
    void r13_enTetesFrancais_les14ColonnesDeLEcran() throws Exception {
        fixture.rendezVous(code(RETENU), JOUR);

        assertThat(lignes(TEXTE, jeton)).contains(String.join(SEPARATEUR, ENTETES));
    }

    @Test
    void r13_dateCroissante() throws Exception {
        fixture.rendezVous(code("C"), JOUR.plusDays(2));
        fixture.rendezVous(code("A"), JOUR);
        fixture.rendezVous(code("B"), JOUR.plusDays(1));

        assertThat(lignes(TEXTE, jeton).stream().filter(ligne -> ligne.contains(jeton))
                .map(ligne -> ligne.substring(0, ligne.indexOf(SEPARATEUR))))
                .containsExactly(code("A"), code("B"), code("C"));
    }

    @Test
    void r13_jamaisUneTache() throws Exception {
        fixture.rendezVous(code(RETENU), JOUR);
        taches.definir(taches.tache(code(ECARTE)), "StartDatePrev", JOUR);

        retenuSeul(lignes(TEXTE, jeton));
    }

    @Test
    void r13_filtreTexte() throws Exception {
        fixture.rendezVous(code(RETENU), JOUR);
        fixture.rendezVous(ECARTE + "-" + FixtureReferentielsAdmin.jeton(), JOUR);

        assertThat(lignes(TEXTE, jeton)).isNotEmpty()
                .anyMatch(ligne -> ligne.contains(code(RETENU)))
                .noneMatch(ligne -> ligne.contains(ECARTE));
    }

    @Test
    void r13_filtreDates_bornesIncluses() throws Exception {
        fixture.rendezVous(code(RETENU), LocalDateTime.of(2031, 3, 12, 23, 59));
        fixture.rendezVous(code(ECARTE), LocalDateTime.of(2031, 3, 13, 0, 0));

        retenuSeul(lignes(TEXTE, jeton, DATE_DEBUT, LE_10, DATE_FIN, LE_12));
    }

    @Test
    void r13_filtreGestionnaire() throws Exception {
        UUID gestionnaire = fixture.tier(code("GES"), "Gestionnaire");
        UUID autre = fixture.tier(code("GET"), "Autre");
        taches.definir(fixture.rendezVous(code(RETENU), JOUR), "CfgTierManagerId", gestionnaire);
        taches.definir(fixture.rendezVous(code(ECARTE), JOUR), "CfgTierManagerId", autre);

        retenuSeul(lignes(TEXTE, jeton, GESTIONNAIRE_ID, gestionnaire.toString()));
    }

    @Test
    void r13_memeValidation_dateFinAvantDateDebut_400() throws Exception {
        mockMvc.perform(requete(DATE_DEBUT, LE_12, DATE_FIN, LE_10)).andExpect(status().isBadRequest());
    }

    @Test
    void r13_plafond_LIGNES_EXPORT_MAX() throws Exception {
        fixture.enMasse(PLAFOND + 3, JOUR, code("MASSE"));

        assertThat(lignes(TEXTE, code("MASSE")).stream().filter(ligne -> ligne.contains(jeton))).hasSize(PLAFOND);
    }
}
