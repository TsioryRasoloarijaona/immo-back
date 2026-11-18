package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureEtatsAchat;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.AU;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.DU;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.FACTURE_ID;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.annee;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.brut;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.lignesDe;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureEtatsAchat.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * État de la TVA déductible — FEATURE-n7-etats-achat.md R1, R2, R3, R4, R8, R9, D4 (réponses Q1, Q2 du 18/11).
 * <p>Fixture (année 2093, une société, un compte habilité) :
 * F1 validée, 15/01, 2 lignes : 2 × 1 000 remise 10 % TVA 20 % → HT (1 000 − 100) × 2 = 1 800, TVA 360 ;
 * 1 × 500 TVA 14 % → HT 500, TVA 70 ; F1 = HT 2 300, TVA 430 (en-tête volontairement différent : 2 275 / 455).
 * F2 en attente, 10/03, sans ligne, en-tête HT 1 000, TVA 200. F3 annulée, 20/03, sans ligne, HT 500, TVA 100.
 * Avoir à lignes (01/02) et facture de 2094 : hors état.
 * Totaux hors annulées : 2 factures, HT 2 300 + 1 000 = 3 300, TVA 430 + 200 = 630 (validée 430, en attente 200).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatsAchatTvaTest {

    private static final String MONTANT_HT = "montantHT";
    private static final String MONTANT_TVA = "montantTva";
    private static final String SANS_LIGNES = "sansLignes";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureEtatsAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;
    private UUID f1;
    private UUID f2;
    private UUID f3;
    private UUID avoir;
    private UUID horsPeriode;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("N7T");
        societe = achat.societe("SOC-" + jeton);
        projet = achat.projet("PRJ-" + jeton, societe);
        nom = "Fournisseur TVA " + jeton;
        fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        UUID valide = achat.statut("VAL-" + jeton, true, true, 10);
        UUID attente = achat.statut("ATT-" + jeton, false, false, 1);
        UUID annule = fixture.statutAnnule("ANN-" + jeton);
        compte = fixture.compte("N7T-" + jeton, societe, projet, FixtureEtatsAchat.DROIT_TVA);

        f1 = facture("-F1", date(1, 15), valide, "2730");
        fixture.ligne(f1, 1, "2", "1000", "0.10", "0.20");
        fixture.ligne(f1, 2, "1", "500", "0", "0.14");
        f2 = facture("-F2", date(3, 10), attente, "1200");
        f3 = facture("-F3", date(3, 20), annule, "600");
        avoir = fixture.piece(jeton + "-AV", FixtureAchat.AVOIR, date(2, 1), fournisseur, nom, societe, projet, valide, "360");
        fixture.ligne(avoir, 1, "1", "300", "0", "0.20");
        horsPeriode = facture("-HP", LocalDate.of(FixtureEtatsAchat.ANNEE + 1, 1, 1), valide, "120");
    }

    private UUID facture(String suffixe, LocalDate jour, UUID statut, String aPayer) {
        return fixture.piece(jeton + suffixe, FixtureAchat.FACTURE, jour, fournisseur, nom, societe, projet, statut, aPayer);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, FixtureEtatsAchat.role());
    }

    private String etat() throws Exception {
        return corps(mockMvc, annee(TVA, qui()));
    }

    /** « ht|tva|taux%|remise%|sansLignes » d'une ligne du fil, nombres sans zéros de queue. */
    private static String resume(Map<String, Object> l) {
        return String.join("|", texte(nombre(l, MONTANT_HT)), texte(nombre(l, MONTANT_TVA)),
                texte(nombre(l, "tauxTvaPct")), texte(nombre(l, "remisePct")), String.valueOf(l.get(SANS_LIGNES)));
    }

    private static String texte(BigDecimal b) {
        return b.stripTrailingZeros().toPlainString();
    }

    @Test
    void r1_factureALignes_uneLigneParLigne_htRemiseDeduiteEtTvaHtFoisTaux() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), FACTURE_ID, f1);

        assertThat(lignes).extracting(EtatsAchatTvaTest::resume)
                .containsExactlyInAnyOrder("1800|360|20|10|false", "500|70|14|0|false");
    }

    @Test
    void r1_ligneAvecRemise_quantiteEtPrixUnitaireDuFil() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), FACTURE_ID, f1);
        Map<String, Object> remisee = lignes.stream()
                .filter(l -> nombre(l, "remisePct").signum() != 0).findFirst().orElseThrow();

        assertAll(
                () -> assertThat(nombre(remisee, "quantite")).isEqualByComparingTo("2"),
                () -> assertThat(nombre(remisee, "prixUnitaireHT")).isEqualByComparingTo("1000"),
                () -> assertThat(remisee).containsEntry("factureCode", jeton + "-F1")
                        .containsEntry("fournisseur", nom).containsEntry("annulee", false));
    }

    @Test
    void r2_factureSansLigne_uneLigneLueSurLEnTete_badgeSansLignes() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), FACTURE_ID, f2);

        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(nombre(l, MONTANT_HT)).isEqualByComparingTo("1000"),
                () -> assertThat(nombre(l, MONTANT_TVA)).isEqualByComparingTo("200"),
                () -> assertThat(nombre(l, "tauxTvaPct")).isEqualByComparingTo("20"),
                () -> assertThat(l).containsEntry(SANS_LIGNES, true)));
    }

    @Test
    void r2_tauxDeLaFactureSansLigne_tvaSurHtEnPourcent() throws Exception {
        fixture.enTete(f2, "800", "112");

        List<Map<String, Object>> lignes = lignesDe(etat(), FACTURE_ID, f2);

        assertThat(lignes).singleElement()
                .satisfies(l -> assertThat(nombre(l, "tauxTvaPct")).isEqualByComparingTo("14"))
                .satisfies(l -> assertThat(nombre(l, MONTANT_TVA)).isEqualByComparingTo("112"));
    }

    @Test
    void r3_avoir_horsEtat() throws Exception {
        assertThat(lignesDe(etat(), FACTURE_ID, avoir)).isEmpty();
    }

    @Test
    void r8_factureHorsPeriode_absente() throws Exception {
        assertThat(lignesDe(etat(), FACTURE_ID, horsPeriode)).isEmpty();
    }

    @Test
    void d4_factureAnnulee_listeeAvecAnnuleeVrai() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), FACTURE_ID, f3);

        assertThat(lignes).singleElement().satisfies(l -> assertThat(l).containsEntry("annulee", true));
    }

    @Test
    void r9_totaux_horsAnnulees_facturesHtTva() throws Exception {
        String json = etat();

        assertAll(
                () -> assertThat(nombre(json, "$.totaux.factures")).isEqualByComparingTo("2"),
                () -> assertThat(nombre(json, "$.totaux.montantHT")).isEqualByComparingTo("3300"),
                () -> assertThat(nombre(json, "$.totaux.montantTva")).isEqualByComparingTo("630"));
    }

    @Test
    void r9_totalTva_egalSommeDesLignesNonAnnulees() throws Exception {
        String json = etat();
        List<Object> tvas = JsonPath.read(json, "$.lignes[?(@.annulee == false)].montantTva");
        BigDecimal somme = tvas.stream().map(v -> new BigDecimal(String.valueOf(v))).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(somme).isEqualByComparingTo(nombre(json, "$.totaux.montantTva"));
    }

    @Test
    void r4_q2_tvaValidee_etTvaEnAttente_horsAnnulees() throws Exception {
        String json = etat();

        assertAll(
                () -> assertThat(nombre(json, "$.totaux.montantTvaValidee")).isEqualByComparingTo("430"),
                () -> assertThat(nombre(json, "$.totaux.montantTvaEnAttente")).isEqualByComparingTo("200"));
    }

    @Test
    void parMois_janvierEtMars_horsAnnulees() throws Exception {
        String json = etat();
        List<Map<String, Object>> janvier = JsonPath.read(json, "$.parMois[?(@.mois == '2093-01-01')]");
        List<Map<String, Object>> mars = JsonPath.read(json, "$.parMois[?(@.mois == '2093-03-01')]");

        assertAll(
                () -> assertThat(janvier).extracting(m -> texte(nombre(m, "factures")) + "|" + texte(nombre(m, MONTANT_HT))
                        + "|" + texte(nombre(m, MONTANT_TVA))).containsExactly("1|2300|430"),
                () -> assertThat(mars).extracting(m -> texte(nombre(m, "factures")) + "|" + texte(nombre(m, MONTANT_HT))
                        + "|" + texte(nombre(m, MONTANT_TVA))).containsExactly("1|1000|200"));
    }

    @Test
    void r8_bornesIncluses_auJusquaMinuit() throws Exception {
        UUID debut = facture("-B1", date(6, 1), null, "120");
        UUID finJour = facture("-B2", date(6, 30), null, "120");
        fixture.dater(finJour, LocalDateTime.of(FixtureEtatsAchat.ANNEE, 6, 30, 23, 30));
        UUID lendemain = facture("-B3", date(7, 1), null, "120");
        UUID veille = facture("-B4", date(5, 31), null, "120");
        fixture.dater(veille, LocalDateTime.of(FixtureEtatsAchat.ANNEE, 5, 31, 23, 59));

        String json = corps(mockMvc, brut(TVA, qui(), DU, "2093-06-01", AU, "2093-06-30"));

        assertAll(
                () -> assertThat(lignesDe(json, FACTURE_ID, debut)).hasSize(1),
                () -> assertThat(lignesDe(json, FACTURE_ID, finJour)).hasSize(1),
                () -> assertThat(lignesDe(json, FACTURE_ID, lendemain)).isEmpty(),
                () -> assertThat(lignesDe(json, FACTURE_ID, veille)).isEmpty());
    }
}
