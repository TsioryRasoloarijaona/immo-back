package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.PROJET_IDS;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.SOLDES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.SOLDES_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.fournisseurs;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.montant;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Visibilité par habilitation projet, sans exemption de rôle, chemins de refus —
 * FEATURE-achat-impayes.md §3.5, §8 test 6 ; société habilitée en plus depuis FEATURE-visibilite-omran.md R4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SoldesFournisseursVisibiliteTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String A_PAYER = "montantAPayer";
    private static final String TOTAL_A_PAYER = "$.total.montantAPayer";
    private static final String TOTAL_RESTE = "$.total.resteARegler";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private String jeton;
    private UUID societe;
    private String soc;
    private UUID projetA;
    private UUID projetB;
    private UUID statutOk;
    private UUID fournisseurX;
    private String nomX;
    private UUID fournisseurZ;
    private String nomZ;
    private AdmUser habiliteA;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ASV");
        soc = "SOC-" + jeton;
        societe = fixture.societe(soc);
        projetA = fixture.projet("PA-" + jeton, societe);
        projetB = fixture.projet("PB-" + jeton, societe);
        statutOk = fixture.statut("VPP-" + jeton, true, false, 10);
        nomX = "Fournisseur X " + jeton;
        fournisseurX = fixture.fournisseur("FOUX-" + jeton, nomX);
        nomZ = "Fournisseur Z " + jeton;
        fournisseurZ = fixture.fournisseur("FOUZ-" + jeton, nomZ);
        fixture.piece(jeton + "-XA", FACTURE, fournisseurX, nomX, societe, projetA, statutOk, "100");
        fixture.piece(jeton + "-XB", FACTURE, fournisseurX, nomX, societe, projetB, statutOk, "200");
        fixture.piece(jeton + "-ZB", FACTURE, fournisseurZ, nomZ, societe, projetB, statutOk, "500");
        habiliteA = fixture.compte("ACH-A-" + jeton, COMMERCIAL);
        visibilite.habiliterSociete(habiliteA, societe);
        fixture.habiliter(habiliteA, projetA);
    }

    private RequestPostProcessor qui() {
        return connecte(habiliteA, COMMERCIAL);
    }

    private RequestPostProcessor adminSansHabilitation() {
        AdmUser admin = fixture.compte("ACH-ADM-" + jeton, ADMINISTRATEUR);
        return connecte(admin, ADMINISTRATEUR);
    }

    @Test
    void r5_habiliteA_soldeDeXNeSommeQueSesPiecesDeA() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, qui()));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc, A_PAYER)).isEqualByComparingTo("100"),
                () -> assertThat(montant(json, fournisseurX, soc, "nombrePieces")).isEqualByComparingTo("1"),
                () -> assertThat(nombre(json, TOTAL_A_PAYER)).isEqualByComparingTo("100"));
    }

    @Test
    void r5_fournisseurSansPieceVisible_absent() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, qui()));

        assertThat(fournisseurs(json)).containsExactly(fournisseurX.toString());
    }

    @Test
    void r5_deuxUtilisateursAuxProjetsDifferents_deuxSoldesPourLeMemeFournisseur() throws Exception {
        AdmUser habiliteB = fixture.compte("ACH-B-" + jeton, COMMERCIAL);
        visibilite.habiliterSociete(habiliteB, societe);
        fixture.habiliter(habiliteB, projetB);

        String json = corps(mockMvc, appel(SOLDES, connecte(habiliteB, COMMERCIAL)));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc, A_PAYER)).isEqualByComparingTo("200"),
                () -> assertThat(montant(json, fournisseurZ, soc, A_PAYER)).isEqualByComparingTo("500"));
    }

    @Test
    void r5_projetIdsHorsPerimetre_lignesVidesEtTotalAZero() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, qui(), PROJET_IDS, projetB.toString()));

        assertAll(
                () -> assertThat(fournisseurs(json)).isEmpty(),
                () -> assertThat(nombre(json, TOTAL_A_PAYER)).isEqualByComparingTo("0"),
                () -> assertThat(nombre(json, TOTAL_RESTE)).isEqualByComparingTo("0"),
                () -> assertThat(nombre(json, "$.total.nombrePieces")).isEqualByComparingTo("0"));
    }

    @Test
    void r5_projetIdsAetB_neCompteQueA() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, qui(), PROJET_IDS, projetA.toString(),
                PROJET_IDS, projetB.toString()));

        assertAll(
                () -> assertThat(fournisseurs(json)).containsExactly(fournisseurX.toString()),
                () -> assertThat(montant(json, fournisseurX, soc, A_PAYER)).isEqualByComparingTo("100"));
    }

    @Test
    void r5_administrateurSansHabilitation_soldesVides_pasDExemption() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, adminSansHabilitation()));

        assertAll(
                () -> assertThat(fournisseurs(json)).isEmpty(),
                () -> assertThat(nombre(json, TOTAL_A_PAYER)).isEqualByComparingTo("0"));
    }

    /** FEATURE-visibilite-omran.md R4 : la pièce sans projet d'une société habilitée entre dans le solde. */
    @Test
    void r4_pieceSansProjet_compteeSiSaSocieteEstHabilitee() throws Exception {
        fixture.habiliter(habiliteA, projetB);
        fixture.piece(jeton + "-X0", FACTURE, fournisseurX, nomX, societe, null, statutOk, "1000");

        String json = corps(mockMvc, appel(SOLDES, qui()));

        assertThat(montant(json, fournisseurX, soc, A_PAYER)).isEqualByComparingTo("1300");
    }

    @Test
    void r5_export_sansLesFournisseursDeB() throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc, appel(SOLDES_EXPORT, qui()));

        assertThat(textes).contains(nomX).doesNotContain(nomZ);
    }

    @Test
    void r5_totauxDocuments_sansHabilitation_nombreZero() throws Exception {
        String json = corps(mockMvc, appel(TOTAUX, adminSansHabilitation()));

        assertAll(
                () -> assertThat(nombre(json, "$.nombre")).isEqualByComparingTo("0"),
                () -> assertThat(nombre(json, "$.resteARegler")).isEqualByComparingTo("0"));
    }

    @Test
    void r5_totauxDocuments_habiliteA_neCompteQueA() throws Exception {
        String json = corps(mockMvc, appel(TOTAUX, qui()));

        assertAll(
                () -> assertThat(nombre(json, "$.nombre")).isEqualByComparingTo("1"),
                () -> assertThat(nombre(json, "$.montantAPayer")).isEqualByComparingTo("100"));
    }

    @Test
    void r5_listeFiltreeSurUnFournisseurSansPieceVisible_pageVide200() throws Exception {
        String json = corps(mockMvc, liste(qui(), "fournisseurId", fournisseurZ.toString()));

        assertThat(total(json)).isZero();
    }
}
