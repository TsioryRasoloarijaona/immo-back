package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.montant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot A2a, E2 (en-tête, retenues / avances, Vérification n° 8) et E5 (historique : transition, heure, n° 7).
 * Les noms des 10 lignes de {@code RetenuesAvancesAchatDto} ne sont pas fixés par le spec : le bloc est lu sans
 * figer ses clés (toutes les valeurs numériques qu'il contient).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFicheEnteteTest {

    private static final String ROLE = "Commercial";
    private static final String RETENUES = "retenuesAvances";

    /** Les 16 colonnes du bloc de droite, valeurs toutes distinctes (ratios en fraction). */
    private static final Map<String, String> COLONNES_RETENUES = new LinkedHashMap<>();

    static {
        COLONNES_RETENUES.put("AdvanceRatio", "0.11");
        COLONNES_RETENUES.put("AdvanceAmount", "111");
        COLONNES_RETENUES.put("AdvanceRest", "112");
        COLONNES_RETENUES.put("WarrantyRatio", "0.07");
        COLONNES_RETENUES.put("WarrantyAmount", "107");
        COLONNES_RETENUES.put("InsuranceTRCRatio", "0.013");
        COLONNES_RETENUES.put("InsuranceTRCAmount", "113");
        COLONNES_RETENUES.put("InsuranceDECRatio", "0.014");
        COLONNES_RETENUES.put("InsuranceDECAmount", "114");
        COLONNES_RETENUES.put("ProportionRatio", "0.015");
        COLONNES_RETENUES.put("ProportionAmount", "115");
        COLONNES_RETENUES.put("RetainedAmount", "116");
        COLONNES_RETENUES.put("WorkingOutAdvanceAmount", "117");
        COLONNES_RETENUES.put("StockAmountToPay", "118");
        COLONNES_RETENUES.put("ProrataRefundAmount", "119");
        COLONNES_RETENUES.put("FluctuationsToPay", "120");
    }

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
        jeton = FixtureAchat.jeton("AFE");
        societe = fixture.societe("SOC-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
    }

    private UUID piece(String suffixe, String nature, String aPayer) {
        return fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 4, 2), fournisseur, nom, societe, projet,
                new BigDecimal(aPayer)).nature(nature));
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    private String fiche(UUID piece) throws Exception {
        return corps(mockMvc, get(AppelsAchat.DOCUMENT, piece).with(connecte(compte, ROLE)));
    }

    // ------------------------------------------------------------------ E2 : champs d'en-tête

    @Test
    void e2_declencheur_idEtNom() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, "1200");
        UUID declencheur = fixture.fournisseur("DEC-" + jeton, "Déclencheur " + jeton);
        fixture.poser(facture, "CfgTierTriggerId", declencheur);

        String json = fiche(facture);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.declencheurId")).isEqualTo(declencheur.toString()),
                () -> assertThat((Object) JsonPath.read(json, "$.declencheurNom")).isEqualTo("Déclencheur " + jeton));
    }

    @Test
    void e2_dateLivraisonSouhaitee_etNote2() throws Exception {
        UUID bc = piece("-BC", FixtureAchat.BON_COMMANDE, "1200");
        fixture.poser(bc, "PrevisionDeliveryDate", LocalDate.of(2026, 6, 15).atStartOfDay());
        fixture.poser(bc, "Notes2", "Seconde note " + jeton);

        String json = fiche(bc);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.dateLivraisonSouhaitee")).isEqualTo("2026-06-15"),
                () -> assertThat((Object) JsonPath.read(json, "$.note2")).isEqualTo("Seconde note " + jeton));
    }

    @Test
    void e2_devisSelectionne_vraiSiIsSelected() throws Exception {
        UUID retenu = piece("-DEV1", FixtureAchat.DEVIS, "100");
        UUID ecarte = piece("-DEV2", FixtureAchat.DEVIS, "100");
        fixture.poser(retenu, "IsSelected", true);
        fixture.poser(ecarte, "IsSelected", false);

        String jsonRetenu = fiche(retenu);
        String jsonEcarte = fiche(ecarte);

        assertAll(
                () -> assertThat((Object) JsonPath.read(jsonRetenu, "$.devisSelectionne")).isEqualTo(true),
                () -> assertThat((Object) JsonPath.read(jsonEcarte, "$.devisSelectionne")).isEqualTo(false));
    }

    @Test
    void e2_devisOrigine_duBonDeCommande() throws Exception {
        UUID devis = piece("-DEV", FixtureAchat.DEVIS, "100");
        UUID bc = piece("-BC", FixtureAchat.BON_COMMANDE, "100");
        fixture.poser(bc, "BuyQuoteId", devis);

        String json = fiche(bc);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.devisOrigineId")).isEqualTo(devis.toString()),
                () -> assertThat((Object) JsonPath.read(json, "$.devisOrigineCode")).isEqualTo(code("-DEV")));
    }

    @Test
    void e2_bonCommande_duBonDeLivraison() throws Exception {
        UUID bc = piece("-BC", FixtureAchat.BON_COMMANDE, "100");
        UUID bl = piece("-BL", FixtureAchat.BON_LIVRAISON, "100");
        fixture.poser(bl, "BuyOrderId", bc);

        String json = fiche(bl);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.bonCommandeId")).isEqualTo(bc.toString()),
                () -> assertThat((Object) JsonPath.read(json, "$.bonCommandeCode")).isEqualTo(code("-BC")));
    }

    @Test
    void e2_factureSource_delAvoir() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, "1200");
        UUID avoir = piece("-AV", FixtureAchat.AVOIR, "300");
        fixture.poser(avoir, "InvoiceSourceId", facture);

        String json = fiche(avoir);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.factureSourceId")).isEqualTo(facture.toString()),
                () -> assertThat((Object) JsonPath.read(json, "$.factureSourceCode")).isEqualTo(code("-FAC")));
    }

    /** E2 + n° 8 : Σ TTC des avoirs de la facture, affiché, jamais soustrait du montant à payer. */
    @Test
    void e2_avoirsMontant_sommeTtcDesAvoirs_nEntrePasDansMontantAPayer() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, "1200");
        fixture.poser(piece("-AV1", FixtureAchat.AVOIR, "300"), "InvoiceSourceId", facture);
        fixture.poser(piece("-AV2", FixtureAchat.AVOIR, "60"), "InvoiceSourceId", facture);

        String json = fiche(facture);

        assertAll(
                () -> assertThat(montant(json, "$.avoirsMontant")).isEqualByComparingTo("360"),
                () -> assertThat(montant(json, "$.montantAPayer")).isEqualByComparingTo("1200"));
    }

    // ------------------------------------------------------------------ E2 : retenues / avances (n° 8)

    @Test
    void e2_retenuesAvances_lesSeizeValeursLuesTellesQuelles() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, "1200");
        COLONNES_RETENUES.forEach((colonne, valeur) -> fixture.poser(facture, colonne, new BigDecimal(valeur)));

        String json = fiche(facture);

        List<BigDecimal> attendues = COLONNES_RETENUES.values().stream().map(BigDecimal::new).toList();
        assertThat(valeursNumeriques(json))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrderElementsOf(attendues);
    }

    @Test
    void e2_retenuesAvances_colonnesNull_valeursNull() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, "1200");
        COLONNES_RETENUES.keySet().forEach(colonne -> fixture.effacer(facture, colonne));

        String json = fiche(facture);

        Map<String, Object> racine = JsonPath.read(json, "$");
        assertAll(
                () -> assertThat(racine).containsKey(RETENUES),
                () -> assertThat(valeursNumeriques(json)).isEmpty());
    }

    @Test
    void e2_retenuesAvances_montantAPayerInchange() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, "1200");
        COLONNES_RETENUES.forEach((colonne, valeur) -> fixture.poser(facture, colonne, new BigDecimal(valeur)));

        String json = fiche(facture);

        assertAll(
                () -> assertThat(valeursNumeriques(json)).hasSize(COLONNES_RETENUES.size()),
                () -> assertThat(montant(json, "$.montantAPayer")).isEqualByComparingTo("1200"));
    }

    // ------------------------------------------------------------------ E5 : historique

    @Test
    void e5_historique_transitionRenseignee_nullALaCreation_heureConservee() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, "1200");
        UUID enCours = fixture.statut("ENC-" + jeton, false, false, 1);
        UUID valide = fixture.statut("VAL-" + jeton, true, false, 2);
        UUID transition = fixture.transition(enCours, valide, "Valider DT " + jeton);
        fixture.historique(facture, enCours, LocalDateTime.of(2026, 5, 4, 8, 15), compte, "Création");
        fixture.historique(facture, valide, LocalDateTime.of(2026, 5, 10, 17, 30), compte, "Validée", transition);

        String json = fiche(facture);

        List<Object> transitions = JsonPath.read(json, "$.historique[*].transition");
        assertAll(
                () -> assertThat(transitions).containsExactly(null, "Valider DT " + jeton),
                () -> assertThat((String) JsonPath.read(json, "$.historique[1].date")).startsWith("2026-05-10T17:30"),
                () -> assertThat((String) JsonPath.read(json, "$.historique[0].date")).startsWith("2026-05-04T08:15"));
    }

    /** Toutes les valeurs numériques non nulles du bloc {@code retenuesAvances}, quelle que soit sa forme. */
    private static List<BigDecimal> valeursNumeriques(String json) {
        List<Object> noeuds = JsonPath.read(json, "$." + RETENUES + "..*");
        return noeuds.stream().filter(Number.class::isInstance).map(v -> new BigDecimal(String.valueOf(v))).toList();
    }
}
