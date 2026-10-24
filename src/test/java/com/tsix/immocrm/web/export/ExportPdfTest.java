package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.lignesXlsx;
import static com.tsix.immocrm.fixtures.LectureExport.pagesPdf;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * P9c §3.1 — moteur {@code ExportPdf} (FEATURE-patrimoine-import-export.md, Question 7), éprouvé par HTTP sur
 * {@code /api/articles/export?format=pdf} : A4 paysage, en-tête de colonnes répété à chaque page, édition (filtres,
 * date, utilisateur), pied « page n / m », montants {@code 1 350 000,00}, plafond de 2 000 lignes (E2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExportPdfTest {

    private static final String EXPORT = "/api/articles/export";
    private static final String MESSAGE_TROP_DE_LIGNES = "Trop de lignes pour un PDF : exportez en .xlsx";
    private static final String COLONNES_PRIX = "code,comSalePrice";
    private static final String FORMAT = "format";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COLONNES = "colonnes";
    private static final float A4_LONG = 842f;
    private static final float A4_COURT = 595f;

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private Clock clock;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureExportPatrimoine articles;

    private AdmUser admin;
    private String jeton;
    private CfgTranche tranche;
    private FixtureExportPatrimoine.Referentiel referentiel;

    @BeforeEach
    void setUp() {
        jeton = FixtureExportPatrimoine.jeton();
        admin = fixture.administrateur();
        admin.setDescription("Editeur " + jeton);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
        referentiel = articles.referentiel(jeton);
    }

    // ------------------------------------------------------------------ mise en page

    @Test
    void pdf_a4Paysage() throws Exception {
        articles.article(referentiel, "ART-" + jeton, tranche, null);
        byte[] pdf = pdfDeLaTranche(COLONNES_PRIX);

        Rectangle page;
        try (PdfReader reader = new PdfReader(pdf)) {
            page = reader.getPageSizeWithRotation(1);
        }
        assertThat(page.getWidth()).isCloseTo(A4_LONG, within(2f));
        assertThat(page.getHeight()).isCloseTo(A4_COURT, within(2f));
    }

    @Test
    void pdf_enTeteDesColonnesRepeteAChaquePage() throws Exception {
        articles.articlesEnMasse(referentiel, tranche, "BLK-" + jeton + "-", 150);
        List<String> pages = pagesPdf(pdfDeLaTranche(COLONNES_PRIX));

        String libelle = compact(ColonneArticle.PRIX_VENTE_TTC.libelle());
        assertThat(pages).hasSizeGreaterThan(1)
                .allSatisfy(texte -> assertThat(compact(texte)).contains(libelle));
    }

    @Test
    void pdf_piedDePage_pageNSurM() throws Exception {
        articles.articlesEnMasse(referentiel, tranche, "BLK-" + jeton + "-", 150);
        List<String> pages = pagesPdf(pdfDeLaTranche(COLONNES_PRIX));

        int total = pages.size();
        assertThat(total).isGreaterThan(1);
        assertThat(IntStream.rangeClosed(1, total).boxed().toList()).allSatisfy(n ->
                assertThat(pages.get(n - 1)).containsPattern(
                        Pattern.compile("(?i)page\\s*" + n + "\\s*/\\s*" + total)));
    }

    /** Filtre appliqué : la recherche en minuscules n'apparaît dans aucune ligne (codes en majuscules). */
    @Test
    void pdf_enTete_filtresAppliques_dateDEdition_utilisateur() throws Exception {
        articles.article(referentiel, "ART-" + jeton, tranche, null);
        String recherche = jeton.toLowerCase(Locale.ROOT);
        relire();

        MvcResult resultat = executer(mockMvc, get(EXPORT).with(connecte(admin)).param(FORMAT, "pdf")
                .param(COLONNES, COLONNES_PRIX).param("search", recherche));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        String premierePage = compact(pagesPdf(resultat.getResponse().getContentAsByteArray()).getFirst());
        String aujourdHui = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        assertThat(premierePage).contains(recherche).contains(aujourdHui).contains(compact("Editeur " + jeton));
    }

    @Test
    void pdf_montantsFormatesALaFrancaise() throws Exception {
        articles.article(referentiel, "ART-" + jeton, tranche, new BigDecimal("1350000"));

        String texte = textePdf(pdfDeLaTranche(COLONNES_PRIX));

        assertThat(texte).contains("1 350 000,00");
    }

    // ------------------------------------------------------------------ E2 : plafond de 2 000 lignes (Question 7)

    @Test
    void e2_plusDe2000Lignes_pdf400AvecLeMessage() throws Exception {
        articles.articlesEnMasse(referentiel, tranche, "BLK-" + jeton + "-", 2001);

        MvcResult resultat = executer(mockMvc, requeteDeLaTranche("pdf", "code"));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultat.getResponse().getContentAsString()).contains(MESSAGE_TROP_DE_LIGNES);
    }

    @Test
    void e2_2000Lignes_pdfAccepte() throws Exception {
        articles.articlesEnMasse(referentiel, tranche, "BLK-" + jeton + "-", 2000);

        MvcResult resultat = executer(mockMvc, requeteDeLaTranche("pdf", "code"));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(estPdf(resultat.getResponse().getContentAsByteArray())).isTrue();
    }

    @Test
    void e2_plusDe2000Lignes_xlsxAccepte() throws Exception {
        articles.articlesEnMasse(referentiel, tranche, "BLK-" + jeton + "-", 2001);

        MvcResult resultat = executer(mockMvc, requeteDeLaTranche("xlsx", "code"));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(lignesXlsx(resultat.getResponse().getContentAsByteArray())).hasSize(2002);
    }

    // ------------------------------------------------------------------ outils

    private MockHttpServletRequestBuilder requeteDeLaTranche(String format, String colonnes) {
        relire();
        return get(EXPORT).with(connecte(admin)).param(FORMAT, format).param(COLONNES, colonnes)
                .param(TRANCHE_ID, tranche.getId().toString());
    }

    private byte[] pdfDeLaTranche(String colonnes) throws Exception {
        MvcResult resultat = executer(mockMvc, requeteDeLaTranche("pdf", colonnes));
        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        byte[] contenu = resultat.getResponse().getContentAsByteArray();
        assertThat(estPdf(contenu)).isTrue();
        return contenu;
    }

    private void relire() {
        em.flush();
        em.clear();
    }
}
