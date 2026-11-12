package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.LigneAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.INTROUVABLE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.supprimer;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-1, E-B3 ({@code DELETE /api/achat/documents/{id}}), Vérification n° 4 : un test par refus 409 (gérée ailleurs — Q-C —,
 * statut en lecture seule BC / facture, règlement, pièce fille, écriture comptable), effets de la réussite, ordre 404 → 403 → 409.
 * Compte Administrateur (tous les droits OMRAN) sans personnel, sauf mention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatSuppressionTest {

    private static final String PIECE = "BuyDocument";
    private static final String CLE = "PKey";
    private static final String LIEN_PIECE = "BuyDocumentId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureConfigurationAchat config;

    private Cadre c;
    private AdmUser admin;
    private UUID lectureSeule;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ASU");
        fixture.tablesComptables();
        admin = fixture.compteHabilite(c, "ASU-ADM", ADMINISTRATEUR);
        fixture.sansPersonnel(admin);
        lectureSeule = config.statut(new Statut("RO-" + c.jeton(), "Validé DT " + c.jeton(), 10, null, true, null, true,
                null, null));
    }

    private UUID piece(String nature) {
        return fixture.piece(c, "-" + nature + (rang++), nature);
    }

    private MvcResult supprimerPar(AdmUser qui, UUID piece) throws Exception {
        return appeler(mockMvc, supprimer(piece), connecte(qui, ROLE_JWT));
    }

    private void assertRefus409(UUID piece) throws Exception {
        MvcResult r = supprimerPar(admin, piece);
        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        fixture.oublier();
        assertThat(fixture.nombre(PIECE, CLE, piece)).as("pièce intacte").isEqualTo(1);
    }

    private void assertSupprimee(UUID piece) throws Exception {
        MvcResult r = supprimerPar(admin, piece);
        assertThat(statut(r)).as(corps(r)).isEqualTo(204);
        assertThat(fixture.nombre(PIECE, CLE, piece)).isZero();
    }

    // ------------------------------------------------------------------ 404 / ordre

    @Test
    void eb3_pieceInconnue_404PieceIntrouvable() throws Exception {
        MvcResult r = supprimerPar(admin, UUID.randomUUID());

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(INTROUVABLE);
    }

    /** Ordre 404 → 403 → 409 : sans le droit, une pièce à règlement répond 403, pas 409. */
    @Test
    void eb3_ordre_sansDroit_pieceBloquee_403AvantLe409() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.reglement(facture, "REG-" + c.jeton(), new BigDecimal("50"), FixtureAchat.EN_COURS, LocalDate.of(2026, 5, 10),
                fixture.typeReglement("TR-" + c.jeton()));
        AdmUser sansDroit = fixture.compteHabilite(c, "ASU-SD", ROLE_JWT);
        fixture.sansPersonnel(sansDroit);

        assertThat(statut(supprimerPar(sansDroit, facture))).isEqualTo(403);
    }

    // ------------------------------------------------------------------ 409 n° 1 : gérée ailleurs (Q-C)

    @Test
    void eb3_1_qC_avanceDocumentARegler_409() throws Exception {
        assertRefus409(piece(FixtureAchat.DOCUMENT_A_REGLER));
    }

    @Test
    void eb3_1_qC_factureDeDecompte_409() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.decompte(facture);

        assertRefus409(facture);
    }

    @Test
    void eb3_1_qC_fraisDeTerrain_referenceTypeNonVide_409() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.poser(facture, "ReferenceType", "LAND_EXPENSE");

        assertRefus409(facture);
    }

    // ------------------------------------------------------------------ 409 n° 2 : statut en lecture seule (BC, facture)

    @Test
    void eb3_2_bonCommandeStatutLectureSeule_409_texteOmran() throws Exception {
        UUID bc = piece(FixtureAchat.BON_COMMANDE);
        fixture.poser(bc, "BuyDocumentStatusId", lectureSeule);

        MvcResult r = supprimerPar(admin, bc);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).startsWith("Vous n'avez pas le droit de supprimer un Bon de Commande avec un statut");
    }

    @Test
    void eb3_2_factureStatutLectureSeule_409_texteOmran() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.poser(facture, "BuyDocumentStatusId", lectureSeule);

        MvcResult r = supprimerPar(admin, facture);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).startsWith("Vous n'avez pas le droit de supprimer une Facture de status");
    }

    /** Devis, BL, avoir : pas de contrôle de statut (comme OMRAN). */
    @ParameterizedTest
    @ValueSource(strings = {FixtureAchat.DEVIS, FixtureAchat.BON_LIVRAISON, FixtureAchat.AVOIR})
    void eb3_2_autresNatures_statutLectureSeule_204(String nature) throws Exception {
        UUID piece = piece(nature);
        fixture.poser(piece, "BuyDocumentStatusId", lectureSeule);

        assertSupprimee(piece);
    }

    @ParameterizedTest
    @ValueSource(strings = {FixtureAchat.BON_COMMANDE, FixtureAchat.FACTURE})
    void eb3_2_temoin_bcOuFacture_statutModifiable_204(String nature) throws Exception {
        UUID piece = piece(nature);
        fixture.poser(piece, "BuyDocumentStatusId", config.statut(Statut.simple("EC-" + c.jeton() + rang, 1)));

        assertSupprimee(piece);
    }

    // ------------------------------------------------------------------ 409 n° 3 : règlement lié

    @Test
    void eb3_3_reglementLie_409() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.reglement(facture, "REG-" + c.jeton(), new BigDecimal("50"), FixtureAchat.EN_COURS, LocalDate.of(2026, 5, 10),
                fixture.typeReglement("TR-" + c.jeton()));

        assertRefus409(facture);
    }

    /** Un règlement annulé reste une ligne {@code StlSettlementLine} (FK NO_ACTION) : refus aussi. */
    @Test
    void eb3_3_reglementAnnuleLie_409() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.reglement(facture, "REG-" + c.jeton(), new BigDecimal("50"), FixtureAchat.ANNULE, LocalDate.of(2026, 5, 10),
                fixture.typeReglement("TR-" + c.jeton()));

        assertRefus409(facture);
    }

    // ------------------------------------------------------------------ 409 n° 4 : pièce fille

    @Test
    void eb3_4_bonCommandeCiteParUnBLViaBuyOrderId_409() throws Exception {
        UUID bc = piece(FixtureAchat.BON_COMMANDE);
        fixture.poser(piece(FixtureAchat.BON_LIVRAISON), "BuyOrderId", bc);

        assertRefus409(bc);
    }

    @Test
    void eb3_4_bonCommandeCiteParUneFactureViaBuyOrderId_409() throws Exception {
        UUID bc = piece(FixtureAchat.BON_COMMANDE);
        fixture.poser(piece(FixtureAchat.FACTURE), "BuyOrderId", bc);

        assertRefus409(bc);
    }

    @Test
    void eb3_4_factureCiteeParUnAvoirViaInvoiceSourceId_409() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.poser(piece(FixtureAchat.AVOIR), "InvoiceSourceId", facture);

        assertRefus409(facture);
    }

    @Test
    void eb3_4_bonCommandeLieAUneFactureParBuyOrderInvoice_409() throws Exception {
        UUID bc = piece(FixtureAchat.BON_COMMANDE);
        fixture.lierFactureACommande(bc, piece(FixtureAchat.FACTURE));

        assertRefus409(bc);
    }

    // ------------------------------------------------------------------ 409 n° 5 : écriture comptable

    @Test
    void eb3_5_ecritureComptableActEntry_409() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        fixture.ecritureComptable(facture);

        assertRefus409(facture);
    }

    // ------------------------------------------------------------------ réussite : effets

    @Test
    void eb3_succes_lignesVentilationsHistoriqueEtLienBuyOrderInvoiceSupprimes_bcConserve() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        UUID bc = piece(FixtureAchat.BON_COMMANDE);
        fixture.ligne(facture, new LigneAchat(1, "ART-1", "Ciment", "U", BigDecimal.TEN, new BigDecimal("5"),
                new BigDecimal("0.2"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("6")));
        fixture.ligne(facture, new LigneAchat(2, "ART-2", "Sable", "U", BigDecimal.ONE, new BigDecimal("8"),
                new BigDecimal("0.2"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("9.6")));
        fixture.ventilation(facture, fixture.categorie("CAT-" + c.jeton()), BigDecimal.ONE);
        fixture.ventilationProjet(facture, c.projet(), BigDecimal.ONE);
        UUID enCours = config.statut(Statut.simple("EC-" + c.jeton(), 1));
        fixture.poser(facture, "BuyDocumentStatusId", enCours);
        fixture.historique(facture, enCours, LocalDateTime.of(2026, 5, 4, 9, 0), admin, "Création");
        fixture.lierFactureACommande(bc, facture);

        assertSupprimee(facture);

        assertAll(
                () -> assertThat(fixture.nombre("BuyDocumentLine", LIEN_PIECE, facture)).as("lignes").isZero(),
                () -> assertThat(fixture.nombre("BuyVentilationCategory", LIEN_PIECE, facture)).as("ventilation").isZero(),
                () -> assertThat(fixture.nombre("BuyVentilationExpense", LIEN_PIECE, facture)).as("ventilation projet")
                        .isZero(),
                () -> assertThat(fixture.nombre("BuyDocumentStatusHistory", LIEN_PIECE, facture)).as("historique").isZero(),
                () -> assertThat(fixture.nombre("BuyOrderInvoice", "BuyInvoiceId", facture)).as("BuyOrderInvoice").isZero(),
                () -> assertThat(fixture.nombre(PIECE, CLE, bc)).as("le BC lié reste").isEqualTo(1));
    }

    /** Yes.Buy/Quote.cs:BeforeDelete : les pièces qui citent le devis perdent leur BuyQuoteId, sans être supprimées. */
    @Test
    void eb3_succes_devisSupprime_buyQuoteIdDesPiecesLieesANull() throws Exception {
        UUID devis = piece(FixtureAchat.DEVIS);
        UUID bc = piece(FixtureAchat.BON_COMMANDE);
        fixture.poser(bc, "BuyQuoteId", devis);

        assertSupprimee(devis);

        assertAll(
                () -> assertThat(fixture.nombre(PIECE, CLE, bc)).isEqualTo(1),
                () -> assertThat(fixture.lienDe(bc, "BuyQuoteId")).isNull());
    }

    /** Invoice.cs l.682 : les BL qui citent la facture perdent leur BuyInvoiceId. */
    @Test
    void eb3_succes_factureSupprimee_buyInvoiceIdDesBLANull() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);
        UUID bl = piece(FixtureAchat.BON_LIVRAISON);
        fixture.poser(bl, "BuyInvoiceId", facture);

        assertSupprimee(facture);

        assertAll(
                () -> assertThat(fixture.nombre(PIECE, CLE, bl)).isEqualTo(1),
                () -> assertThat(fixture.lienDe(bl, "BuyInvoiceId")).isNull());
    }

    /** Un BL rattaché à un BC se supprime (le lien est porté par le BL lui-même). */
    @Test
    void eb3_succes_blRattacheAUnBc_204_bcConserve() throws Exception {
        UUID bc = piece(FixtureAchat.BON_COMMANDE);
        UUID bl = piece(FixtureAchat.BON_LIVRAISON);
        fixture.poser(bl, "BuyOrderId", bc);

        assertSupprimee(bl);

        assertThat(fixture.lienDe(bc, CLE)).isEqualTo(id(bc));
    }
}
