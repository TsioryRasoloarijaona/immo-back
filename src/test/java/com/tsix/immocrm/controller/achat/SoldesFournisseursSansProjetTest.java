package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.PROJET_IDS;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.SOLDES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.fournisseurs;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.montant;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Soldes fournisseurs : même périmètre que les pièces, société ET (projet OU sans projet) (FEATURE-visibilite-omran.md R4).
 * <pre>
 *   société S : X = XA (projet A, 100), XB (projet B, 200), X0 (sans projet, 1000) ; Z = Z0 (sans projet, 500)
 *   « a » habilité à la société S et au projet A
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SoldesFournisseursSansProjetTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String A_PAYER = "montantAPayer";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private String jeton;
    private UUID societe;
    private String soc;
    private UUID projetA;
    private UUID fournisseurX;
    private UUID fournisseurZ;
    private AdmUser habiliteA;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ASP");
        soc = "SOC-" + jeton;
        societe = fixture.societe(soc);
        projetA = fixture.projet("PA-" + jeton, societe);
        UUID projetB = fixture.projet("PB-" + jeton, societe);
        UUID statutOk = fixture.statut("VPP-" + jeton, true, false, 10);
        String nomX = "Fournisseur X " + jeton;
        fournisseurX = fixture.fournisseur("FOUX-" + jeton, nomX);
        String nomZ = "Fournisseur Z " + jeton;
        fournisseurZ = fixture.fournisseur("FOUZ-" + jeton, nomZ);
        fixture.piece(jeton + "-XA", FACTURE, fournisseurX, nomX, societe, projetA, statutOk, "100");
        fixture.piece(jeton + "-XB", FACTURE, fournisseurX, nomX, societe, projetB, statutOk, "200");
        fixture.piece(jeton + "-X0", FACTURE, fournisseurX, nomX, societe, null, statutOk, "1000");
        fixture.piece(jeton + "-Z0", FACTURE, fournisseurZ, nomZ, societe, null, statutOk, "500");
        habiliteA = fixture.compte("ACH-A-" + jeton, COMMERCIAL);
        visibilite.habiliterSociete(habiliteA, societe);
        fixture.habiliter(habiliteA, projetA);
    }

    private RequestPostProcessor a() {
        return connecte(habiliteA, COMMERCIAL);
    }

    @Test
    void r4_habiliteA_soldeDeX_projetA_plusPieceSansProjet() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, a()));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc, A_PAYER)).isEqualByComparingTo("1100"),
                () -> assertThat(montant(json, fournisseurX, soc, "nombrePieces")).isEqualByComparingTo("2"));
    }

    @Test
    void r4_fournisseurAvecSeulementDesPiecesSansProjet_present() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, a()));

        assertAll(
                () -> assertThat(fournisseurs(json)).contains(fournisseurX.toString(), fournisseurZ.toString()),
                () -> assertThat(montant(json, fournisseurZ, soc, A_PAYER)).isEqualByComparingTo("500"));
    }

    @Test
    void r4_compteSansHabilitationSociete_neVoitAucunFournisseurDeLaSociete() throws Exception {
        AdmUser projetSeul = fixture.compte("ACH-0-" + jeton, COMMERCIAL);
        fixture.habiliter(projetSeul, projetA);

        String json = corps(mockMvc, appel(SOLDES, connecte(projetSeul, COMMERCIAL)));

        assertThat(fournisseurs(json)).doesNotContain(fournisseurX.toString(), fournisseurZ.toString());
    }

    @Test
    void r4_habilitationSocieteSeule_neDonnePasLesPiecesDeProjet() throws Exception {
        AdmUser habiliteSociete = fixture.compte("ACH-S-" + jeton, COMMERCIAL);
        visibilite.habiliterSociete(habiliteSociete, societe);

        String json = corps(mockMvc, appel(SOLDES, connecte(habiliteSociete, COMMERCIAL)));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc, A_PAYER)).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, fournisseurX, soc, "nombrePieces")).isEqualByComparingTo("1"));
    }

    @Test
    void r4_filtreProjetIds_neRetientQueCesProjets_sansLesPiecesSansProjet() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, a(), PROJET_IDS, projetA.toString()));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc, A_PAYER)).isEqualByComparingTo("100"),
                () -> assertThat(fournisseurs(json)).doesNotContain(fournisseurZ.toString()));
    }

    @Test
    void r4_totalEgalSommeDesLignes() throws Exception {
        String json = corps(mockMvc, appel(SOLDES, a()));
        List<Object> lignes = JsonPath.read(json, "$.lignes[*]." + A_PAYER);
        BigDecimal somme = lignes.stream().map(v -> new BigDecimal(String.valueOf(v)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertAll(
                () -> assertThat(lignes).isNotEmpty(),
                () -> assertThat(nombre(json, "$.total." + A_PAYER)).isEqualByComparingTo(somme),
                () -> assertThat(somme).isGreaterThanOrEqualTo(new BigDecimal("1600")));
    }
}
