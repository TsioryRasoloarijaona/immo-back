package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureDecomptes;
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

import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_RECALCULER;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Création d'un décompte depuis le lot (FEATURE-decomptes-parite.md « Règles d'écriture (OMRAN) » §4, Contrat
 * {@code POST /api/projet/lots/{lotId}/decomptes}) : Sorting suivant, DocRef = Sorting, Notes « DECOMPTE N° n », Code de
 * séquence (format non fixé, Q7), tiers et ratios du lot, fluctuations reprises du précédent, aucune ligne de bordereau ;
 * refus 409 si le dernier décompte du lot est final, 403 sans A_0152_13, 404 lot inconnu. Corps proposé :
 * {@code {"dateDocument": "aaaa-mm-jj"}}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesPariteCreationTest {

    private static final String CORPS = "{\"dateDocument\":\"2026-04-01\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private JdbcTemplate jdbc;

    private String jeton;
    private UUID projet;
    private UUID fournisseur;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PPC-" + jeton);
        fournisseur = fixture.fournisseur("FPC-" + jeton);
        lot = fixture.lot(projet, "LPC-" + jeton, fournisseur);
        admin = decomptes.compte(projet, jeton, DROIT_MODIFIER, DROIT_SUPPRIMER, DROIT_RECALCULER);
    }

    private UUID decompte(String code, int numero) {
        return fixture.decompte(lot, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private int executer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r.getStatus();
    }

    private int creer(UUID lotId, RequestPostProcessor qui) throws Exception {
        return executer(post(LOTS + "/" + lotId + "/decomptes").contentType(MediaType.APPLICATION_JSON).content(CORPS), qui);
    }

    private List<Map<String, Object>> decomptesDuLot() {
        decomptes.flush();
        return jdbc.queryForList("""
                SELECT CAST(PKey AS varchar(36)) AS id, Code, Sorting, DocRef, Notes, CAST(CfgTierId AS varchar(36)) AS tiers,
                    WarrantyRatio, AdvanceRatio, proportionRatio, insuranceDECRatio, insuranceTRCRatio, StockPileRatio,
                    Fluctuations, FluctuationsOld, FinalWorkingOut
                FROM PrjWorkingOut WHERE PrjMarketId = ? ORDER BY Sorting""", lot.toString());
    }

    private Map<String, Object> cree(UUID... existants) {
        List<String> anciens = Arrays.stream(existants).map(u -> u.toString().toUpperCase()).toList();
        List<Map<String, Object>> nouveaux = decomptesDuLot().stream()
                .filter(m -> !anciens.contains(String.valueOf(m.get("id")).toUpperCase())).toList();
        assertThat(nouveaux).hasSize(1);
        return nouveaux.getFirst();
    }

    private int lignesDe(Object decompte) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM PrjWorkingOutLine WHERE PrjWorkingOutId = ?", Integer.class,
                String.valueOf(decompte));
        return n == null ? 0 : n;
    }

    private static BigDecimal nombre(Map<String, Object> m, String colonne) {
        Object v = m.get(colonne);
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    /** §4 : valeurs initiales {@code W:NewWorkingOut} l.1950-2029. */
    @Test
    void creation_nominal_sortingSuivant_docRefNotesCodeTiersRatiosFluctuations_sansLigne() throws Exception {
        decomptes.poser("PrjMarket", lot, "AdvanceRatio", new BigDecimal("0.05"), "ProportionRatio", new BigDecimal("0.02"),
                "InsuranceDECRatio", new BigDecimal("0.01"), "InsuranceTRCRatio", new BigDecimal("0.03"), "StockPileRatio",
                new BigDecimal("0.5"));
        decomptes.ligneMarche(lot, 1, "10", "100", "0.2");
        UUID d1 = decompte("DN1-", 1);
        UUID d2 = decompte("DN2-", 2);
        decomptes.poser("PrjWorkingOut", d2, "Fluctuations", new BigDecimal("250"));

        int statut = creer(lot, admin);

        assertThat(statut).isIn(200, 201);
        Map<String, Object> n = cree(d1, d2);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(n.get("Sorting")).isEqualTo(3);
            s.assertThat(String.valueOf(n.get("DocRef"))).isEqualTo("3");
            s.assertThat(n.get("Notes")).isEqualTo("DECOMPTE N° 3");
            s.assertThat((String) n.get("Code")).isNotBlank().isNotIn("DN1-" + jeton, "DN2-" + jeton);
            s.assertThat(String.valueOf(n.get("tiers"))).isEqualToIgnoringCase(fournisseur.toString());
            s.assertThat(nombre(n, "WarrantyRatio")).isEqualByComparingTo("0.1");
            s.assertThat(nombre(n, "AdvanceRatio")).isEqualByComparingTo("0.05");
            s.assertThat(nombre(n, "proportionRatio")).isEqualByComparingTo("0.02");
            s.assertThat(nombre(n, "insuranceDECRatio")).isEqualByComparingTo("0.01");
            s.assertThat(nombre(n, "insuranceTRCRatio")).isEqualByComparingTo("0.03");
            s.assertThat(nombre(n, "StockPileRatio")).isEqualByComparingTo("0.5");
            s.assertThat(nombre(n, "FluctuationsOld")).isEqualByComparingTo("250");
            s.assertThat(nombre(n, "Fluctuations")).isEqualByComparingTo("250");
            s.assertThat(n.get("FinalWorkingOut")).isNotEqualTo(Boolean.TRUE);
            s.assertThat(lignesDe(n.get("id"))).isZero();
        });
    }

    @Test
    void creation_lotSansDecompte_sorting1_notesDecompteN1() throws Exception {
        assertThat(creer(lot, admin)).isIn(200, 201);

        Map<String, Object> n = cree();
        assertThat(n.get("Sorting")).isEqualTo(1);
        assertThat(String.valueOf(n.get("DocRef"))).isEqualTo("1");
        assertThat(n.get("Notes")).isEqualTo("DECOMPTE N° 1");
    }

    /** Q11 : A_0152_13 suffit, sans être Administrateur. */
    @Test
    void creation_roleAvecDroitModifier_passe() throws Exception {
        assertThat(creer(lot, decomptes.compte(projet, jeton, DROIT_MODIFIER))).isIn(200, 201);
        assertThat(decomptesDuLot()).hasSize(1);
    }

    /** §4 {@code W:1955-1959} : le dernier décompte du lot est final → 409, rien de créé. */
    @Test
    void creation_refus409_siDernierDecompteFinal() throws Exception {
        UUID d = decompte("DNF-", 1);
        decomptes.poser("PrjWorkingOut", d, "FinalWorkingOut", true);

        assertThat(creer(lot, admin)).isEqualTo(409);
        assertThat(decomptesDuLot()).hasSize(1);
    }

    /** §7 : après « Définir en tant que dernier décompte », plus aucun décompte ne peut être créé sur le lot. */
    @Test
    void creation_apresDefinirDernier_refus409() throws Exception {
        decomptes.ligneMarche(lot, 1, "10", "100", "0.2");
        UUID d = decompte("DND-", 1);
        assertThat(executer(post(AppelsProjet.DECOMPTES + "/" + d + DecomptesPariteEcrituresTest.DERNIER), admin))
                .isIn(200, 204);

        assertThat(creer(lot, admin)).isEqualTo(409);
        assertThat(decomptesDuLot()).hasSize(1);
    }

    @Test
    void creation_refus403_sansDroitModifier() throws Exception {
        assertThat(creer(lot, decomptes.compte(projet, jeton, DROIT_SUPPRIMER, DROIT_RECALCULER))).isEqualTo(403);
        assertThat(decomptesDuLot()).isEmpty();
    }

    @Test
    void creation_refus404_lotInconnu() throws Exception {
        assertThat(creer(UUID.randomUUID(), admin)).isEqualTo(404);
    }
}
