package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.LectureExport;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.EXTRA;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.FLUCTUATIONS;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.RG;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.STOCK;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Liste des décomptes, parité OMRAN (FEATURE-decomptes-parite.md « Liste », « Formules des colonnes de la liste » et
 * « Réponses du manager », Contrat). Ava. TTC / Ava. % / Dépa. TTC / Tot. TTC : noms du « Brief front » ({@code lignesTTC},
 * {@code avaPct} en pourcentage × 100 comme {@code avancementPct} des lots, {@code depassementTTC}, {@code totalTTC}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesPariteListeTest {

    static final String TOTAUX = DECOMPTES + "/totaux";
    static final String EXPORT = DECOMPTES + "/export";
    static final String P_SOCIETE = "societeId";
    static final String FLUCTUATIONS_LISTE = "fluctuationsAPayer";
    static final String APPRO_LISTE = "approvisionnementAPayer";
    static final String NOTES = "notes";
    static final String P_LOT = "lotId";
    static final String P_TAILLE = "size";
    static final String AVA_TTC = "lignesTTC";
    static final String AVA_PCT = "avaPct";
    static final String DEPA_TTC = "depassementTTC";
    static final String TOT_TTC = "totalTTC";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");
    private static final String TVA = "0.2";

    /** Colonnes de montant sommées par {@code /totaux} : même nom que dans la ligne de liste (Ava. % non sommé). */
    private static final List<String> MONTANTS = List.of("montantTTC", "travauxSupplementaires", "retenueGarantie",
            "netAPayer", FLUCTUATIONS_LISTE, APPRO_LISTE, "montantRegle", "resteARegler", AVA_TTC, DEPA_TTC, TOT_TTC);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureAchat achat;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixtureDecomptes decomptes;

    private AppelsProjet appels;
    private String jeton;
    private UUID societe;
    private UUID lot;
    private AdmUser compte;
    private RequestPostProcessor habilite;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        societe = achat.societe("SPL-" + jeton);
        UUID projet = achat.projet("PPL-" + jeton, societe);
        lot = fixture.lot(projet, "LPL-" + jeton, fixture.fournisseur("FPL-" + jeton));
        compte = fixture.compte("prj.pl." + jeton);
        fixture.habiliter(compte, projet);
        habilite = connecte(compte, "Commercial");
    }

    private static Map<String, BigDecimal> montants(String... colonneValeur) {
        Map<String, BigDecimal> m = new HashMap<>();
        for (int i = 0; i + 1 < colonneValeur.length; i += 2) {
            m.put(colonneValeur[i], new BigDecimal(colonneValeur[i + 1]));
        }
        return m;
    }

    private Reponse listeDuLot(RequestPostProcessor qui) throws Exception {
        Reponse r = appels.lire(qui, DECOMPTES, P_LOT, lot.toString(), P_TAILLE, AppelsProjet.TAILLE);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static String element(UUID id) {
        return "$.content[?(@.id == '" + id + "')]";
    }

    private static List<Object> valeurs(Reponse r, String chemin) {
        return r.valeur(chemin) instanceof List<?> l ? new ArrayList<>(l) : List.of();
    }

    private static BigDecimal somme(Reponse r, String colonne) {
        return valeurs(r, "$.content[*]." + colonne).stream()
                .map(v -> v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ colonnes OMRAN ajoutées (ECARTS §3)

    @Test
    void colonne_flucTTC_egaleFluctuationsToPay() throws Exception {
        UUID d = fixture.decompte(lot, "DFL-" + jeton, 1, montants(TTC, "1000", FLUCTUATIONS, "157.25", A_PAYER, "1157.25"));

        Reponse r = listeDuLot(habilite);

        assertThat(valeurs(r, element(d) + "." + FLUCTUATIONS_LISTE)).hasSize(1).first()
                .satisfies(v -> assertThat(new BigDecimal(String.valueOf(v))).isEqualByComparingTo("157.25"));
    }

    @Test
    void colonne_approTTC_egaleStockAmountToPay() throws Exception {
        UUID d = fixture.decompte(lot, "DAP-" + jeton, 1, montants(TTC, "1000", STOCK, "42.50", A_PAYER, "1042.50"));

        Reponse r = listeDuLot(habilite);

        assertThat(valeurs(r, element(d) + "." + APPRO_LISTE)).hasSize(1).first()
                .satisfies(v -> assertThat(new BigDecimal(String.valueOf(v))).isEqualByComparingTo("42.50"));
    }

    @Test
    void colonne_notes_egaleNotesDuDecompte() throws Exception {
        UUID d = fixture.decompte(lot, "DNO-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        jdbc.update("UPDATE PrjWorkingOut SET Notes = ? WHERE PKey = ?", "Note " + jeton, d.toString());

        Reponse r = listeDuLot(habilite);

        assertThat(valeurs(r, element(d) + "." + NOTES)).containsExactly("Note " + jeton);
    }

    /** Témoin (vert avant le code) : les colonnes Arpent en plus d'OMRAN restent dans la ligne. */
    @Test
    void colonnesArpentEnPlus_gardees() throws Exception {
        UUID d = fixture.decompte(lot, "DAR-" + jeton, 1, montants(TTC, "1000", RG, "100", A_PAYER, "900"));
        fixture.facture(d, "FAR-" + jeton, new BigDecimal("900"));

        Reponse r = listeDuLot(habilite);

        Map<String, Object> ligne = JsonPath.read(r.corps(), "$.content[0]");
        assertThat(ligne).containsKeys("montantHT", "retenueGarantie", "netAPayer", "netAPayerRecalcule",
                "ecartNetAPayer", "montantRegle", "resteARegler", "factureCode", "factureStatutCode");
    }

    // ------------------------------------------------------------------ Ava. TTC, Ava. %, Dépa. TTC, Tot. TTC (exemples chiffrés)

    private static BigDecimal colonne(Reponse r, UUID d, String colonne) {
        List<Object> v = valeurs(r, element(d) + "." + colonne);
        assertThat(v).as(colonne).hasSize(1);
        return new BigDecimal(String.valueOf(v.getFirst()));
    }

    /** Copie de {@code 26/00696} (yesRemsProd) : Tot. TTC = Ava 9 900 + Dépa 0 + TraSup −3 600 + Appro 0 + Fluc 1 620. */
    @Test
    void exemple2600696_totTTC_7920_sommeAvaDepaTraSupApproFluc() throws Exception {
        UUID d = fixture.decompte(lot, "D696-" + jeton, 1, montants(TTC, "9900", EXTRA, "-3600", STOCK, "0",
                FLUCTUATIONS, "1620", A_PAYER, "7920"));
        decomptes.poser("PrjWorkingOut", d, "AmountHT", new BigDecimal("9900"), "Fluctuations", new BigDecimal("-21450"),
                "FluctuationsOld", new BigDecimal("-23070"));
        decomptes.extra(d, "1", "-3000", TVA);

        Reponse r = listeDuLot(habilite);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(colonne(r, d, TOT_TTC)).isCloseTo(new BigDecimal("7920"), within(TOLERANCE));
            s.assertThat(colonne(r, d, AVA_TTC)).isCloseTo(new BigDecimal("9900"), within(TOLERANCE));
            s.assertThat(colonne(r, d, DEPA_TTC)).isCloseTo(BigDecimal.ZERO, within(TOLERANCE));
            s.assertThat(colonne(r, d, "travauxSupplementaires")).isCloseTo(new BigDecimal("-3600"), within(TOLERANCE));
            s.assertThat(colonne(r, d, FLUCTUATIONS_LISTE)).isCloseTo(new BigDecimal("1620"), within(TOLERANCE));
            s.assertThat(colonne(r, d, "montantTTC")).isCloseTo(new BigDecimal("9900"), within(TOLERANCE));
        });
    }

    /**
     * Copie chiffrée de {@code 25/00605} (dépassement) : bordereau 100 000 HT (A : 10 × 1 000, B : 90 × 1 000, TVA 20 %) ;
     * décompte 1 = A 10 + B 16,4 (26 400 HT) ; décompte 2 = A 3 (dépasse le marché de 3 000 HT / 3 600 TTC) + B 6,6.
     * Attendu sur le 2 : Ava. TTC 7 920, Dépa. 3 600, Ava. % 33 (33 000 / 100 000), Tot. TTC 11 520.
     */
    @Test
    void exemple2500605_depassement_avaTTC_depaTTC_avaPct_totTTC() throws Exception {
        UUID a = decomptes.ligneMarche(lot, 1, "10", "1000", TVA);
        UUID b = decomptes.ligneMarche(lot, 2, "90", "1000", TVA);
        UUID d1 = fixture.decompte(lot, "D604-" + jeton, 1, montants(TTC, "31680", A_PAYER, "31680"));
        decomptes.poser("PrjWorkingOut", d1, "AmountHT", new BigDecimal("26400"));
        decomptes.ligne(d1, a, 1, "10", "0", "1000", TVA);
        decomptes.ligne(d1, b, 2, "16.4", "0", "1000", TVA);
        UUID d2 = fixture.decompte(lot, "D605-" + jeton, 2, montants(TTC, "11520", A_PAYER, "11520"));
        decomptes.poser("PrjWorkingOut", d2, "AmountHT", new BigDecimal("9600"));
        decomptes.ligne(d2, a, 1, "3", "10", "1000", TVA);
        decomptes.ligne(d2, b, 2, "6.6", "16.4", "1000", TVA);
        fixture.facture(d2, "F605-" + jeton, new BigDecimal("11520"));

        Reponse r = listeDuLot(habilite);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(colonne(r, d2, AVA_TTC)).isCloseTo(new BigDecimal("7920"), within(TOLERANCE));
            s.assertThat(colonne(r, d2, DEPA_TTC)).isCloseTo(new BigDecimal("3600"), within(TOLERANCE));
            s.assertThat(colonne(r, d2, AVA_PCT)).isCloseTo(new BigDecimal("33"), within(TOLERANCE));
            s.assertThat(colonne(r, d2, TOT_TTC)).isCloseTo(new BigDecimal("11520"), within(TOLERANCE));
            s.assertThat(colonne(r, d2, "montantTTC")).isCloseTo(new BigDecimal("11520"), within(TOLERANCE));
            s.assertThat(colonne(r, d1, DEPA_TTC)).isCloseTo(BigDecimal.ZERO, within(TOLERANCE));
            s.assertThat(colonne(r, d1, AVA_TTC)).isCloseTo(new BigDecimal("31680"), within(TOLERANCE));
            s.assertThat(colonne(r, d1, AVA_PCT)).isCloseTo(new BigDecimal("26.4"), within(TOLERANCE));
        });
    }

    /** « Ava. % : 0 si base nulle » : lot sans ligne de bordereau. */
    @Test
    void avaPct_lotSansBordereau_zero() throws Exception {
        UUID d = fixture.decompte(lot, "DAZ-" + jeton, 1, montants(TTC, "1200", A_PAYER, "1200"));
        decomptes.poser("PrjWorkingOut", d, "AmountHT", new BigDecimal("1000"));

        Reponse r = listeDuLot(habilite);

        assertThat(colonne(r, d, AVA_PCT)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ filtre Entité (= PrjProject.CfgCompanyId)

    @Test
    void filtreEntite_neRendQueLesDecomptesDesProjetsDeLaSociete() throws Exception {
        UUID dA = fixture.decompte(lot, "DEA-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        UUID autreSociete = achat.societe("SPB-" + jeton);
        UUID autreProjet = achat.projet("PPB-" + jeton, autreSociete);
        UUID autreLot = fixture.lot(autreProjet, "LPB-" + jeton, fixture.fournisseur("FPB-" + jeton));
        fixture.habiliter(compte, autreProjet);
        UUID dB = fixture.decompte(autreLot, "DEB-" + jeton, 1, montants(TTC, "2000", A_PAYER, "2000"));

        Reponse r = appels.lire(habilite, DECOMPTES, P_SOCIETE, societe.toString(), P_TAILLE, AppelsProjet.TAILLE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids("$.content[*].id")).contains(dA.toString()).doesNotContain(dB.toString());
    }

    /** Visibilité inchangée (règle 6) : une société dont l'appelant n'a aucun projet habilité ne rend rien. */
    @Test
    void filtreEntite_societeSansProjetHabilite_rienDeVisible() throws Exception {
        UUID autreSociete = achat.societe("SPC-" + jeton);
        UUID autreProjet = achat.projet("PPC-" + jeton, autreSociete);
        UUID autreLot = fixture.lot(autreProjet, "LPC-" + jeton, fixture.fournisseur("FPC-" + jeton));
        fixture.decompte(autreLot, "DEC-" + jeton, 1, montants(TTC, "2000", A_PAYER, "2000"));
        fixture.decompte(lot, "DED-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));

        Reponse r = appels.lire(habilite, DECOMPTES, P_SOCIETE, autreSociete.toString(), P_TAILLE, AppelsProjet.TAILLE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.nombre("$.totalElements")).isEqualByComparingTo("0");
    }

    /** Témoin (vert avant le code) : un décompte d'un projet non habilité n'est pas listé. */
    @Test
    void visibiliteInchangee_projetNonHabilite_absent() throws Exception {
        UUID autreProjet = achat.projet("PPV-" + jeton, societe);
        UUID autreLot = fixture.lot(autreProjet, "LPV-" + jeton, fixture.fournisseur("FPV-" + jeton));
        UUID cache = fixture.decompte(autreLot, "DVC-" + jeton, 1, montants(TTC, "2000", A_PAYER, "2000"));

        Reponse r = appels.lire(habilite, DECOMPTES, P_TAILLE, AppelsProjet.TAILLE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids("$.content[*].id")).doesNotContain(cache.toString());
    }

    // ------------------------------------------------------------------ totaux serveur = somme de la liste

    @Test
    void totauxServeur_egalSommeDeLaListe_memeFiltre() throws Exception {
        UUID d1 = fixture.decompte(lot, "DT1-" + jeton, 1, montants(TTC, "12000", RG, "1200", EXTRA, "500",
                FLUCTUATIONS, "0.39", STOCK, "7", A_PAYER, "11307.39"));
        fixture.decompte(lot, "DT2-" + jeton, 2, montants(TTC, "3000", RG, "300", A_PAYER, "2700"));
        UUID facture = fixture.facture(d1, "FT1-" + jeton, new BigDecimal("11307.39"));
        fixture.reglement(facture, "RT1-" + jeton, new BigDecimal("5000"), "POINTED");

        Reponse liste = listeDuLot(habilite);
        Reponse totaux = appels.lire(habilite, TOTAUX, P_LOT, lot.toString());

        assertThat(totaux.statut()).as(totaux.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(totaux.nombre("$.nombre")).isEqualByComparingTo(liste.nombre("$.totalElements"));
            s.assertThat(totaux.nombre("$.montantTTC")).isEqualByComparingTo("15000");
            MONTANTS.forEach(c -> s.assertThat(totaux.nombre("$." + c)).as(c).isEqualByComparingTo(somme(liste, c)));
        });
    }

    /** Règle 6 : les totaux ne voient que les projets habilités, comme la liste. */
    @Test
    void totauxServeur_sansHabilitation_zero() throws Exception {
        fixture.decompte(lot, "DTZ-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        RequestPostProcessor nonHabilite = connecte(fixture.compte("prj.pl.n." + jeton), "Commercial");

        Reponse totaux = appels.lire(nonHabilite, TOTAUX, P_LOT, lot.toString());

        assertThat(totaux.statut()).as(totaux.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(totaux.nombre("$.nombre")).isEqualByComparingTo("0");
            s.assertThat(totaux.nombre("$.montantTTC")).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ export xlsx

    private MvcResult exporter(RequestPostProcessor qui) throws Exception {
        return LectureExport.executer(mockMvc, get(EXPORT).with(qui).param(P_LOT, lot.toString()));
    }

    @Test
    void export_xlsx_memesLignesQueLaListe() throws Exception {
        fixture.decompte(lot, "DX1-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        fixture.decompte(lot, "DX2-" + jeton, 2, montants(TTC, "2000", A_PAYER, "2000"));
        List<String> codes = List.of("DX1-" + jeton, "DX2-" + jeton);

        MvcResult r = exporter(habilite);

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(r.getResponse().getContentType()).startsWith(XLSX);
        assertThat(LectureExport.codesDesLignes(LectureExport.lignesXlsx(r.getResponse().getContentAsByteArray()), codes))
                .containsExactlyInAnyOrderElementsOf(codes);
    }

    @Test
    void export_sansHabilitation_aucuneLigne() throws Exception {
        fixture.decompte(lot, "DXH-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        RequestPostProcessor nonHabilite = connecte(fixture.compte("prj.pl.x." + jeton), "Commercial");

        MvcResult r = exporter(nonHabilite);

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(LectureExport.codesDesLignes(LectureExport.lignesXlsx(r.getResponse().getContentAsByteArray()),
                List.of("DXH-" + jeton))).doesNotContain("DXH-" + jeton);
    }
}
