package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.guidDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.idCree;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.ligne;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.modifier;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2b G4, G5 et lien bordereau (FEATURE-achat-generation.md §3, §4, §8 « G4/G5 ») : {@code GET …/modele} avec
 * {@code sources} — en-tête de la 1ʳᵉ source, sommes, fusion de lignes (clé bordereau, catégorie, PU, remise, FODEC, TVA, code,
 * désignation), quantités restantes BC → BL ; {@code ligneBordereauId} contrôlé et recopié au POST, jamais modifié au PUT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatGenerationPreremplissageTest {

    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String QUANTITE = "quantite";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;
    private UUID lot;
    private UUID bordereauX;
    private UUID bordereauY;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AGP");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AGP", ROLE_JWT, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        generation.parametres(false, false, false);
        generation.tableBuyOrderInvoice();
        lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        bordereauX = generation.ligneBordereau(lot, "X");
        bordereauY = generation.ligneBordereau(lot, "Y");
    }

    private UUID bc() {
        return generation.source(c, "-BC" + (rang++), BON_COMMANDE, s.gestion(FixtureAchat.GESTION_BC), s.valideDg());
    }

    /** BL validé, sans ligne. */
    private UUID bl(UUID bc) {
        UUID bl = generation.source(c, "-BL" + (rang++), BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.valideDg());
        fixture.poser(bl, BUY_ORDER_ID, bc);
        return bl;
    }

    private LigneSource ligneSource(int ordre, String code, String quantite) {
        return LigneSource.simple(ordre, code, quantite, s.categorie());
    }

    private MvcResult par(MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(redacteur, ROLE_JWT));
    }

    private String modeleOk(String nature, UUID... sources) throws Exception {
        MvcResult r = par(modele(nature, sources));
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        return corps(r);
    }

    private static String guid(String json, String chemin) {
        Object v = JsonPath.read(json, chemin);
        return v == null ? null : v.toString().toUpperCase();
    }

    private static BigDecimal decimalDe(String json, String chemin) {
        return decimal(JsonPath.read(json, chemin));
    }

    // ------------------------------------------------------------------ G4 en-tête

    /** Deux BL du même BC aux en-têtes distincts : la facture prend l'en-tête de la 1ʳᵉ source (ordre de {@code sources}). */
    private UUID[] deuxBlAuxEntetesDistincts() {
        UUID bc = bc();
        UUID premier = bl(bc);
        UUID second = bl(bc);
        UUID declencheur = fixture.compte("AGP-EMP-" + c.jeton(), ROLE_JWT).getCfgEmployee().getId();
        UUID autreType = fixture.typeLot("TM2-" + c.jeton(), "Autre " + c.jeton());
        fixture.poser(premier, "PrjMarketId", lot);
        fixture.poser(premier, "Notes", "N1-premier");
        fixture.poser(premier, "Notes2", "N2-premier");
        fixture.poser(premier, "AdmModuleId", s.moduleProjet());
        fixture.poser(premier, "PersonToContact", "M. Premier");
        fixture.poser(premier, "DocRef", "RI-premier");
        fixture.poser(premier, "OfficeOrderRef", "BO-premier");
        fixture.poser(premier, "CfgTierTriggerId", declencheur);
        fixture.poser(premier, "PrjMarketTypeId", autreType);
        fixture.poser(premier, "AmountRegul", new BigDecimal("10"));
        fixture.poser(premier, "FiscalStamp", new BigDecimal("2"));
        fixture.poser(second, "Notes", "N1-second");
        fixture.poser(second, "Notes2", "N2-second");
        fixture.poser(second, "AdmModuleId", s.moduleAchat());
        fixture.poser(second, "PersonToContact", "M. Second");
        fixture.poser(second, "DocRef", "RI-second");
        fixture.poser(second, "OfficeOrderRef", "BO-second");
        fixture.poser(second, "PrjMarketTypeId", s.typeMarche());
        fixture.poser(second, "AmountRegul", new BigDecimal("5"));
        fixture.poser(second, "FiscalStamp", new BigDecimal("3"));
        generation.ligne(premier, ligneSource(1, "A", "1"));
        generation.ligne(second, ligneSource(1, "B", "1"));
        return new UUID[]{premier, second, declencheur, autreType};
    }

    @Test
    void g4_entete_deLaPremiereSource() throws Exception {
        UUID[] d = deuxBlAuxEntetesDistincts();

        String json = modeleOk(FACTURE, d[0], d[1]);

        assertAll(
                () -> assertThat(guid(json, "$.societeId")).isEqualTo(id(c.societe())),
                () -> assertThat(guid(json, "$.projetId")).isEqualTo(id(c.projet())),
                () -> assertThat(guid(json, "$.lotId")).isEqualTo(id(lot)),
                () -> assertThat(guid(json, "$.fournisseurId")).isEqualTo(id(c.fournisseur())),
                () -> assertThat(guid(json, "$.moduleId")).isEqualTo(id(s.moduleProjet())),
                () -> assertThat(guid(json, "$.declencheurId")).isEqualTo(id(d[2])),
                () -> assertThat(guid(json, "$.typeMarcheId")).isEqualTo(id(d[3])),
                () -> assertThat((String) JsonPath.read(json, "$.notes")).isEqualTo("N1-premier"),
                () -> assertThat((String) JsonPath.read(json, "$.notes2")).isEqualTo("N2-premier"),
                () -> assertThat((String) JsonPath.read(json, "$.personneAContacter")).isEqualTo("M. Premier"),
                () -> assertThat((String) JsonPath.read(json, "$.referenceInterne")).isEqualTo("RI-premier"),
                () -> assertThat((String) JsonPath.read(json, "$.referenceBureauOrdre")).isEqualTo("BO-premier"),
                () -> assertThat((String) JsonPath.read(json, "$.nature")).isEqualTo(FACTURE));
    }

    /** G4 : {@code montantRegul} et {@code timbreFiscal} = sommes des sources. */
    @Test
    void g4_montantRegul_timbreFiscal_sommesDesSources() throws Exception {
        UUID[] d = deuxBlAuxEntetesDistincts();

        String json = modeleOk(FACTURE, d[0], d[1]);

        assertAll(
                () -> assertThat(decimalDe(json, "$.montantRegul")).isEqualByComparingTo("15"),
                () -> assertThat(decimalDe(json, "$.timbreFiscal")).isEqualByComparingTo("5"));
    }

    /** §3 : {@code sources} du modèle renseigné, dans l'ordre demandé. */
    @Test
    void g4_sourcesRenseignees_dansLOrdre() throws Exception {
        UUID[] d = deuxBlAuxEntetesDistincts();

        String json = modeleOk(FACTURE, d[1], d[0]);

        List<String> sources = JsonPath.read(json, "$.sources");
        assertThat(sources).isNotNull().extracting(String::toUpperCase).containsExactly(id(d[1]), id(d[0]));
    }

    /** G4 : ventilation projet de la 1ʳᵉ source. */
    @Test
    void g4_ventilationProjets_deLaPremiereSource() throws Exception {
        UUID bc = bc();
        UUID premier = bl(bc);
        UUID second = bl(bc);
        UUID autreProjet = fixture.projet("P2-" + c.jeton(), c.societe());
        fixture.habiliter(redacteur, autreProjet);
        fixture.ventilationProjet(premier, c.projet(), new BigDecimal("0.4"));
        fixture.ventilationProjet(premier, autreProjet, new BigDecimal("0.6"));
        fixture.ventilationProjet(second, autreProjet, BigDecimal.ONE);
        generation.ligne(premier, ligneSource(1, "A", "1"));
        generation.ligne(second, ligneSource(1, "A", "1"));

        String json = modeleOk(FACTURE, premier, second);

        List<String> cles = JsonPath.read(json, "$.ventilationProjets[*].cleId");
        assertAll(
                () -> assertThat(cles).extracting(String::toUpperCase)
                        .containsExactlyInAnyOrder(id(c.projet()), id(autreProjet)),
                () -> assertThat(decimal(JsonPath.<List<Object>>read(json,
                        "$.ventilationProjets[?(@.cleId == '" + autreProjet + "')].taux").get(0)))
                        .isEqualByComparingTo("0.6"));
    }

    // ------------------------------------------------------------------ G4 fusion des lignes

    /**
     * BL1 : A/X 2 (unité M2, rang 1), N sans bordereau 1, Z 0, NEG −1. BL2 : A/X 3 (unité U, rang 7), A/Y 4, N 2 — même
     * catégorie, PU, remise, FODEC, TVA, désignation partout.
     */
    private String modeleFusion() throws Exception {
        UUID bc = bc();
        UUID bl1 = bl(bc);
        UUID bl2 = bl(bc);
        generation.ligne(bl1, ligneSource(1, "A", "2").bordereau(bordereauX).unite("M2"));
        generation.ligne(bl1, ligneSource(2, "N", "1"));
        generation.ligne(bl1, ligneSource(3, "Z", "0"));
        generation.ligne(bl1, ligneSource(4, "NEG", "-1"));
        generation.ligne(bl2, ligneSource(7, "A", "3").bordereau(bordereauX));
        generation.ligne(bl2, ligneSource(8, "A", "4").bordereau(bordereauY));
        generation.ligne(bl2, ligneSource(9, "N", "2"));
        return modeleOk(FACTURE, bl1, bl2);
    }

    /** G4 : même clé (bordereau X) → une ligne, quantités sommées, unité de la 1ʳᵉ occurrence. */
    @Test
    void g4_fusion_memeCle_quantitesSommees_uniteDeLaPremiere() throws Exception {
        String json = modeleFusion();

        assertAll(
                () -> assertThat(champDesLignesDuBordereau(json, bordereauX, QUANTITE)).singleElement()
                        .satisfies(q -> assertThat(decimal(q)).isEqualByComparingTo("5")),
                () -> assertThat(champDesLignesDuBordereau(json, bordereauX, "unite")).containsExactly("M2"));
    }

    /** G4 (décision Q-C) : bordereaux différents jamais fusionnés. */
    @Test
    void g4_fusion_bordereauxDifferents_nonFusionnes() throws Exception {
        String json = modeleFusion();

        assertThat(champDesLignesDuBordereau(json, bordereauY, QUANTITE)).singleElement()
                .satisfies(q -> assertThat(decimal(q)).isEqualByComparingTo("4"));
    }

    /** G4 (décision Q-C) : lignes sans bordereau fusionnées entre elles sur la même clé (null = null). */
    @Test
    void g4_fusion_lignesSansBordereau_fusionnentEntreElles() throws Exception {
        String json = modeleFusion();

        assertAll(
                () -> assertThat(champDesLignesDeCode(json, "N", QUANTITE)).singleElement()
                        .satisfies(q -> assertThat(decimal(q)).isEqualByComparingTo("3")),
                () -> assertThat(champDesLignesDeCode(json, "N", LIGNE_BORDEREAU)).containsExactly((Object) null));
    }

    /** G4 : ligne à quantité 0 omise ; quantité négative conservée ; 4 lignes en tout (A/X, A/Y, N, NEG). */
    @Test
    void g4_ligneQuantiteZeroOmise_negativeConservee() throws Exception {
        String json = modeleFusion();

        assertAll(
                () -> assertThat(champDesLignesDeCode(json, "Z", QUANTITE)).isEmpty(),
                () -> assertThat(champDesLignesDeCode(json, "NEG", QUANTITE)).singleElement()
                        .satisfies(q -> assertThat(decimal(q)).isEqualByComparingTo("-1")),
                () -> assertThat(JsonPath.<List<Object>>read(json, "$.lignes")).hasSize(4));
    }

    /** G4 : PU différent → clé différente, pas de fusion. */
    @Test
    void g4_fusion_prixDifferent_nonFusionne() throws Exception {
        UUID bc = bc();
        UUID bl1 = bl(bc);
        UUID bl2 = bl(bc);
        generation.ligne(bl1, ligneSource(1, "P", "1"));
        generation.ligne(bl2, new LigneSource(1, "P", "Article P", "U", "2", "150", "0", "0", "0.2", s.categorie(), null));

        String json = modeleOk(FACTURE, bl1, bl2);

        assertThat(champDesLignesDeCode(json, "P", QUANTITE)).hasSize(2);
    }

    // ------------------------------------------------------------------ G5 quantités restantes BC → BL

    /** BC : A 10, B 5, C 3 ; BL déjà livrés : A 4 + 1, C 3. */
    private UUID bcPartiellementLivre() {
        UUID bc = bc();
        generation.ligne(bc, ligneSource(1, "A", "10"));
        generation.ligne(bc, ligneSource(2, "B", "5"));
        generation.ligne(bc, ligneSource(3, "C", "3"));
        UUID livre1 = bl(bc);
        generation.ligne(livre1, ligneSource(1, "A", "4"));
        generation.ligne(livre1, ligneSource(2, "C", "3"));
        UUID livre2 = bl(bc);
        generation.ligne(livre2, ligneSource(1, "A", "1"));
        return bc;
    }

    /** G5 / Q-9 : quantité = commandée − Σ déjà livrée par Code ; ligne soldée (C) omise. */
    @Test
    void g5_bl_quantitesRestantes_commandeMoinsLivre_ligneSoldeeOmise() throws Exception {
        String json = modeleOk(BON_LIVRAISON, bcPartiellementLivre());

        assertAll(
                () -> assertThat(champDesLignesDeCode(json, "A", QUANTITE)).singleElement()
                        .satisfies(q -> assertThat(decimal(q)).isEqualByComparingTo("5")),
                () -> assertThat(champDesLignesDeCode(json, "B", QUANTITE)).singleElement()
                        .satisfies(q -> assertThat(decimal(q)).isEqualByComparingTo("5")),
                () -> assertThat(champDesLignesDeCode(json, "C", QUANTITE)).isEmpty());
    }

    /** G5 (décision Q-B) : tout livré → 409, message exact. */
    @Test
    void g5_bl_toutLivre_409_message() throws Exception {
        UUID bc = bc();
        generation.ligne(bc, ligneSource(1, "A", "2"));
        UUID livre = bl(bc);
        generation.ligne(livre, ligneSource(1, "A", "2"));

        MvcResult r = par(modele(BON_LIVRAISON, bc));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(TOUT_LIVRE);
    }

    /** Réponse 5 : le 409 « tout livré » vaut aussi au POST (G1 : le POST recontrôle), rien écrit. */
    @Test
    void r5_bl_toutLivre_409_auPost_rienEcrit() throws Exception {
        UUID bc = bc();
        generation.ligne(bc, ligneSource(1, "ART-1", "2"));
        UUID livre = bl(bc);
        generation.ligne(livre, ligneSource(1, "ART-1", "2"));

        MvcResult r = par(creer(avecSources(corpsValide(c, s, BON_LIVRAISON), bc)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(TOUT_LIVRE);
        fixture.oublier();
        FixtureAchat.SequenceNature seq = FixtureAchat.SEQUENCES_SAISIE.get(BON_LIVRAISON);
        assertThat(fixture.valeurSequence(seq.code())).isEqualTo(seq.valeur());
    }

    /** Réponse 9 : tout BL du BC compte comme livré, quel que soit son statut (BL en cours, non validé, compté). */
    @Test
    void r9_bl_blNonValideCompteCommeLivre() throws Exception {
        UUID bc = bc();
        generation.ligne(bc, ligneSource(1, "A", "10"));
        UUID enCours = generation.source(c, "-BLE" + (rang++), BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.enCours());
        fixture.poser(enCours, BUY_ORDER_ID, bc);
        generation.ligne(enCours, ligneSource(1, "A", "4"));

        String json = modeleOk(BON_LIVRAISON, bc);

        assertThat(champDesLignesDeCode(json, "A", QUANTITE)).singleElement()
                .satisfies(q -> assertThat(decimal(q)).isEqualByComparingTo("6"));
    }

    // ------------------------------------------------------------------ réponses 1 et 2 : date et devise

    /** Réponse 1 : la pièce générée est datée du jour, pas de la date de la source. */
    @Test
    void r1_modele_dateDuJour() throws Exception {
        UUID bc = bc();
        fixture.poser(bc, "DocDate", LocalDate.of(2025, 1, 15));
        generation.ligne(bc, ligneSource(1, "A", "10"));

        String json = modeleOk(BON_LIVRAISON, bc);

        assertThat((String) JsonPath.read(json, "$.dateDocument")).isEqualTo(LocalDate.now().toString());
    }

    /** Réponse 2 : devise de la 1ʳᵉ source au modèle, gardée par le POST avec {@code sources}. */
    @Test
    void r2_devise_deLaPremiereSource_modeleEtPost() throws Exception {
        UUID devise = fixture.devise("DSRC-" + c.jeton());
        UUID bc = bc();
        fixture.poser(bc, "StlCurrencyId", devise);
        generation.ligne(bc, ligneSource(1, "ART-1", "10"));

        String json = modeleOk(BON_LIVRAISON, bc);
        MvcResult r = par(creer(avecSources(corpsValide(c, s, BON_LIVRAISON), bc)));

        assertThat(guid(json, "$.deviseId")).isEqualTo(id(devise));
        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertThat(guidDe(fixture.pieceEnBase(idCree(r)), "StlCurrencyId")).isEqualTo(id(devise));
    }

    // ------------------------------------------------------------------ lien bordereau (§3)

    /** §3 : le modèle renseigne {@code ligneBordereauId} = PrjMarketLineId de la ligne source. */
    @Test
    void bordereau_modele_ligneBordereauIdRenseigne() throws Exception {
        UUID bc = bc();
        generation.ligne(bc, ligneSource(1, "A", "10").bordereau(bordereauX));

        String json = modeleOk(BON_LIVRAISON, bc);

        assertThat(champDesLignesDeCode(json, "A", LIGNE_BORDEREAU)).singleElement()
                .satisfies(b -> assertThat(b.toString().toUpperCase()).isEqualTo(id(bordereauX)));
    }

    private UUID bcAuBordereauX() {
        UUID bc = bc();
        generation.ligne(bc, ligneSource(1, "ART-1", "10").bordereau(bordereauX));
        return bc;
    }

    private CorpsJson corpsBl(UUID bordereau) {
        return corpsValide(c, s, BON_LIVRAISON)
                .avec("lignes", List.of(ligne("ART-1", "10", "100", s.categorie()).avec(LIGNE_BORDEREAU, bordereau)));
    }

    /** §3 : POST, {@code ligneBordereauId} parmi les lignes des sources → recopié dans PrjMarketLineId. */
    @Test
    void bordereau_post_parmiLesSources_recopie() throws Exception {
        UUID bc = bcAuBordereauX();

        MvcResult r = par(creer(avecSources(corpsBl(bordereauX), bc)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertThat(generation.bordereauxDesLignes(idCree(r))).containsExactly(id(bordereauX));
    }

    /** §3 : POST, {@code ligneBordereauId} étranger aux lignes des sources → 400 nommant le champ, rien écrit. */
    @Test
    void bordereau_post_etrangerAuxSources_400_champNomme_rienEcrit() throws Exception {
        UUID bc = bcAuBordereauX();

        MvcResult r = par(creer(avecSources(corpsBl(bordereauY), bc)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertThat(detail(r)).contains(LIGNE_BORDEREAU);
        fixture.oublier();
        assertThat(fixture.valeurSequence(FixtureAchat.SEQUENCES_SAISIE.get(BON_LIVRAISON).code()))
                .isEqualTo(FixtureAchat.SEQUENCES_SAISIE.get(BON_LIVRAISON).valeur());
    }

    /** §3 : création libre (aucune source) avec {@code ligneBordereauId} → 400 (il ne figure parmi aucune source). */
    @Test
    void bordereau_post_creationLibre_ligneBordereauId_400() throws Exception {
        MvcResult r = par(creer(corpsBl(bordereauX)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertThat(detail(r)).contains(LIGNE_BORDEREAU);
    }

    private UUID blGenereAuBordereauX() throws Exception {
        MvcResult r = par(creer(avecSources(corpsBl(bordereauX), bcAuBordereauX())));
        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        return idCree(r);
    }

    private CorpsJson corpsPut(UUID ligneExistante, UUID bordereau) {
        return corpsValide(c, s, BON_LIVRAISON).avec("lignes", List.of(ligne("ART-1", "10", "100", s.categorie())
                .avec("id", ligneExistante).avec(LIGNE_BORDEREAU, bordereau)));
    }

    /** §3 : le PUT ne modifie jamais le lien — {@code ligneBordereauId} null au PUT, la ligne garde X. */
    @Test
    void bordereau_put_null_ligneGardeSonLien() throws Exception {
        UUID bl = blGenereAuBordereauX();
        UUID ligneExistante = generation.lignesDe(bl).get(0);

        MvcResult r = par(modifier(bl, corpsPut(ligneExistante, null)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(generation.bordereauxDesLignes(bl)).containsExactly(id(bordereauX));
    }

    /** §3 : le PUT ne modifie jamais le lien — un autre bordereau au PUT n'est pas écrit. */
    @Test
    void bordereau_put_autreBordereau_ligneGardeSonLien() throws Exception {
        UUID bl = blGenereAuBordereauX();
        UUID ligneExistante = generation.lignesDe(bl).get(0);

        par(modifier(bl, corpsPut(ligneExistante, bordereauY)));

        fixture.oublier();
        assertThat(generation.bordereauxDesLignes(bl)).containsExactly(id(bordereauX));
    }

    /** Réponse 6 : une ligne ajoutée par PUT n'a jamais de {@code ligneBordereauId}, même si le corps en porte un. */
    @Test
    void r6_put_ligneAjoutee_sansLienBordereau() throws Exception {
        UUID bl = blGenereAuBordereauX();
        UUID ligneExistante = generation.lignesDe(bl).get(0);
        CorpsJson ajout = corpsValide(c, s, BON_LIVRAISON).avec("lignes", List.of(
                ligne("ART-1", "10", "100", s.categorie()).avec("id", ligneExistante),
                ligne("ART-2", "1", "100", s.categorie()).avec(LIGNE_BORDEREAU, bordereauX)));

        MvcResult r = par(modifier(bl, ajout));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        fixture.oublier();
        assertThat(generation.bordereauxDesLignes(bl)).containsExactlyInAnyOrder(id(bordereauX), null);
    }
}
