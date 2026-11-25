package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * FEATURE-lc5-decompte-avancement.md : création d'un décompte avec ses lignes (R1), saisie de l'avancement en % sur
 * {@code PUT /api/projet/decomptes/{id}} (R2-R4), lecture de l'avancement (R5), verrous inchangés (R6).
 * Chantier : bordereau A (10 u, 100 HT) et B (90 u) ; décompte 1 : A = 4 ; décompte 2 (dernier, non facturé) : A = 1 sur 4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesAvancementTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String TVA = "0.2";
    private static final String QUANTITE = "Quantity";
    private static final String ANCIENNE = "QuantityOld";
    private static final String LIGNES = "$.lignes";
    private static final String AVANCEMENT = "avancement";
    private static final String SAISIE = "quantiteSaisie";
    private static final String MODE_EXECUTEE = "ExecutedQuantityBasedInsert";
    private static final BigDecimal SIX_DECIMALES = new BigDecimal("0.000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PAV-" + jeton);
        lot = fixture.lot(projet, "LAV-" + jeton, fixture.fournisseur("FAV-" + jeton));
        decomptes.poser("PrjMarket", lot, "WarrantyRatio", BigDecimal.ZERO, "AdvanceRatio", BigDecimal.ZERO,
                "ProportionRatio", BigDecimal.ZERO, "InsuranceDECRatio", BigDecimal.ZERO, "InsuranceTRCRatio",
                BigDecimal.ZERO, "InsuranceDECAmount", BigDecimal.ZERO, "InsuranceTRCAmount", BigDecimal.ZERO,
                "StockPileRatio", BigDecimal.ZERO, "DefaultVat", new BigDecimal(TVA), MODE_EXECUTEE, false,
                "ApplyWithholdingTaxOnAdditionalWork", false);
        AdmUser compte = vente.compte("prj.av.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
    }

    // ------------------------------------------------------------------ fixtures

    private record Chantier(UUID ligneA, UUID ligneB, UUID d1, UUID d2) {
    }

    private UUID decompte(String code, int numero) {
        UUID d = fixture.decompte(lot, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
        decomptes.poser("PrjWorkingOut", d, "WarrantyRatio", BigDecimal.ZERO, "AdvanceRatio", BigDecimal.ZERO,
                "proportionRatio", BigDecimal.ZERO, "insuranceDECRatio", BigDecimal.ZERO, "insuranceTRCRatio",
                BigDecimal.ZERO, "StockPileRatio", BigDecimal.ZERO, "WorkingOutAdvanceAmount", BigDecimal.ZERO,
                "ProrataRefundAmount", BigDecimal.ZERO, "Fluctuations", BigDecimal.ZERO, "FluctuationsOld",
                BigDecimal.ZERO, "StockAmount", BigDecimal.ZERO, "StockAmountOld", BigDecimal.ZERO, "advanceRest",
                BigDecimal.ZERO, "insuranceDECRest", BigDecimal.ZERO, "insuranceTRCRest", BigDecimal.ZERO,
                "InsuranceDECAmountMax", BigDecimal.ZERO, "InsuranceTRCAmountMax", BigDecimal.ZERO,
                "WorkingOutAdvanceAmountRestAtCreation", BigDecimal.ZERO, "AmountRegul", BigDecimal.ZERO,
                "AdvanceBasedOnAmount", false, "WarrantyBasedOnAmount", false, "TRCBasedOnAmount", false,
                "DECBasedOnAmount", false, "ProrataBasedOnAmount", false, "FinalWorkingOut", false);
        return d;
    }

    private Chantier chantier() {
        UUID a = decomptes.ligneMarche(lot, 1, "10", "100", TVA);
        UUID b = decomptes.ligneMarche(lot, 2, "90", "100", TVA);
        UUID d1 = decompte("DA1-", 1);
        decomptes.ligne(d1, a, 1, "4", "0", "100", TVA);
        UUID d2 = decompte("DA2-", 2);
        decomptes.ligne(d2, a, 1, "1", "4", "100", TVA);
        decomptes.ligne(d2, b, 2, "0", "0", "100", TVA);
        return new Chantier(a, b, d1, d2);
    }

    private void modeExecutee(boolean executee) {
        decomptes.poser("PrjMarket", lot, MODE_EXECUTEE, executee);
    }

    // ------------------------------------------------------------------ appels

    private MockHttpServletResponse executer(MockHttpServletRequestBuilder requete) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(admin)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r;
    }

    /** Création par un rôle portant A_0152_13 (l'« Administrateur » de yesRemsTest est restreint : lot invisible, 404). */
    private MockHttpServletResponse creer() throws Exception {
        RequestPostProcessor createur = decomptes.compte(projet, jeton, FixtureDecomptes.DROIT_MODIFIER);
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(post(LOTS + "/" + lot + "/decomptes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"dateDocument\":\"2026-04-01\"}").with(createur))
                .andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r;
    }

    private String fiche(UUID decompte) throws Exception {
        Reponse r = appels.lire(admin, DECOMPTES + "/" + decompte);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.corps();
    }

    /** Fiche relue, la ligne du bordereau {@code ligneMarche} recevant {@code quantiteSaisie} et {@code avancement}. */
    private String saisie(UUID decompte, UUID ligneMarche, BigDecimal quantiteSaisie, BigDecimal avancement)
            throws Exception {
        DocumentContext corps = JsonPath.parse(fiche(decompte));
        List<Map<String, Object>> lignes = corps.read(LIGNES);
        Map<String, Object> ligne = lignes.stream()
                .filter(l -> ligneMarche.toString().equalsIgnoreCase(String.valueOf(l.get("ligneMarcheId"))))
                .findFirst().orElseThrow(() -> new AssertionError("fiche sans ligne " + ligneMarche));
        ligne.put(SAISIE, quantiteSaisie);
        ligne.put(AVANCEMENT, avancement);
        corps.set(LIGNES, lignes);
        return corps.jsonString();
    }

    private MockHttpServletResponse enregistrer(UUID decompte, String corps) throws Exception {
        return executer(put(DECOMPTES + "/" + decompte).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private MockHttpServletResponse avancement(UUID decompte, UUID ligneMarche, String pourcentage) throws Exception {
        return enregistrer(decompte, saisie(decompte, ligneMarche, null, new BigDecimal(pourcentage)));
    }

    // ------------------------------------------------------------------ lectures

    private static BigDecimal nombre(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    private static Map<String, Object> ligneDe(List<Map<String, Object>> lignes, UUID ligneMarche) {
        return lignes.stream().filter(l -> ligneMarche.toString().equalsIgnoreCase(String.valueOf(l.get("marche"))))
                .findFirst().orElseThrow(() -> new AssertionError("aucune ligne pour " + ligneMarche));
    }

    private Map<String, Object> ligneEnBase(UUID decompte, UUID ligneMarche) {
        return ligneDe(decomptes.lignes(decompte), ligneMarche);
    }

    private BigDecimal cumul(UUID decompte, UUID ligneMarche) {
        Map<String, Object> l = ligneEnBase(decompte, ligneMarche);
        return nombre(l, ANCIENNE).add(nombre(l, QUANTITE));
    }

    private List<Map<String, Object>> decomptesDuLot() {
        decomptes.flush();
        return jdbc.queryForList("SELECT CAST(PKey AS varchar(36)) AS id, AmountHT FROM PrjWorkingOut WHERE PrjMarketId = ?",
                lot.toString());
    }

    private Map<String, Object> cree(UUID... existants) {
        List<String> anciens = Arrays.stream(existants).map(u -> u.toString().toUpperCase()).toList();
        List<Map<String, Object>> nouveaux = decomptesDuLot().stream()
                .filter(m -> !anciens.contains(String.valueOf(m.get("id")).toUpperCase())).toList();
        assertThat(nouveaux).hasSize(1);
        return nouveaux.getFirst();
    }

    private List<Map<String, Object>> lignesCompletes(Object decompte) {
        decomptes.flush();
        return jdbc.queryForList("""
                SELECT CAST(PrjMarketLineId AS varchar(36)) AS marche, Quantity, QuantityOld, QuantityOrigin
                FROM PrjWorkingOutLine WHERE PrjWorkingOutId = ? ORDER BY Sorting""", String.valueOf(decompte));
    }

    // ------------------------------------------------------------------ R1 Création chargée

    /** R1 : une ligne par ligne du bordereau, saisie 0, période 0, cumul antérieur = Σ des décomptes précédents. */
    @Test
    void r1_creation_uneLigneParLigneDuBordereau_cumulAnterieurReprisDesDecomptesPrecedents() throws Exception {
        Chantier c = chantier();

        MockHttpServletResponse r = creer();

        String corps = r.getContentAsString(StandardCharsets.UTF_8);
        assertThat(r.getStatus()).as(corps).isIn(200, 201);
        Map<String, Object> nouveau = cree(c.d1(), c.d2());
        List<Map<String, Object>> lignes = lignesCompletes(nouveau.get("id"));
        assertThat(lignes).hasSize(2);
        Map<String, Object> a = ligneDe(lignes, c.ligneA());
        Map<String, Object> b = ligneDe(lignes, c.ligneB());
        SoftAssertions s = new SoftAssertions();
        s.assertThat(nombre(a, ANCIENNE)).as("A antérieure").isEqualByComparingTo("5");
        s.assertThat(nombre(a, QUANTITE)).as("A période").isEqualByComparingTo("0");
        s.assertThat(nombre(a, "QuantityOrigin")).as("A saisie").isEqualByComparingTo("0");
        s.assertThat(nombre(b, ANCIENNE)).as("B antérieure").isEqualByComparingTo("0");
        s.assertThat(nombre(b, QUANTITE)).as("B période").isEqualByComparingTo("0");
        s.assertThat(nombre(b, "QuantityOrigin")).as("B saisie").isEqualByComparingTo("0");
        s.assertThat(nombre(nouveau, "AmountHT")).as("total HT des lignes").isEqualByComparingTo("0");
        s.assertThat((List<?>) JsonPath.read(corps, LIGNES)).as("réponse").hasSize(2);
        s.assertAll();
    }

    /** R1 : lot sans bordereau définitif → décompte créé sans ligne, sans erreur. */
    @Test
    void r1_creation_lotSansBordereau_decompteSansLigne() throws Exception {
        assertThat(creer().getStatus()).isIn(200, 201);

        assertThat(lignesCompletes(cree().get("id"))).isEmpty();
    }

    // ------------------------------------------------------------------ R2 Champ d'écriture

    /** R2 : quantiteSaisie et avancement sur la même ligne → 400, rien d'écrit. */
    @Test
    void r2_quantiteEtAvancementSurLaMemeLigne_refus400() throws Exception {
        Chantier c = chantier();
        String corps = saisie(c.d2(), c.ligneA(), new BigDecimal("3"), new BigDecimal("50"));

        MockHttpServletResponse r = enregistrer(c.d2(), corps);

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(400);
        assertThat(r.getContentAsString(StandardCharsets.UTF_8)).contains("Saisir soit la quantité, soit l'avancement");
        assertThat(nombre(ligneEnBase(c.d2(), c.ligneA()), QUANTITE)).isEqualByComparingTo("1");
    }

    // ------------------------------------------------------------------ R3 Conversion

    /** R3 mode cumul (ExecutedQuantityBasedInsert faux) : 84,5 % de 10 → cumul 8,45, période 4,45 sur 4 antérieurs. */
    @Test
    void r3_modeCumul_avancementDonneCumulCible_avecCumulAnterieur() throws Exception {
        Chantier c = chantier();
        modeExecutee(false);

        MockHttpServletResponse r = avancement(c.d2(), c.ligneA(), "84.5");

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isIn(200, 204);
        Map<String, Object> a = ligneEnBase(c.d2(), c.ligneA());
        assertThat(nombre(a, ANCIENNE)).isEqualByComparingTo("4");
        assertThat(cumul(c.d2(), c.ligneA())).isCloseTo(new BigDecimal("8.45"), within(SIX_DECIMALES));
    }

    /** R3 mode quantité exécutée : même invariant ancienne + période = C (coefficient 1). */
    @Test
    void r3_modeExecutee_avancementDonneCumulCible_avecCumulAnterieur() throws Exception {
        Chantier c = chantier();
        modeExecutee(true);

        MockHttpServletResponse r = avancement(c.d2(), c.ligneA(), "84.5");

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isIn(200, 204);
        Map<String, Object> a = ligneEnBase(c.d2(), c.ligneA());
        assertThat(nombre(a, ANCIENNE)).isEqualByComparingTo("4");
        assertThat(cumul(c.d2(), c.ligneA())).isCloseTo(new BigDecimal("8.45"), within(SIX_DECIMALES));
    }

    // ------------------------------------------------------------------ R4 Refus

    @Test
    void r4_avancementNegatif_refus400() throws Exception {
        Chantier c = chantier();

        MockHttpServletResponse r = avancement(c.d2(), c.ligneA(), "-5");

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(400);
        assertThat(nombre(ligneEnBase(c.d2(), c.ligneA()), QUANTITE)).isEqualByComparingTo("1");
    }

    /** R4 : dépassement accepté, 120 % de 10 → cumul 12. */
    @Test
    void r4_avancementSuperieurA100_accepte_cumulAuDelaDuMarche() throws Exception {
        Chantier c = chantier();

        MockHttpServletResponse r = avancement(c.d2(), c.ligneA(), "120");

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isIn(200, 204);
        assertThat(cumul(c.d2(), c.ligneA())).isCloseTo(new BigDecimal("12"), within(SIX_DECIMALES));
    }

    @Test
    void r4_avancementSurLigneDeQuantiteMarcheNulle_refus400() throws Exception {
        Chantier c = chantier();
        UUID nulle = decomptes.ligneMarche(lot, 3, "0", "100", TVA);
        decomptes.ligne(c.d2(), nulle, 3, "0", "0", "100", TVA);

        MockHttpServletResponse r = avancement(c.d2(), nulle, "50");

        String corps = r.getContentAsString(StandardCharsets.UTF_8);
        assertThat(r.getStatus()).as(corps).isEqualTo(400);
        assertThat(corps).contains("Avancement impossible : quantité du marché nulle");
    }

    // ------------------------------------------------------------------ R5 Lecture

    /** R5 : avancement lu = cumul HT / total HT du bordereau × 100 (A : 5 / 10 → 50). Existe déjà (avancementLignePct). */
    @Test
    void r5_lecture_avancementLigne_egalCumulSurMarcheFois100() throws Exception {
        Chantier c = chantier();

        List<Map<String, Object>> lignes = JsonPath.read(fiche(c.d2()), LIGNES);

        Map<String, Object> a = lignes.stream()
                .filter(l -> c.ligneA().toString().equalsIgnoreCase(String.valueOf(l.get("ligneMarcheId"))))
                .findFirst().orElseThrow(() -> new AssertionError("fiche sans ligne A"));
        assertThat(nombre(a, "avancementLignePct")).isEqualByComparingTo("50");
    }

    // ------------------------------------------------------------------ R6 Verrous inchangés

    @Test
    void r6_avancementSurDecompteFacture_refus409() throws Exception {
        Chantier c = chantier();
        String corps = saisie(c.d2(), c.ligneA(), null, new BigDecimal("50"));
        fixture.facture(c.d2(), "FAV" + jeton, new BigDecimal("1000"));

        assertThat(enregistrer(c.d2(), corps).getStatus()).isEqualTo(409);
        assertThat(nombre(ligneEnBase(c.d2(), c.ligneA()), QUANTITE)).isEqualByComparingTo("1");
    }

    @Test
    void r6_avancementSurDecomptePasDernierDuLot_refus409() throws Exception {
        Chantier c = chantier();

        assertThat(avancement(c.d1(), c.ligneA(), "50").getStatus()).isEqualTo(409);
        assertThat(nombre(ligneEnBase(c.d1(), c.ligneA()), QUANTITE)).isEqualByComparingTo("4");
    }
}
