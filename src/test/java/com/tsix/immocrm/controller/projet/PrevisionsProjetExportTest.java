package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.DROIT_ECRAN;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.PREVISION;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.TOTAL;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.INVOICE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** N4a R11 — export xlsx des prévisions d'un projet (FEATURE-n4-previsions-chantier.md R11, R1, R2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsProjetExportTest {

    private static final int NOM_FEUILLE_MAX = 31;
    private static final String COMMERCIAL = "Commercial";
    private static final String GENERAL = "GENERAL";
    private static final String TOTAL_ENTETE = "Total";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixtureLotSousFenetres sf;

    private PrevisionsProjetSupport appels;
    private String jeton;
    private UUID societe;

    @BeforeEach
    void setUp() {
        appels = new PrevisionsProjetSupport(mockMvc, jdbc, em);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        societe = parite.entiteJuridique("PPX-" + jeton);
    }

    /** Projet à un lot inclus : estimation 1200, prévision 500 au 01/02/2026, réel 1000 au 10/02/2026. */
    private UUID projetAvecUnLot(String code) {
        UUID projet = parite.projet(code + "-" + jeton, societe);
        UUID lot = parite.lot(projet, "PXL-" + code + "-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "PXS-" + code + "-" + jeton));
        prev.planning(lot, LocalDate.of(2026, 1, 1), 90, LocalDate.of(2026, 1, 1), 90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0", "0");
        appels.inclure(lot, true);
        sf.prevision(lot, LocalDate.of(2026, 2, 1), "500", "0.5");
        sf.piece(lot, projet, societe, "PXD-" + code + "-" + jeton, INVOICE, "", LocalDate.of(2026, 2, 10), "1000");
        return projet;
    }

    private RequestPostProcessor admin(String code, UUID... projets) {
        return connecte(parite.administrateur("px." + code + "." + jeton, projets), "Administrateur");
    }

    private static String feuille(String nom) {
        return nom.length() > NOM_FEUILLE_MAX ? nom.substring(0, NOM_FEUILLE_MAX) : nom;
    }

    // ------------------------------------------------------------------ R11 : feuilles et chiffres

    @Test
    void r11_export_feuillesGeneralLotsPuisSixSeries() throws Exception {
        UUID projet = projetAvecUnLot("F");

        MockHttpServletResponse r = appels.exporter(admin("f", projet), projet);

        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(200);
        List<String> noms = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(r.getContentAsByteArray()))) {
            classeur.forEach(f -> noms.add(f.getSheetName()));
        }
        assertThat(noms).containsSubsequence(GENERAL, "Lots", feuille("Prévision - Décaissement"),
                feuille("Prévision - Avancement"), feuille("Prévision ajustée - Décaissement"),
                feuille("Prévision ajustée - Avancement"), feuille("Réel - Décaissement"), feuille("Réel - Avancement"));
    }

    /** R11 « mêmes chiffres que le JSON » : GENERAL, ligne Prévision × colonne Total = totaux.prevision.total (1200). */
    @Test
    void r11_general_totalPrevision_egalAuJson() throws Exception {
        UUID projet = projetAvecUnLot("T");
        RequestPostProcessor qui = admin("t", projet);

        Reponse json = appels.lire(qui, projet);
        MockHttpServletResponse r = appels.exporter(qui, projet);

        assertThat(json.statut()).as(json.corps()).isEqualTo(200);
        assertThat(r.getStatus()).isEqualTo(200);
        BigDecimal attendu = json.nombre(totaux(PREVISION, TOTAL));
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(r.getContentAsByteArray()))) {
            Sheet general = classeur.getSheet(GENERAL);
            assertThat(general).isNotNull();
            SoftAssertions s = new SoftAssertions();
            s.assertThat(attendu).isCloseTo(new BigDecimal("1200"), within(new BigDecimal("0.01")));
            s.assertThat(celluleTotalPrevision(general)).isCloseTo(attendu.doubleValue(), within(0.01));
            s.assertAll();
        }
    }

    @Test
    void r11_lots_contientLeCodeDuLot() throws Exception {
        UUID projet = projetAvecUnLot("L");

        MockHttpServletResponse r = appels.exporter(admin("l", projet), projet);

        assertThat(r.getStatus()).isEqualTo(200);
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(r.getContentAsByteArray()))) {
            Sheet lots = classeur.getSheet("Lots");
            assertThat(lots).isNotNull();
            assertThat(textesDe(lots)).contains("PXL-L-" + jeton);
        }
    }

    /** Colonne dont l'en-tête est « Total », ligne dont la 1re cellule est « Prévision ». */
    private static double celluleTotalPrevision(Sheet feuille) {
        int colonne = -1;
        Row ligne = null;
        for (Row row : feuille) {
            if (colonne < 0) {
                colonne = colonneDe(row, TOTAL_ENTETE);
            }
            Cell premiere = row.getCell(row.getFirstCellNum() < 0 ? 0 : row.getFirstCellNum());
            if (premiere != null && "Prévision".equals(texte(premiere).trim())) {
                ligne = row;
            }
        }
        assertThat(colonne).as("colonne Total").isNotNegative();
        assertThat(ligne).as("ligne Prévision").isNotNull();
        return nombre(ligne.getCell(colonne));
    }

    private static int colonneDe(Row row, String entete) {
        for (Cell c : row) {
            if (entete.equalsIgnoreCase(texte(c).trim())) {
                return c.getColumnIndex();
            }
        }
        return -1;
    }

    private static String texte(Cell c) {
        return c.getCellType() == CellType.STRING ? c.getStringCellValue() : "";
    }

    private static double nombre(Cell c) {
        assertThat(c).as("cellule Total Prévision").isNotNull();
        if (c.getCellType() == CellType.NUMERIC || c.getCellType() == CellType.FORMULA) {
            return c.getNumericCellValue();
        }
        return Double.parseDouble(c.getStringCellValue().replaceAll("[^0-9,.-]", "").replace(',', '.'));
    }

    private static List<String> textesDe(Sheet feuille) {
        List<String> textes = new ArrayList<>();
        feuille.forEach(row -> row.forEach(c -> textes.add(texte(c))));
        return textes;
    }

    // ------------------------------------------------------------------ R2 / R1 / 400 : mêmes codes que la lecture

    @Test
    void r2_export_sansDroitEcran_403() throws Exception {
        UUID projet = parite.projet("PXD-" + jeton, societe);
        RequestPostProcessor sansDroit = connecte(parite.avecDroits("px.r2." + jeton, List.of(), projet), COMMERCIAL);

        MockHttpServletResponse r = appels.exporter(sansDroit, projet);

        assertThat(r.getStatus()).isEqualTo(403);
    }

    /** R1 : projet non habilité → 404 ; témoin 200 sur le projet habilité du même compte. */
    @Test
    void r1_export_projetNonHabilite_404() throws Exception {
        UUID visible = parite.projet("PXV-" + jeton, societe);
        UUID cache = parite.projet("PXH-" + jeton, societe);
        RequestPostProcessor qui = connecte(parite.avecDroits("px.r1." + jeton, List.of(DROIT_ECRAN), visible), COMMERCIAL);

        MockHttpServletResponse temoin = appels.exporter(qui, visible);
        MockHttpServletResponse r = appels.exporter(qui, cache);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.getStatus()).isEqualTo(200);
        s.assertThat(r.getStatus()).isEqualTo(404);
        s.assertAll();
    }

    @Test
    void export_projetIdAbsent_400() throws Exception {
        UUID projet = parite.projet("PXA-" + jeton, societe);
        RequestPostProcessor qui = admin("a", projet);

        MockHttpServletResponse temoin = appels.exporter(qui, projet);
        MockHttpServletResponse r = appels.exporter(qui);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.getStatus()).isEqualTo(200);
        s.assertThat(r.getStatus()).isEqualTo(400);
        s.assertAll();
    }
}
