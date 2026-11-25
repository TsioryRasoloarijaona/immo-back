package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_CODE_GROUPE;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_FODEC;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_ODT;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_PU_TTC;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_REMISE;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.ENTETES;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.TYPE_DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.TYPE_ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.article;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.avec;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.classeur;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.groupe;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.groupesDeCode;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.ligneDId;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.lignesDeCode;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.nb;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.CATEGORIE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.FODEC;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.GROUPE_ID;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.NOTES;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ORDRE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.PU_HT;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.QUANTITE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.GROUPES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Import Excel du bordereau du lot (FEATURE-lc2-bordereau-excel.md R1-R5, R7, D1-D5) et ses 400. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BordereauExcelImportTest {

    private static final List<String> MONTANTS_FICHE = List.of("$.general.marche.ht", "$.general.marche.tva",
            "$.general.marche.ttc", "$.general.marcheAjuste.ht", "$.general.marcheAjuste.tva", "$.general.marcheAjuste.ttc");
    private static final List<String> TOTAUX = List.of("$.totaux.totalHT", "$.totaux.totalTVA", "$.totaux.totalTTC",
            "$.totaux.nbLignes");

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
        projet = parite.projet("BXP-" + jeton, parite.entiteJuridique("BXE-" + jeton));
        statutOuvert = parite.statut(ENTREPRENEURS, "BXO-" + jeton);
        b.ouvrirEstimatif(statutOuvert);
        categorie = b.categorie("BXC-" + jeton);
        admin = connecte(parite.administrateur("lot.bx." + jeton, projet), "Administrateur");
    }

    /** Lot ouvert à l'estimatif, TVA par défaut 20 %, catégorie par défaut {@link #categorie}. */
    private UUID lot(String code) {
        UUID lot = parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statutOuvert);
        b.defauts(lot, "0.2", categorie);
        return lot;
    }

    private UUID ligneEnBase(String table, UUID lot, int ordre, String code, String quantite, String puHT) {
        return b.ligne(table, new Ligne(lot, null, ordre, code, quantite, puHT, categorie, false));
    }

    private Reponse relire(UUID lot, String route) throws Exception {
        return appels.lire(admin, lot, route);
    }

    private static Map<String, Object> premiere(List<Map<String, Object>> lignes) {
        return lignes.isEmpty() ? Map.of() : lignes.get(0);
    }

    /** Montant normalisé (sans zéros de fin) ; "absent" si le chemin manque ou vaut null. */
    private static String montant(Reponse r, String chemin) {
        try {
            Object v = JsonPath.read(r.corps(), chemin);
            return v == null ? "absent" : new BigDecimal(String.valueOf(v)).stripTrailingZeros().toPlainString();
        } catch (RuntimeException e) {
            return "absent";
        }
    }

    private static List<String> normalises(Reponse r, List<String> chemins) {
        return chemins.stream().map(c -> c + "=" + montant(r, c)).toList();
    }

    private List<String> ventilations(UUID lot) {
        return b.ventilations(lot).stream()
                .map(v -> v.get("categorie") + "=" + new BigDecimal(String.valueOf(v.get("taux"))).stripTrailingZeros()
                        .toPlainString())
                .sorted().toList();
    }

    // ------------------------------------------------------------------ R1 / R7 : même chemin que le PUT

    // R1 + R7 : deux lots jumeaux, l'un écrit par le PUT, l'autre par l'import du classeur équivalent.
    @Test
    void r1_r7_importDefinitif_memesMontantsTotauxVentilationsQueLePutEquivalent() throws Exception {
        UUID autreCategorie = b.categorie("BXK-" + jeton);
        UUID parPut = lot("P1");
        UUID parImport = lot("I1");
        UUID e1 = b.ligne(LIGNES_DEFINITIF, new Ligne(parPut, null, 1, "E1", "1", "100", autreCategorie, false));
        b.ligne(LIGNES_DEFINITIF, new Ligne(parImport, null, 1, "E1", "1", "100", autreCategorie, false));

        Reponse put = appels.ecrire(admin, parPut, DEFINITIF, bordereau(List.of(), List.of(
                ligne(e1, null, 1, "E1", "3", "100", autreCategorie),
                ligne(UUID.randomUUID(), null, 2, "X1", "2", "100", categorie))));
        Reponse imp = excel.importer(admin, parImport, TYPE_DEFINITIF,
                classeur(avec(article("E1", 3, 100), COL_ODT, 1), avec(article("X1", 2, 100), COL_ODT, 2)));
        Reponse fichePut = lecture.lire(admin, AppelsProjet.LOTS + "/" + parPut);
        Reponse ficheImport = lecture.lire(admin, AppelsProjet.LOTS + "/" + parImport);
        Reponse bordPut = relire(parPut, DEFINITIF);
        Reponse bordImport = relire(parImport, DEFINITIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(put.statut()).as(put.corps()).isEqualTo(200);
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(montant(fichePut, "$.general.marcheAjuste.ht")).isEqualTo("500");
            s.assertThat(normalises(ficheImport, MONTANTS_FICHE)).isEqualTo(normalises(fichePut, MONTANTS_FICHE));
            s.assertThat(normalises(bordImport, TOTAUX)).isEqualTo(normalises(bordPut, TOTAUX));
            s.assertThat(ventilations(parImport)).isEqualTo(ventilations(parPut));
        });
    }

    // R1 : même comparaison sur l'estimatif (totaux du bordereau relu).
    @Test
    void r1_importEstimatif_memesTotauxQueLePutEquivalent() throws Exception {
        UUID parPut = lot("P2");
        UUID parImport = lot("I2");
        UUID e1 = ligneEnBase(LIGNES_ESTIMATIF, parPut, 1, "E1", "1", "100");
        ligneEnBase(LIGNES_ESTIMATIF, parImport, 1, "E1", "1", "100");

        Reponse put = appels.ecrire(admin, parPut, ESTIMATIF, bordereau(List.of(), List.of(
                ligne(e1, null, 1, "E1", "4", "100", categorie),
                ligne(UUID.randomUUID(), null, 2, "X1", "1", "30", categorie))));
        Reponse imp = excel.importer(admin, parImport, TYPE_ESTIMATIF,
                classeur(avec(article("E1", 4, 100), COL_ODT, 1), avec(article("X1", 1, 30), COL_ODT, 2)));
        Reponse bordPut = relire(parPut, ESTIMATIF);
        Reponse bordImport = relire(parImport, ESTIMATIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(put.statut()).as(put.corps()).isEqualTo(200);
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(montant(bordPut, "$.totaux.totalHT")).isEqualTo("430");
            s.assertThat(normalises(bordImport, TOTAUX)).isEqualTo(normalises(bordPut, TOTAUX));
        });
    }

    // D6 : la réponse de l'import est le bordereau relu (même DTO que le GET).
    @Test
    void d6_reponseImport_egaleAuBordereauRelu() throws Exception {
        UUID lot = lot("D6");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article("A1", 2, 10)));
        Reponse relu = relire(lot, DEFINITIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(lignesDeCode(imp, "A1")).isEqualTo(lignesDeCode(relu, "A1")).hasSize(1);
            s.assertThat(normalises(imp, TOTAUX)).isEqualTo(normalises(relu, TOTAUX));
        });
    }

    // ------------------------------------------------------------------ R2 : appariement par code

    @Test
    void r2_appariementParCodeTrim_misAJourMemeId_absenteGardee_nouvelleCreeeAvecCategorieParDefaut() throws Exception {
        UUID lot = lot("R2");
        UUID a1 = ligneEnBase(LIGNES_DEFINITIF, lot, 1, "A1", "1", "100");
        UUID b1 = ligneEnBase(LIGNES_DEFINITIF, lot, 2, "B1", "2", "50");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article(" A1 ", 5, 200), article("C1", 3, 10)));
        Reponse relu = relire(lot, DEFINITIF);
        Map<String, Object> a = premiere(ligneDId(relu, a1));
        Map<String, Object> bl = premiere(ligneDId(relu, b1));
        Map<String, Object> c = premiere(lignesDeCode(relu, "C1"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(relu.ids("$.lignes[*].id")).hasSize(3);
            s.assertThat(lignesDeCode(relu, "A1")).hasSize(1);
            s.assertThat(nb(a, QUANTITE)).isEqualByComparingTo("5");
            s.assertThat(nb(a, PU_HT)).isEqualByComparingTo("200");
            s.assertThat(nb(bl, QUANTITE)).isEqualByComparingTo("2");
            s.assertThat(nb(bl, PU_HT)).isEqualByComparingTo("50");
            s.assertThat(bl).containsEntry(NOTES, "Note B1");
            s.assertThat(nb(c, QUANTITE)).isEqualByComparingTo("3");
            s.assertThat(c).containsEntry(CATEGORIE, categorie.toString());
        });
    }

    // R2 : appariement sensible à la casse — « b1 » ne met pas à jour « B1 », il crée une ligne.
    @Test
    void r2_casseDistincte_creeUneLigne_sansToucherLExistante() throws Exception {
        UUID lot = lot("R2C");
        UUID b1 = ligneEnBase(LIGNES_DEFINITIF, lot, 1, "B1", "2", "50");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article("b1", 9, 70)));
        Reponse relu = relire(lot, DEFINITIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(nb(premiere(ligneDId(relu, b1)), QUANTITE)).isEqualByComparingTo("2");
            s.assertThat(nb(premiere(lignesDeCode(relu, "b1")), QUANTITE)).isEqualByComparingTo("9");
            s.assertThat(relu.ids("$.lignes[*].id")).hasSize(2);
        });
    }

    // ------------------------------------------------------------------ R3 + D4 : codes

    @Test
    void r3_codeEnDoubleDansLeClasseur_definitif_400() throws Exception {
        Reponse r = excel.importer(admin, lot("R3D"), TYPE_DEFINITIF, classeur(article("K1", 1, 10), article("K1", 2, 10)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).contains("Le code K1 doit être unique pour les lignes du bordereaux");
        });
    }

    @Test
    void r3_codeEnDoubleDansLeClasseur_estimatif_400() throws Exception {
        Reponse r = excel.importer(admin, lot("R3E"), TYPE_ESTIMATIF, classeur(article("K1", 1, 10), article("K1", 2, 10)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).contains("Le code K1 doit être unique pour les lignes du bordereaux");
        });
    }

    @Test
    void d4_codeVide_ligneNonGroupe_400_numeroDeLigneExcel() throws Exception {
        Reponse r = excel.importer(admin, lot("D4V"), TYPE_DEFINITIF, classeur(article(null, 1, 10)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).contains("Ligne 2 : le code est obligatoire");
        });
    }

    @Test
    void d4_codeQuiCorrespondAPlusieursLignesExistantes_400() throws Exception {
        UUID lot = lot("D4X");
        ligneEnBase(LIGNES_DEFINITIF, lot, 1, "D1", "1", "10");
        ligneEnBase(LIGNES_DEFINITIF, lot, 2, "D1", "1", "20");

        Reponse r = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article("D1", 5, 10)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).contains("Le code D1 est en double dans le bordereau : import impossible");
        });
    }

    // ------------------------------------------------------------------ R4 + D2 : P.U. TTC, pourcentages

    // R4 + D2 : PU HT = 121,2 / (1 + 20/100) / (1 + 1/100) = 100.
    @Test
    void r4_puHTNul_deduitDuTTC_tvaEtFodecEnPourcentage() throws Exception {
        UUID lot = lot("R4");
        List<Object> t1 = avec(avec(article("T1", 1, 0), COL_FODEC, 1), COL_PU_TTC, new BigDecimal("121.2"));

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(t1));
        Map<String, Object> l = premiere(lignesDeCode(relire(lot, DEFINITIF), "T1"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(nb(l, PU_HT)).isEqualByComparingTo("100");
            s.assertThat(nb(l, FODEC)).isEqualByComparingTo("1");
            s.assertThat(nb(l, BordereauxEcritureSupport.TVA)).isEqualByComparingTo("20");
        });
    }

    @Test
    void r4_puHTNonNul_ttcIgnore() throws Exception {
        UUID lot = lot("R4N");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(avec(article("T2", 1, 50), COL_PU_TTC, 999)));
        Map<String, Object> l = premiere(lignesDeCode(relire(lot, DEFINITIF), "T2"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(nb(l, PU_HT)).isEqualByComparingTo("50");
        });
    }

    // ------------------------------------------------------------------ R5 + D5 : groupes

    @Test
    void r5_d5_uniteGroupe_parentDeduitDuCodePointe_ouDuCodeGroupe() throws Exception {
        UUID lot = lot("R5");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(groupe("1", null), groupe("1.2", null),
                article("1.1", 1, 10), avec(article("X9", 1, 10), COL_CODE_GROUPE, "1"), article("1.2.1", 1, 10)));
        Reponse relu = relire(lot, DEFINITIF);
        List<String> g1 = groupesDeCode(relu, "1");
        List<String> g12 = groupesDeCode(relu, "1.2");
        List<String> parentDe12 = JsonPath.read(relu.corps(), "$.groupes[?(@.code=='1.2')].parentId");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(g1).hasSize(1);
            s.assertThat(g12).hasSize(1);
            s.assertThat(parentDe12).isEqualTo(g1);
            s.assertThat(lignesDeCode(relu, "1")).isEmpty();
            s.assertThat(premiere(lignesDeCode(relu, "1.1")).get(GROUPE_ID)).isEqualTo(g1.isEmpty() ? "?" : g1.get(0));
            s.assertThat(premiere(lignesDeCode(relu, "X9")).get(GROUPE_ID)).isEqualTo(g1.isEmpty() ? "?" : g1.get(0));
            s.assertThat(premiere(lignesDeCode(relu, "1.2.1")).get(GROUPE_ID)).isEqualTo(g12.isEmpty() ? "?" : g12.get(0));
        });
    }

    // D5 : ligne appariée sans Code groupe et sans point → garde son groupe.
    @Test
    void d5_ligneApparieeSansCodeGroupeNiPoint_gardeSonGroupe() throws Exception {
        UUID lot = lot("D5");
        UUID g = b.groupe(GROUPES_DEFINITIF, lot, null, 1, "G", false);
        UUID z1 = b.ligne(LIGNES_DEFINITIF, new Ligne(lot, g, 1, "Z1", "1", "10", categorie, false));

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article("Z1", 4, 10)));
        Map<String, Object> l = premiere(ligneDId(relire(lot, DEFINITIF), z1));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(nb(l, QUANTITE)).isEqualByComparingTo("4");
            s.assertThat(l).containsEntry(GROUPE_ID, g.toString());
        });
    }

    // ------------------------------------------------------------------ D1 : catégorie par défaut

    @Test
    void d1_lotSansCategorieParDefaut_ligneACreer_400() throws Exception {
        UUID lot = lot("D1");
        b.defauts(lot, "0.2", null);

        Reponse r = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article("N1", 1, 10)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).contains("Définir la catégorie par défaut du lot avant l'import");
        });
    }

    // D1 témoin : sans catégorie par défaut, une ligne seulement appariée passe et garde sa catégorie.
    @Test
    void d1_lotSansCategorieParDefaut_ligneAppariee_200_categorieConservee() throws Exception {
        UUID lot = lot("D1T");
        b.defauts(lot, "0.2", null);
        UUID a1 = ligneEnBase(LIGNES_DEFINITIF, lot, 1, "A1", "1", "100");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article("A1", 2, 100)));
        Map<String, Object> l = premiere(ligneDId(relire(lot, DEFINITIF), a1));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(nb(l, QUANTITE)).isEqualByComparingTo("2");
            s.assertThat(l).containsEntry(CATEGORIE, categorie.toString());
        });
    }

    // ------------------------------------------------------------------ D3 : O.D.T. et Remise

    @Test
    void d3_odtNumerique_devientOrdre_odtNonNumerique_ordreConserve() throws Exception {
        UUID lot = lot("D3");
        UUID a1 = ligneEnBase(LIGNES_DEFINITIF, lot, 1, "A1", "1", "100");
        UUID b1 = ligneEnBase(LIGNES_DEFINITIF, lot, 2, "B1", "1", "100");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF,
                classeur(avec(article("A1", 1, 100), COL_ODT, "abc"), avec(article("B1", 1, 100), COL_ODT, 7)));
        Reponse relu = relire(lot, DEFINITIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(nb(premiere(ligneDId(relu, a1)), ORDRE)).isEqualByComparingTo("1");
            s.assertThat(nb(premiere(ligneDId(relu, b1)), ORDRE)).isEqualByComparingTo("7");
        });
    }

    @Test
    void d3_ligneCreeeSansOdt_placeeALaFin() throws Exception {
        UUID lot = lot("D3F");
        ligneEnBase(LIGNES_DEFINITIF, lot, 1, "A1", "1", "100");
        ligneEnBase(LIGNES_DEFINITIF, lot, 2, "B1", "1", "100");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(article("N1", 1, 10)));
        BigDecimal ordre = nb(premiere(lignesDeCode(relire(lot, DEFINITIF), "N1")), ORDRE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(ordre).isNotNull().isGreaterThan(new BigDecimal("2"));
        });
    }

    @Test
    void d3_remiseIgnoree() throws Exception {
        UUID lot = lot("D3R");

        Reponse imp = excel.importer(admin, lot, TYPE_DEFINITIF, classeur(avec(article("R1", 1, 100), COL_REMISE, 10)));
        Map<String, Object> l = premiere(lignesDeCode(relire(lot, DEFINITIF), "R1"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(imp.statut()).as(imp.corps()).isEqualTo(200);
            s.assertThat(nb(l, PU_HT)).isEqualByComparingTo("100");
        });
    }

    // ------------------------------------------------------------------ 400 : classeur

    @Test
    void import_enteteCodeAbsent_400() throws Exception {
        List<String> sansCode = ENTETES.stream().filter(e -> !"Code".equals(e)).toList();
        byte[] octets = ClasseurDeTest.xlsx(sansCode, List.of(ClasseurDeTest.ligne(null, "ART", "U", 1, 10)));

        Reponse r = excel.importer(admin, lot("H1"), TYPE_DEFINITIF, octets);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).isNotBlank();
        });
    }

    @Test
    void import_fichierIllisible_400() throws Exception {
        Reponse r = excel.importer(admin, lot("H2"), TYPE_DEFINITIF, "pas un classeur".getBytes(StandardCharsets.UTF_8));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // Quantité non numérique sur la 2e ligne de données = ligne Excel 3.
    @Test
    void import_quantiteNonNumerique_400_numeroDeLigneExcel() throws Exception {
        Reponse r = excel.importer(admin, lot("H3"), TYPE_DEFINITIF,
                classeur(article("Q1", 1, 10), article("Q2", "abc", 10)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).isNotNull().containsPattern("\\b3\\b");
        });
    }

    @Test
    void import_prixNonNumerique_400_numeroDeLigneExcel() throws Exception {
        Reponse r = excel.importer(admin, lot("H4"), TYPE_DEFINITIF, classeur(article("Q3", 1, "dix")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).isNotNull().containsPattern("\\b2\\b");
        });
    }
}
