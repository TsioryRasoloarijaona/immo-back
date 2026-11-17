package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.*;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.NON_CONFIGUREE;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.champDesLignesDeCode;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.decimal;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.modele;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NATURE_NON_PRISE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.decimalDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.guidDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.idCree;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.ligne;
import static com.tsix.immocrm.fixtures.FixtureAchat.AVOIR;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot Avoirs (FEATURE-achat-avoirs.md) — création : modèle REFUND (R1, R9), POST à source unique (R2, R3), gestions (R7),
 * ordre 404 → 403 → 400 → 409 et droit (R8, D10, D11), montants sans ligne (R10), avoir financier (R11), net à payer
 * (R12), numéro (R13).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatAvoirTest {

    private static final String QUANTITE = "quantite";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Contexte x;
    private UUID facture;
    private int rang;

    @BeforeEach
    void setUp() {
        x = preparer(fixture, generation, "AAV");
        facture = facture(fixture, x, "-FA");
    }

    private MvcResult par(AdmUser compte, MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(compte, ROLE_JWT));
    }

    private MvcResult par(MockHttpServletRequestBuilder requete) throws Exception {
        return par(x.redacteur(), requete);
    }

    private AdmUser compte(String... droits) {
        AdmUser compte = fixture.compteHabilite(x.c(), "AAV" + (rang++), ROLE_JWT, droits);
        fixture.sansPersonnel(compte);
        return compte;
    }

    private String modeleOk(UUID source) throws Exception {
        MvcResult r = par(modele(AVOIR, source));
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        return corps(r);
    }

    private MvcResult creerAvoir(CorpsJson corps, UUID... sources) throws Exception {
        return par(creer(avecSources(corps, sources)));
    }

    private static String guid(String json, String chemin) {
        Object v = JsonPath.read(json, chemin);
        return v == null ? null : v.toString().toUpperCase();
    }

    private static void assertStatut(MvcResult r, int attendu) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(attendu);
    }

    /** D11 : aucun refus n'écrit d'avoir ni ne consomme de numéro. */
    private void assertRienEcrit() {
        fixture.oublier();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(fixture.valeurSequence(SEQUENCE_AVOIR.code())).isEqualTo(SEQUENCE_AVOIR.valeur());
        sa.assertThat(generation.nombrePieces(x.c().fournisseur(), AVOIR)).isZero();
        sa.assertAll();
    }

    private UUID factureDeGestion(String code) {
        UUID gestion = switch (code) {
            case FixtureAchat.GESTION_FACTURE -> x.s().gestion(code);
            case FixtureGenerationAchat.GESTION_FACTURE_MULTI -> generation.gestion(code);
            case GESTION_DECOMPTE -> fixture.gestionVide(code, PKEY_DECOMPTE);
            case GESTION_FACTURE_STL -> fixture.gestionVide(code, PKEY_FACTURE_STL);
            default -> fixture.gestionVide(code, PKEY_FACTURE_COM);
        };
        UUID f = facture(fixture, x, "-FG" + (rang++));
        fixture.poser(f, "StrEntityId", gestion);
        return f;
    }

    // ------------------------------------------------------------------ R1 modèle

    /** R1 : nature REFUND, gestion d'arrivée BuyRefundDebit, statut de départ de cette gestion, sources = la facture. */
    @Test
    void r1_modele_gestionBuyRefundDebit_statutDeDepart_sources() throws Exception {
        String json = modeleOk(facture);

        List<String> sources = JsonPath.read(json, "$.sources");
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat((String) JsonPath.read(json, "$.nature")).isEqualTo(AVOIR);
        sa.assertThat((String) JsonPath.read(json, "$.gestion")).isEqualTo(GESTION_AVOIR);
        sa.assertThat(guid(json, "$.statutDepartId")).isEqualTo(id(x.departAvoir()));
        sa.assertThat(sources).extracting(String::toUpperCase).containsExactly(id(facture));
        sa.assertAll();
    }

    /** R1 / D3 : avoirFinancier = DefaultRefundIsFinancial (1). */
    @Test
    void r1_modele_avoirFinancier_parametreVrai() throws Exception {
        assertThat((Boolean) JsonPath.read(modeleOk(facture), "$.avoirFinancier")).isTrue();
    }

    /** R1 / D3 : paramètre à 0 → faux. */
    @Test
    void r1_modele_avoirFinancier_parametreFaux() throws Exception {
        generation.parametre(DEFAUT_FINANCIER, false);

        assertThat((Boolean) JsonPath.read(modeleOk(facture), "$.avoirFinancier")).isFalse();
    }

    /** R1 / D3 : ligne DefaultRefundIsFinancial absente → faux. */
    @Test
    void r1_modele_avoirFinancier_parametreAbsent_faux() throws Exception {
        generation.parametre(DEFAUT_FINANCIER, null);

        assertThat((Boolean) JsonPath.read(modeleOk(facture), "$.avoirFinancier")).isFalse();
    }

    /** R13 : le modèle ne consomme pas de numéro. */
    @Test
    void r13_modele_neConsommePasDeNumero() throws Exception {
        modeleOk(facture);

        fixture.oublier();
        assertThat(fixture.valeurSequence(SEQUENCE_AVOIR.code())).isEqualTo(SEQUENCE_AVOIR.valeur());
    }

    // ------------------------------------------------------------------ R9 préremplissage

    /** R9 : en-tête copié (société, tiers, module, type de marché, lot, projet, notes). */
    @Test
    void r9_modele_enteteCopieDeLaFacture() throws Exception {
        UUID lot = fixture.lot("LOT-" + x.c().jeton(), x.c().projet());
        fixture.poser(facture, "PrjMarketId", lot);
        fixture.poser(facture, "AdmModuleId", x.s().moduleProjet());
        fixture.poser(facture, "PrjMarketTypeId", x.s().typeMarche());
        fixture.poser(facture, "Notes", "N-facture");
        generation.ligne(facture, LigneSource.simple(1, "A", "1", x.s().categorie()));

        String json = modeleOk(facture);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(guid(json, "$.societeId")).isEqualTo(id(x.c().societe()));
        sa.assertThat(guid(json, "$.fournisseurId")).isEqualTo(id(x.c().fournisseur()));
        sa.assertThat(guid(json, "$.moduleId")).isEqualTo(id(x.s().moduleProjet()));
        sa.assertThat(guid(json, "$.typeMarcheId")).isEqualTo(id(x.s().typeMarche()));
        sa.assertThat(guid(json, "$.lotId")).isEqualTo(id(lot));
        sa.assertThat(guid(json, "$.projetId")).isEqualTo(id(x.c().projet()));
        sa.assertThat((String) JsonPath.read(json, "$.notes")).isEqualTo("N-facture");
        sa.assertAll();
    }

    /** R9 : toutes les lignes clonées, deux lignes identiques gardées séparées (pas de fusion). */
    @Test
    void r9_modele_lignesCloneesSansFusion() throws Exception {
        generation.ligne(facture, LigneSource.simple(1, "A", "2", x.s().categorie()));
        generation.ligne(facture, LigneSource.simple(2, "A", "3", x.s().categorie()));
        generation.ligne(facture, LigneSource.simple(3, "B", "1", x.s().categorie()));

        String json = modeleOk(facture);

        List<Object> quantitesA = champDesLignesDeCode(json, "A", QUANTITE);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(JsonPath.<List<Object>>read(json, "$.lignes")).hasSize(3);
        sa.assertThat(quantitesA).extracting(q -> decimal(q).stripTrailingZeros())
                .containsExactlyInAnyOrder(new BigDecimal("2"), new BigDecimal("3"));
        sa.assertAll();
    }

    /** R9 : ni régul ni timbre copiés (facture régul 10, timbre 2 → 0 et 0). */
    @Test
    void r9_modele_niRegulNiTimbre() throws Exception {
        fixture.poser(facture, "AmountRegul", new BigDecimal("10"));
        fixture.poser(facture, "FiscalStamp", new BigDecimal("2"));
        generation.ligne(facture, LigneSource.simple(1, "A", "1", x.s().categorie()));

        String json = modeleOk(facture);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(decimal(JsonPath.read(json, "$.montantRegul"))).isEqualByComparingTo("0");
        sa.assertThat(decimal(JsonPath.read(json, "$.timbreFiscal"))).isEqualByComparingTo("0");
        sa.assertAll();
    }

    /** R9 : ventilation projet de la facture copiée. */
    @Test
    void r9_modele_ventilationProjetCopiee() throws Exception {
        UUID autreProjet = fixture.projet("P2-" + x.c().jeton(), x.c().societe());
        fixture.habiliter(x.redacteur(), autreProjet);
        fixture.ventilationProjet(facture, x.c().projet(), new BigDecimal("0.4"));
        fixture.ventilationProjet(facture, autreProjet, new BigDecimal("0.6"));
        generation.ligne(facture, LigneSource.simple(1, "A", "1", x.s().categorie()));

        List<String> cles = JsonPath.read(modeleOk(facture), "$.ventilationProjets[*].cleId");

        assertThat(cles).extracting(String::toUpperCase).containsExactlyInAnyOrder(id(x.c().projet()), id(autreProjet));
    }

    /** R9 / R10 : facture sans ligne → HT et TVA de la facture (1 000 et 200), catégorie de sa ventilation. */
    @Test
    void r9_modele_factureSansLigne_montantsEtCategorieCopies() throws Exception {
        UUID categorie = fixture.categorie("CAT2-" + x.c().jeton());
        fixture.ventilation(facture, categorie, BigDecimal.ONE);

        String json = modeleOk(facture);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(JsonPath.<List<Object>>read(json, "$.lignes")).isEmpty();
        sa.assertThat(decimal(JsonPath.read(json, "$.montantHT"))).isEqualByComparingTo("1000");
        sa.assertThat(decimal(JsonPath.read(json, "$.montantTVA"))).isEqualByComparingTo("200");
        sa.assertThat(guid(json, "$.categorieId")).isEqualTo(id(categorie));
        sa.assertAll();
    }

    /** R9 : après POST, la facture source (InvoiceSourceId) et son BC (BuyOrderId) sont recopiés. */
    @Test
    void r9_post_factureSourceEtBcRecopies() throws Exception {
        UUID bc = fixture.piece(x.c(), "-BC", BON_COMMANDE);
        fixture.poser(facture, "BuyOrderId", bc);

        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), facture);

        assertStatut(r, 201);
        Map<String, Object> avoir = fixture.pieceEnBase(idCree(r));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(guidDe(avoir, SOURCE)).isEqualTo(id(facture));
        sa.assertThat(guidDe(avoir, "BuyOrderId")).isEqualTo(id(bc));
        sa.assertThat(avoir).containsEntry("DocType", AVOIR);
        sa.assertThat(guidDe(avoir, "StrEntityId")).isEqualTo(id(x.gestionAvoir()));
        sa.assertThat(guidDe(avoir, "BuyDocumentStatusId")).isEqualTo(id(x.departAvoir()));
        sa.assertAll();
    }

    // ------------------------------------------------------------------ R2 / R3 source unique de nature INVOICE

    /** R2 : deux factures → 409 générique, rien écrit. */
    @Test
    void r2_post_deuxFactures_409_generique_rienEcrit() throws Exception {
        UUID seconde = facture(fixture, x, "-FA2");

        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), facture, seconde);

        assertStatut(r, 409);
        assertThat(detail(r)).isEqualTo(IMPOSSIBLE);
        assertRienEcrit();
    }

    /** R2 / R3 : un BL (même validé) n'est pas une source d'avoir → 409 générique, rien écrit. */
    @Test
    void r3_post_sourceBl_409_generique_rienEcrit() throws Exception {
        UUID bl = generation.source(x.c(), "-BL", BON_LIVRAISON, x.s().gestion(FixtureAchat.GESTION_BL), x.s().valideDg());
        generation.ligne(bl, LigneSource.simple(1, "A", "1", x.s().categorie()));

        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), bl);

        assertStatut(r, 409);
        assertThat(detail(r)).isEqualTo(IMPOSSIBLE);
        assertRienEcrit();
    }

    /** R2 / R3 : un BC validé n'est pas une source d'avoir → 409 générique, rien écrit. */
    @Test
    void r3_post_sourceBc_409_generique_rienEcrit() throws Exception {
        UUID bc = generation.source(x.c(), "-BCV", BON_COMMANDE, x.s().gestion(FixtureAchat.GESTION_BC), x.s().valideDg());
        generation.ligne(bc, LigneSource.simple(1, "A", "1", x.s().categorie()));

        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), bc);

        assertStatut(r, 409);
        assertThat(detail(r)).isEqualTo(IMPOSSIBLE);
        assertRienEcrit();
    }

    /** R2 : le modèle refuse aussi un BL comme source. */
    @Test
    void r3_modele_sourceBl_409_generique() throws Exception {
        UUID bl = generation.source(x.c(), "-BLM", BON_LIVRAISON, x.s().gestion(FixtureAchat.GESTION_BL), x.s().valideDg());

        MvcResult r = par(modele(AVOIR, bl));

        assertStatut(r, 409);
        assertThat(detail(r)).isEqualTo(IMPOSSIBLE);
    }

    // ------------------------------------------------------------------ R7 gestion de la facture

    /** R7 / D7 : facture d'une gestion autre que PrjInvoiceDebit → 409 « non configurée » au modèle. */
    @ParameterizedTest
    @ValueSource(strings = {GESTION_DECOMPTE, GESTION_FACTURE_STL, GESTION_FACTURE_COM, FixtureAchat.GESTION_FACTURE,
            FixtureGenerationAchat.GESTION_FACTURE_MULTI})
    void r7_modele_autreGestion_409_nonConfiguree(String gestion) throws Exception {
        MvcResult r = par(modele(AVOIR, factureDeGestion(gestion)));

        assertStatut(r, 409);
        assertThat(detail(r)).isEqualTo(NON_CONFIGUREE + " " + gestion);
    }

    /** R7 / D11 : même refus au POST, sans numéro consommé ni avoir écrit. */
    @ParameterizedTest
    @ValueSource(strings = {GESTION_DECOMPTE, GESTION_FACTURE_STL, GESTION_FACTURE_COM, FixtureAchat.GESTION_FACTURE,
            FixtureGenerationAchat.GESTION_FACTURE_MULTI})
    void r7_post_autreGestion_409_nonConfiguree_rienEcrit(String gestion) throws Exception {
        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), factureDeGestion(gestion));

        assertStatut(r, 409);
        assertThat(detail(r)).isEqualTo(NON_CONFIGUREE + " " + gestion);
        assertRienEcrit();
    }

    // ------------------------------------------------------------------ R8 / D11 404

    /** D11 : facture introuvable → 404 au message OMRAN ; témoin : la facture connue → 200. */
    @Test
    void r8_modele_factureIntrouvable_404_messageOmran_temoin200() throws Exception {
        MvcResult inconnue = par(modele(AVOIR, UUID.randomUUID()));
        MvcResult temoin = par(modele(AVOIR, facture));

        assertStatut(temoin, 200);
        assertStatut(inconnue, 404);
        assertThat(detail(inconnue)).isEqualTo(FACTURE_INTROUVABLE);
    }

    /** D11 : même 404 au POST, rien écrit. */
    @Test
    void r8_post_factureIntrouvable_404_messageOmran_rienEcrit() throws Exception {
        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), UUID.randomUUID());

        assertStatut(r, 404);
        assertThat(detail(r)).isEqualTo(FACTURE_INTROUVABLE);
        assertRienEcrit();
    }

    /** Facture PrjInvoiceDebit d'un projet de la société où le compte n'est pas habilité. */
    private UUID factureInvisible() {
        UUID projet = fixture.projet("PNH-" + x.c().jeton(), x.c().societe());
        UUID f = fixture.piece(FixtureAchat.Piece.facture(x.c().jeton() + "-FNH", AppelsSaisieAchat.DATE, x.c().fournisseur(),
                x.c().fournisseurNom(), x.c().societe(), projet, new BigDecimal("1200")).statut(x.s().enCours()));
        fixture.poser(f, "StrEntityId", x.gestionFacture());
        return f;
    }

    /** D11 : facture hors visibilité → 404 au modèle ; témoin 200. */
    @Test
    void r8_modele_factureInvisible_404_temoin200() throws Exception {
        MvcResult invisible = par(modele(AVOIR, factureInvisible()));
        MvcResult temoin = par(modele(AVOIR, facture));

        assertStatut(temoin, 200);
        assertStatut(invisible, 404);
    }

    /** D11 : facture hors visibilité → 404 au POST, rien écrit. */
    @Test
    void r8_post_factureInvisible_404_rienEcrit() throws Exception {
        assertStatut(creerAvoir(corpsSansLigne(x, "100", "20", true), factureInvisible()), 404);
        assertRienEcrit();
    }

    // ------------------------------------------------------------------ R8 / D10 droit et ordre

    /** D10 : sans le droit de créer un avoir (droit de facture seul) → 403 au modèle ; témoin rédacteur 200. */
    @Test
    void r8_modele_sansDroitAvoir_403_temoin200() throws Exception {
        MvcResult sansDroit = par(compte(NOUVEAU_FACTURE), modele(AVOIR, facture));
        MvcResult temoin = par(modele(AVOIR, facture));

        assertStatut(temoin, 200);
        assertStatut(sansDroit, 403);
    }

    /** D10 : 403 au POST, rien écrit. */
    @Test
    void r8_post_sansDroitAvoir_403_rienEcrit() throws Exception {
        assertStatut(par(compte(NOUVEAU_FACTURE), creer(avecSources(corpsSansLigne(x, "100", "20", true), facture))), 403);
        assertRienEcrit();
    }

    /** D10 : code Projet › Lot de l'avoir : facture à lot (lot dans le corps) → 201 ; facture sans lot → 403. */
    @Test
    void r8_post_codeLot_factureALot201_sansLot403() throws Exception {
        AdmUser compteLot = compte(NOUVEL_AVOIR_LOT);
        UUID lot = fixture.lot("LOT-" + x.c().jeton(), x.c().projet());
        UUID factureALot = facture(fixture, x, "-FAL");
        fixture.poser(factureALot, "PrjMarketId", lot);

        MvcResult sansLot = par(compteLot, creer(avecSources(corpsSansLigne(x, "100", "20", true), facture)));
        MvcResult avecLot = par(compteLot, creer(avecSources(corpsSansLigne(x, "100", "20", true).avec("lotId", lot),
                factureALot)));

        assertStatut(sansLot, 403);
        assertStatut(avecLot, 201);
    }

    /** D11 : 404 avant 403 — facture inconnue pour un compte sans droit → 404 ; témoin facture connue → 403. */
    @Test
    void ordre_404avant403_factureInconnue_sansDroit() throws Exception {
        AdmUser sansDroit = compte();

        MvcResult inconnue = par(sansDroit, creer(avecSources(corpsSansLigne(x, "100", "20", true), UUID.randomUUID())));
        MvcResult temoin = par(sansDroit, creer(avecSources(corpsSansLigne(x, "100", "20", true), facture)));

        assertStatut(temoin, 403);
        assertStatut(inconnue, 404);
    }

    private CorpsJson corpsInvalide() {
        return corpsValide(x.c(), x.s(), AVOIR).avec("avoirFinancier", true)
                .avec("lignes", List.of(ligne("ART-1", "1", "100", x.s().categorie()).avec("description", "")));
    }

    /** D11 : 403 avant 400 — ligne sans désignation pour un compte sans droit → 403 ; témoin rédacteur → 400. */
    @Test
    void ordre_403avant400_ligneSansDesignation_sansDroit() throws Exception {
        MvcResult sansDroit = par(compte(NOUVEAU_FACTURE), creer(avecSources(corpsInvalide(), facture)));
        MvcResult temoin = par(creer(avecSources(corpsInvalide(), facture)));

        assertStatut(temoin, 400);
        assertStatut(sansDroit, 403);
    }

    /** D11 : 400 avant 409 — facture StlInvoiceDebit (409 R7) avec une ligne sans désignation → 400 ; témoin 409. */
    @Test
    void ordre_400avant409_ligneSansDesignation_gestionNonConfiguree() throws Exception {
        UUID stl = factureDeGestion(GESTION_FACTURE_STL);

        MvcResult invalide = creerAvoir(corpsInvalide(), stl);
        MvcResult temoin = creerAvoir(corpsSansLigne(x, "100", "20", true), stl);

        assertStatut(temoin, 409);
        assertStatut(invalide, 400);
        assertRienEcrit();
    }

    /** R8 : REFUND sans {@code sources} reste refusé (400, pas de création libre). */
    @Test
    void r8_post_avoirSansSources_400() throws Exception {
        MvcResult r = par(creer(corpsSansLigne(x, "100", "20", true)));

        assertStatut(r, 400);
        assertThat(detail(r)).startsWith(NATURE_NON_PRISE);
        assertRienEcrit();
    }

    // ------------------------------------------------------------------ R10 / R11 / R12 / R13 écriture

    /** R10 / D9 : sans ligne, HT 250 + TVA 50 → TTC 300 en base. */
    @Test
    void r10_post_sansLigne_montantsSaisis() throws Exception {
        MvcResult r = creerAvoir(corpsSansLigne(x, "250", "50", true), facture);

        assertStatut(r, 201);
        Map<String, Object> d = fixture.pieceEnBase(idCree(r));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(decimalDe(d, "AmountHT")).isEqualByComparingTo("250");
        sa.assertThat(decimalDe(d, "AmountVat")).isEqualByComparingTo("50");
        // 250 + 50 = 300
        sa.assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("300");
        sa.assertAll();
    }

    /** R10 / D9 : avec lignes, montantHT / montantTVA ignorés ; ART-1 10 × 100 à 20 % → 1 000 / 200 / 1 200. */
    @Test
    void r10_post_avecLignes_montantsSaisisIgnores() throws Exception {
        CorpsJson corps = corpsValide(x.c(), x.s(), AVOIR).avec("montantHT", new BigDecimal("5"))
                .avec("montantTVA", BigDecimal.ONE).avec("avoirFinancier", true);

        MvcResult r = creerAvoir(corps, facture);

        assertStatut(r, 201);
        Map<String, Object> d = fixture.pieceEnBase(idCree(r));
        SoftAssertions sa = new SoftAssertions();
        // 10 × 100 = 1 000 HT
        sa.assertThat(decimalDe(d, "AmountHT")).isEqualByComparingTo("1000");
        // 1 000 × 20 % = 200
        sa.assertThat(decimalDe(d, "AmountVat")).isEqualByComparingTo("200");
        // 1 000 + 200 = 1 200
        sa.assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("1200");
        sa.assertAll();
    }

    /** R11 / D3 : avoirFinancier faux écrit dans IsFinancialRefund et rendu par la fiche. */
    @Test
    void r11_post_avoirFinancierFaux_ecritEtRendu() throws Exception {
        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", false), facture);

        assertStatut(r, 201);
        UUID avoir = idCree(r);
        String fiche = corps(par(AppelsEcrituresAchat.fiche(avoir)));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(fixture.pieceEnBase(avoir)).containsEntry(FINANCIER, false);
        sa.assertThat((Boolean) JsonPath.read(fiche, "$.avoirFinancier")).isFalse();
        sa.assertThat(guid(fiche, "$.factureSourceId")).isEqualTo(id(facture));
        sa.assertAll();
    }

    /** R11 / D3 : avoirFinancier vrai écrit et rendu. */
    @Test
    void r11_post_avoirFinancierVrai_ecritEtRendu() throws Exception {
        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), facture);

        assertStatut(r, 201);
        UUID avoir = idCree(r);
        String fiche = corps(par(AppelsEcrituresAchat.fiche(avoir)));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(fixture.pieceEnBase(avoir)).containsEntry(FINANCIER, true);
        sa.assertThat((Boolean) JsonPath.read(fiche, "$.avoirFinancier")).isTrue();
        sa.assertAll();
    }

    /** R12 : exemple prod 24/00011 — HT 8 734,27 + TVA 1 746,85, régul −8 940,32 → net à payer 1 540,80. */
    @Test
    void r12_post_netAPayer_ttcPlusRegul() throws Exception {
        CorpsJson corps = corpsSansLigne(x, "8734.27", "1746.85", true).avec("montantRegul", new BigDecimal("-8940.32"));

        MvcResult r = creerAvoir(corps, facture);

        assertStatut(r, 201);
        Map<String, Object> d = fixture.pieceEnBase(idCree(r));
        SoftAssertions sa = new SoftAssertions();
        // 8 734,27 + 1 746,85 = 10 481,12
        sa.assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("10481.12");
        // 10 481,12 − 8 940,32 + 0 = 1 540,80
        sa.assertThat(decimalDe(d, A_PAYER)).isEqualByComparingTo("1540.80");
        sa.assertAll();
    }

    /** R13 : code yy/nnnnn pris dans « Avoir fournisseur » (900 → 21/00901), séquence facture intacte. */
    @Test
    void r13_post_numeroDeLaSequenceAvoirFournisseur() throws Exception {
        MvcResult r = creerAvoir(corpsSansLigne(x, "100", "20", true), facture);

        assertStatut(r, 201);
        FixtureAchat.SequenceNature sequenceFacture = FixtureAchat.SEQUENCES_SAISIE.get(FACTURE);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(fixture.pieceEnBase(idCree(r))).containsEntry("Code", SEQUENCE_AVOIR.codeSuivant());
        sa.assertThat(fixture.valeurSequence(SEQUENCE_AVOIR.code())).isEqualTo(SEQUENCE_AVOIR.valeur() + 1);
        sa.assertThat(fixture.valeurSequence(sequenceFacture.code())).isEqualTo(sequenceFacture.valeur());
        sa.assertAll();
    }
}
