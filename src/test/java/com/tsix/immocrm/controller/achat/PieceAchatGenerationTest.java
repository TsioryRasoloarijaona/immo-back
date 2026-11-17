package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NatureSaisie;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.*;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_DEVIS;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.idCree;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.ligne;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.DEVIS;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2b G1, G2, G6, D4, Q-E, Q-F (FEATURE-achat-generation.md §4, §8 « PieceAchatGenerationTest » et « G6 ») :
 * {@code POST /api/achat/documents} avec {@code sources} — chaque génération permise (201, liens écrits), chaque refus (409,
 * message OMRAN exact, rien écrit, numéro non consommé) ; {@code GET …/modele} avec sources n'écrit rien ; contrôle de
 * quantités du BL au POST. Compte sans personnel (la restriction « mes BC » ne joue pas ici).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatGenerationTest {

    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String BUY_QUOTE_ID = "BuyQuoteId";
    private static final String BUY_INVOICE_ID = "BuyInvoiceId";
    private static final String ART = "ART-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AGN");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AGN", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        generation.parametres(false, false, false);
        generation.tableBuyOrderInvoice();
    }

    // ------------------------------------------------------------------ sources

    private UUID devis() {
        UUID d = generation.source(c, "-DV" + (rang++), DEVIS, s.gestion(FixtureAchat.GESTION_DEVIS), null);
        generation.ligne(d, LigneSource.simple(1, ART, "10", s.categorie()));
        return d;
    }

    /** BC à une ligne ART-1 10 × 100 (celle de {@code corpsValide}). */
    private UUID bc(UUID statut) {
        UUID bc = generation.source(c, "-BC" + (rang++), BON_COMMANDE, s.gestion(FixtureAchat.GESTION_BC), statut);
        generation.ligne(bc, LigneSource.simple(1, ART, "10", s.categorie()));
        return bc;
    }

    /** BL du cadre, une ligne ART-1 × 2 ; {@code bc} nul possible. */
    private UUID bl(UUID statut, UUID bc) {
        UUID bl = generation.source(c, "-BL" + (rang++), BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), statut);
        generation.ligne(bl, LigneSource.simple(1, ART, "2", s.categorie()));
        if (bc != null) {
            fixture.poser(bl, BUY_ORDER_ID, bc);
        }
        return bl;
    }

    private UUID facture() {
        return generation.source(c, "-FA" + (rang++), FACTURE, s.gestion(FixtureAchat.GESTION_FACTURE), s.enCours());
    }

    private MvcResult par(MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(redacteur, ROLE_JWT));
    }

    private MvcResult generer(String nature, UUID... sources) throws Exception {
        return par(creer(avecSources(corpsValide(c, s, nature), sources)));
    }

    private UUID genererOk(String nature, UUID... sources) throws Exception {
        MvcResult r = generer(nature, sources);
        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        return idCree(r);
    }

    /** 409 au message exact ; aucune pièce de la cible écrite, numéro de la cible non consommé. */
    private void assert409RienEcrit(MvcResult r, String nature, String message) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(message);
        assertRienEcrit(nature);
    }

    private void assertRienEcrit(String nature) {
        fixture.oublier();
        FixtureAchat.SequenceNature seq = FixtureAchat.SEQUENCES_SAISIE.get(nature);
        assertAll(
                () -> assertThat(fixture.valeurSequence(seq.code())).as("numéro non consommé").isEqualTo(seq.valeur()),
                () -> assertThat(generation.nombreParCode(c.fournisseur(), seq.codeSuivant())).as("aucune pièce créée").isZero());
    }

    // ------------------------------------------------------------------ G2 : générations permises

    /** G2 / G6 : ORDER ← 1 QUOTE, sans précondition de statut (devis à statut NULL) ; BuyQuoteId = devis. */
    @Test
    void g2_bonDeCommande_depuisUnDevisSansStatut_201_buyQuoteIdEcrit() throws Exception {
        UUID devis = devis();

        UUID bc = genererOk(BON_COMMANDE, devis);

        assertThat(fixture.lienDe(bc, BUY_QUOTE_ID)).isEqualTo(id(devis));
    }

    /** G2 / G6 : DELIVRY ← 1 ORDER validé (statut IsOkForPayement) ; BuyOrderId = BC. */
    @Test
    void g2_bonDeLivraison_depuisBcValide_201_buyOrderIdEcrit() throws Exception {
        UUID bc = bc(s.valideDg());

        UUID bl = genererOk(BON_LIVRAISON, bc);

        assertThat(fixture.lienDe(bl, BUY_ORDER_ID)).isEqualTo(id(bc));
    }

    /** G2 : BC dont le statut n'a pas IsOkForPayement → 409, message OMRAN, rien écrit. */
    @Test
    void g2_bonDeLivraison_depuisBcNonValide_409_message_rienEcrit() throws Exception {
        UUID bc = bc(s.enCours());

        assert409RienEcrit(generer(BON_LIVRAISON, bc), BON_LIVRAISON, BC_NON_VALIDE_POUR_BL);
    }

    /** G2 / G6 : INVOICE ← 1 BL validé ; BuyOrderId = BC du BL, BuyInvoiceId du BL = facture, une ligne BuyOrderInvoice. */
    @Test
    void g2g6_facture_depuisUnBl_201_liensEcrits() throws Exception {
        UUID bc = bc(s.valideDg());
        UUID bl = bl(s.valideDg(), bc);

        UUID facture = genererOk(FACTURE, bl);

        assertAll(
                () -> assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).isEqualTo(id(bc)),
                () -> assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).isEqualTo(id(facture)),
                () -> assertThat(generation.commandesDeFacture(facture)).containsExactly(id(bc)));
    }

    /** G2 / G6 / Q-E : INVOICE ← 2 BL du même BC ; chaque BL facturé, une seule ligne BuyOrderInvoice (BC distinct). */
    @Test
    void g2g6_facture_depuisDeuxBlDuMemeBc_chaqueBlFacture_uneLigneBuyOrderInvoice() throws Exception {
        UUID bc = bc(s.valideDg());
        UUID bl1 = bl(s.valideDg(), bc);
        UUID bl2 = bl(s.valideDg(), bc);

        UUID facture = genererOk(FACTURE, bl1, bl2);

        assertAll(
                () -> assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).isEqualTo(id(bc)),
                () -> assertThat(fixture.lienDe(bl1, BUY_INVOICE_ID)).isEqualTo(id(facture)),
                () -> assertThat(fixture.lienDe(bl2, BUY_INVOICE_ID)).isEqualTo(id(facture)),
                () -> assertThat(generation.commandesDeFacture(facture)).containsExactly(id(bc)));
    }

    /** G4 / G6 : BL sans BC → facture sans BuyOrderId ni ligne BuyOrderInvoice. */
    @Test
    void g4g6_facture_depuisBlSansBc_buyOrderIdNull_aucuneLigneBuyOrderInvoice() throws Exception {
        UUID bl = bl(s.valideDg(), null);

        UUID facture = genererOk(FACTURE, bl);

        assertAll(
                () -> assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).isNull(),
                () -> assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).isEqualTo(id(facture)),
                () -> assertThat(generation.commandesDeFacture(facture)).isEmpty());
    }

    // ------------------------------------------------------------------ G2 : refus de la facture depuis des BL

    /** G2 : BL de fournisseurs différents → 409 (CfgTierId distincts). */
    @Test
    void g2_facture_blDeFournisseursDifferents_409_message_rienEcrit() throws Exception {
        UUID bc = bc(s.valideDg());
        UUID bl1 = bl(s.valideDg(), bc);
        UUID bl2 = blDUnAutreFournisseur(bc);

        assert409RienEcrit(generer(FACTURE, bl1, bl2), FACTURE, FOURNISSEURS_DIFFERENTS);
        assertThat(fixture.lienDe(bl1, BUY_INVOICE_ID)).isNull();
    }

    private UUID blDUnAutreFournisseur(UUID bc) {
        String nom = "Autre " + c.jeton();
        UUID autre = fixture.fournisseur("FOU2-" + c.jeton(), nom);
        UUID bl = fixture.piece(Piece.facture(c.jeton() + "-BLX", LocalDate.of(2026, 5, 4), autre, nom, c.societe(),
                c.projet(), new BigDecimal("100")).nature(BON_LIVRAISON).statut(s.valideDg()));
        fixture.poser(bl, "StrEntityId", s.gestion(FixtureAchat.GESTION_BL));
        fixture.poser(bl, BUY_ORDER_ID, bc);
        generation.ligne(bl, LigneSource.simple(1, ART, "1", s.categorie()));
        return bl;
    }

    /** G2 (Invoice.cs l.909-924, dans cet ordre) : fournisseurs différents l'emporte sur « déjà facturé ». */
    @Test
    void g2_facture_ordreDesRefus_fournisseurAvantDejaFacture() throws Exception {
        UUID bc = bc(s.valideDg());
        UUID bl1 = bl(s.valideDg(), bc);
        UUID bl2 = blDUnAutreFournisseur(bc);
        fixture.poser(bl2, BUY_INVOICE_ID, facture());

        MvcResult r = generer(FACTURE, bl1, bl2);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(FOURNISSEURS_DIFFERENTS);
    }

    /** G2 / Q-F : BL de BC différents, GenerateInvoiceFromManyOrders = 0 → 409, aucun BL facturé. */
    @Test
    void g2_facture_blDeBcDifferents_409_message_rienEcrit() throws Exception {
        UUID bl1 = bl(s.valideDg(), bc(s.valideDg()));
        UUID bl2 = bl(s.valideDg(), bc(s.valideDg()));

        assert409RienEcrit(generer(FACTURE, bl1, bl2), FACTURE, BC_DIFFERENTS);
        assertAll(
                () -> assertThat(fixture.lienDe(bl1, BUY_INVOICE_ID)).isNull(),
                () -> assertThat(fixture.lienDe(bl2, BUY_INVOICE_ID)).isNull());
    }

    /** Q-F (décision manager 17/11) / Q-E : GenerateInvoiceFromManyOrders = 1 lève le refus ; une ligne par BC distinct. */
    @Test
    void qF_facture_blDeBcDifferents_parametrePlusieursBc_201_uneLigneParBc_buyOrderIdNull() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_PLUSIEURS_BC, true);
        UUID bc1 = bc(s.valideDg());
        UUID bc2 = bc(s.valideDg());
        UUID bl1 = bl(s.valideDg(), bc1);
        UUID bl2 = bl(s.valideDg(), bc2);
        UUID bl3 = bl(s.valideDg(), bc1);

        UUID facture = genererOk(FACTURE, bl1, bl2, bl3);

        assertAll(
                () -> assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).as("aucun BC commun unique (l.2413-2431)").isNull(),
                () -> assertThat(generation.commandesDeFacture(facture)).containsExactlyInAnyOrder(id(bc1), id(bc2)),
                () -> assertThat(List.of(bl1, bl2, bl3)).hasSize(3).allSatisfy(
                        bl -> assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).isEqualTo(id(facture))));
    }

    /** G2 : BL déjà facturé (BuyInvoiceId non nul) → 409, son lien inchangé. */
    @Test
    void g2_facture_blDejaFacture_409_message_rienEcrit() throws Exception {
        UUID ancienne = facture();
        UUID bl = bl(s.valideDg(), bc(s.valideDg()));
        fixture.poser(bl, BUY_INVOICE_ID, ancienne);

        assert409RienEcrit(generer(FACTURE, bl), FACTURE, BL_DEJA_FACTURE);
        assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).isEqualTo(id(ancienne));
    }

    /** G2 : plusieurs BL dont un déjà facturé → 409. */
    @Test
    void g2_facture_plusieursBlDontUnDejaFacture_409_message() throws Exception {
        UUID bc = bc(s.valideDg());
        UUID bl1 = bl(s.valideDg(), bc);
        UUID bl2 = bl(s.valideDg(), bc);
        fixture.poser(bl2, BUY_INVOICE_ID, facture());

        assert409RienEcrit(generer(FACTURE, bl1, bl2), FACTURE, BL_DEJA_FACTURE);
    }

    /** G2 : BL dont le statut n'a pas IsOkForPayement → 409. */
    @Test
    void g2_facture_blNonValide_409_message_rienEcrit() throws Exception {
        UUID bl = bl(s.enCours(), bc(s.valideDg()));

        assert409RienEcrit(generer(FACTURE, bl), FACTURE, BL_NON_VALIDE);
        assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).isNull();
    }

    /** G2 (Create(Delivery) l.869-876) : un BL déjà facturé ET non validé → « déjà facturé » d'abord. */
    @Test
    void g2_facture_unBl_dejaFactureAvantNonValide() throws Exception {
        UUID bl = bl(s.enCours(), bc(s.valideDg()));
        fixture.poser(bl, BUY_INVOICE_ID, facture());

        MvcResult r = generer(FACTURE, bl);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(BL_DEJA_FACTURE);
    }

    // ------------------------------------------------------------------ D4 : facture depuis un BC

    /** D4 : GenerateInvoiceFromOrder = 1, BC validé → 201 ; BuyOrderId = BC, une ligne BuyOrderInvoice. */
    @Test
    void d4_facture_depuisBc_parametreVrai_201_buyOrderId_uneLigneBuyOrderInvoice() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, true);
        UUID bc = bc(s.valideDg());

        UUID facture = genererOk(FACTURE, bc);

        assertAll(
                () -> assertThat(fixture.lienDe(facture, BUY_ORDER_ID)).isEqualTo(id(bc)),
                () -> assertThat(generation.commandesDeFacture(facture)).containsExactly(id(bc)));
    }

    /** D4 : GenerateInvoiceFromOrder = 0 → 409, rien écrit. */
    @Test
    void d4_facture_depuisBc_parametreFaux_409_rienEcrit() throws Exception {
        MvcResult r = generer(FACTURE, bc(s.valideDg()));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertRienEcrit(FACTURE);
    }

    /** D4 (décision Q-D) : ligne AdmParamsBoolean absente = faux → 409. */
    @Test
    void d4_facture_depuisBc_parametreAbsent_409_rienEcrit() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, null);

        MvcResult r = generer(FACTURE, bc(s.valideDg()));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertRienEcrit(FACTURE);
    }

    /** G2 (Create(Order) l.855-858) : paramètre vrai mais BC non validé → 409, message OMRAN. */
    @Test
    void g2d4_facture_depuisBcNonValide_parametreVrai_409_message() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, true);

        assert409RienEcrit(generer(FACTURE, bc(s.enCours())), FACTURE, BC_NON_VALIDE_POUR_FACTURE);
    }

    // ------------------------------------------------------------------ G2 : toute autre combinaison

    /** Combinaisons hors G2 : nature cible et sources. */
    enum Combinaison {
        BL_DEPUIS_DEVIS, FACTURE_DEPUIS_DEVIS, BC_DEPUIS_BC, BC_DEPUIS_DEUX_DEVIS, BL_DEPUIS_DEUX_BC, BL_DEPUIS_BL,
        FACTURE_DEPUIS_BL_ET_BC, BC_DEPUIS_FACTURE, FACTURE_DEPUIS_FACTURE
    }

    /** G2 « toute autre combinaison → 409 », rien écrit (D4 activé : seule la combinaison est en cause). */
    @ParameterizedTest
    @EnumSource(Combinaison.class)
    void g2_autreCombinaison_409_rienEcrit(Combinaison combinaison) throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, true);
        UUID bcValide = bc(s.valideDg());
        String cible = switch (combinaison) {
            case BL_DEPUIS_DEVIS, BL_DEPUIS_DEUX_BC, BL_DEPUIS_BL -> BON_LIVRAISON;
            case FACTURE_DEPUIS_DEVIS, FACTURE_DEPUIS_BL_ET_BC, FACTURE_DEPUIS_FACTURE -> FACTURE;
            default -> BON_COMMANDE;
        };
        UUID[] sources = switch (combinaison) {
            case BL_DEPUIS_DEVIS, FACTURE_DEPUIS_DEVIS -> new UUID[]{devis()};
            case BC_DEPUIS_BC -> new UUID[]{bcValide};
            case BC_DEPUIS_DEUX_DEVIS -> new UUID[]{devis(), devis()};
            case BL_DEPUIS_DEUX_BC -> new UUID[]{bcValide, bc(s.valideDg())};
            case BL_DEPUIS_BL -> new UUID[]{bl(s.valideDg(), bcValide)};
            case FACTURE_DEPUIS_BL_ET_BC -> new UUID[]{bl(s.valideDg(), bcValide), bcValide};
            default -> new UUID[]{facture()};
        };

        MvcResult r = generer(cible, sources);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertRienEcrit(cible);
    }

    // ------------------------------------------------------------------ G1

    /** G1 : {@code sources} sur une cible non générable (QUOTE) → 400 au POST, rien écrit. */
    @Test
    void g1_post_sourcesSurDevis_400_rienEcrit() throws Exception {
        MvcResult r = generer(DEVIS, bc(s.valideDg()));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertRienEcrit(DEVIS);
    }

    /** G1 : {@code sources} sur QUOTE → 400 au modèle. */
    @Test
    void g1_modele_sourcesSurDevis_400() throws Exception {
        MvcResult r = par(modele(DEVIS, bc(s.valideDg())));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
    }

    /** G1 : le modèle avec sources (200) n'écrit rien — aucun numéro, aucun lien, aucune ligne BuyOrderInvoice. */
    @Test
    void g1_modele_avecSources_200_nEcritRien() throws Exception {
        UUID bc = bc(s.valideDg());
        UUID bl = bl(s.valideDg(), bc);

        MvcResult r = par(modele(FACTURE, bl));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertRienEcrit(FACTURE);
        assertAll(
                () -> assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).isNull(),
                () -> assertThat(fixture.nombre("BuyOrderInvoice", "BuyOrderId", bc)).isZero());
    }

    /** G1 : le POST recontrôle — un modèle obtenu (BC validé) puis le BC repassé non validé → POST 409. */
    @Test
    void g1_post_recontroleLesPreconditions_apresLeModele() throws Exception {
        UUID bc = bc(s.valideDg());
        assertThat(statut(par(modele(BON_LIVRAISON, bc)))).isEqualTo(200);
        fixture.poser(bc, "BuyDocumentStatusId", s.enCours());

        assert409RienEcrit(generer(BON_LIVRAISON, bc), BON_LIVRAISON, BC_NON_VALIDE_POUR_BL);
    }

    // ------------------------------------------------------------------ G6 : quantités du BL au POST

    private MvcResult genererBl(UUID bc, CorpsJson ligne) throws Exception {
        return par(creer(avecSources(corpsValide(c, s, BON_LIVRAISON).avec("lignes", List.of(ligne)), bc)));
    }

    /** G6 : LimitDeliveryAndInvoiceQuantityToOrder = 1, quantité > commandée → 409 (message de QuantitesBonCommande). */
    @Test
    void g6_bl_quantiteSuperieureALaCommande_409_message_rienEcrit() throws Exception {
        generation.parametre(FixtureAchat.LIMITE_QUANTITES, true);
        UUID bc = bc(s.valideDg());

        assert409RienEcrit(genererBl(bc, ligne(ART, "11", "100", s.categorie())), BON_LIVRAISON, depasse(ART));
    }

    /** G6 : article absent du BC → 409 (message de QuantitesBonCommande). */
    @Test
    void g6_bl_articleAbsentDuBc_409_message_rienEcrit() throws Exception {
        generation.parametre(FixtureAchat.LIMITE_QUANTITES, true);
        UUID bc = bc(s.valideDg());

        assert409RienEcrit(genererBl(bc, ligne("ART-9", "1", "100", s.categorie())), BON_LIVRAISON, absentDuBc("ART-9"));
    }

    /** G6 : le déjà livré par les autres BL du BC compte (10 commandés, 4 livrés, 7 demandés → 409). */
    @Test
    void g6_bl_dejaLivreCompte_409_message() throws Exception {
        generation.parametre(FixtureAchat.LIMITE_QUANTITES, true);
        UUID bc = bc(s.valideDg());
        UUID livre = generation.source(c, "-BLL", BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.valideDg());
        fixture.poser(livre, BUY_ORDER_ID, bc);
        generation.ligne(livre, LigneSource.simple(1, ART, "4", s.categorie()));

        assert409RienEcrit(genererBl(bc, ligne(ART, "7", "100", s.categorie())), BON_LIVRAISON, depasse(ART));
    }

    /** G6 : dans la limite (10 commandés, 4 livrés, 6 demandés) → 201. */
    @Test
    void g6_bl_dansLaLimite_201() throws Exception {
        generation.parametre(FixtureAchat.LIMITE_QUANTITES, true);
        UUID bc = bc(s.valideDg());
        UUID livre = generation.source(c, "-BLL", BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.valideDg());
        fixture.poser(livre, BUY_ORDER_ID, bc);
        generation.ligne(livre, LigneSource.simple(1, ART, "4", s.categorie()));

        MvcResult r = genererBl(bc, ligne(ART, "6", "100", s.categorie()));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertThat(fixture.lienDe(idCree(r), BUY_ORDER_ID)).isEqualTo(id(bc));
    }

    /** G6 : paramètre faux → aucun contrôle de quantités (11 > 10 accepté), lien écrit. */
    @Test
    void g6_bl_parametreFaux_pasDeControle_201() throws Exception {
        UUID bc = bc(s.valideDg());

        MvcResult r = genererBl(bc, ligne(ART, "11", "100", s.categorie()));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertThat(fixture.lienDe(idCree(r), BUY_ORDER_ID)).isEqualTo(id(bc));
    }

    /** Témoin G6 : la même création libre (sans sources) n'écrit aucun lien. */
    @Test
    void g6_creationLibre_aucunLien() throws Exception {
        MvcResult r = par(creer(corpsValide(c, s, NatureSaisie.BL.code)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        UUID bl = idCree(r);
        assertAll(
                () -> assertThat(fixture.lienDe(bl, BUY_ORDER_ID)).isNull(),
                () -> assertThat(fixture.lienDe(bl, BUY_QUOTE_ID)).isNull());
    }
}
