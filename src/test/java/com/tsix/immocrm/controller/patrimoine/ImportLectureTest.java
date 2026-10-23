package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine.Import;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.ClasseurDeTest.ligne;
import static com.tsix.immocrm.fixtures.ClasseurDeTest.xlsx;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lecture durcie du classeur — FEATURE-patrimoine-import-export.md §1.1 (404 type / tranche, 400 tranche manquante),
 * §1.2 (10 Mo → 413, .xls / non-ZIP / bombe / archive corrompue → 400, DOCTYPE externe, 1 000 lignes, feuille, en-tête en
 * double), §5 I2–I4. Chaque refus : rien écrit (import réel demandé, {@code simulation=false}).
 * Sans {@code @Transactional} : l'import ouvre sa propre transaction ; tout est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportLectureTest {

    private static final String DETAIL = "$.detail";
    private static final long DIX_MO = 10L * 1024 * 1024;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportPatrimoine fixture;

    @TempDir Path dossierTemporaire;

    private Import i;

    @BeforeEach
    void setUp() {
        i = fixture.preparer("P9L");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(i);
    }

    private ResultActions importer(byte[] fichier) throws Exception {
        return mockMvc.perform(requete(ARTICLES, i.trancheId(), false, fichier, connecte(i.admin())));
    }

    private byte[] classeurValide() {
        return xlsx(List.of(PIVOT, DESCRIPTION), List.of(ligne("LEC-" + i.jeton(), "Article lecture")));
    }

    private void rienEcrit() {
        assertThat(fixture.nombreArticles(i)).as("articles de la tranche").isZero();
    }

    // ------------------------------------------------------------------ §1.1 tranche et type

    @Test
    void r1_1_trancheIdManquant_400_messageOmran() throws Exception {
        mockMvc.perform(requete(ARTICLES, null, false, classeurValide(), connecte(i.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("Vous devez sélectionner une Tranche !")));

        rienEcrit();
    }

    @Test
    void r1_1_typeInconnu_404() throws Exception {
        mockMvc.perform(requete("inconnu", i.trancheId(), false, classeurValide(), connecte(i.admin())))
                .andExpect(status().isNotFound());

        rienEcrit();
    }

    @Test
    void r1_1_trancheDEntiteInactive_404() throws Exception {
        UUID invisible = fixture.trancheInvisible(i).getId();

        mockMvc.perform(requete(ARTICLES, invisible, false, classeurValide(), connecte(i.admin())))
                .andExpect(status().isNotFound());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItem WHERE StkHierarchyId = ?", invisible.toString()))
                .isZero();
    }

    // ------------------------------------------------------------------ I2 taille

    @Test
    void i2_fichierDePlusDe10Mo_413_messageNommeLaLimite() throws Exception {
        byte[] valide = classeurValide();
        byte[] gros = Arrays.copyOf(valide, (int) DIX_MO + 1);

        importer(gros)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath(DETAIL).value(containsString("10")));

        rienEcrit();
    }

    // ------------------------------------------------------------------ I3 format

    @Test
    void i3_fichierXls_400_enregistrezEnXlsx() throws Exception {
        importer(ClasseurDeTest.xls(List.of(PIVOT, DESCRIPTION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("Format .xls non pris en charge")))
                .andExpect(jsonPath(DETAIL).value(containsString(".xlsx")));

        rienEcrit();
    }

    @Test
    void i3_contenuNonZip_400() throws Exception {
        importer("Pivot;Description\nLEC;Article".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());

        rienEcrit();
    }

    @Test
    void i3_archiveCorrompue_400_fichierIllisible() throws Exception {
        byte[] corrompu = Arrays.copyOf(classeurValide(), 200);

        importer(corrompu)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("illisible")));

        rienEcrit();
    }

    /** I3 : feuille de 30 Mo d'espaces compressée à ~30 Ko (ratio ≈ 0,001 &lt; 0,01 de POI). */
    @Test
    void i3_bombeZip_400_fichierIllisibleOuPiege() throws Exception {
        String feuille = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                %s<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>%s</sheetData></worksheet>"""
                .formatted(" ".repeat(30_000_000), lignesEnLigne("BOMBE-" + i.jeton()));
        byte[] bombe = ClasseurDeTest.remplacerEntree(classeurValide(), ClasseurDeTest.ENTREE_FEUILLE_1,
                feuille.getBytes(StandardCharsets.UTF_8));

        importer(bombe)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("illisible")));

        rienEcrit();
    }

    /** §1.2 : DOCTYPE à entité externe → 400, ou lecture sans résolution ; jamais le contenu du fichier visé. */
    @Test
    void r1_2_doctypeEntiteExterne_400OuNonResolue() throws Exception {
        String marqueur = "SECRET-XXE-" + i.jeton();
        Path cible = Files.writeString(dossierTemporaire.resolve("cible.txt"), marqueur);
        String feuille = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <!DOCTYPE worksheet [<!ENTITY xxe SYSTEM "%s">]>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>%s</sheetData></worksheet>"""
                .formatted(cible.toUri(), lignesEnLigne("&xxe;"));
        byte[] piege = ClasseurDeTest.remplacerEntree(classeurValide(), ClasseurDeTest.ENTREE_FEUILLE_1,
                feuille.getBytes(StandardCharsets.UTF_8));

        MvcResult resultat = importer(piege).andReturn();

        int statut = resultat.getResponse().getStatus();
        assertAll(
                () -> assertThat(statut).as("400 ou bilan 200, jamais 404 / 500").isIn(200, 400),
                () -> assertThat(corps(resultat)).doesNotContain(marqueur),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StkItem WHERE Code LIKE ?", "%" + marqueur + "%"))
                        .isZero());
    }

    /** Feuille 1 en chaînes en ligne : en-têtes Pivot / Description, une ligne de données. */
    private static String lignesEnLigne(String pivot) {
        return """
                <row r="1"><c r="A1" t="inlineStr"><is><t>Pivot</t></is></c><c r="B1" t="inlineStr"><is><t>Description</t></is></c></row>\
                <row r="2"><c r="A2" t="inlineStr"><is><t>%s</t></is></c><c r="B2" t="inlineStr"><is><t>Article</t></is></c></row>"""
                .formatted(pivot);
    }

    // ------------------------------------------------------------------ I4 / feuille / en-têtes

    @Test
    void i4_plusDe1000Lignes_400_avantEcriture() throws Exception {
        List<List<Object>> lignes = new ArrayList<>();
        for (int n = 0; n <= 1000; n++) {
            lignes.add(ligne("L" + n + "-" + i.jeton(), "Article " + n));
        }

        importer(xlsx(List.of(PIVOT, DESCRIPTION), lignes)).andExpect(status().isBadRequest());

        rienEcrit();
    }

    @Test
    void r1_2_classeurSansFeuille_400_messageOmran() throws Exception {
        importer(ClasseurDeTest.sansFeuille())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("Votre fichier ne contient pas de feuille de clacul !")));
    }

    @Test
    void r1_2_feuilleSansLigneDeDonnees_400_messageOmran() throws Exception {
        importer(xlsx(List.of(PIVOT, DESCRIPTION), List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("Votre fichier ne contient pas de données !")));
    }

    @Test
    void r1_2_feuilleVide_400_messageOmran() throws Exception {
        Map<String, List<List<Object>>> feuilles = new LinkedHashMap<>();
        feuilles.put(ClasseurDeTest.FEUILLE, List.of());

        importer(xlsx(feuilles))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("Votre fichier ne contient pas de données !")));
    }

    @Test
    void r1_2_enTeteEnDouble_400() throws Exception {
        importer(xlsx(List.of(PIVOT, DESCRIPTION, DESCRIPTION),
                List.of(ligne("DBL-" + i.jeton(), "Article a", "Article b"))))
                .andExpect(status().isBadRequest());

        rienEcrit();
    }
}
