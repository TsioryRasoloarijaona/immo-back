package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NatureSaisie;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.LigneAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a E-C5 (FEATURE-achat-fiche-creation.md, Vérification n° 2 / 3) : {@code PUT /api/achat/documents/{id}} — lignes
 * synchronisées par id, montants recalculés, verrou « lecture seule » 409, pièce gérée ailleurs 409, ordre des refus, quantités
 * du BC relancées sur les lignes du corps, champs jamais modifiés, Q-7 ({@code null} efface).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatModificationTest {

    private static final String LIGNES = """
            SELECT CAST(Pkey AS varchar(36)) AS Id, Code, Description, Quantity, UnitPriceHT, Sorting
            FROM BuyDocumentLine WHERE BuyDocumentId = :id ORDER BY Sorting ASC""";
    private static final String ID = "Id";
    private static final String NOTES = "Notes";
    private static final String STATUT = "BuyDocumentStatusId";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String TRIGGER = "CfgTierTriggerId";
    private static final String MODIFIEE = "modifiée";
    private static final BigDecimal QUANTITE_BC = BigDecimal.TEN;
    private static final BigDecimal PU = new BigDecimal("100");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AMO");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AMO", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
    }

    private MvcResult modifierPar(AdmUser compte, UUID piece, CorpsJson corps) throws Exception {
        return appeler(mockMvc, modifier(piece, corps), connecte(compte, ROLE_JWT));
    }

    /** Pièce de la nature, gestion de saisie, statut EN COURS (devis : sans statut), une ligne ART-1 1 × 100. */
    private UUID piece(NatureSaisie n) {
        UUID id = fixture.piece(c, "-" + n.name() + (rang++), n.code);
        fixture.poser(id, "StrEntityId", s.gestion(n.gestion));
        if (n != NatureSaisie.DEVIS) {
            fixture.poser(id, STATUT, s.enCours());
        }
        ligneEnBase(id, 1, "ART-1", BigDecimal.ONE);
        return id;
    }

    private UUID ligneEnBase(UUID piece, int ordre, String code, BigDecimal quantite) {
        return fixture.ligne(piece, new LigneAchat(ordre, code, "Article " + code, "U", quantite, PU, TVA_20, ZERO, ZERO,
                new BigDecimal("120")));
    }

    private List<Map<String, Object>> lignes(UUID piece) {
        fixture.oublier();
        return fixture.lire(LIGNES, piece);
    }

    private Map<String, Object> enBase(UUID piece) {
        fixture.oublier();
        return fixture.pieceEnBase(piece);
    }

    private static List<String> ids(List<Map<String, Object>> lignes) {
        return lignes.stream().map(l -> guidDe(l, ID)).toList();
    }

    private void assertInchangee(UUID piece, int nombreDeLignes) {
        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(d).containsEntry(NOTES, null),
                () -> assertThat(lignes(piece)).hasSize(nombreDeLignes));
    }

    // ------------------------------------------------------------------ E-C5.3 lignes synchronisées par id

    /** E-C5.3 : A mise à jour (même PKey), B gardée, C supprimée, D créée ; montants et LinesCount recalculés. */
    @Test
    void ec53_lignesSynchroniseesParId_montantsRecalcules() throws Exception {
        UUID piece = fixture.piece(c, "-SYN", FixtureAchat.FACTURE);
        fixture.poser(piece, "StrEntityId", s.gestion(FixtureAchat.GESTION_FACTURE));
        fixture.poser(piece, STATUT, s.enCours());
        UUID a = ligneEnBase(piece, 1, "ART-A", BigDecimal.ONE);
        UUID b = ligneEnBase(piece, 2, "ART-B", BigDecimal.ONE);
        UUID supprimee = ligneEnBase(piece, 3, "ART-C", BigDecimal.ONE);
        List<CorpsJson> corpsLignes = List.of(
                ligne("ART-A", "5", "100", s.categorie()).avec("id", a),
                ligne("ART-B", "2", "50", s.categorie()).avec("id", b),
                ligne("ART-D", "1", "400", s.categorie()));

        MvcResult r = modifierPar(redacteur, piece, corpsValide(c, s, FixtureAchat.FACTURE).avec("lignes", corpsLignes));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        List<Map<String, Object>> apres = lignes(piece);
        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(ids(apres)).hasSize(3).startsWith(id(a), id(b)).doesNotContain(id(supprimee)),
                () -> assertThat(apres.get(0)).containsEntry("Code", "ART-A"),
                () -> assertThat(decimalDe(apres.get(0), "Quantity")).isEqualByComparingTo("5"),
                () -> assertThat(decimalDe(apres.get(1), "UnitPriceHT")).isEqualByComparingTo("50"),
                () -> assertThat(apres.get(2)).containsEntry("Code", "ART-D"),
                () -> assertThat(decimalDe(d, "LinesCount")).isEqualByComparingTo("3"),
                () -> assertThat(decimalDe(d, "AmountHT")).isEqualByComparingTo("1000"),
                () -> assertThat(decimalDe(d, "AmountVat")).isEqualByComparingTo("200"),
                () -> assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("1200"),
                () -> assertThat(decimalDe(d, "AmountToPay")).isEqualByComparingTo("1200"));
    }

    /** E-C5.3 : un id de ligne qui n'appartient pas à la pièce → 400, rien n'est modifié. */
    @Test
    void ec53_idDeLigneEtranger_400_rienModifie() throws Exception {
        UUID piece = piece(NatureSaisie.BC);
        UUID autre = piece(NatureSaisie.BC);
        UUID etrangere = lignes(autre).stream().map(l -> UUID.fromString(l.get(ID).toString())).findFirst().orElseThrow();

        MvcResult r = modifierPar(redacteur, piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec(NOTES.toLowerCase(), MODIFIEE)
                .avec("lignes", List.of(ligne("ART-1", "3", "100", s.categorie()).avec("id", etrangere))));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertAll(
                () -> assertInchangee(piece, 1),
                () -> assertThat(decimalDe(lignes(autre).get(0), "Quantity")).isEqualByComparingTo("1"));
    }

    // ------------------------------------------------------------------ E-C5.1 lecture seule, ordre des refus

    /** E-C5.1 : BC, BL, facture en statut {@code IsReadOnly} → 409 « Cette pièce est en lecture seule (statut X). », intacte. */
    @ParameterizedTest
    @EnumSource(value = NatureSaisie.class, names = {"BC", "BL", "FACTURE"})
    void ec51_statutLectureSeule_409_pieceIntacte(NatureSaisie n) throws Exception {
        UUID piece = piece(n);
        fixture.poser(piece, STATUT, s.valideDt());

        MvcResult r = modifierPar(redacteur, piece, corpsValide(c, s, n.code).avec(NOTES.toLowerCase(), MODIFIEE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).startsWith(LECTURE_SEULE);
        assertInchangee(piece, 1);
    }

    /** E-C5.1 : statut non {@code IsReadOnly} (VALIDÉ DG) → modifiable. */
    @Test
    void ec51_statutNonLectureSeule_200() throws Exception {
        UUID piece = piece(NatureSaisie.BL);
        fixture.poser(piece, STATUT, s.valideDg());

        MvcResult r = modifierPar(redacteur, piece, corpsValide(c, s, FixtureAchat.BON_LIVRAISON).avec(NOTES.toLowerCase(), MODIFIEE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(enBase(piece)).containsEntry(NOTES, MODIFIEE);
    }

    /** E-C5.1 : un devis (statut NULL) n'a pas de verrou → 200. */
    @Test
    void ec51_devis_sansStatut_200() throws Exception {
        UUID devis = piece(NatureSaisie.DEVIS);

        MvcResult r = modifierPar(redacteur, devis, corpsValide(c, s, FixtureAchat.DEVIS).avec(NOTES.toLowerCase(), MODIFIEE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(enBase(devis)).containsEntry(NOTES, MODIFIEE);
    }

    /** E-C5.1 ordre : 403 (droit) avant 409 lecture seule. */
    @Test
    void ec51_ordre_sansDroit_lectureSeule_403() throws Exception {
        UUID piece = piece(NatureSaisie.BC);
        fixture.poser(piece, STATUT, s.valideDt());
        AdmUser sansDroit = fixture.compteHabilite(c, "AMO-SD", ROLE_JWT);
        fixture.sansPersonnel(sansDroit);

        assertThat(statut(modifierPar(sansDroit, piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE)))).isEqualTo(403);
    }

    /** Ordre au PUT (réponse 1 du manager) : 409 lecture seule avant 400 → lecture seule + corps invalide = 409. */
    @Test
    void ordre_lectureSeule_corpsInvalide_409() throws Exception {
        UUID piece = piece(NatureSaisie.BC);
        fixture.poser(piece, STATUT, s.valideDt());

        MvcResult r = modifierPar(redacteur, piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE)
                .avec("lignes", List.of(ligne("ART-1", "1", "100", s.categorie()).avec("description", ""))));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).startsWith(LECTURE_SEULE);
    }

    /** Ordre au PUT : 409 gérée ailleurs avant 403 (compte sans droit). */
    @Test
    void ordre_gereeAilleurs_sansDroit_409() throws Exception {
        UUID facture = piece(NatureSaisie.FACTURE);
        fixture.decompte(facture);
        AdmUser sansDroit = fixture.compteHabilite(c, "AMO-SD", ROLE_JWT);
        fixture.sansPersonnel(sansDroit);

        MvcResult r = modifierPar(sansDroit, facture, corpsValide(c, s, FixtureAchat.FACTURE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(GEREE_AILLEURS);
    }

    /** Ordre au PUT : 400 avant les autres 409 → quantité au-delà du BC + ligne sans désignation = 400. */
    @Test
    void ordre_corpsInvalide_quantiteDepasseLeBc_400() throws Exception {
        UUID facture = factureAffectee();

        MvcResult r = modifierPar(redacteur, facture, corpsValide(c, s, FixtureAchat.FACTURE).avec("lignes", List.of(
                ligne("ART-1", "12", "100", s.categorie()), ligne("ART-1", "1", "100", s.categorie()).avec("description", ""))));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
    }

    // ------------------------------------------------------------------ E-C1.5 gérée ailleurs → 409

    /** E-C1.5 : facture de décompte, frais de terrain, facture de commission → 409 {@code GEREE_AILLEURS}, même administrateur. */
    @ParameterizedTest
    @EnumSource(Ailleurs.class)
    void ec15_pieceGereeAilleurs_409_memeAdministrateur(Ailleurs cas) throws Exception {
        AdmUser admin = fixture.compteHabilite(c, "AMO-ADM", ADMINISTRATEUR);
        fixture.sansPersonnel(admin);
        UUID facture = piece(NatureSaisie.FACTURE);
        rendreGereeAilleurs(cas, facture);

        MvcResult r = modifierPar(admin, facture, corpsValide(c, s, FixtureAchat.FACTURE).avec(NOTES.toLowerCase(), MODIFIEE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(GEREE_AILLEURS);
        assertInchangee(facture, 1);
    }

    /** E-C1.5 : un document à régler (avance) n'est pas modifiable depuis sa fiche → 409. */
    @Test
    void ec15_documentARegler_409() throws Exception {
        AdmUser admin = fixture.compteHabilite(c, "AMO-ADM", ADMINISTRATEUR);
        fixture.sansPersonnel(admin);
        UUID avance = fixture.piece(c, "-DAR", FixtureAchat.DOCUMENT_A_REGLER);

        MvcResult r = modifierPar(admin, avance, corpsValide(c, s, FixtureAchat.FACTURE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(GEREE_AILLEURS);
    }

    enum Ailleurs { DECOMPTE, FRAIS_TERRAIN, COMMISSION }

    private void rendreGereeAilleurs(Ailleurs cas, UUID facture) {
        switch (cas) {
            case DECOMPTE -> fixture.decompte(facture);
            case FRAIS_TERRAIN -> fixture.poser(facture, "ReferenceType", "LAND_EXPENSE");
            case COMMISSION -> fixture.poser(facture, "StrEntityId", fixture.gestionVide("ComInvoiceDebitCommission",
                    UUID.fromString("5A0E7A51-2B2A-4C0D-9E11-0C0441551001")));
        }
    }

    // ------------------------------------------------------------------ E-C5.4 quantités du BC

    /** Facture EN COURS affectée à un BC (ART-1, PU 100, 10 commandés), contrôle des quantités actif. */
    private UUID factureAffectee() {
        UUID bc = fixture.piece(c, "-BCQ", FixtureAchat.BON_COMMANDE);
        ligneEnBase(bc, 1, "ART-1", QUANTITE_BC);
        UUID facture = piece(NatureSaisie.FACTURE);
        fixture.poser(facture, BUY_ORDER_ID, bc);
        fixture.parametreBooleen(FixtureAchat.LIMITE_QUANTITES, true);
        return facture;
    }

    @Test
    void ec54_factureAffectee_quantiteDuCorpsDepasseLeBc_409() throws Exception {
        UUID facture = factureAffectee();

        MvcResult r = modifierPar(redacteur, facture, corpsValide(c, s, FixtureAchat.FACTURE)
                .avec("lignes", List.of(ligne("ART-1", "12", "100", s.categorie()))));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("La quantité de l'article 'ART-1' dépasse celle définie dans le bon de commande !");
        assertThat(decimalDe(lignes(facture).get(0), "Quantity")).isEqualByComparingTo("1");
    }

    @Test
    void ec54_factureAffectee_articleDuCorpsAbsentDuBc_409() throws Exception {
        UUID facture = factureAffectee();

        MvcResult r = modifierPar(redacteur, facture, corpsValide(c, s, FixtureAchat.FACTURE)
                .avec("lignes", List.of(ligne("ART-9", "1", "100", s.categorie()))));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("L'article 'ART-9' n'existe pas dans le bon de commande !");
    }

    @Test
    void ec54_factureAffectee_quantiteEgaleAuBc_200() throws Exception {
        UUID facture = factureAffectee();

        MvcResult r = modifierPar(redacteur, facture, corpsValide(c, s, FixtureAchat.FACTURE)
                .avec("lignes", List.of(ligne("ART-1", "10", "100", s.categorie()))));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(decimalDe(lignes(facture).get(0), "Quantity")).isEqualByComparingTo("10");
    }

    // ------------------------------------------------------------------ E-C5.2 champs jamais modifiés, Q-7

    /** E-C5.2 : Code, DocType, gestion, statut, lien BC, IsAccounted, CanBePayed inchangés ; la nature du corps est ignorée. */
    @Test
    void ec52_champsJamaisModifies_natureDuCorpsIgnoree() throws Exception {
        fixture.parametreBooleen(FixtureAchat.LIMITE_QUANTITES, false);
        UUID bc = fixture.piece(c, "-BC52", FixtureAchat.BON_COMMANDE);
        UUID facture = piece(NatureSaisie.FACTURE);
        fixture.poser(facture, BUY_ORDER_ID, bc);
        fixture.poser(facture, "IsAccounted", true);
        fixture.poser(facture, "CanBePayed", false);
        Map<String, Object> avant = enBase(facture);

        MvcResult r = modifierPar(redacteur, facture, corpsValide(c, s, FixtureAchat.DEVIS).avec(NOTES.toLowerCase(), MODIFIEE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        Map<String, Object> d = enBase(facture);
        assertAll(
                () -> assertThat(d).containsEntry(NOTES, MODIFIEE).containsEntry("Code", avant.get("Code"))
                        .containsEntry("DocType", FixtureAchat.FACTURE).containsEntry("IsAccounted", true)
                        .containsEntry("CanBePayed", false),
                () -> assertThat(guidDe(d, "StrEntityId")).isEqualTo(id(s.gestion(FixtureAchat.GESTION_FACTURE))),
                () -> assertThat(guidDe(d, STATUT)).isEqualTo(id(s.enCours())),
                () -> assertThat(guidDe(d, BUY_ORDER_ID)).isEqualTo(id(bc)));
    }

    /** Q-7 : {@code null} efface — textes → '', lot et déclencheur → NULL. */
    @Test
    void q7_nullEfface_textesVides_lotEtDeclencheurNuls() throws Exception {
        UUID lot = fixture.lot("L-" + c.jeton(), c.projet());
        UUID piece = piece(NatureSaisie.BC);
        fixture.poser(piece, "PrjMarketId", lot);
        fixture.poser(piece, TRIGGER, c.fournisseur());
        fixture.poser(piece, NOTES, "ancienne note");
        CorpsJson corps = corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec(NOTES.toLowerCase(), null)
                .avec("referenceInterne", null).avec("lotId", null).avec("declencheurId", null);

        MvcResult r = modifierPar(redacteur, piece, corps);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        Map<String, Object> d = enBase(piece);
        assertThat(d).containsEntry(NOTES, "").containsEntry("DocRef", "").containsEntry("PrjMarketId", null)
                .containsEntry(TRIGGER, null);
    }

    /** Réponse 7 du manager : la fiche expose {@code societeId} et {@code moduleId} (le front rebâtit le corps du PUT). */
    @Test
    void fiche_societeIdEtModuleId() throws Exception {
        UUID piece = piece(NatureSaisie.BC);
        fixture.poser(piece, "AdmModuleId", s.moduleProjet());

        MvcResult r = appeler(mockMvc, fiche(piece), connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        String json = corps(r);
        assertAll(
                () -> assertThat((String) JsonPath.read(json, "$.societeId")).isEqualToIgnoringCase(c.societe().toString()),
                () -> assertThat((String) JsonPath.read(json, "$.moduleId")).isEqualToIgnoringCase(s.moduleProjet().toString()));
    }

    /** E-C5.3 : {@code UserUpdate*} posés, {@code UserCreate} inchangé. */
    @Test
    void ec53_userUpdatePose_userCreateInchange() throws Exception {
        UUID piece = piece(NatureSaisie.BC);

        MvcResult r = modifierPar(redacteur, piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(d).containsEntry("UserCreate", "TEST-ACHAT"),
                () -> assertThat(d.get("UserUpdate")).isNotNull(),
                () -> assertThat(d.get("UserUpdateDateTime")).isNotNull());
    }

    /** E-C3.3 : en modification, la ventilation catégorie est recalculée sur les lignes du corps. */
    @Test
    void ec33_modification_ventilationCategorieRecalculee() throws Exception {
        UUID piece = piece(NatureSaisie.BC);
        UUID ancienne = fixture.categorie("CAT2-" + c.jeton());
        fixture.ventilation(piece, ancienne, BigDecimal.ONE);

        MvcResult r = modifierPar(redacteur, piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        fixture.oublier();
        assertAll(
                () -> assertThat(fixture.nombre("BuyVentilationCategory", "StlCategoryId", ancienne)).isZero(),
                () -> assertThat(fixture.lire("SELECT VentilationRate FROM BuyVentilationCategory WHERE BuyDocumentId = :id",
                        piece)).singleElement().satisfies(l -> assertThat(decimalDe(l, "VentilationRate"))
                        .isEqualByComparingTo("1")));
    }
}
