package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet.Jeu;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static com.tsix.immocrm.controller.projet.LectureTresorerie.ANNEE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.ANNEE_TEST;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.EXPORT;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.PROJETS;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.SOCIETE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N5a Trésorerie projet, export xlsx (FEATURE-n5-tableaux-de-bord-projet.md D7, R15 « export idem ») : mêmes droits,
 * même périmètre et même refus sans projet que l'arbre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieProjetExportVisibiliteTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(FixtureTresorerieProjet.AUJOURDHUI.atTime(10, 0).atZone(FuseauMetier.ZONE).toInstant(),
                FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerieProjet fixture;

    private Jeu jeu;

    @BeforeEach
    void setUp() {
        jeu = fixture.jeu();
    }

    private MockHttpServletResponse exporter(AdmUser compte, String... projets) throws Exception {
        MockHttpServletRequestBuilder requete = get(EXPORT).with(connecte(compte, FixtureTresorerieProjet.ROLE))
                .param(ANNEE, ANNEE_TEST).param(SOCIETE, jeu.societe().toString());
        for (String projet : projets) {
            requete = requete.param(PROJETS, projet);
        }
        return mockMvc.perform(requete).andReturn().getResponse();
    }

    private static List<String> textes(MockHttpServletResponse r) throws IOException {
        List<String> textes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(r.getContentAsByteArray()))) {
            for (Sheet feuille : classeur) {
                for (Row ligne : feuille) {
                    for (Cell cellule : ligne) {
                        if (cellule.getCellType() == CellType.STRING) {
                            textes.add(cellule.getStringCellValue());
                        }
                    }
                }
            }
        }
        return textes;
    }

    private static String corps(MockHttpServletResponse r) throws IOException {
        return r.getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void r15_export_habiliteA_projetsAetB_seulementA() throws Exception {
        MockHttpServletResponse r = exporter(jeu.compteA(), jeu.projetA().toString(), jeu.projetB().toString());

        assertThat(r.getStatus()).as(corps(r)).isEqualTo(200);
        List<String> textes = textes(r);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.getContentType()).startsWith(XLSX);
            s.assertThat(textes).contains(jeu.fournisseurNom()).doesNotContain(jeu.fournisseurBNom());
        });
    }

    @Test
    void r15_export_temoin_habiliteAetB_contientB() throws Exception {
        MockHttpServletResponse r = exporter(jeu.compteAB(), jeu.projetA().toString(), jeu.projetB().toString());

        assertThat(r.getStatus()).as(corps(r)).isEqualTo(200);
        assertThat(textes(r)).contains(jeu.fournisseurNom(), jeu.fournisseurBNom());
    }

    @Test
    void r15_export_societeNonHabilitee_lignesAbsentes() throws Exception {
        MockHttpServletResponse r = exporter(jeu.compteSansSociete(), jeu.projetA().toString());

        assertThat(r.getStatus()).as(corps(r)).isEqualTo(200);
        assertThat(textes(r)).doesNotContain(jeu.fournisseurNom(), jeu.lotA2Libelle());
    }

    @Test
    void d2_export_aucunProjet_400() throws Exception {
        MockHttpServletResponse refus = exporter(jeu.compteA());
        MockHttpServletResponse temoin = exporter(jeu.compteA(), jeu.projetA().toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.getStatus()).isEqualTo(400);
            s.assertThat(temoin.getStatus()).as("témoin").isEqualTo(200);
        });
        assertThat(corps(refus)).contains("Veuillez sélectionner au moins un projet !");
    }

    @Test
    void d1_export_sansDroit_403() throws Exception {
        MockHttpServletResponse refus = exporter(jeu.compteSansDroit(), jeu.projetA().toString());
        MockHttpServletResponse temoin = exporter(jeu.compteA(), jeu.projetA().toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.getStatus()).isEqualTo(403);
            s.assertThat(temoin.getStatus()).as("témoin").isEqualTo(200);
        });
    }
}
