package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import com.tsix.immocrm.web.export.ExportTableur;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.BILAN;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.CATEGORIE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.ESTIMATION_BASE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.ESTIMATION_CONFIDENTIELLE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.MARCHE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.SUIVI;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.FORMAT;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.PROJET;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requeteExport;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * R3 (FEATURE-n5b-b-rapports-projet.md) : visibilité, droits, exports, cohérence avec N5a. Catégorie V portée par le lot LV
 * de PA1 (habilité, 1200 TTC) ; le lot LH de PB (non habilité) est ventilé V 0,5 / H 0,5 (1200 TTC) : visible, il ferait
 * V = 1800 et ferait apparaître H. Chaque refus a son témoin 200 (sans lui, le 404 du rapport inconnu passerait).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetBilanVisibiliteTest {

    private static final BigDecimal PRECISION = new BigDecimal("0.01");
    private static final int OK = 200;
    private static final int REFUS = 400;
    private static final int INTERDIT = 403;
    private static final String XLSX = "xlsx";
    private static final String AN = "2025";
    private static final String V = "V";
    private static final String H = "H";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureBilansProjet bilans;
    @Autowired private FixtureProjetLots lots;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private AppelsRapports appels;
    private Cadre c;
    private RapportsProjetBilanSupport s;
    private Map<String, UUID> ids;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
        ids = RapportsProjetBilanSupport.rapports(rapports);
        s = new RapportsProjetBilanSupport(c, achat, fixture, parite, bilans, lots, jdbc, em);
        UUID v = s.categorie(V, null, 1);
        UUID h = s.categorie(H, null, 2);
        UUID lv = s.lot(c.pa1(), "LV");
        s.ventiler(lv, v, "1");
        s.estimationBase(lv, "1000");
        s.decompte(lv, "LVD", 1, "100", LocalDate.of(2025, 3, 1));
        UUID lh = s.lot(c.pb(), "LH");
        s.ventiler(lh, v, "0.5");
        s.ventiler(lh, h, "0.5");
        s.estimationBase(lh, "1000");
        s.decompte(lh, "LHD", 1, "900", LocalDate.of(2025, 3, 1));
    }

    private Map<String, Object> bilan(UUID... projets) throws Exception {
        return premiere(appels.sections(requete(BILAN, c.compte(), projets)));
    }

    private List<Object> libelles(Map<String, Object> section) {
        return AppelsRapports.lignes(section).stream().map(l -> l.get(CATEGORIE)).toList();
    }

    private BigDecimal estimationV(Map<String, Object> section) {
        List<Map<String, Object>> l = AppelsRapports.lignesOu(section, CATEGORIE, s.libelle(V));
        assertThat(l).as("ligne V").hasSize(1);
        return montant(l.getFirst(), ESTIMATION_BASE);
    }

    private MockHttpServletRequestBuilder suivi(String route, AdmUser compte, UUID projet) {
        MockHttpServletRequestBuilder r = get(route).with(connecte(compte, FixtureRapportsAchat.role())).param(ANNEE, AN);
        if (projet != null) {
            r.param(PROJET, projet.toString());
        }
        return r;
    }

    private MockHttpServletResponse exporterBilan(String format, AdmUser compte, UUID... projets) throws Exception {
        return appels.exporter(requeteExport(BILAN, format, compte, projets)).getResponse();
    }

    private MockHttpServletResponse exporterSuivi(String format, AdmUser compte, UUID projet) throws Exception {
        return appels.exporter(suivi(AppelsRapports.export(PROJET, SUIVI), compte, projet).param(FORMAT, format)).getResponse();
    }

    private int resultatSuivi(AdmUser compte, UUID projet) throws Exception {
        return appels.statut(suivi(AppelsRapports.resultat(PROJET, SUIVI), compte, projet));
    }

    /** Texte de toutes les cellules du classeur. */
    private static List<String> cellules(MockHttpServletResponse reponse) throws Exception {
        List<String> textes = new ArrayList<>();
        DataFormatter format = new DataFormatter();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            for (Sheet feuille : classeur) {
                for (Row rangee : feuille) {
                    for (Cell cellule : rangee) {
                        textes.add(format.formatCellValue(cellule));
                    }
                }
            }
        }
        return textes;
    }

    // ------------------------------------------------------------------ R3a : visibilité

    @Test
    void r3a_bilan_projetHorsPerimetreDemande_ignore_absentDesMontants() throws Exception {
        Map<String, Object> r = bilan(c.pa1(), c.pb());

        assertThat(estimationV(r)).isCloseTo(new BigDecimal("1200"), within(PRECISION));
        assertThat(libelles(r)).doesNotContain(s.libelle(H));
    }

    @Test
    void r3a_bilan_sansProjets_lotHorsPerimetreAbsent() throws Exception {
        Map<String, Object> r = bilan();

        assertThat(estimationV(r)).isCloseTo(new BigDecimal("1200"), within(PRECISION));
        assertThat(libelles(r)).doesNotContain(s.libelle(H));
    }

    @Test
    void r3a_bilan_seulementProjetHorsPerimetre_200_aucuneLigneDuTest() throws Exception {
        assertThat(libelles(bilan(c.pb()))).doesNotContain(s.libelle(V), s.libelle(H));
    }

    @Test
    void r3a_r3c_bilan_export_memesLignes_horsPerimetreIgnore() throws Exception {
        MockHttpServletResponse mixte = exporterBilan(XLSX, c.compte(), c.pa1(), c.pb());
        MockHttpServletResponse horsSeul = exporterBilan(XLSX, c.compte(), c.pb());

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(mixte.getStatus()).as("PA1 + PB").isEqualTo(OK);
        sa.assertThat(horsSeul.getStatus()).as("PB seul").isEqualTo(OK);
        sa.assertAll();
        assertThat(cellules(mixte)).contains(s.libelle(V)).doesNotContain(s.libelle(H));
        assertThat(cellules(horsSeul)).doesNotContain(s.libelle(V), s.libelle(H));
    }

    @Test
    void r3a_suivi_export_sansProjetOuHorsPerimetre_400() throws Exception {
        int temoin = exporterSuivi(XLSX, c.compte(), c.pa1()).getStatus();
        int sans = exporterSuivi(XLSX, c.compte(), null).getStatus();
        int hors = exporterSuivi(XLSX, c.compte(), c.pb()).getStatus();

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(temoin).as("témoin xlsx PA1").isEqualTo(OK);
            sa.assertThat(sans).as("sans projet").isEqualTo(REFUS);
            sa.assertThat(hors).as("seulement PB").isEqualTo(REFUS);
        });
    }

    // ------------------------------------------------------------------ R3b : droits

    @Test
    void r3b_roleRestreintSansLien_403_resultatsEtExports_temoinLibre200() throws Exception {
        AdmUser restreint = comptes.compte("N5R-" + c.jeton(), true, c.societe(), c.pa1());

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(appels.statut(requete(BILAN, c.compte(), c.pa1()))).as("témoin bilan").isEqualTo(OK);
        sa.assertThat(resultatSuivi(c.compte(), c.pa1())).as("témoin suivi").isEqualTo(OK);
        sa.assertThat(appels.statut(requete(BILAN, restreint, c.pa1()))).as("bilan").isEqualTo(INTERDIT);
        sa.assertThat(resultatSuivi(restreint, c.pa1())).as("suivi").isEqualTo(INTERDIT);
        sa.assertThat(exporterBilan(XLSX, restreint, c.pa1()).getStatus()).as("export bilan").isEqualTo(INTERDIT);
        sa.assertThat(exporterSuivi(XLSX, restreint, c.pa1()).getStatus()).as("export suivi").isEqualTo(INTERDIT);
        sa.assertAll();
    }

    @Test
    void r3b_roleRestreintLieAuBilan_bilan200_suivi403() throws Exception {
        AdmUser restreint = comptes.compte("N5L-" + c.jeton(), true, c.societe(), c.pa1());
        comptes.lier(restreint, ids.get(BILAN));

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(appels.statut(requete(BILAN, restreint, c.pa1()))).as("bilan, lié").isEqualTo(OK);
        sa.assertThat(resultatSuivi(restreint, c.pa1())).as("suivi, non lié").isEqualTo(INTERDIT);
        sa.assertAll();
    }

    // ------------------------------------------------------------------ R3c : exports

    @Test
    void r3c_export_xlsx_200_classeur_lesDeuxRapports() throws Exception {
        MockHttpServletResponse b = exporterBilan(XLSX, c.compte(), c.pa1());
        MockHttpServletResponse m = exporterSuivi(XLSX, c.compte(), c.pa1());

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(b.getStatus()).as("bilan").isEqualTo(OK);
        sa.assertThat(b.getContentType()).as("bilan").isEqualTo(ExportTableur.XLSX.toString());
        sa.assertThat(m.getStatus()).as("suivi").isEqualTo(OK);
        sa.assertThat(m.getContentType()).as("suivi").isEqualTo(ExportTableur.XLSX.toString());
        sa.assertAll();
        assertThat(cellules(m)).as("12 mois").contains("Janvier", "Décembre");
    }

    @Test
    void r3c_export_pdf_400_temoinXlsx200() throws Exception {
        int bilanXlsx = exporterBilan(XLSX, c.compte(), c.pa1()).getStatus();
        int bilanPdf = exporterBilan("pdf", c.compte(), c.pa1()).getStatus();
        int suiviXlsx = exporterSuivi(XLSX, c.compte(), c.pa1()).getStatus();
        int suiviPdf = exporterSuivi("pdf", c.compte(), c.pa1()).getStatus();

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(bilanXlsx).as("témoin bilan xlsx").isEqualTo(OK);
            sa.assertThat(bilanPdf).as("bilan pdf").isEqualTo(REFUS);
            sa.assertThat(suiviXlsx).as("témoin suivi xlsx").isEqualTo(OK);
            sa.assertThat(suiviPdf).as("suivi pdf").isEqualTo(REFUS);
        });
    }

    // ------------------------------------------------------------------ R3d : cohérence avec N5a

    /** Lot non résilié ventilé D1 0,6 / D2 0,4 sous D : estimation 1000 HT, ligne d'estimation 500 HT, devis retenu 2400 TTC. */
    @Test
    void r3d_estimationsEtMarche_egalesAuxColonnesTtcDesBilansN5a() throws Exception {
        bilans.preparer();
        UUID projet = bilans.projet("R3D", false);
        UUID d = bilans.categorie(s.libelle("D"), null, 1);
        UUID d1 = bilans.categorie(s.libelle("D1"), d, 1);
        UUID d2 = bilans.categorie(s.libelle("D2"), d, 2);
        UUID lot = bilans.lot("R3D", projet, false);
        bilans.ventiler(lot, d1, "0.6");
        bilans.ventiler(lot, d2, "0.4");
        bilans.estimationBase(lot, "1000");
        bilans.ligneEstimation(lot, "R3DE-" + c.jeton(), "1", "500");
        fixture.marche(lot, "EstimatedPriceTTC", new BigDecimal("600"));
        bilans.devis(lot, projet, "R3DV", "2400", LocalDate.of(2026, 2, 10), true);
        AdmUser compte = comptes.compte("N5D-" + c.jeton(), false, bilans.societe(), projet);
        AdmUser lecteur = bilans.compte("r3d", true, projet);

        List<Map<String, Object>> l = AppelsRapports.lignesOu(premiere(appels.sections(requete(BILAN, compte, projet))),
                CATEGORIE, s.libelle("D"));
        String n5a = appels.corps(get(FixtureBilansProjet.BILANS).with(connecte(lecteur, "Commercial"))
                .param("projets", projet.toString()).param("ttc", "true"));

        assertThat(l).as("ligne D").hasSize(1);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(montant(l.getFirst(), ESTIMATION_BASE)).as("non nul").isCloseTo(new BigDecimal("1200"), within(PRECISION));
        for (String colonne : List.of(ESTIMATION_BASE, ESTIMATION_CONFIDENTIELLE, MARCHE)) {
            List<Object> valeurs = JsonPath.read(n5a, "$..[?(@.categorieId == '" + d + "')].valeurs." + colonne);
            sa.assertThat(valeurs).as("N5a %s", colonne).hasSize(1);
            if (valeurs.size() == 1) {
                sa.assertThat(montant(l.getFirst(), colonne)).as("R1 %s = N5a TTC", colonne)
                        .isCloseTo(new BigDecimal(String.valueOf(valeurs.getFirst())), within(PRECISION));
            }
        }
        sa.assertAll();
    }
}
