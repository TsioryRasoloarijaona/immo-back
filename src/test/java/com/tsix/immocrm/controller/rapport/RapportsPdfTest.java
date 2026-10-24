package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.fixtures.LectureExport;
import com.tsix.immocrm.services.rapport.RegistreRapports;
import com.tsix.immocrm.web.export.ExportPdf;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.pagesPdf;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * PDF des rapports (FEATURE-rapports-pdf.md §4 règles 1–9) : même route {@code /export}, {@code format=pdf}.
 * <pre>
 *   TA, TC : projet A (commercial A affecté), dossiers à 1 350 000 et 200 000 ; TB : projet B, hors périmètre de A
 *   TP     : tranche en vente des rapports patrimoine (pièces d'articles, mouvements)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsPdfTest {

    private static final String FORMAT = "format";
    private static final String PDF = "pdf";
    private static final String XLSX = "xlsx";
    private static final String TRANCHE_ID = ParametreRapport.TRANCHE_ID;
    private static final String PATRIMOINE = "patrimoine";
    private static final String TROP_DE_LIGNES = "Trop de lignes pour un PDF : exportez en .xlsx";
    private static final String MESSAGE_FORMAT = "format doit être 'xlsx' ou 'pdf'";
    private static final String TOTAL = "Total";
    private static final float A4_LONG = 842f;
    private static final float A4_COURT = 595f;
    private static final int LIGNES_PDF_MAX = 2_000;
    private static final int COLONNES_LARGES = 17;

    @Autowired private MockMvc mockMvc;
    @Autowired private Clock clock;
    @Autowired private RegistreRapports registre;
    @Autowired private ExportPdf exportPdf;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureExportPatrimoine articlesPatrimoine;

    private AppelsRapports appels;
    private String prefixe;
    private String libelleSuiviGlobal;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche tc;
    private CfgTranche tp;
    private AdmUser commercialA;
    private AdmUser libre;
    private AdmUser restreint;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("PDF");
        fixture.tables();
        libelleSuiviGlobal = "SUIVI GLOBAL PDF " + prefixe;
        for (ModuleRapport module : ModuleRapport.values()) {
            for (String code : registre.duModule(module).keySet()) {
                String libelle = SUIVI_GLOBAL.equals(code) ? libelleSuiviGlobal : "RAPPORT " + code;
                fixture.rapport(module.types().getFirst(), code, libelle, "", 1);
            }
        }
        AdmRole roleLibre = fixture.role("Libre " + prefixe, false);
        AdmRole roleRestreint = fixture.role("Restreint " + prefixe, true);

        CfgProject projetA = ev.projet("PA-" + prefixe, true);
        ta = ev.tranche("TA-" + prefixe, projetA, true);
        tc = ev.tranche("TC-" + prefixe, projetA, true);
        tb = ev.tranche("TB-" + prefixe, ev.projet("PB-" + prefixe, true), true);
        tp = ev.tranche("TP-" + prefixe, ev.projet("PP-" + prefixe, true), true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);
        vendre(ta, categorie, type, "1350000");
        vendre(tc, categorie, type, "200000");
        ev.article(tb.getCode() + "-1", tb, categorie, type);

        commercialA = fixture.compte("pdf.com." + prefixe, roleLibre);
        ev.affecter(commercialA, projetA);
        libre = fixture.compte("pdf.libre." + prefixe, roleLibre);
        restreint = fixture.compte("pdf.restreint." + prefixe, roleRestreint);
        fixture.relire();
    }

    private void vendre(CfgTranche tranche, StkItemCategory categorie, StkItemType type, String prix) {
        ComFolder dossier = ev.vendre(ev.article(tranche.getCode() + "-1", tranche, categorie, type),
                "DOS-" + tranche.getCode(), null, null, null);
        ev.prix(dossier, new BigDecimal(prix));
    }

    // ------------------------------------------------------------------ R1 : chaque rapport du registre

    @Test
    void r1_chaqueRapportDuRegistre_pdf200_applicationPdf_commenceParPdf_nomCodeHorodatage() throws Exception {
        List<Rapport> rapports = tousLesRapports();
        assertThat(rapports).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        for (Rapport rapport : rapports) {
            String code = rapport.code();
            // Témoin de fixture : le résultat et l'xlsx du même rapport passent déjà, sous le plafond PDF.
            soft.assertThat(nombreDeLignes(rapport)).as("fixture %s : lignes ≤ %d", code, LIGNES_PDF_MAX)
                    .isLessThanOrEqualTo(LIGNES_PDF_MAX);
            soft.assertThat(exporter(rapport, XLSX, libre).getStatus()).as("xlsx %s", code).isEqualTo(200);

            MockHttpServletResponse pdf = exporter(rapport, PDF, libre);
            soft.assertThat(pdf.getStatus()).as("pdf %s : statut", code).isEqualTo(200);
            soft.assertThat(pdf.getContentType()).as("pdf %s : type", code).startsWith("application/pdf");
            soft.assertThat(estPdf(pdf.getContentAsByteArray())).as("pdf %s : %%PDF", code).isTrue();
            soft.assertThat(pdf.getHeader(HttpHeaders.CONTENT_DISPOSITION)).as("pdf %s : nom", code)
                    .contains(code + "_").contains(".pdf");
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R2 : droits et 404

    @Test
    void r2_rapportInterdit_pdf403() throws Exception {
        assertThat(appels.statut(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF).with(connecte(restreint))))
                .isEqualTo(403);
    }

    @Test
    void r2_moduleInconnu_pdf404() throws Exception {
        assertThat(appels.statut(get("/api/rapports/inconnu/" + SUIVI_GLOBAL + "/export").param(FORMAT, PDF)
                .with(connecte(libre)))).isEqualTo(404);
    }

    @Test
    void r2_codeInconnu_pdf404_avantLesDroits() throws Exception {
        assertThat(appels.statut(get(export(VENTE, "Inconnu" + prefixe)).param(FORMAT, PDF)
                .with(connecte(restreint)))).isEqualTo(404);
    }

    // ------------------------------------------------------------------ R3 : format inconnu ; absent = xlsx (§1.1)

    @Test
    void r3_formatDocx_400_messageXlsxOuPdf() throws Exception {
        MockHttpServletResponse reponse = appels.exporter(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, "docx")
                .with(connecte(libre))).getResponse();

        assertThat(reponse.getStatus()).isEqualTo(400);
        assertThat(reponse.getContentAsString()).contains(MESSAGE_FORMAT);
    }

    @Test
    void r3_rapportInterdit_formatDocx_403_leDroitDAbord() throws Exception {
        assertThat(appels.statut(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, "docx")
                .with(connecte(restreint)))).isEqualTo(403);
    }

    @Test
    void r3_formatAbsent_xlsx200() throws Exception {
        MockHttpServletResponse reponse = appels.exporter(get(export(VENTE, SUIVI_GLOBAL)).with(connecte(libre)))
                .getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        assertThat(reponse.getContentType()).isEqualTo(ExportTableur.XLSX.toString());
    }

    // ------------------------------------------------------------------ R4 : même visibilité que l'xlsx

    @Test
    void r4_utilisateurRestreintAuProjetA_memesTranchesDansPdfEtXlsx() throws Exception {
        List<String> candidats = List.of(ta.getCode(), tb.getCode(), tc.getCode());
        MockHttpServletRequestBuilder xlsx = get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, XLSX)
                .with(connecte(commercialA));

        String texteXlsx = compact(cellulesXlsx(appels.exporter(xlsx).getResponse()));
        String textePdf = compact(textePdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .with(connecte(commercialA)))));

        List<String> dansXlsx = candidats.stream().filter(texteXlsx::contains).toList();
        assertThat(dansXlsx).containsExactlyInAnyOrder(ta.getCode(), tc.getCode());
        assertThat(candidats.stream().filter(textePdf::contains).toList()).isEqualTo(dansXlsx);
    }

    @Test
    void r4_trancheMasqueeDemandee_absenteDesFiltresImprimes() throws Exception {
        String texte = compact(textePdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .param(TRANCHE_ID, ta.getId().toString(), tb.getId().toString()).with(connecte(commercialA)))));

        assertThat(texte).contains(compact("Tranches : " + ta.getDescription()))
                .doesNotContain(compact(tb.getCode()), tb.getId().toString(), ta.getId().toString());
    }

    // ------------------------------------------------------------------ R5 : totaux

    @Test
    void r5_rapportATotaux_pdfContientTotalEtLaSomme() throws Exception {
        String texte = compact(textePdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .with(connecte(commercialA)))));

        // 1 550 000 = 1 350 000 (TA) + 200 000 (TC) : n'existe que dans la ligne « Total » de la section ventes.
        assertThat(texte).contains(TOTAL).contains(compact("1 550 000,00"));
    }

    @Test
    void r5_sansTotaux_pasDeTotal() throws Exception {
        MockHttpServletRequestBuilder horsPerimetre = get(resultat(VENTE, SUIVI_GLOBAL))
                .param(TRANCHE_ID, tb.getId().toString()).with(connecte(commercialA));
        List<Map<String, Object>> sections = appels.sections(horsPerimetre);
        assertThat(sections).isNotEmpty().allSatisfy(s -> assertThat(totaux(s)).isNull());

        String texte = textePdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .param(TRANCHE_ID, tb.getId().toString()).with(connecte(commercialA))));

        assertThat(texte).doesNotContain(TOTAL);
    }

    // ------------------------------------------------------------------ R6 : plafond de 2 000 lignes

    @Test
    void r6_plusDe2000Lignes_pdf400_xlsx200() throws Exception {
        String jeton = FixtureExportPatrimoine.jeton();
        articlesPatrimoine.articlesEnMasse(articlesPatrimoine.referentiel(jeton), tp, "BLK-" + jeton + "-",
                LIGNES_PDF_MAX + 1);
        fixture.relire();

        MockHttpServletResponse pdf = appels.exporter(verification(PDF)).getResponse();
        MockHttpServletResponse xlsx = appels.exporter(verification(XLSX)).getResponse();

        assertThat(pdf.getStatus()).isEqualTo(400);
        assertThat(pdf.getContentAsString()).contains(TROP_DE_LIGNES);
        assertThat(xlsx.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequestBuilder verification(String format) {
        return get(export(PATRIMOINE, FixtureRapportsPatrimoine.VERIFICATION)).param(FORMAT, format)
                .param(TRANCHE_ID, tp.getId().toString()).with(connecte(libre));
    }

    // ------------------------------------------------------------------ R7 : largeur et page

    /** Section la plus large du registre (manager 25/10, Q1) : tous ses libellés d'en-tête, dans l'ordre. */
    @Test
    void r7_sectionLaPlusLargeDuRegistre_tousLesLibellesDEnTeteDansLeTexte() throws Exception {
        List<SectionDeRapport> toutes = new ArrayList<>();
        for (Rapport rapport : tousLesRapports()) {
            appels.sections(avecParametres(get(resultat(slug(rapport), rapport.code())), rapport)
                    .with(connecte(libre))).forEach(s -> toutes.add(new SectionDeRapport(rapport, s)));
        }
        SectionDeRapport large = toutes.stream()
                .max(Comparator.comparingInt((SectionDeRapport s) -> colonnes(s.section()).size()))
                .orElseThrow(() -> new AssertionError("aucune section dans le registre"));
        List<String> libelles = colonnes(large.section()).stream().map(c -> (String) c.get("libelle")).toList();
        String cible = "%s / %s (%d colonnes)".formatted(large.rapport().code(), large.section().get("id"),
                libelles.size());
        assertThat(libelles).as(cible).hasSizeGreaterThanOrEqualTo(COLONNES_LARGES);

        MockHttpServletResponse reponse = exporter(large.rapport(), PDF, libre);

        assertThat(reponse.getStatus()).as(cible).isEqualTo(200);
        assertThat(compact(textePdf(reponse.getContentAsByteArray())))
                .as(cible).contains(String.join("", libelles.stream().map(LectureExport::compact).toList()));
    }

    /** Témoin du moyen de lecture (moteur existant) : en-têtes longs coupés en plusieurs lignes, ordre conservé. */
    @Test
    void r7_moteur_dixNeufColonnesLongues_libellesLusDansLOrdre() throws Exception {
        List<ColonneRapport> colonnesLongues = IntStream.rangeClosed(1, 19)
                .mapToObj(i -> ColonneRapport.texte("c" + i, "Libellé de colonne numéro %02d assez long".formatted(i)))
                .toList();
        Map<String, Object> ligne = new LinkedHashMap<>();
        colonnesLongues.forEach(c -> ligne.put(c.nom(), "valeur " + c.nom()));

        byte[] contenu = exportPdf.ecrire("Titre", "Auteur", List.of(),
                List.of(new FeuilleExport("Section", colonnesLongues, List.of(ligne), null)));

        assertThat(compact(textePdf(contenu)))
                .contains(String.join("", colonnesLongues.stream().map(c -> compact(c.libelle())).toList()));
    }

    @Test
    void r7_pageA4Paysage() throws Exception {
        byte[] contenu = pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF).with(connecte(commercialA)));

        Rectangle page;
        try (PdfReader reader = new PdfReader(contenu)) {
            page = reader.getPageSizeWithRotation(1);
        }
        assertThat(page.getWidth()).isCloseTo(A4_LONG, within(2f));
        assertThat(page.getHeight()).isCloseTo(A4_COURT, within(2f));
    }

    // ------------------------------------------------------------------ R8 : descripteur

    @Test
    void r8_descripteur_formatsExport_xlsxPuisPdf_pourChaqueRapport() throws Exception {
        List<Rapport> rapports = tousLesRapports();
        assertThat(rapports).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        for (Rapport rapport : rapports) {
            List<String> formats = JsonPath.read(appels.corps(get(descripteur(slug(rapport), rapport.code()))
                    .with(connecte(libre))), "$.formatsExport");
            soft.assertThat(formats).as(rapport.code()).containsExactly(XLSX, PDF);
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R9 : titre, filtres, édition

    @Test
    void r9_titreDuPdf_egalLibelleDuRapport() throws Exception {
        List<String> pages = pagesPdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .with(connecte(commercialA))));

        assertThat(pages).isNotEmpty();
        assertThat(pages.getFirst().strip()).startsWith(libelleSuiviGlobal);
    }

    @Test
    void r9_dateDEdition_ddMMyyyy() throws Exception {
        String aujourdHui = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String premierePage = compact(pagesPdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .with(connecte(commercialA)))).getFirst());

        assertThat(premierePage).contains(aujourdHui);
    }

    @Test
    void r9_filtreTranche_libelleEtDescription_pasLUuid() throws Exception {
        String texte = compact(textePdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .param(TRANCHE_ID, ta.getId().toString()).with(connecte(commercialA)))));

        assertThat(texte).contains(compact("Tranches : " + ta.getDescription()))
                .doesNotContain(ta.getId().toString());
    }

    @Test
    void r9_parametreVide_absentDesFiltres() throws Exception {
        String texte = compact(textePdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .with(connecte(commercialA)))));

        assertThat(texte).doesNotContain(compact("Tranches :"));
    }

    @Test
    void r9_filtreBooleen_ouiNon() throws Exception {
        String texte = compact(textePdf(pdf(get(export(PATRIMOINE, FixtureRapportsPatrimoine.VERIFICATION))
                .param(FORMAT, PDF).param(TRANCHE_ID, tp.getId().toString()).param("avecSousArticles", "true")
                .with(connecte(libre))))).toLowerCase(Locale.ROOT);

        assertThat(texte).contains(compact("avec les sous-articles : oui"));
    }

    @Test
    void r9_filtreDate_ddMMyyyy() throws Exception {
        String texte = compact(textePdf(pdf(get(export(PATRIMOINE, FixtureRapportsPatrimoine.MOUVEMENTS))
                .param(FORMAT, PDF).param(TRANCHE_ID, tp.getId().toString()).param("du", "2025-03-01")
                .with(connecte(libre)))));

        assertThat(texte).contains(compact("Du : 01/03/2025")).doesNotContain("2025-03-01");
    }

    @Test
    void r9_avertissements_uneLigneChacun() throws Exception {
        List<String> avertissements = JsonPath.read(appels.corps(get(resultat(VENTE, SUIVI_GLOBAL))
                .with(connecte(commercialA))), "$.avertissements");

        String texte = compact(textePdf(pdf(get(export(VENTE, SUIVI_GLOBAL)).param(FORMAT, PDF)
                .with(connecte(commercialA)))));

        assertThat(avertissements).isNotEmpty()
                .allSatisfy(a -> assertThat(texte).contains(compact("Avertissement : " + a)));
    }

    // ------------------------------------------------------------------ outils

    private record SectionDeRapport(Rapport rapport, Map<String, Object> section) {
    }

    private List<Rapport> tousLesRapports() {
        List<Rapport> rapports = new ArrayList<>();
        for (ModuleRapport module : ModuleRapport.values()) {
            rapports.addAll(registre.duModule(module).values());
        }
        rapports.sort(Comparator.comparing((Rapport r) -> r.module().name()).thenComparing(Rapport::code));
        return rapports;
    }

    private static String slug(Rapport rapport) {
        return rapport.module().slug();
    }

    /** Tranches = TA (données bornées) ; un paramètre obligatoire sans défaut reçoit une valeur de son type. */
    private MockHttpServletRequestBuilder avecParametres(MockHttpServletRequestBuilder requete, Rapport rapport) {
        for (ParametreRapport p : rapport.parametres()) {
            if (p.type() == TypeParametre.TRANCHES) {
                requete.param(p.nom(), ta.getId().toString());
            } else if (p.obligatoire() && p.valeurParDefaut() == null) {
                String valeur = valeurObligatoire(p);
                if (valeur != null) {
                    requete.param(p.nom(), valeur);
                }
            }
        }
        return requete;
    }

    private static String valeurObligatoire(ParametreRapport p) {
        return switch (p.type()) {
            case ANNEE -> "2025";
            case DATE -> "2025-01-01";
            case ENTIER -> String.valueOf(p.min() == null ? 1 : p.min());
            case CHOIX -> p.options().isEmpty() ? null : p.options().getFirst().valeur();
            case BOOLEEN -> "false";
            case TEXTE -> "A";
            case TRANCHES, COMMERCIAUX -> null;
        };
    }

    private int nombreDeLignes(Rapport rapport) throws Exception {
        return appels.sections(avecParametres(get(resultat(slug(rapport), rapport.code())), rapport)
                .with(connecte(libre))).stream().mapToInt(s -> lignes(s).size()).sum();
    }

    private MockHttpServletResponse exporter(Rapport rapport, String format, AdmUser qui) throws Exception {
        RequestPostProcessor session = connecte(qui);
        return appels.exporter(avecParametres(get(export(slug(rapport), rapport.code())), rapport)
                .param(FORMAT, format).with(session)).getResponse();
    }

    /** PDF exigé (200, %PDF). */
    private byte[] pdf(MockHttpServletRequestBuilder requete) throws Exception {
        MockHttpServletResponse reponse = appels.exporter(requete).getResponse();
        assertThat(reponse.getStatus()).as("statut du PDF").isEqualTo(200);
        byte[] contenu = reponse.getContentAsByteArray();
        assertThat(estPdf(contenu)).isTrue();
        return contenu;
    }

    private static String cellulesXlsx(MockHttpServletResponse reponse) throws IOException {
        assertThat(reponse.getStatus()).isEqualTo(200);
        StringBuilder texte = new StringBuilder();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            for (Sheet feuille : classeur) {
                for (Row ligne : feuille) {
                    for (Cell cellule : ligne) {
                        texte.append(Objects.toString(cellule, "")).append(' ');
                    }
                }
            }
        }
        return texte.toString();
    }
}
