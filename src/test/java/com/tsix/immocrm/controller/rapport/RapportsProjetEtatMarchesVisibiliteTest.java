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
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requeteExport;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * R6 (FEATURE-n5b-c-etat-marches.md) : visibilité, droits, export, cohérence avec N5a. Type T / catégorie X : lot LA de PA1
 * (habilité, estimation et marché 1200 TTC) et lot LBX de PB (non habilité, 600 TTC : visible, il ferait 1800) ; lot LBH de
 * PB seul en catégorie H. Chaque refus a son témoin 200 (sans lui, le 404 du rapport inconnu passerait).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetEtatMarchesVisibiliteTest {

    private static final String ETAT = "PrjMarketTresorie";
    private static final String TYPE_LOT = "typeLot";
    private static final String CATEGORIE = "categorie";
    private static final String MARCHE = "marche";
    private static final String ESTIMATION_BASE = "estimationBase";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");
    private static final BigDecimal DOUZE_CENTS = new BigDecimal("1200");
    private static final int OK = 200;
    private static final int REFUS = 400;
    private static final int INTERDIT = 403;
    private static final String XLSX = "xlsx";
    private static final String X = "X";
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
    private UUID rapportId;
    private String libelleType;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
        rapportId = rapports.rapport(FixtureRapportsProjet.TYPE_PROJET, ETAT, "BILAN", "PROJET", 7);
        rapports.relire();
        s = new RapportsProjetBilanSupport(c, achat, fixture, parite, bilans, lots, jdbc, em);
        libelleType = "TYPE " + c.jeton();
        UUID type = achat.typeLot(c.code("T"), libelleType);
        UUID x = s.categorie(X, null, 1);
        UUID h = s.categorie(H, null, 2);
        lotType(c.pa1(), "LA", type, x, "1000");
        lotType(c.pb(), "LBX", type, x, "500");
        lotType(c.pb(), "LBH", type, h, "500");
    }

    /** Lot du type et de la catégorie donnés : estimation de base et bordereau de {@code ht} HT (TTC = × 1,2). */
    private void lotType(UUID projet, String code, UUID type, UUID categorie, String ht) {
        UUID lot = s.lot(projet, code);
        typer(lot, type, categorie);
        s.estimationBase(lot, ht);
        bilans.ligneDefinitive(lot, c.code(code + "B"), "1", ht);
    }

    private void typer(UUID lot, UUID type, UUID categorie) {
        em.flush();
        jdbc.update("UPDATE PrjMarket SET PrjMarketTypeId = ?, StlCategoryId = ? WHERE PKey = ?",
                type == null ? null : type.toString(), categorie.toString(), lot.toString());
        em.clear();
    }

    private Map<String, Object> etat(UUID... projets) throws Exception {
        return premiere(appels.sections(requete(ETAT, c.compte(), projets)));
    }

    private List<Object> categories(Map<String, Object> section) {
        return AppelsRapports.lignes(section).stream().map(l -> l.get(CATEGORIE)).toList();
    }

    private Map<String, Object> ligneX(Map<String, Object> section) {
        List<Map<String, Object>> l = AppelsRapports.lignesOu(section, CATEGORIE, s.libelle(X));
        assertThat(l).as("ligne X").hasSize(1);
        return l.getFirst();
    }

    private void verifierSeulementPa1(Map<String, Object> r) {
        Map<String, Object> ligne = ligneX(r);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(ligne).as("type").containsEntry(TYPE_LOT, libelleType);
        sa.assertThat(montant(ligne, MARCHE)).as("marché PA1 seul").isCloseTo(DOUZE_CENTS, within(PRECISION));
        sa.assertThat(montant(ligne, ESTIMATION_BASE)).as("estimation PA1 seul").isCloseTo(DOUZE_CENTS, within(PRECISION));
        sa.assertThat(categories(r)).as("catégorie H de PB").doesNotContain(s.libelle(H));
        sa.assertAll();
    }

    private MockHttpServletResponse exporter(String format, AdmUser compte, UUID... projets) throws Exception {
        return appels.exporter(requeteExport(ETAT, format, compte, projets)).getResponse();
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

    // ------------------------------------------------------------------ R6a : visibilité

    @Test
    void r6a_resultat_projetHorsPerimetreDemande_ignore_absentDesMontants() throws Exception {
        verifierSeulementPa1(etat(c.pa1(), c.pb()));
    }

    @Test
    void r6a_resultat_sansProjets_lotHorsPerimetreAbsent() throws Exception {
        verifierSeulementPa1(etat());
    }

    @Test
    void r6a_resultat_seulementProjetHorsPerimetre_200_aucuneLigne() throws Exception {
        int statut = appels.statut(requete(ETAT, c.compte(), c.pb()));
        Map<String, Object> r = etat(c.pb());

        assertThat(statut).as("jamais 403").isEqualTo(OK);
        assertThat(AppelsRapports.lignes(r)).as("aucune ligne").isEmpty();
    }

    @Test
    void r6a_r6c_export_memesLignes_horsPerimetreIgnore() throws Exception {
        MockHttpServletResponse mixte = exporter(XLSX, c.compte(), c.pa1(), c.pb());
        MockHttpServletResponse horsSeul = exporter(XLSX, c.compte(), c.pb());

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(mixte.getStatus()).as("PA1 + PB").isEqualTo(OK);
        sa.assertThat(horsSeul.getStatus()).as("PB seul").isEqualTo(OK);
        sa.assertAll();
        assertThat(cellules(mixte)).contains(libelleType, s.libelle(X)).doesNotContain(s.libelle(H));
        assertThat(cellules(horsSeul)).doesNotContain(libelleType, s.libelle(X), s.libelle(H));
    }

    // ------------------------------------------------------------------ R6b : droits

    @Test
    void r6b_roleRestreintSansLien_403_resultatEtExport_temoinLibre200() throws Exception {
        AdmUser restreint = comptes.compte("N5CR-" + c.jeton(), true, c.societe(), c.pa1());

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(appels.statut(requete(ETAT, c.compte(), c.pa1()))).as("témoin résultat").isEqualTo(OK);
        sa.assertThat(exporter(XLSX, c.compte(), c.pa1()).getStatus()).as("témoin export").isEqualTo(OK);
        sa.assertThat(appels.statut(requete(ETAT, restreint, c.pa1()))).as("résultat").isEqualTo(INTERDIT);
        sa.assertThat(exporter(XLSX, restreint, c.pa1()).getStatus()).as("export").isEqualTo(INTERDIT);
        sa.assertAll();
    }

    @Test
    void r6b_roleRestreintLieAuRapport_200() throws Exception {
        AdmUser restreint = comptes.compte("N5CL-" + c.jeton(), true, c.societe(), c.pa1());
        comptes.lier(restreint, rapportId);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(appels.statut(requete(ETAT, restreint, c.pa1()))).as("résultat, lié").isEqualTo(OK);
        sa.assertThat(exporter(XLSX, restreint, c.pa1()).getStatus()).as("export, lié").isEqualTo(OK);
        sa.assertAll();
    }

    // ------------------------------------------------------------------ R6c : export

    @Test
    void r6c_export_xlsx_200_pdf_400() throws Exception {
        MockHttpServletResponse xlsx = exporter(XLSX, c.compte(), c.pa1());
        int pdf = exporter("pdf", c.compte(), c.pa1()).getStatus();

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(xlsx.getStatus()).as("témoin xlsx").isEqualTo(OK);
        sa.assertThat(xlsx.getContentType()).as("xlsx").isEqualTo(ExportTableur.XLSX.toString());
        sa.assertThat(pdf).as("pdf").isEqualTo(REFUS);
        sa.assertAll();
    }

    // ------------------------------------------------------------------ R6d : cohérence avec N5a

    /** Lot non résilié, catégorie du lot = catégorie de ventilation (taux 1), bordereau 2 × 500 HT = 1200 TTC. */
    @Test
    void r6d_marcheLotNonResilie_egalMarcheAjusteTtcDesBilansN5a() throws Exception {
        bilans.preparer();
        UUID projet = bilans.projet("R6D", false);
        UUID d = bilans.categorie(s.libelle("D"), null, 1);
        UUID lot = bilans.lot("R6D", projet, false);
        typer(lot, null, d);
        bilans.ventiler(lot, d, "1");
        bilans.ligneDefinitive(lot, "R6DB-" + c.jeton(), "2", "500");
        AdmUser compte = comptes.compte("N5CD-" + c.jeton(), false, bilans.societe(), projet);
        AdmUser lecteur = bilans.compte("r6d", true, projet);

        List<Map<String, Object>> l = AppelsRapports.lignesOu(premiere(appels.sections(requete(ETAT, compte, projet))),
                CATEGORIE, s.libelle("D"));
        String n5a = appels.corps(get(FixtureBilansProjet.BILANS).with(connecte(lecteur, "Commercial"))
                .param("projets", projet.toString()).param("ttc", "true"));
        List<Object> valeurs = JsonPath.read(n5a, "$..[?(@.categorieId == '" + d + "')].valeurs.marcheAjuste");

        assertThat(l).as("ligne D").hasSize(1);
        assertThat(valeurs).as("N5a marcheAjuste").hasSize(1);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(montant(l.getFirst(), MARCHE)).as("non nul").isCloseTo(DOUZE_CENTS, within(PRECISION));
        sa.assertThat(montant(l.getFirst(), MARCHE)).as("R6d marche = marcheAjuste TTC N5a")
                .isCloseTo(new BigDecimal(String.valueOf(valeurs.getFirst())), within(PRECISION));
        sa.assertAll();
    }
}
