package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.stream.Collectors;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a E-C3 (FEATURE-achat-fiche-creation.md, Vérification n° 2 et 4) : ventilation catégorie calculée (lignes, à défaut
 * catégorie d'en-tête, à défaut catégorie du lot, sinon 400 OMRAN) ; ventilation projet obligatoire sans projet (Σ = 100 %),
 * non contrôlée avec projet (Q-2) ; projets ventilés habilités ; remplacement des deux ventilations au {@code PUT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatVentilationsTest {

    private static final String CATEGORIES = "SELECT * FROM BuyVentilationCategory WHERE BuyDocumentId = :id";
    private static final String PROJETS = "SELECT * FROM BuyVentilationExpense WHERE BuyDocumentId = :id";
    private static final String TAUX = "VentilationRate";
    private static final String CATEGORIE = "StlCategoryId";
    private static final String PROJET = "PrjProjectId";
    private static final String LIGNES_CHAMP = "lignes";
    private static final String CATEGORIE_CHAMP = "categorieId";
    private static final String PROJET_CHAMP = "projetId";
    private static final String VENTILATION_CHAMP = "ventilationProjets";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;
    private UUID projet2;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AVT");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AVT", ROLE_JWT, NOUVEAU_BC, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        projet2 = fixture.projet("P2-" + c.jeton(), c.societe());
        fixture.habiliter(redacteur, projet2);
    }

    private MvcResult creerPar(CorpsJson corps) throws Exception {
        return appeler(mockMvc, creer(corps), connecte(redacteur, ROLE_JWT));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        MvcResult r = creerPar(corps);
        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        return idCree(r);
    }

    private CorpsJson facture() {
        return corpsValide(c, s, FixtureAchat.FACTURE);
    }

    /** Facture sans projet d'en-tête, ventilée sur les parts données. */
    private CorpsJson factureSansProjet(CorpsJson... parts) {
        return facture().avec(PROJET_CHAMP, null).avec(VENTILATION_CHAMP, List.of(parts));
    }

    /** Taux par clé (catégorie ou projet), en majuscules. */
    private Map<String, BigDecimal> taux(String sql, String cle, UUID piece) {
        return fixture.lire(sql, piece).stream()
                .collect(Collectors.toMap(l -> guidDe(l, cle), l -> decimalDe(l, TAUX)));
    }

    private static BigDecimal somme(Map<String, BigDecimal> parts) {
        return parts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertRefus(MvcResult r, int code, String texte) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(code);
        assertThat(detail(r)).isEqualTo(texte);
        fixture.oublier();
        assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero();
    }

    // ------------------------------------------------------------------ E-C3.1 catégories

    /** E-C3.1 : part = Σ TTC de la catégorie / TTC (300 / 1 200 et 900 / 1 200). */
    @Test
    void ec31_categories_calculeesParLignes_auProrataDuTTC() throws Exception {
        UUID cat2 = fixture.categorie("CAT2-" + c.jeton());
        UUID piece = creerOk(facture().avec(LIGNES_CHAMP, List.of(ligne("A", "1", "250", s.categorie()),
                ligne("B", "1", "750", cat2))));

        Map<String, BigDecimal> parts = taux(CATEGORIES, CATEGORIE, piece);
        assertAll(
                () -> assertThat(parts).hasSize(2),
                () -> assertThat(parts.get(id(s.categorie()))).isEqualByComparingTo("0.25"),
                () -> assertThat(parts.get(id(cat2))).isEqualByComparingTo("0.75"));
    }

    /** E-C3.1 : lignes de même catégorie regroupées en une part ; la catégorie d'en-tête est ignorée s'il y a des lignes. */
    @Test
    void ec31_categories_memeCategorie_unePart_enTeteIgnoree() throws Exception {
        UUID enTete = fixture.categorie("CATE-" + c.jeton());
        UUID piece = creerOk(facture().avec(CATEGORIE_CHAMP, enTete).avec(LIGNES_CHAMP,
                List.of(ligne("A", "1", "250", s.categorie()), ligne("B", "2", "10", s.categorie()))));

        Map<String, BigDecimal> parts = taux(CATEGORIES, CATEGORIE, piece);
        assertThat(parts).hasSize(1).containsOnlyKeys(id(s.categorie()));
        assertThat(parts.get(id(s.categorie()))).isEqualByComparingTo("1");
    }

    /** E-C3.1 / E-C3.3 : la dernière part complète Σ = 1 exactement (trois tiers). */
    @Test
    void ec33_categories_derniereComplete_sommeExacte() throws Exception {
        UUID cat2 = fixture.categorie("CAT2-" + c.jeton());
        UUID cat3 = fixture.categorie("CAT3-" + c.jeton());
        UUID piece = creerOk(facture().avec(LIGNES_CHAMP, List.of(ligne("A", "1", "100", s.categorie()),
                ligne("B", "1", "100", cat2), ligne("C", "1", "100", cat3))));

        Map<String, BigDecimal> parts = taux(CATEGORIES, CATEGORIE, piece);
        assertAll(
                () -> assertThat(parts).hasSize(3),
                () -> assertThat(somme(parts)).isEqualByComparingTo("1"));
    }

    /** E-C3.1 : sans ligne, une part à 1 sur la catégorie d'en-tête. */
    @Test
    void ec31_sansLigne_categorieDEnTete_partA1() throws Exception {
        UUID piece = creerOk(facture().avec(LIGNES_CHAMP, List.of()));

        Map<String, BigDecimal> parts = taux(CATEGORIES, CATEGORIE, piece);
        assertThat(parts).hasSize(1).containsOnlyKeys(id(s.categorie()));
        assertThat(parts.get(id(s.categorie()))).isEqualByComparingTo("1");
    }

    /** E-C3.1 : TTC nul (PU 0) → catégorie d'en-tête à 1, comme sans ligne. */
    @Test
    void ec31_ttcNul_categorieDEnTete_partA1() throws Exception {
        UUID enTete = fixture.categorie("CATE-" + c.jeton());
        UUID piece = creerOk(facture().avec(CATEGORIE_CHAMP, enTete)
                .avec(LIGNES_CHAMP, List.of(ligne("GR", "5", "0", s.categorie()))));

        Map<String, BigDecimal> parts = taux(CATEGORIES, CATEGORIE, piece);
        assertThat(parts).hasSize(1).containsOnlyKeys(id(enTete));
    }

    /** E-C3.1 : sans ligne ni catégorie d'en-tête → catégorie du lot ({@code PrjMarket.StlCategoryId}). */
    @Test
    void ec31_sansLigneNiEnTete_categorieDuLot() throws Exception {
        UUID lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        UUID categorieLot = fixture.categorie("CATL-" + c.jeton());
        fixture.poserSur("PrjMarket", lot, CATEGORIE, categorieLot);

        UUID piece = creerOk(facture().avec(LIGNES_CHAMP, List.of()).avec(CATEGORIE_CHAMP, null).avec("lotId", lot));

        assertThat(taux(CATEGORIES, CATEGORIE, piece)).hasSize(1).containsOnlyKeys(id(categorieLot));
    }

    /** E-C3.1 : ni ligne, ni catégorie d'en-tête, ni lot → 400 texte OMRAN, rien d'écrit. */
    @Test
    void ec31_aucuneCategorie_400_texteOmran() throws Exception {
        MvcResult r = creerPar(facture().avec(LIGNES_CHAMP, List.of()).avec(CATEGORIE_CHAMP, null));

        assertRefus(r, 400, VENTILATION_CATEGORIES);
    }

    // ------------------------------------------------------------------ E-C3.2 projets

    @Test
    void ec32_sansProjet_ventilationVide_400_texteOmran() throws Exception {
        assertRefus(creerPar(factureSansProjet()), 400, VENTILATION_PROJETS);
    }

    @Test
    void ec32_sansProjet_ventilationAbsente_400_texteOmran() throws Exception {
        assertRefus(creerPar(facture().avec(PROJET_CHAMP, null).sans(VENTILATION_CHAMP)), 400, VENTILATION_PROJETS);
    }

    @Test
    void ec32_sansProjet_somme90_400_texteOmran100() throws Exception {
        MvcResult r = creerPar(factureSansProjet(part(c.projet(), "0.4"), part(projet2, "0.5")));

        assertRefus(r, 400, VENTILATION_PROJETS_100);
    }

    @Test
    void ec32_sansProjet_somme100_201_deuxParts() throws Exception {
        UUID piece = creerOk(factureSansProjet(part(c.projet(), "0.4"), part(projet2, "0.6")));

        Map<String, BigDecimal> parts = taux(PROJETS, PROJET, piece);
        assertAll(
                () -> assertThat(parts).hasSize(2),
                () -> assertThat(parts.get(id(c.projet()))).isEqualByComparingTo("0.4"),
                () -> assertThat(parts.get(id(projet2))).isEqualByComparingTo("0.6"),
                () -> assertThat(fixture.pieceEnBase(piece)).containsEntry(PROJET, null));
    }

    /** E-C3.2 / E-C3.3 : contrôle sur round2(Σ) ; trois tiers à 0,333 → 201, la dernière part complète Σ = 1. */
    @Test
    void ec33_sansProjet_tiersArrondis_201_derniereComplete() throws Exception {
        UUID projet3 = fixture.projet("P3-" + c.jeton(), c.societe());
        fixture.habiliter(redacteur, projet3);

        UUID piece = creerOk(factureSansProjet(part(c.projet(), "0.333"), part(projet2, "0.333"), part(projet3, "0.333")));

        Map<String, BigDecimal> parts = taux(PROJETS, PROJET, piece);
        assertAll(
                () -> assertThat(parts).hasSize(3),
                () -> assertThat(somme(parts)).isEqualByComparingTo("1"));
    }

    /** E-C3.2 / Q-2 : avec un projet d'en-tête, la ventilation projet est facultative et non contrôlée. */
    @Test
    void ec32_avecProjet_ventilationNonControlee_201() throws Exception {
        UUID piece = creerOk(facture().avec(VENTILATION_CHAMP, List.of(part(projet2, "0.5"))));

        assertThat(guidDe(fixture.pieceEnBase(piece), PROJET)).isEqualTo(id(c.projet()));
    }

    @Test
    void ec32_avecProjet_sansVentilation_201_aucunePart() throws Exception {
        UUID piece = creerOk(facture());

        assertThat(fixture.lire(PROJETS, piece)).isEmpty();
    }

    /** E-C3.2 : un projet ventilé non habilité → 404 « Projet introuvable », rien d'écrit. */
    @Test
    void ec32_projetVentileNonHabilite_404() throws Exception {
        UUID cache = fixture.projet("PX-" + c.jeton(), c.societe());

        MvcResult r = creerPar(factureSansProjet(part(c.projet(), "0.5"), part(cache, "0.5")));

        assertRefus(r, 404, PROJET_INTROUVABLE);
    }

    // ------------------------------------------------------------------ E-C3.3 remplacement au PUT

    @Test
    void ec33_modification_deuxVentilationsRemplacees() throws Exception {
        UUID cat2 = fixture.categorie("CAT2-" + c.jeton());
        UUID piece = creerOk(factureSansProjet(part(c.projet(), "0.4"), part(projet2, "0.6")));

        MvcResult r = appeler(mockMvc, modifier(piece, factureSansProjet(part(projet2, "1"))
                .avec(LIGNES_CHAMP, List.of(ligne("B", "1", "100", cat2)))), connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        Map<String, BigDecimal> projets = taux(PROJETS, PROJET, piece);
        Map<String, BigDecimal> categories = taux(CATEGORIES, CATEGORIE, piece);
        assertAll(
                () -> assertThat(projets).hasSize(1).containsOnlyKeys(id(projet2)),
                () -> assertThat(projets.get(id(projet2))).isEqualByComparingTo("1"),
                () -> assertThat(categories).hasSize(1).containsOnlyKeys(id(cat2)));
    }

    /** E-C3.2 au PUT : mêmes contrôles qu'à la création (Σ ≠ 100 % → 400). */
    @Test
    void ec32_modification_sansProjet_somme90_400() throws Exception {
        UUID piece = creerOk(facture());

        MvcResult r = appeler(mockMvc, modifier(piece, factureSansProjet(part(c.projet(), "0.9"))),
                connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertThat(detail(r)).isEqualTo(VENTILATION_PROJETS_100);
    }
}
