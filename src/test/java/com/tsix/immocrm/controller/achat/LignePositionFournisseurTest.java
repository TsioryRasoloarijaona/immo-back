package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixturePositionsFournisseurs;
import com.tsix.immocrm.fixtures.FixturePositionsFournisseurs.Reglement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsPositions.AU;
import static com.tsix.immocrm.controller.achat.AppelsPositions.MASQUER;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOCIETE_ID;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOLDE_FIN;
import static com.tsix.immocrm.controller.achat.AppelsPositions.detail;
import static com.tsix.immocrm.controller.achat.AppelsPositions.montant;
import static com.tsix.immocrm.controller.achat.AppelsPositions.nombre;
import static com.tsix.immocrm.controller.achat.AppelsPositions.positions;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Fenêtre « Ligne de solde » ({@code GET /api/achat/fournisseurs/{fournisseurId}/position}) : une ligne par mouvement,
 * colonnes de {@code PositionLine.cs}, invariant Σ crédit − Σ débit = solde fin — FEATURE-achat-impayes-soldes.md §5, §7.
 * Montant d'une ligne pour l'invariant : {@code aPayer} pour une pièce, {@code reglement} pour un règlement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LignePositionFournisseurTest {

    private static final String ROLE = "Commercial";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String DOCUMENT = FixtureAchat.DOCUMENT_A_REGLER;
    private static final String AVOIR = FixtureAchat.AVOIR;
    private static final String REGLEMENT = "REGLEMENT";
    private static final String NON_RATTACHE = "REGLEMENT_NON_RATTACHE";
    private static final Set<String> PIECES = Set.of(FACTURE, DOCUMENT, AVOIR);
    private static final String CREDIT = "credit";
    private static final String A_PAYER = "aPayer";
    private static final String REGLE = "reglement";
    private static final String PROJET_ID = "projetId";
    private static final String DATE_DOCUMENT = "dateDocument";
    private static final LocalDate DATE = LocalDate.of(2025, 2, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixturePositionsFournisseurs fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private String projetCode;
    private UUID entite;
    private UUID x;
    private String nomX;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("APL");
        societe = achat.societe("SOC-" + jeton);
        projetCode = "P-" + jeton;
        projet = achat.projet(projetCode, societe);
        entite = fixture.entiteReglement(jeton);
        nomX = "Fournisseur X " + jeton;
        x = fixture.fournisseur("FOUX-" + jeton, nomX);
        compte = achat.compte("APL-" + jeton, ROLE);
        achat.habiliterSociete(compte, societe);
        achat.habiliter(compte, projet);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private UUID pieceX(String suffixe, String nature, LocalDate date, String aPayer) {
        return fixture.piece(jeton + suffixe, nature, date, x, nomX, societe, projet, aPayer);
    }

    private Reglement reglementX(String suffixe, LocalDate date, String base) {
        return Reglement.debit("RG" + suffixe + "-" + jeton, date, societe, x, entite, base);
    }

    /**
     * F1 facture (TTC 1 200, à payer 1 100, stock 10, fluctuations 5, prorata 2), D1 document 300, avoir financier 200,
     * R1 règlement 500 dont 400 sur F1, R2 règlement 50 {@code IsCredit} NULL sans ligne : solde fin 750.
     */
    private UUID jeuComplet() {
        UUID f1 = pieceX("-F1", FACTURE, DATE, "1100");
        achat.montants(f1, "1000", "1200", "1100");
        achat.poser(f1, "StockAmountToPay", new BigDecimal("10"));
        achat.poser(f1, "FluctuationsToPay", new BigDecimal("5"));
        achat.poser(f1, "ProrataRefundAmount", new BigDecimal("2"));
        pieceX("-D1", DOCUMENT, LocalDate.of(2025, 3, 1), "300");
        fixture.avoir(jeton + "-AV", LocalDate.of(2025, 4, 1), x, nomX, societe, projet, "200", true, null);
        UUID r1 = fixture.reglement(reglementX("1", LocalDate.of(2025, 5, 1), "500"));
        fixture.ligne(r1, f1, "400");
        fixture.reglement(reglementX("2", LocalDate.of(2025, 6, 1), "50").credit(null));
        return f1;
    }

    private List<Map<String, Object>> lignes(String... params) throws Exception {
        String[] tous = new String[params.length + 2];
        tous[0] = SOCIETE_ID;
        tous[1] = societe.toString();
        System.arraycopy(params, 0, tous, 2, params.length);
        return JsonPath.read(corps(mockMvc, detail(qui(), x, tous)), "$");
    }

    private static List<Map<String, Object>> deType(List<Map<String, Object>> lignes, String type) {
        return lignes.stream().filter(l -> type.equals(l.get("type"))).toList();
    }

    private static Map<String, Object> seule(List<Map<String, Object>> lignes, String type) {
        List<Map<String, Object>> trouvees = deType(lignes, type);
        assertThat(trouvees).as("ligne %s", type).hasSize(1);
        return trouvees.get(0);
    }

    /** Σ crédit − Σ débit ; montant = à payer (pièce) ou règlement (règlement). */
    private static BigDecimal solde(List<Map<String, Object>> lignes) {
        BigDecimal s = BigDecimal.ZERO;
        for (Map<String, Object> l : lignes) {
            BigDecimal m = nombre(l.get(PIECES.contains((String) l.get("type")) ? A_PAYER : REGLE));
            s = Boolean.TRUE.equals(l.get(CREDIT)) ? s.add(m) : s.subtract(m);
        }
        return s;
    }

    // ------------------------------------------------------------------ invariant

    @Test
    void r5_invariant_sommeDesLignes_egaleSoldeFin_sansPeriode() throws Exception {
        jeuComplet();

        List<Map<String, Object>> lignes = lignes();
        String positions = corps(mockMvc, positions(qui(), MASQUER, "false"));

        assertAll(
                () -> assertThat(lignes).isNotEmpty(),
                () -> assertThat(solde(lignes)).isEqualByComparingTo("750"),
                () -> assertThat(montant(positions, x, societe, SOLDE_FIN)).isEqualByComparingTo(solde(lignes)));
    }

    @Test
    void r5_invariant_avecAu_sansLesMouvementsPosterieurs() throws Exception {
        jeuComplet();
        pieceX("-F26", FACTURE, LocalDate.of(2026, 2, 1), "9000");

        List<Map<String, Object>> lignes = lignes(AU, "2025-12-31");
        String positions = corps(mockMvc, positions(qui(), AU, "2025-12-31", MASQUER, "false"));

        assertAll(
                () -> assertThat(solde(lignes)).isEqualByComparingTo("750"),
                () -> assertThat(montant(positions, x, societe, SOLDE_FIN)).isEqualByComparingTo("750"));
    }

    // ------------------------------------------------------------------ colonnes

    @Test
    void r5_uneLigneParMouvement_typesDuSpec() throws Exception {
        jeuComplet();

        List<String> types = lignes().stream().map(l -> (String) l.get("type")).toList();

        assertThat(types).containsExactlyInAnyOrder(FACTURE, DOCUMENT, AVOIR, REGLEMENT, NON_RATTACHE, NON_RATTACHE);
    }

    /** {@code PositionLine.cs} : TTC = (TTC + stock + fluctuations + prorata) × taux ; retenues = à payer − TTC. */
    @Test
    void r5_facture_colonnes() throws Exception {
        jeuComplet();

        Map<String, Object> f = seule(lignes(), FACTURE);

        assertAll(
                () -> assertThat(f).containsEntry("code", jeton + "-F1"),
                () -> assertThat(f).containsEntry(DATE_DOCUMENT, "2025-02-01"),
                () -> assertThat(f).containsEntry("dateEcheance", "2025-03-03"),
                () -> assertThat(f).containsEntry(PROJET_ID, projet.toString()),
                () -> assertThat(f).containsEntry("projetCode", projetCode),
                () -> assertThat(f).containsEntry(CREDIT, true),
                () -> assertThat(nombre(f.get("montantTTC"))).isEqualByComparingTo("1217"),
                () -> assertThat(nombre(f.get(A_PAYER))).isEqualByComparingTo("1100"),
                () -> assertThat(nombre(f.get("retenues"))).isEqualByComparingTo("-117"));
    }

    @Test
    void r5_avoir_auDebit() throws Exception {
        jeuComplet();

        Map<String, Object> av = seule(lignes(), AVOIR);

        assertAll(
                () -> assertThat(av).containsEntry(CREDIT, false),
                () -> assertThat(nombre(av.get(A_PAYER))).isEqualByComparingTo("200"));
    }

    @Test
    void r5_reglementRattache_auDebit_avecLeProjetDeLaPiece() throws Exception {
        jeuComplet();

        Map<String, Object> r = seule(lignes(), REGLEMENT);

        assertAll(
                () -> assertThat(r).containsEntry(CREDIT, false),
                () -> assertThat(r).containsEntry(PROJET_ID, projet.toString()),
                () -> assertThat(r).containsEntry(DATE_DOCUMENT, "2025-05-01"),
                () -> assertThat(nombre(r.get(REGLE))).isEqualByComparingTo("400"));
    }

    @Test
    void r5_partNonRattachee_sansProjet_sensDuReglement() throws Exception {
        jeuComplet();

        List<Map<String, Object>> nonRattachees = deType(lignes(), NON_RATTACHE);
        Map<String, Object> debit = nonRattachees.stream().filter(l -> Boolean.FALSE.equals(l.get(CREDIT)))
                .findFirst().orElseThrow();
        Map<String, Object> credit = nonRattachees.stream().filter(l -> Boolean.TRUE.equals(l.get(CREDIT)))
                .findFirst().orElseThrow();

        assertAll(
                () -> assertThat(nonRattachees).hasSize(2),
                () -> assertThat(debit.get(PROJET_ID)).isNull(),
                () -> assertThat(nombre(debit.get(REGLE))).isEqualByComparingTo("100"),
                () -> assertThat(credit.get(PROJET_ID)).isNull(),
                () -> assertThat(nombre(credit.get(REGLE))).isEqualByComparingTo("50"));
    }

    @Test
    void r5_reglement_baseEtRas() throws Exception {
        fixture.reglement(reglementX("B", DATE, "1000").ras("100"));

        Map<String, Object> r = seule(lignes(), NON_RATTACHE);

        assertAll(
                () -> assertThat(nombre(r.get("base"))).isEqualByComparingTo("1000"),
                () -> assertThat(nombre(r.get("ras"))).isEqualByComparingTo("100"));
    }

    @Test
    void r31_tauxDeProjet_appliqueAuxColonnesDeLaPiece() throws Exception {
        UUID f = pieceX("-F", FACTURE, DATE, "1000");
        fixture.ventilation(f, projet, "0.5");

        Map<String, Object> ligne = seule(lignes(), FACTURE);

        assertAll(
                () -> assertThat(nombre(ligne.get(A_PAYER))).isEqualByComparingTo("500"),
                () -> assertThat(nombre(ligne.get("montantTTC"))).isEqualByComparingTo("500"));
    }

    // ------------------------------------------------------------------ périmètre de la fenêtre

    @Test
    void r5_seulementLaSocieteDemandee() throws Exception {
        pieceX("-F", FACTURE, DATE, "100");
        UUID societe2 = achat.societe("SOC2-" + jeton);
        UUID projet2 = achat.projet("P2-" + jeton, societe2);
        achat.habiliterSociete(compte, societe2);
        achat.habiliter(compte, projet2);
        fixture.piece(jeton + "-F2", FACTURE, DATE, x, nomX, societe2, projet2, "200");

        List<String> codes = lignes().stream().map(l -> (String) l.get("code")).toList();

        assertThat(codes).contains(jeton + "-F").doesNotContain(jeton + "-F2");
    }

    @Test
    void r31_pieceNonPayable_absente() throws Exception {
        pieceX("-F", FACTURE, DATE, "100");
        UUID nonPayable = pieceX("-NP", FACTURE, DATE, "500");
        achat.poser(nonPayable, "CanBePayed", false);

        List<String> codes = lignes().stream().map(l -> (String) l.get("code")).toList();

        assertThat(codes).contains(jeton + "-F").doesNotContain(jeton + "-NP");
    }
}
