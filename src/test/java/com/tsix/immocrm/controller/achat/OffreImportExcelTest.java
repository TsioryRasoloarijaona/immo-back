package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.RACINE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_AJOUT_OFFRE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.sauf;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * FEATURE-lc7-appel-offres.md R7 (import Excel des prix d'une offre), D4 (format et lecteur LC2), D5 (Remise en pourcentage comme LC2,
 * aller-retour depuis l'export LC2 du bordereau du lot).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreImportExcelTest {

    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final List<String> ENTETES = List.of("O.D.T.", "Code", "Description", "Unité", "Quantité", "P.U. HT", "TVA",
            "Remise", "FODEC", "P.U. TTC", "Notes", "Code groupe");
    private static final String LIGNE_OFFRE = "SELECT * FROM BuyDocumentLine WHERE PKey = ?";
    private static final String PU_HT = "UnitPriceHT";
    private static final String REMISE = "DiscountRatio";
    private static final String PU_A1 = "$.offre.lignes[?(@.code=='A1')].prixUnitaireHT";
    private static final String MISES_A_JOUR = "$.lignesMisesAJour";
    private static final String IGNORES = "$.codesIgnores";
    private static final String A1 = "A1";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureLotsParite parite;

    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        c = b.cadre("IX");
        qLibre = qui(b.libre(c, "lc7.ix.libre." + c.jeton()));
    }

    /** Offre présélectionnée, non sélectionnée, sur une DIA du statut donné. */
    private record Offre(UUID dia, UUID offre) {
    }

    private Offre offre(String suffixe, UUID statut) {
        UUID dia = demandes.demande(c, c.code(suffixe), statut);
        UUID tiers = achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
        b.preselection(dia, tiers, LocalDate.of(2026, 3, 20));
        return new Offre(dia, b.offre(dia, tiers, c.code("O" + suffixe), false));
    }

    /** Rangée au format LC2 : description, unité U, quantité 1, TVA 20 %. */
    private static List<Object> rangee(String code, Object puHT, Object remise, Object fodec) {
        return ClasseurDeTest.ligne(null, code, "Article " + code, "U", 1, puHT, 20, remise, fodec, null, null, null);
    }

    @SafeVarargs
    private static byte[] classeur(List<Object>... rangees) {
        return ClasseurDeTest.xlsx(ENTETES, List.of(rangees));
    }

    private Reponse importer(RequestPostProcessor qui, UUID dia, UUID offre, byte[] octets) throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "offre.xlsx", MIME_XLSX, octets);
        MockHttpServletResponse r = mockMvc.perform(multipart(RACINE + "/" + dia + "/offres/" + offre + "/import").file(fichier)
                .with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private int modifier(UUID dia, UUID offre, String json) throws Exception {
        return mockMvc.perform(put(RACINE + "/" + dia + "/offres/" + offre).with(qLibre).contentType(MediaType.APPLICATION_JSON)
                .content(json)).andReturn().getResponse().getStatus();
    }

    private Reponse temoin(String suffixe) throws Exception {
        Offre o = offre(suffixe, c.prep());
        b.ligneOffre(o.offre(), 1, A1, "0", "0");
        return importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 100, 0, 0)));
    }

    private static BigDecimal nombre(Reponse r, String chemin) {
        Object v = JsonPath.read(r.corps(), chemin);
        if (v instanceof List<?> liste) {
            v = liste.isEmpty() ? null : liste.getFirst();
        }
        return v == null ? null : new BigDecimal(v.toString());
    }

    private Map<String, Object> ligneEnBase(UUID ligne) {
        return b.lignes(LIGNE_OFFRE, ligne.toString()).getFirst();
    }

    private Object puApresRefus(UUID ligne) {
        return b.lignesApresRefus(LIGNE_OFFRE, ligne.toString()).getFirst().get(PU_HT);
    }

    // ------------------------------------------------------------------ R7 : remplacement par code

    @Test
    void r7_prixEtRemiseRemplacesParCode_ligneAbsenteDuFichierInchangee_totalHTRecalcule() throws Exception {
        Offre o = offre("PRX", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "0", "0");
        UUID a2 = b.ligneOffre(o.offre(), 2, "A2", "50", "0");

        Reponse r = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 200, 10, 0)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(r, PU_A1)).isEqualByComparingTo("200");
            s.assertThat(nombre(r, "$.offre.lignes[?(@.code=='A1')].remise")).as("D5 : 10 % → 0,1").isEqualByComparingTo("0.1");
            s.assertThat(nombre(r, "$.offre.totalHT")).as("200 × 0,9 + 50").isEqualByComparingTo("230");
            s.assertThat(nombre(r, MISES_A_JOUR)).isEqualByComparingTo("1");
            s.assertThat(nombre(r, IGNORES)).isEqualByComparingTo("0");
            s.assertThat((BigDecimal) ligneEnBase(a1).get(PU_HT)).isEqualByComparingTo("200");
            s.assertThat((BigDecimal) ligneEnBase(a1).get(REMISE)).isEqualByComparingTo("0.1");
            s.assertThat((BigDecimal) ligneEnBase(a2).get(PU_HT)).isEqualByComparingTo("50");
        });
    }

    @Test
    void r7_fodecEnPourcentage_ecritEnFraction() throws Exception {
        Offre o = offre("FOD", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "0", "0");

        Reponse r = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 100, 0, 5)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat((BigDecimal) ligneEnBase(a1).get("FODECRatio")).isEqualByComparingTo("0.05");
    }

    @Test
    void r7_codeInconnuDeLOffre_ignoreEtCompte_200() throws Exception {
        Offre o = offre("INC", c.prep());
        b.ligneOffre(o.offre(), 1, A1, "0", "0");

        Reponse r = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 100, 0, 0), rangee("Z9", 70, 0, 0)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(r, MISES_A_JOUR)).isEqualByComparingTo("1");
            s.assertThat(nombre(r, IGNORES)).isEqualByComparingTo("1");
            s.assertThat(nombre(r, PU_A1)).isEqualByComparingTo("100");
        });
    }

    @Test
    void r7_celluleRemiseVide_remiseInchangee_prixRemplace() throws Exception {
        Offre o = offre("RMV", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "10", "0.25");

        Reponse r = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 300, null, 0)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Map<String, Object> l = ligneEnBase(a1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat((BigDecimal) l.get(PU_HT)).isEqualByComparingTo("300");
            s.assertThat((BigDecimal) l.get(REMISE)).isEqualByComparingTo("0.25");
        });
    }

    // ------------------------------------------------------------------ R7 : mêmes contrôles que PUT …/offres/{oid}

    @Test
    void r7_puNegatif_memeStatutQueLaModification_rienNEstEcrit() throws Exception {
        Offre o = offre("PUN", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");

        Reponse refus = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, -10, 0, 0)));
        Object apresRefus = puApresRefus(a1);
        int statutModification = modifier(o.dia(), o.offre(), "[{\"ligneId\":\"%s\",\"prixUnitaireHT\":-10,\"remise\":0}]".formatted(a1));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isNotEqualTo(200).isEqualTo(statutModification);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
        });
    }

    @Test
    void r7_remiseHorsBornes_memeStatutQueLaModification_rienNEstEcrit() throws Exception {
        Offre o = offre("RHB", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");

        Reponse refus = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 100, 150, 0)));
        Object apresRefus = puApresRefus(a1);
        int statutModification = modifier(o.dia(), o.offre(), "[{\"ligneId\":\"%s\",\"prixUnitaireHT\":100,\"remise\":1.5}]".formatted(a1));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isNotEqualTo(200).isEqualTo(statutModification);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
        });
    }

    @Test
    void r7_offreEcartee_409_rienNEstEcrit_temoin200() throws Exception {
        Offre o = offre("ECA", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");
        achat.poser(o.offre(), "IsRejected", true);

        Reponse refus = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 100, 0, 0)));
        Object apresRefus = puApresRefus(a1);
        Reponse temoin = temoin("ECT");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r7_horsPhaseOffres_409_rienNEstEcrit_temoin200() throws Exception {
        Offre o = offre("HPH", c.depouil());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");

        Reponse refus = importer(qLibre, o.dia(), o.offre(), classeur(rangee(A1, 100, 0, 0)));
        Object apresRefus = puApresRefus(a1);
        Reponse temoin = temoin("HPT");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r7_offreDUneAutreDia_404_rienNEstEcrit_temoin200() throws Exception {
        Offre o = offre("AUT", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");
        UUID autreDia = demandes.demande(c, c.code("AUD"), c.prep());

        Reponse refus = importer(qLibre, autreDia, o.offre(), classeur(rangee(A1, 100, 0, 0)));
        Object apresRefus = puApresRefus(a1);
        Reponse temoin = temoin("AUT2");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(404);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r7_octetsNonXlsx_400_temoin200() throws Exception {
        Offre o = offre("NXL", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");

        Reponse refus = importer(qLibre, o.dia(), o.offre(), "pas un classeur".getBytes(StandardCharsets.UTF_8));
        Object apresRefus = puApresRefus(a1);
        Reponse temoin = temoin("NXT");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(400);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r7_sansEnteteCode_400_temoin200() throws Exception {
        Offre o = offre("SEC", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");
        List<String> sansCode = ENTETES.stream().map(e -> "Code".equals(e) ? "Référence" : e).toList();

        Reponse refus = importer(qLibre, o.dia(), o.offre(), ClasseurDeTest.xlsx(sansCode, List.of(rangee(A1, 100, 0, 0))));
        Object apresRefus = puApresRefus(a1);
        Reponse temoin = temoin("SET");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(400);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r7_sansDroitAjoutOffre_403_rienNEstEcrit_temoin200() throws Exception {
        Offre o = offre("DRO", c.prep());
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "5", "0");
        RequestPostProcessor sansDroit = qui(demandes.libre(c, "lc7.ix.sans." + c.jeton(), sauf(DROIT_AJOUT_OFFRE)));

        Reponse refus = importer(sansDroit, o.dia(), o.offre(), classeur(rangee(A1, 100, 0, 0)));
        Object apresRefus = puApresRefus(a1);
        Reponse temoin = temoin("DRT");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat((BigDecimal) apresRefus).isEqualByComparingTo("5");
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ D5 : aller-retour depuis l'export LC2 du lot

    @Test
    void d5_allerRetour_exportDefinitifDuLot_importDansLOffre_prixRepris() throws Exception {
        Offre o = offre("ART", c.prep());
        UUID lot = b.lotDe(c, o.dia(), "LART-" + c.jeton(), false);
        b.ligneDefinitive(lot, A1);
        UUID a1 = b.ligneOffre(o.offre(), 1, A1, "0", "0");
        RequestPostProcessor admin = connecte(parite.administrateur("lc7.ix.adm." + c.jeton(), c.projet()), "Administrateur");
        MockHttpServletResponse export = mockMvc.perform(get("/api/projet/lots/" + lot + "/bordereau/export")
                .param("type", "DEFINITIF").param("format", "xlsx").with(admin)).andReturn().getResponse();
        assertThat(export.getStatus()).as(export.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);

        Reponse r = importer(qLibre, o.dia(), o.offre(), export.getContentAsByteArray());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat((BigDecimal) ligneEnBase(a1).get(PU_HT)).isEqualByComparingTo("100");
            s.assertThat(nombre(r, MISES_A_JOUR)).isEqualByComparingTo("1");
        });
    }
}
