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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsPositions.AU;
import static com.tsix.immocrm.controller.achat.AppelsPositions.DU;
import static com.tsix.immocrm.controller.achat.AppelsPositions.MASQUER;
import static com.tsix.immocrm.controller.achat.AppelsPositions.NET_A_PAYER;
import static com.tsix.immocrm.controller.achat.AppelsPositions.POSITIONS_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsPositions.REGLE;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SEARCH;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOCIETE_IDS;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOLDE_DEBUT;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOLDE_FIN;
import static com.tsix.immocrm.controller.achat.AppelsPositions.avecParams;
import static com.tsix.immocrm.controller.achat.AppelsPositions.champ;
import static com.tsix.immocrm.controller.achat.AppelsPositions.fournisseurs;
import static com.tsix.immocrm.controller.achat.AppelsPositions.lignesDe;
import static com.tsix.immocrm.controller.achat.AppelsPositions.montant;
import static com.tsix.immocrm.controller.achat.AppelsPositions.positions;
import static com.tsix.immocrm.controller.achat.AppelsPositions.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Position fournisseur : mouvements (§3.1), fenêtres début / période / fin et arrondi (§3.2), masquage ±1, type FOU,
 * tri, total, filtres — FEATURE-achat-impayes-soldes.md §3, §5, §7. Compte habilité à la société et au projet : le
 * calcul est testé seul, la visibilité est dans {@code PositionsFournisseursVisibiliteTest}. Les règles du test unitaire
 * prévu au §7 ({@code PositionFournisseurTest}) sont pinées ici sur le fil (API du domaine non nommée par le spec).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PositionsFournisseursTest {

    private static final String ROLE = "Commercial";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String DOCUMENT = FixtureAchat.DOCUMENT_A_REGLER;
    private static final String FAUX = "false";
    private static final LocalDate D_2024 = LocalDate.of(2024, 6, 1);
    private static final LocalDate D_2025 = LocalDate.of(2025, 3, 1);
    private static final LocalDate D_2026 = LocalDate.of(2026, 2, 1);
    private static final String DEBUT_2025 = "2025-01-01";
    private static final String FIN_2025 = "2025-12-31";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixturePositionsFournisseurs fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID entite;
    private UUID x;
    private String nomX;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("APF");
        societe = achat.societe("SOC-" + jeton);
        projet = achat.projet("P-" + jeton, societe);
        entite = fixture.entiteReglement(jeton);
        nomX = "Fournisseur X " + jeton;
        x = fixture.fournisseur("FOUX-" + jeton, nomX);
        compte = achat.compte("APF-" + jeton, ROLE);
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

    /** Position de X sans masquage (les soldes proches de zéro restent visibles). */
    private String positionsSansMasque(String... params) throws Exception {
        String[] tous = new String[params.length + 2];
        System.arraycopy(params, 0, tous, 0, params.length);
        tous[params.length] = MASQUER;
        tous[params.length + 1] = FAUX;
        return corps(mockMvc, positions(qui(), tous));
    }

    // ------------------------------------------------------------------ §3.1 mouvements

    @Test
    void r31_factureEtDocumentARegler_auCredit_sansPeriode_debutZero() throws Exception {
        pieceX("-F", FACTURE, D_2025, "1000");
        pieceX("-D", DOCUMENT, D_2025, "200");

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, SOLDE_DEBUT)).isEqualByComparingTo("0"),
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("1200"),
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("0"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("1200"));
    }

    @Test
    void r31_pieceNonPayable_ignoree() throws Exception {
        pieceX("-F", FACTURE, D_2025, "100");
        UUID nonPayable = pieceX("-NP", FACTURE, D_2025, "500");
        achat.poser(nonPayable, "CanBePayed", false);

        String json = positionsSansMasque();

        assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("100");
    }

    @Test
    void r31_avoirFinancier_auDebit() throws Exception {
        pieceX("-F", FACTURE, D_2025, "1000");
        fixture.avoir(jeton + "-AV", D_2025, x, nomX, societe, projet, "300", true, null);

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("300"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("700"));
    }

    /** §3.1 point 1 : l'avoir non financier est réajouté au crédit de sa facture source (et reste au débit, point 2). */
    @Test
    void r31_avoirNonFinancier_reajouteAuCreditDeSaFactureSource() throws Exception {
        UUID facture = pieceX("-F", FACTURE, D_2025, "700");
        fixture.avoir(jeton + "-AV", D_2025, x, nomX, societe, projet, "300", false, facture);

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("300"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("700"));
    }

    @Test
    void r31_reglementRattache_auDebit() throws Exception {
        UUID facture = pieceX("-F", FACTURE, D_2025, "1000");
        UUID rg = fixture.reglement(reglementX("1", D_2025, "400"));
        fixture.ligne(rg, facture, "400");

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("400"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("600"));
    }

    @Test
    void r31_reglementPartiellementRattache_partNonRattacheeComptee() throws Exception {
        UUID facture = pieceX("-F", FACTURE, D_2025, "1000");
        UUID rg = fixture.reglement(reglementX("1", D_2025, "500"));
        fixture.ligne(rg, facture, "300");

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("500"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("500"));
    }

    @Test
    void r31_reglementSansLigne_entierementCompte() throws Exception {
        pieceX("-F", FACTURE, D_2025, "1000");
        fixture.reglement(reglementX("1", D_2025, "250"));

        String json = positionsSansMasque();

        assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("250");
    }

    /** §3.1 point 3 : {@code IsCredit} 1 ou NULL → crédit. */
    @Test
    void r31_reglementIsCreditNullOu1_auCredit() throws Exception {
        pieceX("-F", FACTURE, D_2025, "1000");
        fixture.reglement(reglementX("N", D_2025, "40").credit(null));
        fixture.reglement(reglementX("C", D_2025, "60").credit(true));

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("1100"),
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("0"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("1100"));
    }

    /** Question 2 : un règlement CANCELED compte dans la position, comme la vue OMRAN. */
    @Test
    void r31_q2_reglementAnnule_compte() throws Exception {
        UUID facture = pieceX("-F", FACTURE, D_2025, "1000");
        UUID rg = fixture.reglement(reglementX("A", D_2025, "400").statut(FixtureAchat.ANNULE));
        fixture.ligne(rg, facture, "400");

        String json = positionsSansMasque();

        assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("400");
    }

    @Test
    void r31_reglementSansEntite_ignore() throws Exception {
        pieceX("-F", FACTURE, D_2025, "1000");
        fixture.reglement(reglementX("SE", D_2025, "400").entite(null));

        String json = positionsSansMasque();

        assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("0");
    }

    @Test
    void r31_reglementDeBaseNulle_ignoreAvecSesLignes() throws Exception {
        UUID facture = pieceX("-F", FACTURE, D_2025, "1000");
        UUID rg = fixture.reglement(reglementX("Z", D_2025, "0"));
        fixture.ligne(rg, facture, "50");

        String json = positionsSansMasque();

        assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("0");
    }

    /** §3.1 point 4 : pièce et lignes de règlement multipliées par le taux {@code BuyVentilationExpense} du projet. */
    @Test
    void r31_tauxDeProjet_appliqueALaPieceEtALaLigneDeReglement() throws Exception {
        UUID facture = pieceX("-F", FACTURE, D_2025, "1000");
        fixture.ventilation(facture, projet, "0.5");
        UUID rg = fixture.reglement(reglementX("1", D_2025, "200"));
        fixture.ligne(rg, facture, "200");

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("500"),
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("100"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("400"));
    }

    // ------------------------------------------------------------------ §3.2 fenêtres

    @Test
    void r32_periode_debutAvantDu_periodeEntreDuEtAu_finJusquAAu() throws Exception {
        UUID ancienne = pieceX("-F24", FACTURE, D_2024, "1000");
        pieceX("-F25", FACTURE, D_2025, "500");
        UUID rg = fixture.reglement(reglementX("1", LocalDate.of(2025, 6, 1), "300"));
        fixture.ligne(rg, ancienne, "300");
        pieceX("-F26", FACTURE, D_2026, "700");

        String json = positionsSansMasque(DU, DEBUT_2025, AU, FIN_2025);

        assertAll(
                () -> assertThat(montant(json, x, societe, SOLDE_DEBUT)).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("500"),
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("300"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("1200"));
    }

    @Test
    void r32_bornesInclusives() throws Exception {
        pieceX("-VEILLE", FACTURE, LocalDate.of(2024, 12, 31), "100");
        pieceX("-DU", FACTURE, LocalDate.of(2025, 1, 1), "10");
        pieceX("-AU", FACTURE, LocalDate.of(2025, 12, 31), "1");
        pieceX("-APRES", FACTURE, LocalDate.of(2026, 1, 1), "1000");

        String json = positionsSansMasque(DU, DEBUT_2025, AU, FIN_2025);

        assertAll(
                () -> assertThat(montant(json, x, societe, SOLDE_DEBUT)).isEqualByComparingTo("100"),
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("11"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("111"));
    }

    @Test
    void r32_duAbsent_debutZero_periodeEtFinJusquAAu() throws Exception {
        pieceX("-F24", FACTURE, D_2024, "1000");
        pieceX("-F25", FACTURE, D_2025, "500");
        pieceX("-F26", FACTURE, D_2026, "700");

        String json = positionsSansMasque(AU, FIN_2025);

        assertAll(
                () -> assertThat(montant(json, x, societe, SOLDE_DEBUT)).isEqualByComparingTo("0"),
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("1500"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("1500"));
    }

    @Test
    void r32_auAbsent_finToutLHistorique() throws Exception {
        pieceX("-F24", FACTURE, D_2024, "1000");
        pieceX("-F25", FACTURE, D_2025, "500");
        pieceX("-F26", FACTURE, D_2026, "700");

        String json = positionsSansMasque(DU, DEBUT_2025);

        assertAll(
                () -> assertThat(montant(json, x, societe, SOLDE_DEBUT)).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("1200"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("2200"));
    }

    @Test
    void r32_arrondiTroisDecimales_demiVersLeHaut() throws Exception {
        pieceX("-F", FACTURE, D_2025, "10.0005");

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(json, x, societe, NET_A_PAYER)).isEqualByComparingTo("10.001"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("10.001"));
    }

    // ------------------------------------------------------------------ §3.2 masquage ±1, §2 n° 4 type FOU

    @Test
    void r32_masquerResteZero_defautVrai_retireSoldeFinEntreMoinsUnEtUn() throws Exception {
        UUID a = fournisseurAvecSolde("A", "1", false);
        UUID b = fournisseurAvecSolde("B", "1", true);
        UUID c = fournisseurAvecSolde("C", "1.001", false);
        UUID d = fournisseurAvecSolde("D", "50", false);

        String parDefaut = corps(mockMvc, positions(qui()));
        String sansMasque = positionsSansMasque();

        assertAll(
                () -> assertThat(montant(sansMasque, b, societe, SOLDE_FIN)).isEqualByComparingTo("-1"),
                () -> assertThat(fournisseurs(parDefaut)).contains(c.toString(), d.toString())
                        .doesNotContain(a.toString(), b.toString()),
                () -> assertThat(fournisseurs(sansMasque)).contains(a.toString(), b.toString(), c.toString(),
                        d.toString()));
    }

    /** Fournisseur au solde fin {@code montant} (facture) ou −{@code montant} (avoir financier seul). */
    private UUID fournisseurAvecSolde(String lettre, String montant, boolean negatif) {
        String nom = "Fournisseur " + lettre + " " + jeton;
        UUID f = fixture.fournisseur("FOU" + lettre + "-" + jeton, nom);
        if (negatif) {
            fixture.avoir(jeton + "-AV" + lettre, D_2025, f, nom, societe, projet, montant, true, null);
        } else {
            fixture.piece(jeton + "-F" + lettre, FACTURE, D_2025, f, nom, societe, projet, montant);
        }
        return f;
    }

    @Test
    void r2_fournisseurDeTypeAutreQueFou_absent() throws Exception {
        pieceX("-F", FACTURE, D_2025, "100");
        String nomC = "Client C " + jeton;
        UUID client = fixture.client("CLIC-" + jeton, nomC);
        fixture.piece(jeton + "-FC", FACTURE, D_2025, client, nomC, societe, projet, "900");

        String json = positionsSansMasque();

        assertAll(
                () -> assertThat(fournisseurs(json)).contains(x.toString()).doesNotContain(client.toString()),
                () -> assertThat(total(json, SOLDE_FIN)).isEqualByComparingTo("100"));
    }

    // ------------------------------------------------------------------ §3.2 tri, total ; §5 filtres

    @Test
    void r32_tri_parNomDeFournisseur() throws Exception {
        String nomB = "B-Fourn " + jeton;
        String nomA = "A-Fourn " + jeton;
        UUID b = fixture.fournisseur("FOUB-" + jeton, nomB);
        UUID a = fixture.fournisseur("FOUA-" + jeton, nomA);
        fixture.piece(jeton + "-B", FACTURE, D_2025, b, nomB, societe, projet, "10");
        fixture.piece(jeton + "-A", FACTURE, D_2025, a, nomA, societe, projet, "10");

        String json = corps(mockMvc, positions(qui()));
        List<String> noms = JsonPath.read(json, "$.lignes[*].fournisseurNom");

        assertThat(noms).containsSubsequence(nomA, nomB);
    }

    @Test
    void r32_total_sommeDesLignesRetenues_sansIdentifiants() throws Exception {
        pieceX("-F", FACTURE, D_2025, "1000");
        String nomY = "Fournisseur Y " + jeton;
        UUID y = fixture.fournisseur("FOUY-" + jeton, nomY);
        UUID fy = fixture.piece(jeton + "-FY", FACTURE, D_2025, y, nomY, societe, projet, "300");
        UUID rg = fixture.reglement(Reglement.debit("RGY-" + jeton, D_2025, societe, y, entite, "100"));
        fixture.ligne(rg, fy, "100");
        fournisseurAvecSolde("M", "0.5", false);

        String json = corps(mockMvc, positions(qui()));
        Map<String, Object> ligneTotal = JsonPath.read(json, "$.total");

        assertAll(
                () -> assertThat(total(json, NET_A_PAYER)).isEqualByComparingTo("1300"),
                () -> assertThat(total(json, REGLE)).isEqualByComparingTo("100"),
                () -> assertThat(total(json, SOLDE_FIN)).isEqualByComparingTo("1200"),
                () -> assertThat(total(json, SOLDE_DEBUT)).isEqualByComparingTo("0"),
                () -> assertThat(ligneTotal).extractingByKeys("fournisseurId", "societeId", "societeCode")
                        .containsOnlyNulls());
    }

    @Test
    void r5_recherche_nomFournisseur_espacesEtCasseIgnores() throws Exception {
        pieceX("-F", FACTURE, D_2025, "100");
        String nomZ = "Fourn Zqx " + jeton;
        UUID z = fixture.fournisseur("FOUZ-" + jeton, nomZ);
        fixture.piece(jeton + "-FZ", FACTURE, D_2025, z, nomZ, societe, projet, "100");

        String json = corps(mockMvc, positions(qui(), SEARCH, ("fournzqx" + jeton).toLowerCase()));

        assertThat(fournisseurs(json)).contains(z.toString()).doesNotContain(x.toString());
    }

    @Test
    void r5_uneLigneParSociete_etFiltreSocieteIds() throws Exception {
        String code2 = "SOC2-" + jeton;
        UUID societe2 = achat.societe(code2);
        UUID projet2 = achat.projet("P2-" + jeton, societe2);
        achat.habiliterSociete(compte, societe2);
        achat.habiliter(compte, projet2);
        pieceX("-F1", FACTURE, D_2025, "100");
        fixture.piece(jeton + "-F2", FACTURE, D_2025, x, nomX, societe2, projet2, "200");

        String tous = corps(mockMvc, positions(qui()));
        String surS2 = corps(mockMvc, positions(qui(), SOCIETE_IDS, societe2.toString()));

        assertAll(
                () -> assertThat(montant(tous, x, societe, SOLDE_FIN)).isEqualByComparingTo("100"),
                () -> assertThat(montant(tous, x, societe2, SOLDE_FIN)).isEqualByComparingTo("200"),
                () -> assertThat(champ(tous, x, societe2, "societeCode")).isEqualTo(code2),
                () -> assertThat(champ(tous, x, societe2, "fournisseurNom")).isEqualTo(nomX),
                () -> assertThat(lignesDe(surS2, x, societe)).isEmpty(),
                () -> assertThat(montant(surS2, x, societe2, SOLDE_FIN)).isEqualByComparingTo("200"));
    }

    @Test
    void r5_export_memesLignesQueLaListe() throws Exception {
        pieceX("-F", FACTURE, D_2025, "100");
        String nomM = "Fournisseur M " + jeton;
        UUID m = fixture.fournisseur("FOUM-" + jeton, nomM);
        fixture.piece(jeton + "-FM", FACTURE, D_2025, m, nomM, societe, projet, "0.5");

        List<String> textes = AppelsAchat.textesExport(mockMvc, avecParams(get(POSITIONS_EXPORT).with(qui())));

        assertThat(textes).contains(nomX).doesNotContain(nomM);
    }
}
