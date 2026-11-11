package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NATURES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NON_SOLDES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.PROJET_IDS;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pièces d'achat : société habilitée ET (projet habilité OU sans projet) (FEATURE-visibilite-omran.md R3, réponse Q2).
 * <pre>
 *   société S1 : P1 (projet A, 1200), P2 (projet B, 2400), P3 (sans projet, 600)   société S2 : P4 (projet C, 900)
 *   « socA » : S1 + projet A      « soc » : S1 seule      « prj » : projet A seul (aucune société)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatVisibiliteSocieteTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String XLSX = "xlsx";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private String jeton;
    private UUID societe1;
    private UUID projetA;
    private UUID projetB;
    private UUID p1;
    private UUID p2;
    private UUID p3;
    private UUID p4;
    private String code1;
    private String code2;
    private String code3;
    private String code4;
    private AdmUser habiliteSocieteEtProjet;
    private AdmUser habiliteSociete;
    private AdmUser habiliteProjet;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AVS");
        societe1 = fixture.societe("S1-" + jeton);
        UUID societe2 = fixture.societe("S2-" + jeton);
        projetA = fixture.projet("PA-" + jeton, societe1);
        projetB = fixture.projet("PB-" + jeton, societe1);
        UUID projetC = fixture.projet("PC-" + jeton, societe2);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        code1 = jeton + "-1";
        code2 = jeton + "-2";
        code3 = jeton + "-3";
        code4 = jeton + "-4";
        p1 = fixture.piece(facture(code1, fournisseur, nom, societe1, projetA, "1200"));
        p2 = fixture.piece(facture(code2, fournisseur, nom, societe1, projetB, "2400"));
        p3 = fixture.piece(facture(code3, fournisseur, nom, societe1, null, "600"));
        p4 = fixture.piece(facture(code4, fournisseur, nom, societe2, projetC, "900"));
        habiliteSocieteEtProjet = fixture.compte("ACH-SA-" + jeton, COMMERCIAL);
        visibilite.habiliterSociete(habiliteSocieteEtProjet, societe1);
        fixture.habiliter(habiliteSocieteEtProjet, projetA);
        habiliteSociete = fixture.compte("ACH-S-" + jeton, COMMERCIAL);
        visibilite.habiliterSociete(habiliteSociete, societe1);
        habiliteProjet = fixture.compte("ACH-P-" + jeton, COMMERCIAL);
        fixture.habiliter(habiliteProjet, projetA);
    }

    private static Piece facture(String code, UUID fournisseur, String nom, UUID societe, UUID projet, String montant) {
        return Piece.facture(code, LocalDate.of(2026, 3, 10), fournisseur, nom, societe, projet, new BigDecimal(montant));
    }

    private RequestPostProcessor socA() {
        return connecte(habiliteSocieteEtProjet, COMMERCIAL);
    }

    private RequestPostProcessor soc() {
        return connecte(habiliteSociete, COMMERCIAL);
    }

    // ------------------------------------------------------------------ R3 : qui voit, qui ne voit pas

    @Test
    void r3_societeEtProjet_voitSesProjetsEtLesPiecesSansProjetDeSaSociete() throws Exception {
        String json = corps(mockMvc, liste(socA()));

        assertThat(codes(json)).containsExactlyInAnyOrder(code1, code3);
    }

    @Test
    void r3_societeSeule_neVoitQueLesPiecesSansProjet() throws Exception {
        String json = corps(mockMvc, liste(soc()));

        assertThat(codes(json)).containsExactly(code3);
    }

    @Test
    void r3_projetSansSociete_neVoitAucunePiece() throws Exception {
        String json = corps(mockMvc, liste(connecte(habiliteProjet, COMMERCIAL)));

        assertThat(total(json)).isZero();
    }

    @Test
    void r3_filtreProjet_intersecteLePerimetre() throws Exception {
        String surA = corps(mockMvc, liste(socA(), PROJET_IDS, projetA.toString()));
        String surB = corps(mockMvc, liste(socA(), PROJET_IDS, projetB.toString()));

        assertAll(
                () -> assertThat(codes(surA)).containsExactly(code1),
                () -> assertThat(total(surB)).isZero());
    }

    @Test
    void r3_fiche_horsPerimetre_404CommeIdInconnu() throws Exception {
        mockMvc.perform(get(AppelsAchat.DOCUMENT, p1).with(socA())).andExpect(status().isOk());
        mockMvc.perform(get(AppelsAchat.DOCUMENT, p3).with(socA())).andExpect(status().isOk());
        mockMvc.perform(get(AppelsAchat.DOCUMENT, p2).with(socA())).andExpect(status().isNotFound());
        mockMvc.perform(get(AppelsAchat.DOCUMENT, p4).with(socA())).andExpect(status().isNotFound());
    }

    @Test
    void r3_fiche_projetSansSociete_404() throws Exception {
        mockMvc.perform(get(AppelsAchat.DOCUMENT, p1).with(connecte(habiliteProjet, COMMERCIAL)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(AppelsAchat.DOCUMENT, p3).with(soc())).andExpect(status().isOk());
    }

    @Test
    void r3_export_memesPiecesQueLaListe() throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc,
                get(AppelsAchat.EXPORT).param("format", XLSX).with(socA()));

        assertThat(textes).contains(code1, code3).doesNotContain(code2, code4);
    }

    @Test
    void r3_vuesFacturesEtNonSoldees_memePerimetre() throws Exception {
        String factures = corps(mockMvc, liste(socA(), NATURES, FixtureAchat.FACTURE));
        String nonSoldees = corps(mockMvc, liste(socA(), NON_SOLDES, "true"));

        assertAll(
                () -> assertThat(codes(factures)).containsExactlyInAnyOrder(code1, code3),
                () -> assertThat(codes(nonSoldees)).containsExactlyInAnyOrder(code1, code3));
    }

    @Test
    void r3_totaux_nombreEgalTotalElementsDeLaListe() throws Exception {
        String totaux = corps(mockMvc, appel(TOTAUX, socA()));
        String json = corps(mockMvc, liste(socA()));

        assertAll(
                () -> assertThat(total(json)).isEqualTo(2),
                () -> assertThat(nombre(totaux, "$.nombre")).isEqualByComparingTo(String.valueOf(total(json))),
                () -> assertThat(nombre(totaux, "$.montantAPayer")).isEqualByComparingTo("1800"));
    }

    @Test
    void r3_totauxNonSoldes_projetSansSociete_zero_societeSeule_un() throws Exception {
        String sansSociete = corps(mockMvc, appel(TOTAUX, connecte(habiliteProjet, COMMERCIAL), NON_SOLDES, "true"));
        String societeSeule = corps(mockMvc, appel(TOTAUX, soc(), NON_SOLDES, "true"));

        assertAll(
                () -> assertThat(nombre(sansSociete, "$.nombre")).isEqualByComparingTo("0"),
                () -> assertThat(nombre(societeSeule, "$.nombre")).isEqualByComparingTo("1"),
                () -> assertThat(nombre(societeSeule, "$.montantAPayer")).isEqualByComparingTo("600"));
    }

    // ------------------------------------------------------------------ R3 : l'Administrateur suit la même règle

    @Test
    void r3_administrateurProjetSansSociete_neVoitRien() throws Exception {
        AdmUser admin = fixture.compte("ACH-ADM-" + jeton, ADMINISTRATEUR);
        fixture.habiliter(admin, projetA);

        String json = corps(mockMvc, liste(connecte(admin, ADMINISTRATEUR)));

        assertThat(total(json)).isZero();
    }

    @Test
    void r3_administrateurSocieteEtProjet_memeRegleQueLesAutres() throws Exception {
        AdmUser admin = fixture.compte("ACH-ADM-" + jeton, ADMINISTRATEUR);
        visibilite.habiliterSociete(admin, societe1);
        fixture.habiliter(admin, projetA);

        String json = corps(mockMvc, liste(connecte(admin, ADMINISTRATEUR)));

        assertThat(codes(json)).containsExactlyInAnyOrder(code1, code3);
    }
}
