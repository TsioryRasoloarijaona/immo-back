package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.MESSAGE_SANS_PROJET;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.codes;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requeteExport;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.DEC;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.PRORATA;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.RETENUES;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.TRC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R5b visibilité, R5c droits, R5d exports des 4 rapports Projet (FEATURE-n5b-rapports-projet.md). Lot « LV » dans PA1
 * (habilité) et lot « LH » dans PB (non habilité), chacun présent dans les 4 rapports s'il était visible.
 * Chaque refus a son témoin 200 : sans lui, le 404 du module inconnu passerait pour le refus attendu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetDroitsTest {

    private static final int OK = 200;
    private static final int REFUS = 400;
    private static final int INTERDIT = 403;
    private static final String XLSX = "xlsx";
    private static final String LV = "LV";
    private static final String LH = "LH";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;

    private AppelsRapports appels;
    private Cadre c;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
        lotComplet(c.pa1(), LV);
        lotComplet(c.pb(), LH);
    }

    /** Lot à RG facturée 80, DEC 30, TRC 20, ratios DEC / TRC / prorata à 1 % : il entre dans les 4 rapports. */
    private void lotComplet(UUID projet, String code) {
        UUID lot = parite.lot(projet, c.code(code), ENTREPRENEURS, c.statutMono());
        fixture.fournisseur(lot, c.fournisseur());
        fiche.devisRetenu(lot, c.code(code) + "-Q", "1000", "1200");
        BigDecimal ratio = new BigDecimal("0.01");
        fixture.marche(lot, "InsuranceDECRatio", ratio);
        fixture.marche(lot, "InsuranceTRCRatio", ratio);
        fixture.marche(lot, "ProportionRatio", ratio);
        fixture.decompte(lot, c.code(code) + "-D", 1, "80", "10", "0", LocalDate.of(2026, 3, 5));
        fixture.facture(lot, c.code(code) + "-F", "80", "30", "20");
    }

    /** Codes de lot de toutes les sections. */
    private List<Object> codesLots(MockHttpServletRequestBuilder requete) throws Exception {
        List<Object> tous = new ArrayList<>();
        for (Map<String, Object> section : appels.sections(requete)) {
            tous.addAll(codes(section));
        }
        return tous;
    }

    private String detail(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andReturn().getResponse().getContentAsString();
        return corps.isBlank() ? null : JsonPath.read(corps, "$.detail");
    }

    private MockHttpServletResponse exporter(String code, String format, AdmUser compte, UUID... projets) throws Exception {
        return appels.exporter(requeteExport(code, format, compte, projets)).getResponse();
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

    // ------------------------------------------------------------------ R5b : projets obligatoires (R1-R3), périmètre

    @ParameterizedTest
    @ValueSource(strings = {RETENUES, DEC, TRC})
    void r5b_sansProjet_400_veuillezSelectionner(String code) throws Exception {
        int temoin = appels.statut(requete(code, c.compte(), c.pa1()));
        int statut = appels.statut(requete(code, c.compte()));
        String message = detail(requete(code, c.compte()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin).as("témoin avec PA1").isEqualTo(OK);
            s.assertThat(statut).isEqualTo(REFUS);
            s.assertThat(message).isEqualTo(MESSAGE_SANS_PROJET);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {RETENUES, DEC, TRC})
    void r5b_seulementProjetHorsPerimetre_400_memeMessage(String code) throws Exception {
        int temoin = appels.statut(requete(code, c.compte(), c.pa1()));
        int statut = appels.statut(requete(code, c.compte(), c.pb()));
        String message = detail(requete(code, c.compte(), c.pb()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin).as("témoin avec PA1").isEqualTo(OK);
            s.assertThat(statut).isEqualTo(REFUS);
            s.assertThat(message).isEqualTo(MESSAGE_SANS_PROJET);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {RETENUES, DEC, TRC, PRORATA})
    void r5b_projetHorsPerimetreDemande_ignore_seulsLesLotsHabilites(String code) throws Exception {
        assertThat(codesLots(requete(code, c.compte(), c.pa1(), c.pb())))
                .contains(c.code(LV)).doesNotContain(c.code(LH));
    }

    @Test
    void r5b_prorataSansProjet_projetsHabilitesSeulement() throws Exception {
        assertThat(codesLots(requete(PRORATA, c.compte()))).contains(c.code(LV)).doesNotContain(c.code(LH));
    }

    @Test
    void r5b_prorataSeulementProjetHorsPerimetre_aucunLotDeCeProjet() throws Exception {
        int statut = appels.statut(requete(PRORATA, c.compte(), c.pb()));

        assertThat(statut).as("400 ou intersection vide").isIn(OK, REFUS);
        if (statut == OK) {
            assertThat(codesLots(requete(PRORATA, c.compte(), c.pb()))).doesNotContain(c.code(LH));
        }
    }

    // ------------------------------------------------------------------ R5b sur les exports

    @ParameterizedTest
    @ValueSource(strings = {RETENUES, DEC, TRC})
    void r5b_export_sansProjetOuHorsPerimetre_400(String code) throws Exception {
        int temoin = exporter(code, XLSX, c.compte(), c.pa1()).getStatus();
        int sans = exporter(code, XLSX, c.compte()).getStatus();
        int hors = exporter(code, XLSX, c.compte(), c.pb()).getStatus();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin).as("témoin xlsx PA1").isEqualTo(OK);
            s.assertThat(sans).as("sans projet").isEqualTo(REFUS);
            s.assertThat(hors).as("seulement PB").isEqualTo(REFUS);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {RETENUES, DEC, TRC, PRORATA})
    void r5b_r5d_export_memesLots_projetHorsPerimetreIgnore(String code) throws Exception {
        MockHttpServletResponse reponse = exporter(code, XLSX, c.compte(), c.pa1(), c.pb());

        assertThat(reponse.getStatus()).isEqualTo(OK);
        assertThat(cellules(reponse)).contains(c.code(LV)).doesNotContain(c.code(LH));
    }

    @Test
    void r5b_export_prorataSansProjet_projetsHabilitesSeulement() throws Exception {
        MockHttpServletResponse reponse = exporter(PRORATA, XLSX, c.compte());

        assertThat(reponse.getStatus()).isEqualTo(OK);
        assertThat(cellules(reponse)).contains(c.code(LV)).doesNotContain(c.code(LH));
    }

    // ------------------------------------------------------------------ R5c : droits

    @Test
    void r5c_roleRestreintSansLien_403_temoinLibre200() throws Exception {
        AdmUser restreint = comptes.compte("N5R-" + c.jeton(), true, c.societe(), c.pa1());

        SoftAssertions s = new SoftAssertions();
        for (String code : FixtureRapportsProjet.CODES) {
            s.assertThat(appels.statut(requete(code, c.compte(), c.pa1()))).as("témoin libre %s", code).isEqualTo(OK);
            s.assertThat(appels.statut(requete(code, restreint, c.pa1()))).as("résultat %s", code).isEqualTo(INTERDIT);
            s.assertThat(exporter(code, XLSX, restreint, c.pa1()).getStatus()).as("export %s", code).isEqualTo(INTERDIT);
        }
        s.assertAll();
    }

    @Test
    void r5c_decTrc_refusesARoleRestreint_lieAuxDeuxAutres() throws Exception {
        AdmUser restreint = comptes.compte("N5L-" + c.jeton(), true, c.societe(), c.pa1());
        comptes.lier(restreint, c.ids().get(RETENUES));
        comptes.lier(restreint, c.ids().get(PRORATA));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(appels.statut(requete(RETENUES, restreint, c.pa1()))).as("retenues, lié").isEqualTo(OK);
        s.assertThat(appels.statut(requete(PRORATA, restreint, c.pa1()))).as("prorata, lié").isEqualTo(OK);
        s.assertThat(appels.statut(requete(DEC, restreint, c.pa1()))).as("DEC").isEqualTo(INTERDIT);
        s.assertThat(appels.statut(requete(TRC, restreint, c.pa1()))).as("TRC").isEqualTo(INTERDIT);
        s.assertAll();
    }

    // ------------------------------------------------------------------ R5d : exports

    @ParameterizedTest
    @ValueSource(strings = {RETENUES, DEC, TRC, PRORATA})
    void r5d_export_xlsx_200_classeur(String code) throws Exception {
        MockHttpServletResponse reponse = exporter(code, XLSX, c.compte(), c.pa1());

        assertThat(reponse.getStatus()).isEqualTo(OK);
        assertThat(reponse.getContentType()).isEqualTo(ExportTableur.XLSX.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {RETENUES, DEC, TRC, PRORATA})
    void r5d_export_pdf_400_temoinXlsx200(String code) throws Exception {
        int xlsx = exporter(code, XLSX, c.compte(), c.pa1()).getStatus();
        int pdf = exporter(code, "pdf", c.compte(), c.pa1()).getStatus();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(xlsx).as("témoin xlsx").isEqualTo(OK);
            s.assertThat(pdf).as("pdf").isEqualTo(REFUS);
        });
    }
}
