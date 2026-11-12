package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixturePositionsFournisseurs;
import com.tsix.immocrm.fixtures.FixturePositionsFournisseurs.Reglement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static com.tsix.immocrm.controller.achat.AppelsPositions.MASQUER;
import static com.tsix.immocrm.controller.achat.AppelsPositions.POSITIONS_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsPositions.PROJET_IDS;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOCIETE_ID;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOCIETE_IDS;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOLDE_FIN;
import static com.tsix.immocrm.controller.achat.AppelsPositions.detail;
import static com.tsix.immocrm.controller.achat.AppelsPositions.fournisseurs;
import static com.tsix.immocrm.controller.achat.AppelsPositions.lignesDe;
import static com.tsix.immocrm.controller.achat.AppelsPositions.montant;
import static com.tsix.immocrm.controller.achat.AppelsPositions.positions;
import static com.tsix.immocrm.controller.achat.AppelsPositions.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité de la position fournisseur (règle 6) : société habilitée ET (projet habilité OU sans projet), règlement non
 * rattaché visible avec sa société, filtre projets explicite, refus — FEATURE-achat-impayes-soldes.md §4, §5.
 * Projet A habilité, projet B non ; société T non habilitée (son projet l'est, pour prouver que la société prime).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PositionsFournisseursVisibiliteTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String FAUX = "false";
    private static final LocalDate DATE = LocalDate.of(2025, 3, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixturePositionsFournisseurs fixture;

    private String jeton;
    private UUID societe;
    private UUID societeT;
    private UUID projetA;
    private UUID projetB;
    private UUID projetT;
    private UUID entite;
    private UUID x;
    private String nomX;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("APV");
        societe = achat.societe("SOC-" + jeton);
        societeT = achat.societe("SOCT-" + jeton);
        projetA = achat.projet("PA-" + jeton, societe);
        projetB = achat.projet("PB-" + jeton, societe);
        projetT = achat.projet("PT-" + jeton, societeT);
        entite = fixture.entiteReglement(jeton);
        nomX = "Fournisseur X " + jeton;
        x = fixture.fournisseur("FOUX-" + jeton, nomX);
        compte = achat.compte("APV-" + jeton, COMMERCIAL);
        achat.habiliterSociete(compte, societe);
        achat.habiliter(compte, projetA);
        achat.habiliter(compte, projetT);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, COMMERCIAL);
    }

    private UUID facture(String suffixe, UUID soc, UUID projet, String aPayer) {
        return fixture.piece(jeton + suffixe, FACTURE, DATE, x, nomX, soc, projet, aPayer);
    }

    private UUID reglement(String suffixe, String base) {
        return fixture.reglement(Reglement.debit("RG" + suffixe + "-" + jeton, DATE, societe, x, entite, base));
    }

    @Test
    void r4_projetNonHabilite_sesPiecesExclues() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-B", societe, projetB, "1000");

        String json = corps(mockMvc, positions(qui()));

        assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("100");
    }

    @Test
    void r4_ligneDeReglementSurPieceDUnProjetNonHabilite_exclue_partNonRattacheeVisible() throws Exception {
        facture("-A", societe, projetA, "100");
        UUID b = facture("-B", societe, projetB, "1000");
        UUID rg = reglement("1", "500");
        fixture.ligne(rg, b, "300");

        String json = corps(mockMvc, positions(qui()));

        assertAll(
                () -> assertThat(montant(json, x, societe, "regle")).isEqualByComparingTo("200"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("-100"));
    }

    @Test
    void r4_pieceSansProjet_visibleSiSaSocieteEstHabilitee() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-0", societe, null, "50");

        String json = corps(mockMvc, positions(qui()));

        assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("150");
    }

    @Test
    void r4_societeNonHabilitee_aucuneLigne_memeAvecProjetHabilite() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-T", societeT, projetT, "700");

        String json = corps(mockMvc, positions(qui(), MASQUER, FAUX));

        assertAll(
                () -> assertThat(lignesDe(json, x, societeT)).isEmpty(),
                () -> assertThat(total(json, SOLDE_FIN)).isEqualByComparingTo("100"));
    }

    @Test
    void r4_filtreSocieteNonHabilitee_ignore_jamais403() throws Exception {
        facture("-T", societeT, projetT, "700");

        String json = corps(mockMvc, positions(qui(), SOCIETE_IDS, societeT.toString(), MASQUER, FAUX));

        assertAll(
                () -> assertThat(lignesDe(json, x, societeT)).isEmpty(),
                () -> assertThat(total(json, SOLDE_FIN)).isEqualByComparingTo("0"));
    }

    @Test
    void r4_compteSansHabilitationSociete_listeVide() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-0", societe, null, "50");
        AdmUser projetSeul = achat.compte("APV-P-" + jeton, COMMERCIAL);
        achat.habiliter(projetSeul, projetA);

        String json = corps(mockMvc, positions(connecte(projetSeul, COMMERCIAL)));

        assertAll(
                () -> assertThat(fournisseurs(json)).isEmpty(),
                () -> assertThat(total(json, SOLDE_FIN)).isEqualByComparingTo("0"));
    }

    @Test
    void r4_societeSeuleSansProjet_neVoitQueLesMouvementsSansProjet() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-0", societe, null, "50");
        reglement("NR", "20");
        AdmUser societeSeule = achat.compte("APV-S-" + jeton, COMMERCIAL);
        achat.habiliterSociete(societeSeule, societe);

        String json = corps(mockMvc, positions(connecte(societeSeule, COMMERCIAL)));

        assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("30");
    }

    @Test
    void r4_filtreProjetsExplicite_exclutLesMouvementsSansProjet() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-0", societe, null, "50");
        reglement("NR", "20");

        String json = corps(mockMvc, positions(qui(), PROJET_IDS, projetA.toString()));

        assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("100");
    }

    @Test
    void r4_filtreProjetHorsPerimetre_ignore_rienCompte() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-B", societe, projetB, "1000");

        String json = corps(mockMvc, positions(qui(), PROJET_IDS, projetB.toString()));

        assertAll(
                () -> assertThat(fournisseurs(json)).doesNotContain(x.toString()),
                () -> assertThat(total(json, SOLDE_FIN)).isEqualByComparingTo("0"));
    }

    @Test
    void r4_administrateurSansHabilitation_rien_pasDExemption() throws Exception {
        facture("-A", societe, projetA, "100");
        AdmUser admin = achat.compte("APV-ADM-" + jeton, ADMINISTRATEUR);

        String json = corps(mockMvc, positions(connecte(admin, ADMINISTRATEUR)));

        assertAll(
                () -> assertThat(fournisseurs(json)).isEmpty(),
                () -> assertThat(total(json, SOLDE_FIN)).isEqualByComparingTo("0"));
    }

    @Test
    void r4_export_memeVisibilite() throws Exception {
        facture("-A", societe, projetA, "100");
        String nomY = "Fournisseur Y " + jeton;
        UUID y = fixture.fournisseur("FOUY-" + jeton, nomY);
        fixture.piece(jeton + "-YB", FACTURE, DATE, y, nomY, societe, projetB, "900");

        List<String> textes = AppelsAchat.textesExport(mockMvc, get(POSITIONS_EXPORT).with(qui()));

        assertThat(textes).contains(nomX).doesNotContain(nomY);
    }

    // ------------------------------------------------------------------ fenêtre « Ligne de solde »

    @Test
    void r4_detail_pieceDUnProjetNonHabilite_absente() throws Exception {
        facture("-A", societe, projetA, "100");
        facture("-B", societe, projetB, "1000");

        String json = corps(mockMvc, detail(qui(), x, SOCIETE_ID, societe.toString()));
        List<String> codes = JsonPath.read(json, "$[*].code");

        assertThat(codes).contains(jeton + "-A").doesNotContain(jeton + "-B");
    }

    @Test
    void r4_detail_societeNonHabilitee_listeVide200() throws Exception {
        facture("-T", societeT, projetT, "700");

        String json = corps(mockMvc, detail(qui(), x, SOCIETE_ID, societeT.toString()));
        List<Map<String, Object>> lignes = JsonPath.read(json, "$");

        assertThat(lignes).isEmpty();
    }

    // ------------------------------------------------------------------ non connecté

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {AppelsPositions.POSITIONS, POSITIONS_EXPORT})
    void r4_nonConnecte_401(String chemin) throws Exception {
        mockMvc.perform(get(chemin)).andExpect(status().isUnauthorized());
    }

    @Test
    void r4_detail_nonConnecte_401() throws Exception {
        mockMvc.perform(get(AppelsPositions.DETAIL, x).param(SOCIETE_ID, societe.toString()))
                .andExpect(status().isUnauthorized());
    }
}
