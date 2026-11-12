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
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot A2a, E4 (Vérification n° 4 et 6) + réponses du manager Q3 (onglets filtrés par {@code VISIBLE}), Q4 (factures
 * d'un BC = {@code BuyOrderId} ∪ {@code BuyOrderInvoice}, dédoublonné), décision « lot croisé » (12/11).
 * <pre>
 *   société S, projet P habilités ; projet Q (société S2 non habilitée)
 *   BC (P) ← BL1, BL2 (BuyOrderId, P) ; BL-HORS (BuyOrderId, Q : masqué)
 *        ← F1 (BuyOrderId) ; F2 (BuyOrderInvoice seul) ; F3 (les deux) ; F-HORS (BuyOrderId, Q : masquée)
 *   FAC (P) ← AV1 (InvoiceSourceId, 2 lignes) ; AV-HORS (InvoiceSourceId, Q : masqué)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFicheLiensTest {

    private static final String ROLE = "Commercial";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String INVOICE_SOURCE_ID = "InvoiceSourceId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID autreSociete;
    private UUID projet;
    private UUID projetHors;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AFI");
        societe = fixture.societe("SOC-" + jeton);
        autreSociete = fixture.societe("SOC2-" + jeton);
        projet = fixture.projet("A-" + jeton, societe);
        projetHors = fixture.projet("B-" + jeton, autreSociete);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
    }

    private UUID piece(String suffixe, String nature, boolean horsPerimetre, String aPayer) {
        UUID s = horsPerimetre ? autreSociete : societe;
        UUID p = horsPerimetre ? projetHors : projet;
        return fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 4, 2), fournisseur, nom, s, p,
                new BigDecimal(aPayer)).nature(nature));
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    private String fiche(UUID piece) throws Exception {
        return corps(mockMvc, get(AppelsAchat.DOCUMENT, piece).with(connecte(compte, ROLE)));
    }

    /** BC déclenché par le personnel du compte : visible malgré la restriction des BC (Q2), seul le projet filtre. */
    private UUID bonDeCommande() {
        UUID bc = piece("-BC", FixtureAchat.BON_COMMANDE, false, "5000");
        fixture.poser(bc, "CfgTierTriggerId", compte.getCfgEmployee().getId());
        return bc;
    }

    // ------------------------------------------------------------------ BC › Bons de livraison

    @Test
    void e4_bc_bonsLivraison_piecesDelivryRattachees_parBuyOrderId() throws Exception {
        UUID bc = bonDeCommande();
        fixture.poser(piece("-BL1", FixtureAchat.BON_LIVRAISON, false, "100"), BUY_ORDER_ID, bc);
        fixture.poser(piece("-BL2", FixtureAchat.BON_LIVRAISON, false, "200"), BUY_ORDER_ID, bc);

        String json = fiche(bc);

        assertThat((List<Object>) JsonPath.read(json, "$.bonsLivraison[*].code"))
                .containsExactlyInAnyOrder(code("-BL1"), code("-BL2"));
    }

    /** Q3 : une pièce liée hors périmètre n'apparaît jamais. */
    @Test
    void q3_bc_bonLivraisonHorsPerimetre_masque() throws Exception {
        UUID bc = bonDeCommande();
        fixture.poser(piece("-BL1", FixtureAchat.BON_LIVRAISON, false, "100"), BUY_ORDER_ID, bc);
        fixture.poser(piece("-BL-HORS", FixtureAchat.BON_LIVRAISON, true, "100"), BUY_ORDER_ID, bc);

        String json = fiche(bc);

        assertThat((List<Object>) JsonPath.read(json, "$.bonsLivraison[*].code")).containsExactly(code("-BL1"));
    }

    @Test
    void e4_pieceLiee_champsDuFil() throws Exception {
        UUID bc = bonDeCommande();
        UUID statut = fixture.statutDEtat("ST-" + jeton, FixtureAchat.ETAT_INTERMEDIAIRE, "Orange");
        UUID lot = fixture.lot("LOT-" + jeton, projet);
        UUID bl = fixture.piece(Piece.facture(code("-BL1"), LocalDate.of(2026, 4, 7), fournisseur, nom, societe, projet,
                new BigDecimal("120")).nature(FixtureAchat.BON_LIVRAISON).statut(statut).lot(lot)
                .textes("REF-BL-" + jeton, "BL " + jeton));
        fixture.poser(bl, BUY_ORDER_ID, bc);

        String json = fiche(bc);

        String ligne = "$.bonsLivraison[0].";
        assertAll(
                () -> assertThat((Object) JsonPath.read(json, ligne + "id")).isEqualTo(bl.toString()),
                () -> assertThat((Object) JsonPath.read(json, ligne + "code")).isEqualTo(code("-BL1")),
                () -> assertThat((Object) JsonPath.read(json, ligne + "dateDocument")).isEqualTo("2026-04-07"),
                () -> assertThat((Object) JsonPath.read(json, ligne + "statutCode")).isEqualTo("ST-" + jeton),
                () -> assertThat((Object) JsonPath.read(json, ligne + "statutDescription")).isEqualTo("Statut ST-" + jeton),
                () -> assertThat((Object) JsonPath.read(json, ligne + "statutCouleur")).isEqualTo("Orange"),
                () -> assertThat((Object) JsonPath.read(json, ligne + "reference")).isEqualTo("REF-BL-" + jeton),
                () -> assertThat(nombre(json, ligne + "montantHT")).isEqualByComparingTo("100"),
                () -> assertThat(nombre(json, ligne + "montantTVA")).isEqualByComparingTo("20"),
                () -> assertThat(nombre(json, ligne + "montantTTC")).isEqualByComparingTo("120"),
                () -> assertThat((Object) JsonPath.read(json, ligne + "projetCode")).isEqualTo("A-" + jeton),
                () -> assertThat((Object) JsonPath.read(json, ligne + "lotCode")).isEqualTo("LOT-" + jeton));
    }

    // ------------------------------------------------------------------ BC › Factures (Q4 : union dédoublonnée)

    @Test
    void q4_bc_factures_buyOrderIdUnionBuyOrderInvoice_dedoublonnees() throws Exception {
        UUID bc = bonDeCommande();
        fixture.poser(piece("-F1", FixtureAchat.FACTURE, false, "100"), BUY_ORDER_ID, bc);
        fixture.lierFactureACommande(bc, piece("-F2", FixtureAchat.FACTURE, false, "200"));
        UUID f3 = piece("-F3", FixtureAchat.FACTURE, false, "300");
        fixture.poser(f3, BUY_ORDER_ID, bc);
        fixture.lierFactureACommande(bc, f3);

        String json = fiche(bc);

        assertThat((List<Object>) JsonPath.read(json, "$.factures[*].code"))
                .containsExactlyInAnyOrder(code("-F1"), code("-F2"), code("-F3"));
    }

    @Test
    void q3_bc_factureHorsPerimetre_masquee() throws Exception {
        UUID bc = bonDeCommande();
        fixture.poser(piece("-F1", FixtureAchat.FACTURE, false, "100"), BUY_ORDER_ID, bc);
        fixture.poser(piece("-F-HORS", FixtureAchat.FACTURE, true, "100"), BUY_ORDER_ID, bc);
        fixture.lierFactureACommande(bc, piece("-F-HORS2", FixtureAchat.FACTURE, true, "100"));

        String json = fiche(bc);

        assertThat((List<Object>) JsonPath.read(json, "$.factures[*].code")).containsExactly(code("-F1"));
    }

    // ------------------------------------------------------------------ Facture › Avoirs

    @Test
    void e4_facture_avoirs_parInvoiceSourceId_avecLeursLignes() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, false, "1200");
        UUID avoir = fixture.piece(Piece.facture(code("-AV1"), LocalDate.of(2026, 4, 20), fournisseur, nom, societe,
                projet, new BigDecimal("300")).nature(FixtureAchat.AVOIR));
        fixture.poser(avoir, INVOICE_SOURCE_ID, facture);
        fixture.ligne(avoir, new LigneAchat(1, "AV-L1", "Retour ciment", "SAC", new BigDecimal("2"),
                new BigDecimal("100"), new BigDecimal("0.2"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("120")));
        fixture.ligne(avoir, new LigneAchat(2, "AV-L2", "Retour sable", "M3", new BigDecimal("1"),
                new BigDecimal("50"), new BigDecimal("0.2"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("60")));

        String json = fiche(facture);

        String av = "$.avoirs[0].";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.avoirs[*].code")).containsExactly(code("-AV1")),
                () -> assertThat((Object) JsonPath.read(json, av + "id")).isEqualTo(avoir.toString()),
                () -> assertThat((Object) JsonPath.read(json, av + "dateDocument")).isEqualTo("2026-04-20"),
                () -> assertThat((List<Object>) JsonPath.read(json, av + "lignes[*].designation"))
                        .containsExactly("Retour ciment", "Retour sable"),
                () -> assertThat(nombre(json, av + "lignes[0].totalHT")).isEqualByComparingTo("200"));
    }

    @Test
    void q3_facture_avoirHorsPerimetre_masque() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, false, "1200");
        fixture.poser(piece("-AV1", FixtureAchat.AVOIR, false, "300"), INVOICE_SOURCE_ID, facture);
        fixture.poser(piece("-AV-HORS", FixtureAchat.AVOIR, true, "50"), INVOICE_SOURCE_ID, facture);

        String json = fiche(facture);

        assertThat((List<Object>) JsonPath.read(json, "$.avoirs[*].code")).containsExactly(code("-AV1"));
    }

    // ------------------------------------------------------------------ natures sans onglet

    @Test
    void e4_bc_sansOngletAvoirs_listeVide_memeSiUnAvoirLePointe() throws Exception {
        UUID bc = bonDeCommande();
        fixture.poser(piece("-AV1", FixtureAchat.AVOIR, false, "300"), INVOICE_SOURCE_ID, bc);

        String json = fiche(bc);

        assertThat((List<Object>) JsonPath.read(json, "$.avoirs")).isEmpty();
    }

    @Test
    void e4_facture_sansOngletsBlEtFactures_listesVides() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, false, "1200");
        fixture.poser(piece("-BL1", FixtureAchat.BON_LIVRAISON, false, "100"), BUY_ORDER_ID, facture);

        String json = fiche(facture);

        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.bonsLivraison")).isEmpty(),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.factures")).isEmpty());
    }

    @Test
    void e4_devisEtBl_listesDeLiensVides() throws Exception {
        String devis = fiche(piece("-DEV", FixtureAchat.DEVIS, false, "100"));
        String bl = fiche(piece("-BL", FixtureAchat.BON_LIVRAISON, false, "100"));

        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(devis, "$.bonsLivraison")).isEmpty(),
                () -> assertThat((List<Object>) JsonPath.read(devis, "$.factures")).isEmpty(),
                () -> assertThat((List<Object>) JsonPath.read(devis, "$.avoirs")).isEmpty(),
                () -> assertThat((List<Object>) JsonPath.read(bl, "$.bonsLivraison")).isEmpty(),
                () -> assertThat((List<Object>) JsonPath.read(bl, "$.factures")).isEmpty(),
                () -> assertThat((List<Object>) JsonPath.read(bl, "$.avoirs")).isEmpty());
    }

    // ------------------------------------------------------------------ Ventilation › Par projet (non filtrée)

    @Test
    void e4_ventilationProjets_tousLesProjets_memeNonHabilite_triParCode() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, false, "1200");
        fixture.ventilationProjet(facture, projetHors, new BigDecimal("0.4"));
        fixture.ventilationProjet(facture, projet, new BigDecimal("0.6"));

        String json = fiche(facture);

        String hors = "$.ventilationProjets[1].";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.ventilationProjets[*].projetCode"))
                        .containsExactly("A-" + jeton, "B-" + jeton),
                () -> assertThat((Object) JsonPath.read(json, hors + "projetId")).isEqualTo(projetHors.toString()),
                () -> assertThat((Object) JsonPath.read(json, hors + "projetDescription")).isEqualTo("Projet B-" + jeton),
                () -> assertThat(nombre(json, hors + "taux")).isEqualByComparingTo("0.4"),
                () -> assertThat(nombre(json, "$.ventilationProjets[0].taux")).isEqualByComparingTo("0.6"));
    }

    @Test
    void e4_ventilationParCategorie_conservee() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, false, "1200");
        fixture.ventilation(facture, fixture.categorie("GO-" + jeton), BigDecimal.ONE);
        fixture.ventilationProjet(facture, projet, BigDecimal.ONE);

        String json = fiche(facture);

        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.ventilation[*].categorieCode"))
                        .containsExactly("GO-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.ventilationProjets[*].projetCode"))
                        .containsExactly("A-" + jeton));
    }

    // ------------------------------------------------------------------ lot croisé (décision du user 12/11)

    @Test
    void lotCroise_pieceVisibleDontLeLotEstDUnAutreProjet_200_lotAffiche() throws Exception {
        UUID lotAutreProjet = fixture.lot("LOTX-" + jeton, projetHors);
        UUID facture = fixture.piece(Piece.facture(code("-FAC"), LocalDate.of(2026, 4, 2), fournisseur, nom, societe,
                projet, new BigDecimal("1200")).lot(lotAutreProjet));

        String json = fiche(facture);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.lotId")).isEqualTo(lotAutreProjet.toString()),
                () -> assertThat((Object) JsonPath.read(json, "$.lotCode")).isEqualTo("LOTX-" + jeton));
    }

    private static BigDecimal nombre(String json, String chemin) {
        return new BigDecimal(String.valueOf((Object) JsonPath.read(json, chemin)));
    }
}
