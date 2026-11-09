package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 1 (visibilité par habilitation {@code PrjProject}, comme OMRAN) et règle 4 (lecture seule) —
 * FEATURE-achat-lecture-factures.md §2, §7 tests 1 à 3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatVisibiliteTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String ADMINISTRATEUR = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID projetA;
    private UUID projetB;
    private UUID pieceA;
    private UUID pieceB;
    private String codeA;
    private String codeB;
    private AdmUser habiliteA;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AVI");
        UUID societe = fixture.societe("SOC-" + jeton);
        projetA = fixture.projet("PA-" + jeton, societe);
        projetB = fixture.projet("PB-" + jeton, societe);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        codeA = jeton + "-A";
        codeB = jeton + "-B";
        pieceA = fixture.piece(Piece.facture(codeA, LocalDate.of(2026, 3, 10), fournisseur, nom, societe, projetA,
                new BigDecimal("1200")));
        pieceB = fixture.piece(Piece.facture(codeB, LocalDate.of(2026, 3, 11), fournisseur, nom, societe, projetB,
                new BigDecimal("2400")));
        habiliteA = fixture.compte("ACH-A-" + jeton, COMMERCIAL);
        fixture.habiliter(habiliteA, projetA);
    }

    private RequestPostProcessor qui() {
        return connecte(habiliteA, COMMERCIAL);
    }

    // ------------------------------------------------------------------ §7.1 habilité au projet A seulement

    @Test
    void r1_liste_habiliteA_neContientQueLesPiecesDeA() throws Exception {
        String json = corps(mockMvc, liste(qui()));

        assertThat(codes(json)).containsExactly(codeA);
    }

    @Test
    void r1_fiche_pieceDeA_200() throws Exception {
        mockMvc.perform(get(AppelsAchat.DOCUMENT, pieceA).with(qui()))
                .andExpect(status().isOk());
    }

    @Test
    void r1_fiche_pieceDeB_404CommeIdInconnu() throws Exception {
        mockMvc.perform(get(AppelsAchat.DOCUMENT, pieceB).with(qui()))
                .andExpect(status().isNotFound());
    }

    @Test
    void r1_fiche_idInconnu_404() throws Exception {
        mockMvc.perform(get(AppelsAchat.DOCUMENT, UUID.randomUUID()).with(qui()))
                .andExpect(status().isNotFound());
    }

    @Test
    void r1_filtreProjetB_horsPerimetre_pageVide200() throws Exception {
        String json = corps(mockMvc, liste(qui(), "projetIds", projetB.toString()));

        assertThat(total(json)).isZero();
    }

    @Test
    void r1_filtreProjetsAetB_neRendQueA() throws Exception {
        String json = corps(mockMvc, liste(qui(), "projetIds", projetA.toString(), "projetIds", projetB.toString()));

        assertThat(codes(json)).containsExactly(codeA);
    }

    @Test
    void r1_export_contientAsansB() throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc,
                get(AppelsAchat.EXPORT).param("format", "xlsx").with(qui()));

        assertThat(textes).contains(codeA).doesNotContain(codeB);
    }

    @Test
    void r1_referentiels_projets_neMontrentQueLesProjetsVisibles() throws Exception {
        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        List<String> ids = JsonPath.read(json, "$.projets[*].id");
        assertThat(ids).contains(projetA.toString()).doesNotContain(projetB.toString());
    }

    // ------------------------------------------------------------------ §7.2 pas d'exemption, appelant inconnu

    @Test
    void r1_administrateurSansHabilitation_listeVide() throws Exception {
        AdmUser admin = fixture.compte("ACH-ADM-" + jeton, ADMINISTRATEUR);

        String json = corps(mockMvc, liste(connecte(admin, ADMINISTRATEUR)));

        assertThat(total(json)).isZero();
    }

    @Test
    void r1_administrateurSansHabilitation_fiche404() throws Exception {
        AdmUser admin = fixture.compte("ACH-ADM-" + jeton, ADMINISTRATEUR);

        mockMvc.perform(get(AppelsAchat.DOCUMENT, pieceA).with(connecte(admin, ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r1_administrateurSansHabilitation_exportSansLaPiece() throws Exception {
        AdmUser admin = fixture.compte("ACH-ADM-" + jeton, ADMINISTRATEUR);

        List<String> textes = AppelsAchat.textesExport(mockMvc,
                get(AppelsAchat.EXPORT).param("format", "xlsx").with(connecte(admin, ADMINISTRATEUR)));

        assertThat(textes).isNotEmpty().doesNotContain(codeA, codeB);
    }

    @Test
    void r1_appelantNonIdentifiable_listeVide() throws Exception {
        AuthenticatedUser anonyme = new AuthenticatedUser(null, "cle-sans-compte", null, false, true, 0);
        RequestPostProcessor sansCompte = authentication(new UsernamePasswordAuthenticationToken(anonyme, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + ADMINISTRATEUR))));

        String json = corps(mockMvc, liste(sansCompte));

        assertThat(total(json)).isZero();
    }

    // ------------------------------------------------------------------ §7.3 pièce sans projet

    @Test
    void r1_pieceSansProjet_invisibleMemePourUnHabiliteATousLesProjets() throws Exception {
        fixture.habiliter(habiliteA, projetB);
        String codeSans = jeton + "-SANS";
        UUID societe = fixture.societe("SOC2-" + jeton);
        String nom = "Fournisseur sans projet " + jeton;
        UUID sansProjet = fixture.piece(Piece.facture(codeSans, LocalDate.of(2026, 3, 12),
                fixture.fournisseur("FOU2-" + jeton, nom), nom, societe, null, new BigDecimal("100")));

        String json = corps(mockMvc, liste(qui()));

        assertThat(codes(json)).containsExactlyInAnyOrder(codeA, codeB).doesNotContain(codeSans);
        mockMvc.perform(get(AppelsAchat.DOCUMENT, sansProjet).with(qui()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ règle 4 : lecture seule

    @Test
    void r4_lectureSeule_aucuneEcritureSurLaListe_405() throws Exception {
        mockMvc.perform(post(AppelsAchat.DOCUMENTS).with(qui()).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
