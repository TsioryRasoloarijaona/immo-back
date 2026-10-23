package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.Categories;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.ROLE_DG;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.avecRole;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Export Excel d'une étude — FEATURE-patrimoine-faisabilite-workflow.md §7 et décision 1 (22/10) : 11 feuilles nommées, en-têtes,
 * une ligne de chaque feuille rapprochée des valeurs stockées (bilan, crédit, ligne d'échéancier dans « Données »), formats,
 * nom de fichier, étude bloquée exportable. Les positions de cellule ne sont pas figées : on cherche la valeur dans la feuille.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExportFaisabiliteTest {

    private static final List<String> FEUILLES = List.of("Identité", "Bilan", "Terrain", "Exécution", "Délais", "Financement",
            "Données", "Unités", "Tranches", "Unités par tranche", "Bilan détaillé");
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final double TOLERANCE = 0.00001;
    private static final DataFormatter FORMAT = new DataFormatter(Locale.FRANCE);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;

    private AdmUser admin;
    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;
    private Categories cat;
    private UUID etude;
    private Workbook classeur;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        admin = patrimoine.administrateur();
        api = new ApiFaisabilite(mockMvc, admin);
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
        cat = finance.categoriesDeTest(jeton);
        etude = etudeComplete();
    }

    @AfterEach
    void fermer() throws IOException {
        if (classeur != null) {
            classeur.close();
        }
    }

    private String nom(String prefixe) {
        return prefixe + "-" + jeton;
    }

    private CorpsJson ficheAvecPhases(String code) {
        return phases(yestest(fiche(code, societe.getId())), "2026-01-01", 0, 0, 0)
                .avec(ECART_TYPE_TRAVAUX, d("1")).avec(ECART_TYPE_CONCRETISATION, d("1"))
                .avec(ECART_TYPE_FINALISATION, d("1")).avec(DELAI_RETOUR, 37);
    }

    /**
     * Unité « U » de 100 ventilée sur la tranche « TR1 » (poids 1) ; « Gros oeuvres » 50 × 100 à TVA 0,2 (5 000 HT, 6 000 TTC) ;
     * un crédit de 2 000 au taux 0,1 remboursé 1 234 + 766 (à régler 1 357,4 et 842,6, total 2 200).
     */
    private UUID etudeComplete() throws Exception {
        UUID id = api.etude(ficheAvecPhases(nom("EXP")));
        UUID unite = api.unite(nom("U"), false, null);
        api.unites(id, lignes(ligneUniteCorps(1, unite, d("100")))).andExpect(status().isOk());
        UUID ligne = fixture.ligneUnite(id, unite);
        UUID tranche = patrimoine.tranche(nom("CT"), patrimoine.projet(nom("PRJ"), societe)).getId();
        api.tranches(id, lignes(tranche(ligneTrancheCorps(nom("TR1"), 1, tranche, d("1")), ventilation(ligne, d("100")))))
                .andExpect(status().isOk());
        api.remplacer(PREVISIONS, id, lignes(parUnite(cat.grosOeuvres(), ligne, "50", "0.2"))).andExpect(status().isOk());
        api.remplacer(FINANCEMENTS, id, lignes(credit("2026-02-01", "2000", 2, 1, 0, "0.1").avec(DESCRIPTION, nom("CRED"))
                .avec(REMBOURSEMENTS, List.of(remboursement("2026-02-01", "1234", "0.1"),
                        remboursement("2026-03-01", "766", "0.1"))))).andExpect(status().isOk());
        return id;
    }

    private MockHttpServletResponse telecharger(UUID id) throws Exception {
        return mockMvc.perform(get(EXPORT, id).with(connecte(admin))).andExpect(status().isOk()).andReturn().getResponse();
    }

    private Workbook classeur(UUID id) throws Exception {
        classeur = new XSSFWorkbook(new ByteArrayInputStream(telecharger(id).getContentAsByteArray()));
        return classeur;
    }

    private Sheet feuille(int index) throws Exception {
        return classeur(etude).getSheetAt(index);
    }

    // ------------------------------------------------------------------ lecture des feuilles

    private static List<Row> rangees(Sheet feuille) {
        return StreamSupport.stream(feuille.spliterator(), false).toList();
    }

    private static List<Cell> cellules(Row ligne) {
        return StreamSupport.stream(ligne.spliterator(), false).toList();
    }

    private static String texte(Cell c) {
        return FORMAT.formatCellValue(c);
    }

    private static boolean estNombre(Cell c, double attendu) {
        return c.getCellType() == CellType.NUMERIC && Math.abs(c.getNumericCellValue() - attendu) < TOLERANCE;
    }

    private static boolean contientNombre(Row ligne, BigDecimal attendu) {
        return cellules(ligne).stream().anyMatch(c -> estNombre(c, attendu.doubleValue()));
    }

    private static Optional<Cell> celluleNombre(Sheet feuille, BigDecimal attendu) {
        return rangees(feuille).stream().flatMap(l -> cellules(l).stream())
                .filter(c -> estNombre(c, attendu.doubleValue())).findFirst();
    }

    private static boolean contientTexte(Sheet feuille, String attendu) {
        return rangees(feuille).stream().flatMap(l -> cellules(l).stream())
                .anyMatch(c -> texte(c).trim().equalsIgnoreCase(attendu));
    }

    /** En-tête : une cellule dont le texte contient {@code attendu}, casse ignorée. */
    private static boolean contientEnTete(Sheet feuille, String attendu) {
        String cherche = attendu.toLowerCase(Locale.ROOT);
        return rangees(feuille).stream().flatMap(l -> cellules(l).stream())
                .anyMatch(c -> texte(c).toLowerCase(Locale.ROOT).contains(cherche));
    }

    private static Row ligneDe(Sheet feuille, String texteCellule) {
        List<Row> trouvees = rangees(feuille).stream()
                .filter(l -> cellules(l).stream().anyMatch(c -> texte(c).trim().equals(texteCellule))).toList();
        assertThat(trouvees).as("ligne « %s » de la feuille %s", texteCellule, feuille.getSheetName()).hasSize(1);
        return trouvees.get(0);
    }

    private static int indentation(Row ligne, String libelle) {
        return cellules(ligne).stream().map(ExportFaisabiliteTest::texte).filter(t -> t.trim().equals(libelle))
                .map(t -> t.length() - t.stripLeading().length()).findFirst().orElse(-1);
    }

    private BigDecimal stocke(String colonne) {
        Tuple e = fixture.etude(etude);
        return decimal(e.get(colonne));
    }

    // ------------------------------------------------------------------ fichier

    /** §7 : 11 feuilles, dans l'ordre et sous les noms du spec. */
    @Test
    void r7_onzeFeuilles_nomsDansLOrdre() throws Exception {
        Workbook w = classeur(etude);

        List<String> noms = IntStream.range(0, w.getNumberOfSheets()).mapToObj(w::getSheetName).toList();
        assertThat(noms).containsExactlyElementsOf(FEUILLES);
    }

    /** §7 : type XLSX, nom « etude-<code>_<horodatage>.xlsx » (horodatage d'ExportTableur). */
    @Test
    void r7_typeEtNomDeFichier() throws Exception {
        MockHttpServletResponse r = telecharger(etude);

        assertAll(
                () -> assertThat(r.getContentType()).startsWith(XLSX),
                () -> assertThat(r.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                        .matches(".*filename=\"?etude-" + nom("EXP") + "_\\d{8}_\\d{6}\\.xlsx\"?.*"));
    }

    // ------------------------------------------------------------------ une ligne par feuille

    /** Feuille 1 « Identité » : code, description, société. */
    @Test
    void r7_feuille1_identite() throws Exception {
        Sheet f = feuille(0);

        assertAll(
                () -> assertThat(contientTexte(f, nom("EXP"))).isTrue(),
                () -> assertThat(contientTexte(f, "Etude " + nom("EXP"))).isTrue(),
                () -> assertThat(contientTexte(f, "ENT-" + jeton) || contientTexte(f, "Entite ENT-" + jeton))
                        .as("société (code ou libellé)").isTrue());
    }

    /** Feuille 2 « Bilan » : coût global HT et TTC = colonnes stockées ; crédit ; montants au format #,##0.00. */
    @Test
    void r7_feuille2_bilan_rapprocheDesColonnesStockees() throws Exception {
        BigDecimal coutHt = stocke("TotalCostHT");
        BigDecimal coutTtc = stocke("TotalCost");
        Sheet f = feuille(1);

        Optional<Cell> ht = celluleNombre(f, coutHt);
        assertAll(
                () -> assertThat(coutHt).isEqualByComparingTo("5000"),
                () -> assertThat(ht).isPresent(),
                () -> assertThat(celluleNombre(f, coutTtc)).isPresent(),
                () -> assertThat(celluleNombre(f, d("2000"))).isPresent(),
                () -> assertThat(ht.map(c -> c.getCellStyle().getDataFormatString())).contains("#,##0.00"));
    }

    /** Feuille 3 « Terrain » : superficie et prix d'achat du m² stockés. */
    @Test
    void r7_feuille3_terrain() throws Exception {
        BigDecimal superficie = stocke("Area");
        BigDecimal prixAchat = stocke("PurchaseUnitCost");
        Sheet f = feuille(2);

        assertAll(
                () -> assertThat(celluleNombre(f, superficie)).isPresent(),
                () -> assertThat(celluleNombre(f, prixAchat)).isPresent());
    }

    /** Feuille 4 « Exécution » : délai de retour (jours) stocké. */
    @Test
    void r7_feuille4_execution_delaiDeRetour() throws Exception {
        BigDecimal delai = stocke("BackTimeOnDays");

        assertThat(celluleNombre(feuille(3), delai)).isPresent();
    }

    /** Feuille 5 « Délais » : dates au format dd/MM/yyyy (début des phases au 01/01/2026). */
    @Test
    void r7_feuille5_delais_dateFormatee() throws Exception {
        assertThat(contientTexte(feuille(4), "01/01/2026")).isTrue();
    }

    /** Feuille 6 « Financement » : une ligne par crédit — description, déblocage 2 000, montant à régler 2 200. */
    @Test
    void r7_feuille6_financement_ligneDuCredit() throws Exception {
        Row credit = ligneDe(feuille(5), nom("CRED"));

        assertAll(
                () -> assertThat(contientNombre(credit, d("2000"))).isTrue(),
                () -> assertThat(contientNombre(credit, d("2200"))).isTrue());
    }

    /** Feuille 7 « Données » : en-têtes date / débit / crédit / description ; un remboursement (1 357,4) au débit. */
    @Test
    void r7_feuille7_donnees_ligneDEcheancier() throws Exception {
        Sheet f = feuille(6);

        assertAll(
                () -> assertThat(contientEnTete(f, "Date")).isTrue(),
                () -> assertThat(contientEnTete(f, "Débit")).isTrue(),
                () -> assertThat(contientEnTete(f, "Crédit")).isTrue(),
                () -> assertThat(contientEnTete(f, "Description")).isTrue(),
                () -> assertThat(celluleNombre(f, d("1357.4"))).isPresent());
    }

    /** Feuille 8 « Unités » : ordre, code, quantité — la ligne de l'unité porte 100. */
    @Test
    void r7_feuille8_unites() throws Exception {
        Sheet f = feuille(7);

        assertAll(
                () -> assertThat(contientEnTete(f, "Code")).isTrue(),
                () -> assertThat(contientEnTete(f, "Quantité")).isTrue(),
                () -> assertThat(contientNombre(ligneDe(f, nom("U")), d("100"))).isTrue());
    }

    /** Feuille 9 « Tranches » : ordre, code, poids, tranche — la ligne de la tranche porte le poids 1. */
    @Test
    void r7_feuille9_tranches() throws Exception {
        Sheet f = feuille(8);

        assertAll(
                () -> assertThat(contientEnTete(f, "Poids")).isTrue(),
                () -> assertThat(contientNombre(ligneDe(f, nom("TR1")), BigDecimal.ONE)).isTrue());
    }

    /** Feuille 10 « Unités par tranche » : colonnes « code (Qte) » et « code (%) », ligne de l'unité ventilée (100). */
    @Test
    void r7_feuille10_unitesParTranche() throws Exception {
        Sheet f = feuille(9);

        assertAll(
                () -> assertThat(contientEnTete(f, nom("TR1") + " (Qte)")).isTrue(),
                () -> assertThat(contientEnTete(f, nom("TR1") + " (%)")).isTrue(),
                () -> assertThat(contientNombre(ligneDe(f, nom("U")), d("100"))).isTrue());
    }

    /** Feuille 11 « Bilan détaillé » : en-têtes, indentation de 4 espaces par niveau, Oui/Non, montants, colonnes par tranche. */
    @Test
    void r7_feuille11_bilanDetaille() throws Exception {
        Sheet f = feuille(10);
        String travaux = nom("TRA");
        String grosOeuvres = nom("GO");
        Row ligneGo = ligneDe(f, grosOeuvres);

        assertAll(
                () -> assertThat(contientEnTete(f, "Visible module Projet")).isTrue(),
                () -> assertThat(contientEnTete(f, "Montant HT")).isTrue(),
                () -> assertThat(contientEnTete(f, "Poids")).isTrue(),
                () -> assertThat(contientEnTete(f, nom("TR1") + " (HT)")).isTrue(),
                () -> assertThat(contientEnTete(f, nom("TR1") + " (TTC)")).isTrue(),
                () -> assertThat(indentation(ligneGo, grosOeuvres) - indentation(ligneDe(f, travaux), travaux)).isEqualTo(4),
                () -> assertThat(cellules(ligneGo)).extracting(ExportFaisabiliteTest::texte).contains("Oui"),
                () -> assertThat(contientNombre(ligneGo, d("5000"))).isTrue(),
                () -> assertThat(contientNombre(ligneGo, d("6000"))).isTrue());
    }

    // ------------------------------------------------------------------ cas limites

    /** §7 : sans tranche, feuilles 9 et 10 vides (titres seuls). */
    @Test
    void r7_sansTranche_feuilles9Et10_titresSeuls() throws Exception {
        UUID seule = api.etude(ficheAvecPhases(nom("SANS")));
        UUID unite = api.unite(nom("US"), false, null);
        api.unites(seule, lignes(ligneUniteCorps(1, unite, d("10")))).andExpect(status().isOk());

        Workbook w = classeur(seule);

        List<Integer> lignesParFeuille = new ArrayList<>();
        for (int i : new int[]{8, 9}) {
            lignesParFeuille.add(w.getSheetAt(i).getPhysicalNumberOfRows());
        }
        assertAll(
                () -> assertThat(w.getNumberOfSheets()).isEqualTo(11),
                () -> assertThat(lignesParFeuille).containsExactly(1, 1));
    }

    /** §7 : lecture, étude bloquée incluse (OMRAN laisse « Exporter » actif en verrouillé). */
    @Test
    void r7_etudeBloquee_exportPermis() throws Exception {
        api.bloquer(etude).andExpect(status().isOk());

        assertThat(classeur(etude).getNumberOfSheets()).isEqualTo(11);
    }

    /** §7 : Administrateur et Direction Générale. */
    @Test
    void r7_directionGenerale_200() throws Exception {
        mockMvc.perform(get(EXPORT, etude).with(avecRole(patrimoine.administrateur(), ROLE_DG))).andExpect(status().isOk());
    }

    @Test
    void r7_commercial_403() throws Exception {
        mockMvc.perform(get(EXPORT, etude).with(avecRole(patrimoine.administrateur(), "Commercial")))
                .andExpect(status().isForbidden());
    }

    @Test
    void r7_etudeInconnue_404() throws Exception {
        mockMvc.perform(get(EXPORT, UUID.fromString("8E9F0A1B-2C3D-4E4F-9A5B-7C8D9E0F1A2B")).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
