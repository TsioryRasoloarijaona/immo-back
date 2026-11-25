package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.controller.projet.BordereauExcelSupport.Fichier;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.TYPE_DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.TYPE_ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.article;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.classeur;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Refus de l'import / export Excel du bordereau (FEATURE-lc2-bordereau-excel.md Endpoints, D8 : 404 → 403 → 409 → 400). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BordereauExcelSecuriteTest {

    private static final String DROIT_LIGNE = "Yes.Prj.Front.Market.MarketMonitoring.toolAddMarketLine";
    private static final String MSG_CADENAS = "Le bordereau estimatif est verrouillé par le statut du lot";
    private static final String XLSX = "xlsx";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;

    private BordereauxEcritureSupport appels;
    private BordereauExcelSupport excel;
    private String jeton;
    private UUID projet;
    private UUID statutOuvert;
    private UUID statutFerme;
    private UUID categorie;
    private RequestPostProcessor admin;
    private int comptes;

    @BeforeEach
    void setUp() {
        appels = new BordereauxEcritureSupport(mockMvc);
        excel = new BordereauExcelSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        projet = parite.projet("BSP-" + jeton, parite.entiteJuridique("BSE-" + jeton));
        statutOuvert = parite.statut(ENTREPRENEURS, "BSO-" + jeton);
        b.ouvrirEstimatif(statutOuvert);
        statutFerme = parite.statut(ENTREPRENEURS, "BSF-" + jeton);
        categorie = b.categorie("BSC-" + jeton);
        admin = connecte(parite.administrateur("lot.bs." + jeton, projet), "Administrateur");
    }

    private UUID lot(String code, UUID statut) {
        UUID lot = parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statut);
        b.defauts(lot, "0.2", categorie);
        return lot;
    }

    /** Compte non Administrateur, habilité sur le projet, portant exactement ces droits. */
    private RequestPostProcessor avec(String... droits) {
        comptes++;
        return connecte(parite.avecDroits("lot.bs" + comptes + "." + jeton, List.of(droits), projet), "Commercial");
    }

    private static byte[] unArticle() {
        return classeur(article("A1", 1, 10));
    }

    private static byte[] illisible() {
        return "pas un classeur".getBytes(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ 404

    // 404 : même detail que la lecture du bordereau (une route absente rendrait un corps vide).
    @Test
    void import404_lotInconnu_lesDeuxTypes_memeMessageQueLaLecture() throws Exception {
        UUID inconnu = UUID.randomUUID();
        Reponse lecture = appels.lire(admin, inconnu, DEFINITIF);

        Reponse d = excel.importer(admin, inconnu, TYPE_DEFINITIF, unArticle());
        Reponse e = excel.importer(admin, inconnu, TYPE_ESTIMATIF, unArticle());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(d.statut()).as(d.corps()).isEqualTo(404);
            s.assertThat(e.statut()).as(e.corps()).isEqualTo(404);
            s.assertThat(detail(d)).isNotBlank().isEqualTo(detail(lecture));
            s.assertThat(detail(e)).isNotBlank().isEqualTo(detail(lecture));
        });
    }

    @Test
    void import404_lotHorsPerimetre_compteSansDroit_avantLe403() throws Exception {
        UUID autreProjet = parite.projet("BSX-" + jeton, parite.entiteJuridique("BSY-" + jeton));
        UUID lot = parite.lot(autreProjet, "BSH-" + jeton, ENTREPRENEURS, statutOuvert);
        Reponse lecture = appels.lire(admin, UUID.randomUUID(), DEFINITIF);

        Reponse r = excel.importer(avec(), lot, TYPE_DEFINITIF, unArticle());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
            s.assertThat(detail(r)).isNotBlank().isEqualTo(detail(lecture));
        });
    }

    @Test
    void export404_lotInconnu_memeMessageQueLaLecture() throws Exception {
        UUID inconnu = UUID.randomUUID();
        Reponse lecture = appels.lire(admin, inconnu, DEFINITIF);

        Fichier f = excel.exporter(admin, inconnu, TYPE_DEFINITIF, XLSX);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.statut()).as(f.corps()).isEqualTo(404);
            s.assertThat(detail(new Reponse(f.statut(), f.corps()))).isNotBlank().isEqualTo(detail(lecture));
        });
    }

    @Test
    void export404_lotHorsPerimetre() throws Exception {
        UUID autreProjet = parite.projet("BSV-" + jeton, parite.entiteJuridique("BSW-" + jeton));
        UUID lot = parite.lot(autreProjet, "BSK-" + jeton, ENTREPRENEURS, statutOuvert);
        Reponse lecture = appels.lire(admin, UUID.randomUUID(), DEFINITIF);

        Fichier f = excel.exporter(avec(DROIT_NOUVEAU, DROIT_LIGNE), lot, TYPE_DEFINITIF, XLSX);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.statut()).as(f.corps()).isEqualTo(404);
            s.assertThat(detail(new Reponse(f.statut(), f.corps()))).isNotBlank().isEqualTo(detail(lecture));
        });
    }

    // ------------------------------------------------------------------ 403 : mêmes droits que le PUT

    @Test
    void import403_definitif_sansToolNew() throws Exception {
        Reponse r = excel.importer(avec(DROIT_LIGNE), lot("S1", statutOuvert), TYPE_DEFINITIF, unArticle());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void import403_definitif_toolNewSansToolAddMarketLine() throws Exception {
        Reponse r = excel.importer(avec(DROIT_NOUVEAU), lot("S2", statutOuvert), TYPE_DEFINITIF, unArticle());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void import200_definitif_lesDeuxDroits_temoin() throws Exception {
        Reponse r = excel.importer(avec(DROIT_NOUVEAU, DROIT_LIGNE), lot("S3", statutOuvert), TYPE_DEFINITIF, unArticle());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void import403_estimatif_sansToolNew() throws Exception {
        Reponse r = excel.importer(avec(DROIT_LIGNE), lot("S4", statutOuvert), TYPE_ESTIMATIF, unArticle());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void import200_estimatif_toolNew_temoin() throws Exception {
        Reponse r = excel.importer(avec(DROIT_NOUVEAU), lot("S5", statutOuvert), TYPE_ESTIMATIF, unArticle());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // D8 : les droits sont contrôlés avant la lecture du classeur.
    @Test
    void d8_403AvantFichierIllisible() throws Exception {
        Reponse r = excel.importer(avec(DROIT_LIGNE), lot("S6", statutOuvert), TYPE_DEFINITIF, illisible());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ 409 : estimatif verrouillé

    @Test
    void import409_estimatifVerrouilleParLeStatut() throws Exception {
        Reponse r = excel.importer(admin, lot("V1", statutFerme), TYPE_ESTIMATIF, unArticle());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_CADENAS);
        });
    }

    // D8 : le cadenas est contrôlé avant la lecture du classeur.
    @Test
    void d8_409AvantFichierIllisible() throws Exception {
        Reponse r = excel.importer(admin, lot("V2", statutFerme), TYPE_ESTIMATIF, illisible());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_CADENAS);
        });
    }

    // Témoin : même lot fermé, l'import définitif n'a pas de cadenas.
    @Test
    void import200_definitif_statutFerme_pasDeCadenas() throws Exception {
        Reponse r = excel.importer(admin, lot("V3", statutFerme), TYPE_DEFINITIF, unArticle());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }
}
