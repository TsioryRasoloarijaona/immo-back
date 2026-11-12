package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
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
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.BC_INTROUVABLE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.INTROUVABLE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.MODIF_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.affecter;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.affecterCorps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-1, E-B4 ({@code PUT /api/achat/documents/{id}/bon-commande}), Vérification n° 5. Compte « Commercial » portant
 * {@code InvoiceList.toolNew}, habilité, personnel = déclencheur des BC du jeu ;
 * {@code LimitDeliveryAndInvoiceQuantityToOrder} = 1 (valeur de prod).
 * <pre>
 *   FAC  : facture, fournisseur F, société S, projet P, lot L, ligne ART-A 4 × 100
 *   BC   : BC, F / S / P / L, déclencheur = compte, ligne ART-A 10 × 100
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatAffectationTest {

    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String TRIGGER = "CfgTierTriggerId";
    private static final String ARTICLE = "ART-A";
    private static final BigDecimal PU = new BigDecimal("100");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureConfigurationAchat config;

    private Cadre c;
    private UUID lot;
    private AdmUser compte;
    private UUID facture;
    private UUID bc;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AAF");
        lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        fixture.parametreBooleen(FixtureAchat.LIMITE_QUANTITES, true);
        compte = fixture.compteHabilite(c, "AAF", ROLE_JWT, MODIF_FACTURE);
        facture = piece(FixtureAchat.FACTURE, c, lot);
        ligne(facture, ARTICLE, "4", PU);
        bc = bonCommande(c, lot);
        ligne(bc, ARTICLE, "10", PU);
    }

    private UUID piece(String nature, Cadre cadre, UUID lotDeLaPiece) {
        UUID id = fixture.piece(cadre, "-" + nature + (rang++), nature);
        if (lotDeLaPiece != null) {
            fixture.poser(id, "PrjMarketId", lotDeLaPiece);
        }
        return id;
    }

    /** BC dont le compte est le déclencheur (« mes BC »). */
    private UUID bonCommande(Cadre cadre, UUID lotDuBc) {
        UUID id = piece(FixtureAchat.BON_COMMANDE, cadre, lotDuBc);
        fixture.poser(id, TRIGGER, compte.getCfgEmployee().getId());
        return id;
    }

    private void ligne(UUID piece, String article, String quantite, BigDecimal pu) {
        fixture.ligne(piece, new LigneAchat(1, article, "Désignation " + article, "U", new BigDecimal(quantite), pu,
                new BigDecimal("0.2"), BigDecimal.ZERO, BigDecimal.ZERO, pu.multiply(new BigDecimal("1.2"))));
    }

    private MvcResult affecterPar(AdmUser qui, UUID piece, UUID bonCommande) throws Exception {
        return appeler(mockMvc, affecter(piece, bonCommande), connecte(qui, ROLE_JWT));
    }

    private void assertConflit(MvcResult r) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        fixture.oublier();
        assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).as("BuyOrderId inchangé").isNull();
    }

    // ------------------------------------------------------------------ réussite

    @Test
    void eb4_succes_200_buyOrderIdEcrit_aucuneLigneBuyOrderInvoice() throws Exception {
        MvcResult r = affecterPar(compte, facture, bc);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertAll(
                () -> assertThat(((String) JsonPath.read(corps(r), "$.bonCommandeId")).toUpperCase()).isEqualTo(id(bc)),
                () -> assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).isEqualTo(id(bc)),
                () -> assertThat(fixture.nombre("BuyOrderInvoice", "BuyInvoiceId", facture)).isZero());
    }

    @Test
    void eb4_succes_ongletFacturesDuBc_montreLaFacture() throws Exception {
        assertThat(statut(affecterPar(compte, facture, bc))).isEqualTo(200);

        MvcResult r = appeler(mockMvc, fiche(bc), connecte(compte, ROLE_JWT));

        List<String> factures = JsonPath.read(corps(r), "$.factures[*].id");
        assertThat(factures).extracting(String::toUpperCase).contains(id(facture));
    }

    @Test
    void eb4_succes_reaffecterUneFactureDejaAffectee_200() throws Exception {
        UUID autre = bonCommande(c, lot);
        ligne(autre, ARTICLE, "10", PU);
        fixture.poser(facture, BUY_ORDER_ID, bc);

        MvcResult r = affecterPar(compte, facture, autre);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).isEqualTo(id(autre));
    }

    /** Q-H : le statut de la facture n'est pas regardé (bouton actif sur facture en lecture seule, l.293-298). */
    @Test
    void eb4_qH_factureEnLectureSeule_200() throws Exception {
        fixture.poser(facture, "BuyDocumentStatusId", config.statut(new Statut("RO-" + c.jeton(), "Validé DT", 10, null,
                true, null, true, null, null)));

        assertThat(statut(affecterPar(compte, facture, bc))).isEqualTo(200);
    }

    /** Lot comparé « si la facture a un lot » : facture sans lot, BC à lot → accepté. */
    @Test
    void eb4_factureSansLot_bcAvecLot_200() throws Exception {
        UUID sansLot = piece(FixtureAchat.FACTURE, c, null);
        ligne(sansLot, ARTICLE, "4", PU);

        assertThat(statut(affecterPar(compte, sansLot, bc))).isEqualTo(200);
    }

    // ------------------------------------------------------------------ ordre : 404 → 409 nature → 403 → 400 → 404 BC

    @Test
    void eb4_pieceInconnue_404PieceIntrouvable() throws Exception {
        MvcResult r = affecterPar(compte, UUID.randomUUID(), bc);

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(INTROUVABLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {FixtureAchat.BON_COMMANDE, FixtureAchat.BON_LIVRAISON, FixtureAchat.DEVIS, FixtureAchat.AVOIR})
    void eb4_natureAutreQueFacture_409(String nature) throws Exception {
        UUID piece = piece(nature, c, lot);
        if (FixtureAchat.BON_COMMANDE.equals(nature)) {
            fixture.poser(piece, TRIGGER, compte.getCfgEmployee().getId());
        }

        MvcResult r = affecterPar(compte, piece, bc);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("Seule une facture s'affecte à un bon de commande");
    }

    /** Ordre : la nature (409) est jugée avant le droit (403). */
    @Test
    void eb4_natureAutreQueFacture_sansDroit_409AvantLe403() throws Exception {
        AdmUser sansDroit = fixture.compteHabilite(c, "AAF-SD", ROLE_JWT);
        UUID devis = piece(FixtureAchat.DEVIS, c, lot);

        assertThat(statut(affecterPar(sansDroit, devis, bc))).isEqualTo(409);
    }

    @Test
    void eb4_sansDroitDeModificationDeLaFacture_403() throws Exception {
        AdmUser sansDroit = fixture.compteHabilite(c, "AAF-SD", ROLE_JWT);
        fixture.poser(bc, TRIGGER, sansDroit.getCfgEmployee().getId());

        MvcResult r = affecterPar(sansDroit, facture, bc);

        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
        fixture.oublier();
        assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).isNull();
    }

    /** Ordre : 403 avant le 400 du corps. */
    @Test
    void eb4_sansDroit_corpsVide_403AvantLe400() throws Exception {
        AdmUser sansDroit = fixture.compteHabilite(c, "AAF-SD", ROLE_JWT);

        assertThat(statut(appeler(mockMvc, affecterCorps(facture, "{}"), connecte(sansDroit, ROLE_JWT)))).isEqualTo(403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"bonCommandeId\":null}"})
    void eb4_bonCommandeIdAbsent_400(String json) throws Exception {
        MvcResult r = appeler(mockMvc, affecterCorps(facture, json), connecte(compte, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
    }

    @Test
    void eb4_bcInconnu_404BonDeCommandeIntrouvable() throws Exception {
        MvcResult r = affecterPar(compte, facture, UUID.randomUUID());

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(BC_INTROUVABLE);
    }

    /** Une facture passée comme « BC » n'est pas un bon de commande : même 404. */
    @Test
    void eb4_cibleQuiNEstPasUnBc_404BonDeCommandeIntrouvable() throws Exception {
        UUID autreFacture = piece(FixtureAchat.FACTURE, c, lot);

        MvcResult r = affecterPar(compte, facture, autreFacture);

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(BC_INTROUVABLE);
    }

    @Test
    void eb4_bcHorsPerimetre_projetNonHabilite_404MemeMessage() throws Exception {
        UUID projetNonHabilite = fixture.projet("P2-" + c.jeton(), c.societe());
        UUID horsPerimetre = fixture.piece(new Cadre(c.jeton(), c.societe(), projetNonHabilite, c.fournisseur(),
                c.fournisseurNom()), "-BC-PNH", FixtureAchat.BON_COMMANDE);
        fixture.poser(horsPerimetre, TRIGGER, compte.getCfgEmployee().getId());

        MvcResult r = affecterPar(compte, facture, horsPerimetre);

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(BC_INTROUVABLE);
    }

    @Test
    void eb4_bcDUnAutreDeclencheur_compteRestreint_404MemeMessage() throws Exception {
        AdmUser autre = fixture.compteHabilite(c, "AAF-AUTRE", ROLE_JWT);
        fixture.poser(bc, TRIGGER, autre.getCfgEmployee().getId());

        MvcResult r = affecterPar(compte, facture, bc);

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(BC_INTROUVABLE);
    }

    // ------------------------------------------------------------------ 409 incohérences

    @Test
    void eb4_bcDUnAutreFournisseur_409() throws Exception {
        UUID autreFournisseur = fixture.fournisseur("FOU2-" + c.jeton(), "Autre fournisseur");
        fixture.poser(bc, "CfgTierId", autreFournisseur);

        assertConflit(affecterPar(compte, facture, bc));
    }

    @Test
    void eb4_bcDUneAutreSociete_409() throws Exception {
        UUID autreSociete = fixture.societe("SOC2-" + c.jeton());
        UUID autreProjet = fixture.projet("P3-" + c.jeton(), autreSociete);
        fixture.habiliterSociete(compte, autreSociete);
        fixture.habiliter(compte, autreProjet);
        UUID bcAutreSociete = bonCommande(new Cadre(c.jeton(), autreSociete, autreProjet, c.fournisseur(),
                c.fournisseurNom()), lot);
        ligne(bcAutreSociete, ARTICLE, "10", PU);

        assertConflit(affecterPar(compte, facture, bcAutreSociete));
    }

    @Test
    void eb4_factureALot_bcDUnAutreLot_409() throws Exception {
        UUID autreLot = fixture.lot("LOT2-" + c.jeton(), c.projet());
        fixture.poser(bc, "PrjMarketId", autreLot);

        assertConflit(affecterPar(compte, facture, bc));
    }

    @Test
    void eb4_factureALot_bcSansLot_409() throws Exception {
        fixture.effacer(bc, "PrjMarketId");

        assertConflit(affecterPar(compte, facture, bc));
    }

    @Test
    void eb4_factureAAvance_409() throws Exception {
        fixture.poser(facture, "AdvanceAmount", new BigDecimal("100"));

        assertConflit(affecterPar(compte, facture, bc));
    }

    @Test
    void eb4_factureDeDecompte_gereeAilleurs_409() throws Exception {
        fixture.decompte(facture);

        assertConflit(affecterPar(compte, facture, bc));
    }

    @Test
    void eb4_fraisDeTerrain_referenceTypeNonVide_409() throws Exception {
        fixture.poser(facture, "ReferenceType", "LAND_EXPENSE");

        assertConflit(affecterPar(compte, facture, bc));
    }

    // ------------------------------------------------------------------ 409 quantités (LimitDeliveryAndInvoiceQuantityToOrder = 1)

    @Test
    void eb4_articleAbsentDuBc_409() throws Exception {
        ligne(facture, "ART-X", "1", PU);

        MvcResult r = affecterPar(compte, facture, bc);

        assertConflit(r);
        assertThat(detail(r)).contains("n'existe pas dans le bon de commande");
    }

    @Test
    void eb4_articleAuPrixHtDifferentDuBc_409() throws Exception {
        UUID autre = piece(FixtureAchat.FACTURE, c, lot);
        ligne(autre, ARTICLE, "1", new BigDecimal("120"));

        MvcResult r = affecterPar(compte, autre, bc);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).contains("n'existe pas dans le bon de commande");
    }

    /** Σ quantités déjà facturées sur ce BC (6) + celles de la facture (5) > commandé (10). */
    @Test
    void eb4_quantiteCumuleeDepasseLeBc_409() throws Exception {
        UUID dejaFacturee = piece(FixtureAchat.FACTURE, c, lot);
        ligne(dejaFacturee, ARTICLE, "6", PU);
        fixture.poser(dejaFacturee, BUY_ORDER_ID, bc);
        UUID nouvelle = piece(FixtureAchat.FACTURE, c, lot);
        ligne(nouvelle, ARTICLE, "5", PU);

        MvcResult r = affecterPar(compte, nouvelle, bc);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).contains("dépasse celle définie dans le bon de commande");
        fixture.oublier();
        assertThat(fixture.lienDe(nouvelle, BUY_ORDER_ID)).isNull();
    }

    /** Limite atteinte sans la dépasser (6 + 4 = 10) : accepté. */
    @Test
    void eb4_quantiteCumuleeEgaleAuBc_200() throws Exception {
        UUID dejaFacturee = piece(FixtureAchat.FACTURE, c, lot);
        ligne(dejaFacturee, ARTICLE, "6", PU);
        fixture.poser(dejaFacturee, BUY_ORDER_ID, bc);

        MvcResult r = affecterPar(compte, facture, bc);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }
}
