package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsBilans.PROJETS;
import static com.tsix.immocrm.controller.projet.AppelsBilans.valeur;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.ESTIMATION_BASE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** N5a R15 Bilans, export xlsx : mêmes refus que la lecture (403, 400), mêmes chiffres, projet hors périmètre ignoré. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BilansProjetExportVisibiliteTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String LIBELLE = "Export bilan";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBilansProjet f;

    private AppelsBilans appels;
    private UUID projet;
    private UUID categorie;

    @BeforeEach
    void setUp() {
        appels = new AppelsBilans(mockMvc);
        f.preparer();
        projet = f.projet("XP", false);
        categorie = f.categorie(LIBELLE, null, 1);
        lotEstime("LX", projet, "1234");
    }

    private void lotEstime(String code, UUID p, String ht) {
        UUID lot = f.lot(code, p, false);
        f.ventiler(lot, categorie, "1");
        f.estimationBase(lot, ht);
    }

    private RequestPostProcessor qui(String code, boolean droit, UUID... projets) {
        return connecte(f.compte("x" + code, droit, projets), COMMERCIAL);
    }

    private record Contenu(List<String> textes, List<Double> nombres) {
    }

    private static Contenu lireClasseur(MockHttpServletResponse r) throws Exception {
        List<String> textes = new ArrayList<>();
        List<Double> nombres = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(r.getContentAsByteArray()))) {
            classeur.forEach(feuille -> feuille.forEach(ligne -> ligne.forEach(c -> ranger(c, textes, nombres))));
        }
        return new Contenu(textes, nombres);
    }

    private static void ranger(Cell c, List<String> textes, List<Double> nombres) {
        if (c.getCellType() == CellType.STRING) {
            textes.add(c.getStringCellValue().trim());
        } else if (c.getCellType() == CellType.NUMERIC || c.getCellType() == CellType.FORMULA) {
            nombres.add(c.getNumericCellValue());
        }
    }

    @Test
    void r15_export_sansDroit_403_temoinAvecDroit200() throws Exception {
        MockHttpServletResponse temoin = appels.exporter(qui("avec", true, projet), PROJETS, projet.toString());
        MockHttpServletResponse r = appels.exporter(qui("sans", false, projet), PROJETS, projet.toString());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.getStatus()).isEqualTo(200);
        s.assertThat(r.getStatus()).isEqualTo(403);
        s.assertAll();
    }

    @Test
    void r15_export_aucunProjet_400_messageOmran_temoin200() throws Exception {
        RequestPostProcessor qui = qui("vide", true, projet);

        MockHttpServletResponse temoin = appels.exporter(qui, PROJETS, projet.toString());
        MockHttpServletResponse r = appels.exporter(qui);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.getStatus()).isEqualTo(200);
        s.assertThat(r.getStatus()).isEqualTo(400);
        s.assertThat(r.getContentAsString(StandardCharsets.UTF_8))
                .contains(BilansProjetVisibiliteTest.MESSAGE_SANS_PROJET);
        s.assertAll();
    }

    /** Le classeur porte le libellé de la catégorie et le montant que le JSON donne pour elle (1234). */
    @Test
    void export_memesChiffresQueLeJson() throws Exception {
        RequestPostProcessor qui = qui("json", true, projet);

        Reponse json = appels.lire(qui, PROJETS, projet.toString());
        MockHttpServletResponse r = appels.exporter(qui, PROJETS, projet.toString());

        assertThat(r.getStatus()).isEqualTo(200);
        double attendu = valeur(json, categorie, ESTIMATION_BASE).doubleValue();
        Contenu contenu = lireClasseur(r);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(attendu).isCloseTo(1234.0, within(0.01));
        s.assertThat(contenu.textes()).anyMatch(t -> t.contains(LIBELLE));
        s.assertThat(contenu.nombres()).anyMatch(n -> Math.abs(n - attendu) < 0.01);
        s.assertAll();
    }

    /** Projet hors périmètre (5000) demandé avec le projet habilité : 200, et aucun total 6234 dans le classeur. */
    @Test
    void r15_export_projetHorsPerimetre_ignore() throws Exception {
        UUID cache = f.projetHorsPerimetre("XH");
        lotEstime("LXH", cache, "5000");

        MockHttpServletResponse r = appels.exporter(qui("hors", true, projet), PROJETS, projet.toString(), PROJETS,
                cache.toString());

        assertThat(r.getStatus()).isEqualTo(200);
        Contenu contenu = lireClasseur(r);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(contenu.nombres()).isNotEmpty().anyMatch(n -> Math.abs(n - 1234) < 0.01).noneMatch(n -> Math.abs(n - 6234) < 0.01 || Math.abs(n - 5000) < 0.01);
        s.assertAll();
    }
}
