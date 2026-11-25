package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.controller.projet.BordereauExcelSupport.Fichier;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.Ligne;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_CODE;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_REMISE;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_TVA;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.ENTETES;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.TYPE_DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.TYPE_ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.GROUPES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.GROUPES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Export Excel du bordereau du lot et aller-retour (FEATURE-lc2-bordereau-excel.md R6, D2, D3, 400 format). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BordereauExcelExportTest {

    private static final String XLSX = "xlsx";
    private static final List<String> COMPARES = List.of("$.lignes", "$.groupes", "$.totaux");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;

    private BordereauxEcritureSupport appels;
    private BordereauExcelSupport excel;
    private AppelsProjet lecture;
    private String jeton;
    private UUID projet;
    private UUID statutOuvert;
    private UUID categorie;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new BordereauxEcritureSupport(mockMvc);
        excel = new BordereauExcelSupport(mockMvc);
        lecture = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        projet = parite.projet("BEP-" + jeton, parite.entiteJuridique("BEE-" + jeton));
        statutOuvert = parite.statut(ENTREPRENEURS, "BEO-" + jeton);
        b.ouvrirEstimatif(statutOuvert);
        categorie = b.categorie("BEC-" + jeton);
        admin = connecte(parite.administrateur("lot.be." + jeton, projet), "Administrateur");
    }

    private UUID lot(String code) {
        UUID lot = parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statutOuvert);
        b.defauts(lot, "0.2", categorie);
        return lot;
    }

    /** Un groupe G (ordre 1) avec deux lignes, et une ligne hors groupe. */
    private void remplir(String tableGroupes, String tableLignes, UUID lot) {
        UUID g = b.groupe(tableGroupes, lot, null, 1, "G", false);
        b.ligne(tableLignes, new Ligne(lot, g, 1, "G1", "2", "100", categorie, false));
        b.ligne(tableLignes, new Ligne(lot, g, 2, "G2", "3", "12.5", categorie, false));
        b.ligne(tableLignes, new Ligne(lot, null, 3, "L3", "1", "40", categorie, false));
    }

    private static Object lu(Reponse r, String chemin) {
        return JsonPath.read(r.corps(), chemin);
    }

    private static List<List<String>> premiereFeuille(byte[] octets) {
        Map<String, List<List<String>>> feuilles = ClasseurDeTest.lire(octets);
        return feuilles.isEmpty() ? List.of() : feuilles.values().iterator().next();
    }

    private static List<String> rangeeDeCode(List<List<String>> feuille, String code) {
        return feuille.stream().filter(r -> r.size() > COL_CODE && code.equals(r.get(COL_CODE))).findFirst()
                .orElse(List.of());
    }

    private void allerRetour(String type, String route, UUID lot) throws Exception {
        Reponse avant = appels.lire(admin, lot, route);
        Reponse ficheAvant = lecture.lire(admin, AppelsProjet.LOTS + "/" + lot);

        Fichier f = excel.exporter(admin, lot, type, XLSX);
        Reponse imp = excel.importer(admin, lot, type, f.octets());
        Reponse apres = appels.lire(admin, lot, route);
        Reponse ficheApres = lecture.lire(admin, AppelsProjet.LOTS + "/" + lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.statut()).as(f.corps()).isEqualTo(200);
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(avant.ids("$.lignes[*].id")).hasSize(3);
            COMPARES.forEach(c -> s.assertThat(lu(apres, c)).as(c).isEqualTo(lu(avant, c)));
            s.assertThat(lu(ficheApres, "$.general")).as("fiche").isEqualTo(lu(ficheAvant, "$.general"));
        });
    }

    // ------------------------------------------------------------------ R6 : aller-retour

    @Test
    void r6_allerRetour_definitif_bordereauEtMontantsDuLotIdentiques() throws Exception {
        UUID lot = lot("R6D");
        remplir(GROUPES_DEFINITIF, LIGNES_DEFINITIF, lot);

        allerRetour(TYPE_DEFINITIF, DEFINITIF, lot);
    }

    @Test
    void r6_allerRetour_estimatif_bordereauEtMontantsDuLotIdentiques() throws Exception {
        UUID lot = lot("R6E");
        remplir(GROUPES_ESTIMATIF, LIGNES_ESTIMATIF, lot);

        allerRetour(TYPE_ESTIMATIF, ESTIMATIF, lot);
    }

    // ------------------------------------------------------------------ D2 / D3 : colonnes OMRAN

    @Test
    void d2_d3_export_entetesOmran_tvaEnPourcentage_remiseVide() throws Exception {
        UUID lot = lot("D2");
        b.ligne(LIGNES_DEFINITIF, new Ligne(lot, null, 1, "A1", "2", "100", categorie, false));

        Fichier f = excel.exporter(admin, lot, TYPE_DEFINITIF, XLSX);
        List<List<String>> feuille = f.statut() == 200 ? premiereFeuille(f.octets()) : List.of();
        List<String> a1 = rangeeDeCode(feuille, "A1");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.statut()).as(f.corps()).isEqualTo(200);
            s.assertThat(feuille).isNotEmpty();
            s.assertThat(feuille.isEmpty() ? List.of() : feuille.get(0)).isEqualTo(ENTETES);
            s.assertThat(a1).hasSizeGreaterThan(COL_REMISE);
            s.assertThat(a1.size() > COL_TVA ? a1.get(COL_TVA) : null).isEqualTo("20");
            s.assertThat(a1.size() > COL_REMISE ? a1.get(COL_REMISE) : null).isEmpty();
        });
    }

    // Le type choisit le bordereau exporté.
    @Test
    void export_estimatif_contientLesLignesEstimatives_pasLesDefinitives() throws Exception {
        UUID lot = lot("ET");
        b.ligne(LIGNES_DEFINITIF, new Ligne(lot, null, 1, "DEF1", "1", "10", categorie, false));
        b.ligne(LIGNES_ESTIMATIF, new Ligne(lot, null, 1, "EST1", "1", "10", categorie, false));

        Fichier f = excel.exporter(admin, lot, TYPE_ESTIMATIF, XLSX);
        List<List<String>> feuille = f.statut() == 200 ? premiereFeuille(f.octets()) : List.of();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.statut()).as(f.corps()).isEqualTo(200);
            s.assertThat(rangeeDeCode(feuille, "EST1")).isNotEmpty();
            s.assertThat(rangeeDeCode(feuille, "DEF1")).isEmpty();
        });
    }

    @Test
    void export_formatPdf_400() throws Exception {
        Fichier f = excel.exporter(admin, lot("PDF"), TYPE_DEFINITIF, "pdf");

        assertThat(f.statut()).as(f.corps()).isEqualTo(400);
    }
}
