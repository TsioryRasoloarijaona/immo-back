package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.LigneAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.montant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot A2a, E3 (Vérification n° 5) : colonnes calculées des lignes, règle de la vue OMRAN {@code BuyDocumentLineView} —
 * net = PU HT × (1 − remise) × (1 + FODEC) ; total HT = net × qté ; TVA = total HT × taux ; TTC = HT + TVA ; pleine
 * échelle, sans arrondi par ligne ; récap {@code recapTva} groupé par taux ({@code base} = Σ HT, {@code montantTVA} = Σ TVA).
 * <pre>
 *   L1 « R+F »  : qté 10, PU 100, remise 10 %, FODEC 1 %, TVA 20 % → net 90,9 ; HT 909 ; TVA 181,8 ; TTC 1 090,8
 *   L2 « T0 »   : qté 3,  PU 50,  remise 0,    FODEC 0,   TVA 0    → net 50   ; HT 150 ; TVA 0     ; TTC 150
 *   L3 « T20 »  : qté 2,  PU 200, remise 0,    FODEC 0,   TVA 20 % → net 200  ; HT 400 ; TVA 80    ; TTC 480 ; sans code
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFicheLignesTest {

    private static final String ROLE = "Commercial";
    private static final BigDecimal VINGT = new BigDecimal("0.2");
    private static final String L1 = "$.lignes[?(@.ordre == 1)].";
    private static final String L2 = "$.lignes[?(@.ordre == 2)].";
    private static final String L3 = "$.lignes[?(@.ordre == 3)].";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AFL");
        societe = fixture.societe("SOC-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
    }

    private UUID facture(String suffixe) {
        return fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 5, 4), fournisseur, nom, societe, projet,
                new BigDecimal("1750.8")));
    }

    private UUID factureATroisLignes() {
        UUID piece = facture("-TROIS");
        fixture.ligne(piece, new LigneAchat(1, "C-RF", "Remise et FODEC", "U", new BigDecimal("10"),
                new BigDecimal("100"), VINGT, new BigDecimal("0.1"), new BigDecimal("0.01"), new BigDecimal("121.2")));
        fixture.ligne(piece, new LigneAchat(2, "C-T0", "Taux zéro", "M2", new BigDecimal("3"),
                new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50")));
        fixture.ligne(piece, new LigneAchat(3, null, "Sans code", "SAC", new BigDecimal("2"),
                new BigDecimal("200"), VINGT, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("240")));
        fixture.poser(piece, "AmountHT", new BigDecimal("1459"));
        return piece;
    }

    private String fiche(UUID piece) throws Exception {
        return corps(mockMvc, get(AppelsAchat.DOCUMENT, piece).with(connecte(compte, ROLE)));
    }

    @Test
    void e3_ligneAvecRemiseEtFodec_netHtTvaTtcPleineEchelle() throws Exception {
        String json = fiche(factureATroisLignes());

        assertAll(
                () -> assertThat(un(json, L1 + "prixUnitaireHTNet")).isEqualByComparingTo("90.9"),
                () -> assertThat(un(json, L1 + "totalHT")).isEqualByComparingTo("909"),
                () -> assertThat(un(json, L1 + "totalTVA")).isEqualByComparingTo("181.8"),
                () -> assertThat(un(json, L1 + "totalTTC")).isEqualByComparingTo("1090.8"),
                () -> assertThat(un(json, L1 + "fodecPourcent")).isEqualByComparingTo("1"));
    }

    @Test
    void e3_ligneTauxZero_tvaNulle_ttcEgalHt() throws Exception {
        String json = fiche(factureATroisLignes());

        assertAll(
                () -> assertThat(un(json, L2 + "prixUnitaireHTNet")).isEqualByComparingTo("50"),
                () -> assertThat(un(json, L2 + "totalHT")).isEqualByComparingTo("150"),
                () -> assertThat(un(json, L2 + "totalTVA")).isEqualByComparingTo("0"),
                () -> assertThat(un(json, L2 + "totalTTC")).isEqualByComparingTo("150"));
    }

    @Test
    void e3_ligneSansRemiseNiFodec_tva20() throws Exception {
        String json = fiche(factureATroisLignes());

        assertAll(
                () -> assertThat(un(json, L3 + "prixUnitaireHTNet")).isEqualByComparingTo("200"),
                () -> assertThat(un(json, L3 + "totalHT")).isEqualByComparingTo("400"),
                () -> assertThat(un(json, L3 + "totalTVA")).isEqualByComparingTo("80"),
                () -> assertThat(un(json, L3 + "totalTTC")).isEqualByComparingTo("480"));
    }

    @Test
    void e3_code_etPuTtcStocke_luTelsQuels_codeNulPossible() throws Exception {
        String json = fiche(factureATroisLignes());

        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, L1 + "code")).containsExactly("C-RF"),
                () -> assertThat((List<Object>) JsonPath.read(json, L3 + "code")).hasSize(1).containsOnlyNulls(),
                () -> assertThat(un(json, L1 + "prixUnitaireTTC")).isEqualByComparingTo("121.2"),
                () -> assertThat(un(json, L2 + "prixUnitaireTTC")).isEqualByComparingTo("50"));
    }

    @Test
    void e3_recapTva_groupeParTaux_baseEtMontantSommes() throws Exception {
        String json = fiche(factureATroisLignes());

        List<Object> recap = JsonPath.read(json, "$.recapTva[*]");
        assertAll(
                () -> assertThat(recap).hasSize(2),
                () -> assertThat(un(json, "$.recapTva[?(@.base == 1309)].montantTVA")).isEqualByComparingTo("261.8"),
                () -> assertThat(un(json, "$.recapTva[?(@.base == 150)].montantTVA")).isEqualByComparingTo("0"));
    }

    @Test
    void e3_sommeDesTotauxHt_egaleMontantHtDeLaPiece_aDeuxCentimesPres() throws Exception {
        String json = fiche(factureATroisLignes());

        List<Object> totaux = JsonPath.read(json, "$.lignes[*].totalHT");
        BigDecimal somme = totaux.stream().map(v -> new BigDecimal(String.valueOf(v)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somme.subtract(montant(json, "$.montantHT")).abs()).isLessThanOrEqualTo(new BigDecimal("0.02"));
    }

    @Test
    void e3_pieceSansLigne_lignesEtRecapVides() throws Exception {
        String json = fiche(facture("-VIDE"));

        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.lignes")).isEmpty(),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.recapTva")).isEmpty());
    }

    /** Seule valeur d'un chemin filtré, en montant. */
    private static BigDecimal un(String json, String chemin) {
        List<Object> valeurs = JsonPath.read(json, chemin);
        assertThat(valeurs).as(chemin).hasSize(1).doesNotContainNull();
        return new BigDecimal(String.valueOf(valeurs.get(0)));
    }
}
