package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.HorlogeReglable;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.AJUSTEE;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.AVANCEMENT;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.DROIT_ECRAN;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.DUE;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.PREVISION;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.PROJET_ID;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.REEL;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.RESTE;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.TOTAL;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.detail;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.somme;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.textes;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.totaux;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.valeurOuNull;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.INVOICE;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.REFUND;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * N4a — Prévisions des lots d'un projet (FEATURE-n4-previsions-chantier.md R1-R10, R12, réponse Q3 du manager).
 * Projet de référence, « aujourdhui » = 15/03/2026 (horloge métier réglée) ; lots A et B inclus (planning 01/01/2026 + 90 j,
 * sans retenue : estimation = TTC de base en 3 décomptes fin janvier / fin février / fin mars), X exclu
 * ({@code IncludedInPaymentPrevision = 0}), Y à NULL. Chiffres faits à la main dans chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsProjetLectureTest {

    private static final HorlogeReglable HORLOGE = new HorlogeReglable();
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 3, 15);
    private static final LocalDate DEBUT_2026 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DEBUT_2027 = LocalDate.of(2027, 1, 1);
    private static final int DUREE_90 = 90;
    private static final BigDecimal CENTIME = new BigDecimal("0.01");
    private static final double PRECISION = 0.0001;
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String COMMERCIAL = "Commercial";
    private static final String LOTS_IDS = "$.lots[*].lotId";
    private static final String MOIS = "$.mois";
    private static final String MOIS_ESTIMATION = "$.mois[*].estimation";
    private static final String DEC_PREVISION = "$.decaissements.prevision";
    private static final String DEC_AJUSTEE = "$.decaissements.previsionAjustee";
    private static final String DEC_REEL = "$.decaissements.reel";
    private static final String AVANCEMENTS_PREVISION = "$.avancements.prevision";

    private static final AtomicInteger ORDRES = new AtomicInteger();
    private static final AtomicInteger ECRITURES = new AtomicInteger();
    private static final Set<String> CREATION_ORDRE = Set.of("prepareStatement", "prepareCall");

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return HORLOGE;
    }

    /** Compte les ordres SQL préparés et, parmi eux, les écritures (R12). */
    @TestConfiguration
    static class CompteurOrdresSql {
        @Bean
        static BeanPostProcessor compteurEcrituresPrevisions() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nom) {
                    return bean instanceof DataSource ds ? proxy(DataSource.class, ds) : bean;
                }
            };
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixtureLotSousFenetres sf;

    private PrevisionsProjetSupport appels;
    private String jeton;
    private UUID societe;
    private int rang;

    private record Reference(UUID projet, UUID lotA, UUID lotB, UUID lotX, UUID lotY) {
    }

    @BeforeEach
    void setUp() {
        HORLOGE.regler(AUJOURDHUI);
        appels = new PrevisionsProjetSupport(mockMvc, jdbc, em);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        societe = parite.entiteJuridique("PPE-" + jeton);
    }

    // ------------------------------------------------------------------ fixtures

    private UUID projet(String code) {
        return parite.projet(code + "-" + jeton, societe);
    }

    private RequestPostProcessor admin(UUID... projets) {
        rang++;
        return connecte(parite.administrateur("pp.adm" + rang + "." + jeton, projets), ADMINISTRATEUR);
    }

    /** Lot inclus sur ce statut, planning {@code debut} + 90 j, estimation de base HT / TVA / TTC, sans retenue. */
    private UUID lot(UUID projet, String genre, UUID statut, LocalDate debut, String ht, String tva, String ttc) {
        rang++;
        UUID lot = parite.lot(projet, "PPL" + rang + "-" + jeton, genre, statut);
        prev.planning(lot, debut, DUREE_90, debut, DUREE_90);
        prev.estimationBase(lot, ht, tva, ttc, "0.2");
        prev.retenues(lot, "0", "0", "0");
        appels.inclure(lot, true);
        return lot;
    }

    private UUID lot(UUID projet, UUID statut, String ht, String tva, String ttc) {
        return lot(projet, ENTREPRENEURS, statut, DEBUT_2026, ht, tva, ttc);
    }

    private UUID statut() {
        rang++;
        return parite.statut(ENTREPRENEURS, "PPS" + rang + "-" + jeton);
    }

    private UUID piece(UUID lot, UUID projet, String nature, LocalDate echeance, String montant) {
        rang++;
        return sf.piece(lot, projet, societe, "PPD" + rang + "-" + jeton, nature, "", echeance, montant);
    }

    /**
     * Projet de référence. Estimation : A 1200 → 400 × 3, B 2400 → 800 × 3. P : A 01/02 500, 01/04 700 (+ 999 sans date,
     * exclu) ; B 10/03 300, 20/03 900. R : A 10/02 1000, 20/04 600 (avoir 50, pièce non payable 9000, pièce sans échéance
     * 777 exclus) ; B 05/03 400. X (exclu) et Y (NULL) portent chacun estimation 6000, P 10000, R 20000.
     */
    private Reference reference(UUID statutA, UUID statutB) {
        UUID projet = projet("PPR");
        UUID a = lot(projet, statutA, "1000", "200", "1200");
        UUID b = lot(projet, statutB, "2000", "400", "2400");
        UUID x = lot(projet, statutA, "5000", "1000", "6000");
        UUID y = lot(projet, statutA, "5000", "1000", "6000");
        appels.inclure(x, false);
        appels.inclure(y, null);
        sf.prevision(a, LocalDate.of(2026, 2, 1), "500", "0.5");
        sf.prevision(a, LocalDate.of(2026, 4, 1), "700", "1");
        sf.prevision(a, null, "999", "1");
        sf.prevision(b, LocalDate.of(2026, 3, 10), "300", "0.25");
        sf.prevision(b, LocalDate.of(2026, 3, 20), "900", "1");
        piece(a, projet, INVOICE, LocalDate.of(2026, 2, 10), "1000");
        piece(a, projet, INVOICE, LocalDate.of(2026, 4, 20), "600");
        piece(a, projet, REFUND, LocalDate.of(2026, 2, 15), "50");
        sf.nonPayable(piece(a, projet, INVOICE, LocalDate.of(2026, 2, 16), "9000"));
        sf.echeance(piece(a, projet, INVOICE, LocalDate.of(2026, 2, 17), "777"), null);
        piece(b, projet, INVOICE, LocalDate.of(2026, 3, 5), "400");
        for (UUID exclu : List.of(x, y)) {
            sf.prevision(exclu, LocalDate.of(2026, 2, 1), "10000", "1");
            piece(exclu, projet, INVOICE, LocalDate.of(2026, 2, 1), "20000");
        }
        return new Reference(projet, a, b, x, y);
    }

    private Reference reference() {
        UUID s = statut();
        return reference(s, s);
    }

    private Reponse ok(RequestPostProcessor qui, UUID projet) throws Exception {
        Reponse r = appels.lire(qui, projet);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static void montant(SoftAssertions s, Reponse r, String chemin, String attendu) {
        s.assertThat(r.nombre(chemin)).as(chemin).isCloseTo(new BigDecimal(attendu), within(CENTIME));
    }

    private static String ligneDuLot(UUID lot, String champ) {
        return "$.lots[?(@.lotId == '" + lot + "')]." + champ;
    }

    private static BigDecimal unique(Reponse r, String chemin) {
        List<String> valeurs = textes(r, chemin);
        assertThat(valeurs).as(chemin).hasSize(1);
        return new BigDecimal(valeurs.getFirst());
    }

    // ------------------------------------------------------------------ R4 / R5 / R6 : totaux

    /** R4 : total 1200 + 2400 = 3600 ; dû (< 15/03) = A 400 + 400, B 800 + 800 = 2400 ; reste 1200. */
    @Test
    void r4_prevision_totalDueReste_lotsInclusSeuls() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.valeur("$.aujourdhui")).isEqualTo(AUJOURDHUI.toString());
        s.assertThat(r.valeur("$.projetId")).isEqualTo(ref.projet().toString());
        s.assertThat(r.valeur("$.projetDescription")).isEqualTo("Projet PPR-" + jeton);
        montant(s, r, totaux(PREVISION, TOTAL), "3600");
        montant(s, r, totaux(PREVISION, DUE), "2400");
        montant(s, r, totaux(PREVISION, RESTE), "1200");
        s.assertAll();
    }

    /** R5 : ΣP = 500 + 700 + 300 + 900 = 2400 (sans date exclue), ΣR = 1000 + 600 + 400 = 2000 ; ΣP(< 15/03) = 800. */
    @Test
    void r5_previsionAjustee_totalPPlusR_dueEchuPlusR_resteNonEchu() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        SoftAssertions s = new SoftAssertions();
        montant(s, r, totaux(AJUSTEE, TOTAL), "4400");
        montant(s, r, totaux(AJUSTEE, DUE), "2800");
        montant(s, r, totaux(AJUSTEE, RESTE), "1600");
        s.assertAll();
    }

    /** R6 : total null, dû = ΣR = 2000 (avoir, non payable, sans échéance exclus), reste = ΣP = 2400 (code OMRAN l.220). */
    @Test
    void r6_reel_totalNul_dueSommeReel_resteSommePrevisions() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(valeurOuNull(r, totaux(REEL, TOTAL))).as("reel.total").isNull();
        montant(s, r, totaux(REEL, DUE), "2000");
        montant(s, r, totaux(REEL, RESTE), "2400");
        s.assertAll();
    }

    /** R5 : décaissement ajusté = P (4 lignes datées) puis R (3 lignes) ; Σ = 4400 ; aucun lot exclu. */
    @Test
    void r5_decaissementAjuste_previsionsPuisReel() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        List<String> natures = textes(r, DEC_AJUSTEE + "[*].nature");
        SoftAssertions s = new SoftAssertions();
        s.assertThat(natures).containsExactly("PREVISION", "PREVISION", "PREVISION", "PREVISION", "REEL", "REEL", "REEL");
        s.assertThat(somme(r, DEC_AJUSTEE + "[*].montant")).isCloseTo(new BigDecimal("4400"), within(CENTIME));
        s.assertThat(textes(r, DEC_AJUSTEE + "[*].lotId")).isNotEmpty()
                .allMatch(id -> id.equals(ref.lotA().toString()) || id.equals(ref.lotB().toString()));
        s.assertAll();
    }

    /** R4 / R6 : décaissements Prévision Σ = 3600 et Réel Σ = 2000 en 3 lignes, lots A et B seulement. */
    @Test
    void r4_r6_decaissementsPrevisionEtReel_sommesEgalesAuxTotaux() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        List<String> lotsVus = new ArrayList<>(textes(r, DEC_PREVISION + "[*].lotId"));
        lotsVus.addAll(textes(r, DEC_REEL + "[*].lotId"));
        SoftAssertions s = new SoftAssertions();
        s.assertThat(somme(r, DEC_PREVISION + "[*].montant")).isCloseTo(new BigDecimal("3600"), within(CENTIME));
        s.assertThat(textes(r, DEC_REEL + "[*].montant")).hasSize(3);
        s.assertThat(somme(r, DEC_REEL + "[*].montant")).isCloseTo(new BigDecimal("2000"), within(CENTIME));
        s.assertThat(lotsVus).isNotEmpty().doesNotContain(ref.lotX().toString(), ref.lotY().toString());
        s.assertAll();
    }

    // ------------------------------------------------------------------ R8 : mois du graphe

    /** R8 : mois consécutifs ; Σ estimation = 3600, Σ prévision = 2400, Σ réel = 2000 (sommes des lots inclus). */
    @Test
    void r8_moisConsecutifs_sommesDesLotsInclus() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        List<LocalDate> mois = textes(r, "$.mois[*].mois").stream().map(LocalDate::parse).toList();
        SoftAssertions s = new SoftAssertions();
        s.assertThat(mois).isNotEmpty();
        for (int i = 1; i < mois.size(); i++) {
            s.assertThat(mois.get(i)).as("mois %d", i).isEqualTo(mois.get(i - 1).plusMonths(1));
        }
        s.assertThat(somme(r, MOIS_ESTIMATION)).isCloseTo(new BigDecimal("3600"), within(CENTIME));
        s.assertThat(somme(r, "$.mois[*].prevision")).isCloseTo(new BigDecimal("2400"), within(CENTIME));
        s.assertThat(somme(r, "$.mois[*].reel")).isCloseTo(new BigDecimal("2000"), within(CENTIME));
        s.assertAll();
    }

    // ------------------------------------------------------------------ R9 : grille Lots

    @Test
    void r9_grilleLots_uneLigneParLotDuPerimetre() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        assertThat(textes(r, LOTS_IDS)).containsExactlyInAnyOrder(ref.lotA().toString(), ref.lotB().toString());
    }

    /**
     * R9 colonnes « calculée » (Market.cs l.386, 458, 480) du lot A : prévision = Σ estimation 1200 ; coût prévisionnel =
     * ΣP 1200 + ΣR 1600 = 2800 ; position = ΣR 1600. Différence = base − calculée.
     */
    @Test
    void r9_colonnesCalculees_etDifferences_lotA() throws Exception {
        Reference ref = reference();

        Reponse r = ok(admin(ref.projet()), ref.projet());

        UUID a = ref.lotA();
        SoftAssertions s = new SoftAssertions();
        s.assertThat(textes(r, ligneDuLot(a, "code")))
                .containsExactly(jdbc.queryForObject("SELECT Code FROM PrjMarket WHERE PKey = ?", String.class, a.toString()));
        s.assertThat(unique(r, ligneDuLot(a, "previsionNetAPayerCalculee"))).isCloseTo(new BigDecimal("1200"), within(CENTIME));
        s.assertThat(unique(r, ligneDuLot(a, "coutPrevisionnelNetAPayerCalcule"))).isCloseTo(new BigDecimal("2800"), within(CENTIME));
        s.assertThat(unique(r, ligneDuLot(a, "positionNetAPayerCalculee"))).isCloseTo(new BigDecimal("1600"), within(CENTIME));
        for (String[] c : new String[][] {
                {"previsionNetAPayer", "previsionNetAPayerCalculee", "previsionNetAPayerDifference"},
                {"coutPrevisionnelNetAPayer", "coutPrevisionnelNetAPayerCalcule", "coutPrevisionnelNetAPayerDifference"},
                {"positionNetAPayer", "positionNetAPayerCalculee", "positionNetAPayerDifference"}}) {
            BigDecimal attendu = unique(r, ligneDuLot(a, c[0])).subtract(unique(r, ligneDuLot(a, c[1])));
            s.assertThat(unique(r, ligneDuLot(a, c[2]))).as(c[2]).isCloseTo(attendu, within(CENTIME));
        }
        s.assertAll();
    }

    /** R9 : coût prévisionnel net à payer de base = celui de la liste des lots (même {@code CalculLot}). */
    @Test
    void r9_coutPrevisionnelNetAPayer_egalALaListeDesLots() throws Exception {
        Reference ref = reference();
        RequestPostProcessor qui = admin(ref.projet());

        Reponse r = ok(qui, ref.projet());
        Reponse liste = new AppelsProjet(mockMvc).lire(qui, AppelsProjet.LOTS, "projetIds", ref.projet().toString(),
                "size", AppelsProjet.TAILLE);

        assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
        BigDecimal attendu = unique(liste, "$.content[?(@.id == '" + ref.lotA() + "')].coutPrevisionnelNetAPayer");
        assertThat(unique(r, ligneDuLot(ref.lotA(), "coutPrevisionnelNetAPayer"))).isCloseTo(attendu, within(CENTIME));
    }

    // ------------------------------------------------------------------ R3 : une seule source de calcul

    /** R3 : projet à 1 lot (estimation, P et R) → {@code mois} du projet = {@code mois} du lot, champ à champ. */
    @Test
    void r3_projetAUnLot_moisEgauxAuxMoisDuLot() throws Exception {
        UUID projet = projet("PP1");
        UUID lot = lot(projet, statut(), "1000", "200", "1200");
        sf.prevision(lot, LocalDate.of(2026, 2, 1), "500", "0.5");
        sf.prevision(lot, LocalDate.of(2026, 4, 1), "700", "1");
        piece(lot, projet, INVOICE, LocalDate.of(2026, 2, 10), "1000");
        RequestPostProcessor qui = admin(projet);

        Reponse r = ok(qui, projet);
        Reponse duLot = new AppelsProjet(mockMvc).lire(qui, AppelsProjet.LOTS + "/" + lot + "/previsions");

        assertThat(duLot.statut()).as(duLot.corps()).isEqualTo(200);
        List<Map<String, String>> attendus = normaliser(duLot.valeur(MOIS));
        assertThat(attendus).isNotEmpty();
        assertThat(normaliser(r.valeur(MOIS))).isEqualTo(attendus);
    }

    /** Points du graphe, nombres ramenés à leur écriture canonique ({@code 400.000000} = {@code 400}). */
    private static List<Map<String, String>> normaliser(Object points) {
        List<Map<String, String>> resultat = new ArrayList<>();
        for (Object p : (List<?>) points) {
            Map<String, String> ligne = new java.util.TreeMap<>();
            ((Map<?, ?>) p).forEach((cle, valeur) -> ligne.put(String.valueOf(cle), canonique(valeur)));
            resultat.add(ligne);
        }
        return resultat;
    }

    private static String canonique(Object valeur) {
        if (valeur instanceof Number n) {
            return new BigDecimal(String.valueOf(n)).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(valeur);
    }

    // ------------------------------------------------------------------ R7 : avancements

    /**
     * Poids : A (non multi, HT 4000, planning 2026) 0,8 ; B (non multi, HT 1000, planning 2027) 0,2 ; C multi (HT 5000)
     * hors poids. Au 15/07/2026 : A = 1 (dernier point mars), B = 0 → 0,8 × 1 + 0,2 × 0 = 0,8.
     */
    private UUID projetPondere() {
        UUID projet = projet("PPW");
        UUID s = statut();
        lot(projet, ENTREPRENEURS, s, DEBUT_2026, "4000", "800", "4800");
        lot(projet, ENTREPRENEURS, s, DEBUT_2027, "1000", "200", "1200");
        UUID multi = parite.statut(FOURNISSEURS, "PPM-" + jeton);
        lot(projet, FOURNISSEURS, multi, DEBUT_2027, "5000", "1000", "6000");
        return projet;
    }

    @Test
    void r7_avancementPrevision_poidsHtDesLotsNonMulti() throws Exception {
        UUID projet = projetPondere();
        HORLOGE.regler(LocalDate.of(2026, 7, 15));

        Reponse r = ok(admin(projet), projet);

        assertThat(r.nombre(totaux(PREVISION, AVANCEMENT)).doubleValue()).isCloseTo(0.8, within(PRECISION));
    }

    @Test
    void r7_aujourdhuiAvantDebutDuProjet_avancements0() throws Exception {
        UUID projet = projetPondere();
        HORLOGE.regler(LocalDate.of(2025, 6, 15));

        Reponse r = ok(admin(projet), projet);

        SoftAssertions s = new SoftAssertions();
        for (String serie : List.of(PREVISION, AJUSTEE, REEL)) {
            s.assertThat(r.nombre(totaux(serie, AVANCEMENT))).as(serie).isEqualByComparingTo("0");
        }
        s.assertAll();
    }

    @Test
    void r7_aujourdhuiApresFinDuProjet_avancements1() throws Exception {
        UUID projet = projetPondere();
        HORLOGE.regler(LocalDate.of(2028, 6, 15));

        Reponse r = ok(admin(projet), projet);

        SoftAssertions s = new SoftAssertions();
        for (String serie : List.of(PREVISION, AJUSTEE, REEL)) {
            s.assertThat(r.nombre(totaux(serie, AVANCEMENT))).as(serie).isEqualByComparingTo("1");
        }
        s.assertAll();
    }

    /**
     * R7 liste « Avancement » : seulement les mois où la valeur change. Février 2026 : 0,8 × 2/3 = 0,5333 ; février 2027 :
     * 0,8 + 0,2 × 2/3 = 0,9333 ; juin 2026 (A déjà à 1, B pas commencé) absent.
     */
    @Test
    void r7_listeAvancements_seulementLesMoisQuiChangent() throws Exception {
        UUID projet = projetPondere();

        Reponse r = ok(admin(projet), projet);

        List<String> mois = textes(r, AVANCEMENTS_PREVISION + "[*].mois");
        List<BigDecimal> valeurs = textes(r, AVANCEMENTS_PREVISION + "[*].avancement").stream().map(BigDecimal::new).toList();
        SoftAssertions s = new SoftAssertions();
        s.assertThat(mois).hasSameSizeAs(valeurs).noneMatch(m -> m.startsWith("2026-06"));
        s.assertThat(avancementDuMois(mois, valeurs, "2026-02")).isCloseTo(0.8 * 2 / 3, within(PRECISION));
        s.assertThat(avancementDuMois(mois, valeurs, "2027-02")).isCloseTo(0.8 + 0.2 * 2 / 3, within(PRECISION));
        for (int i = 1; i < valeurs.size(); i++) {
            s.assertThat(valeurs.get(i)).as("mois %s", mois.get(i)).isNotEqualByComparingTo(valeurs.get(i - 1));
        }
        s.assertAll();
    }

    private static double avancementDuMois(List<String> mois, List<BigDecimal> valeurs, String prefixe) {
        for (int i = 0; i < mois.size() && i < valeurs.size(); i++) {
            if (mois.get(i).startsWith(prefixe)) {
                return valeurs.get(i).doubleValue();
            }
        }
        return Double.NaN;
    }

    // ------------------------------------------------------------------ R10 : projet sans lot du périmètre

    @Test
    void r10_projetSansAucunLot_200_toutAZero() throws Exception {
        UUID projet = projet("PP0");

        Reponse r = ok(admin(projet), projet);

        zeros(r);
    }

    @Test
    void r10_projetDontLeSeulLotEstExclu_200_toutAZero() throws Exception {
        UUID projet = projet("PPZ");
        UUID exclu = lot(projet, statut(), "1000", "200", "1200");
        appels.inclure(exclu, false);
        sf.prevision(exclu, LocalDate.of(2026, 2, 1), "500", "0.5");
        piece(exclu, projet, INVOICE, LocalDate.of(2026, 2, 10), "1000");

        Reponse r = ok(admin(projet), projet);

        zeros(r);
    }

    private static void zeros(Reponse r) {
        SoftAssertions s = new SoftAssertions();
        for (String serie : List.of(PREVISION, AJUSTEE)) {
            for (String champ : List.of(TOTAL, DUE, RESTE, AVANCEMENT)) {
                s.assertThat(r.nombre(totaux(serie, champ))).as(serie + "." + champ).isEqualByComparingTo("0");
            }
        }
        s.assertThat(valeurOuNull(r, totaux(REEL, TOTAL))).as("reel.total").isNull();
        for (String champ : List.of(DUE, RESTE, AVANCEMENT)) {
            s.assertThat(r.nombre(totaux(REEL, champ))).as("reel." + champ).isEqualByComparingTo("0");
        }
        for (String liste : List.of(MOIS, "$.lots", DEC_PREVISION, DEC_AJUSTEE, DEC_REEL, AVANCEMENTS_PREVISION,
                "$.avancements.previsionAjustee", "$.avancements.reel")) {
            s.assertThat((List<?>) r.valeur(liste)).as(liste).isEmpty();
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R1 : visibilité du projet

    /** R1 : projet inconnu et projet existant non habilité → même 404 ; témoin 200 sur le projet habilité. */
    @Test
    void r1_projetInconnuOuNonHabilite_404_memeMessage() throws Exception {
        UUID visible = projet("PPV");
        UUID cache = projet("PPH");
        RequestPostProcessor qui = connecte(parite.avecDroits("pp.r1." + jeton, List.of(DROIT_ECRAN), visible), COMMERCIAL);

        Reponse temoin = appels.lire(qui, visible);
        Reponse inconnu = appels.lire(qui, UUID.randomUUID());
        Reponse nonHabilite = appels.lire(qui, cache);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(inconnu.statut()).as(inconnu.corps()).isEqualTo(404);
        s.assertThat(nonHabilite.statut()).as(nonHabilite.corps()).isEqualTo(404);
        s.assertThat(detail(nonHabilite)).isNotBlank().isEqualTo(detail(inconnu));
        s.assertAll();
    }

    // ------------------------------------------------------------------ R2 : droit d'écran

    @Test
    void r2_sansDroitEcran_403() throws Exception {
        UUID projet = projet("PPD");
        RequestPostProcessor sansDroit = connecte(parite.avecDroits("pp.r2a." + jeton, List.of(), projet), COMMERCIAL);

        Reponse r = appels.lire(sansDroit, projet);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r2_avecDroitEcranSeul_200() throws Exception {
        UUID projet = projet("PPO");
        RequestPostProcessor avecDroit = connecte(parite.avecDroits("pp.r2b." + jeton, List.of(DROIT_ECRAN), projet),
                COMMERCIAL);

        Reponse r = appels.lire(avecDroit, projet);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ 400 : projetId

    @Test
    void projetIdAbsent_400() throws Exception {
        UUID projet = projet("PPA");
        RequestPostProcessor qui = admin(projet);

        Reponse temoin = appels.lire(qui, projet);
        Reponse r = appels.lireBrut(qui);

        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void projetIdMalForme_400() throws Exception {
        UUID projet = projet("PPF");
        RequestPostProcessor qui = admin(projet);

        Reponse temoin = appels.lire(qui, projet);
        Reponse r = appels.lireBrut(qui, PROJET_ID, "pas-un-uuid");

        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ Q3 : visibilité des lots (règle 6)

    /**
     * Compte restreint (intervenant sur le statut de A seulement), droit d'écran, projet habilité : ni la ligne ni les
     * montants de B. Totaux = A seul : prévision 1200, ajustée ΣP 1200 + ΣR 1600 = 2800, réel dû 1600.
     */
    @Test
    void q3_compteRestreint_neVoitNiLignesNiMontantsDesLotsHorsPerimetre() throws Exception {
        UUID statutA = statut();
        UUID statutB = statut();
        Reference ref = reference(statutA, statutB);
        appels.statutProjet(ref.projet(), statutA);
        AdmUser restreint = parite.restreint("pp.q3." + jeton, ref.projet());
        UUID role = parite.roleDe(restreint);
        appels.droitEcran(role);
        parite.intervenant(ENTREPRENEURS, role, statutA, null);

        Reponse r = ok(connecte(restreint, COMMERCIAL), ref.projet());

        List<String> lotsVus = new ArrayList<>(textes(r, LOTS_IDS));
        for (String dec : List.of(DEC_PREVISION, DEC_AJUSTEE, DEC_REEL)) {
            lotsVus.addAll(textes(r, dec + "[*].lotId"));
        }
        SoftAssertions s = new SoftAssertions();
        s.assertThat(textes(r, LOTS_IDS)).containsExactly(ref.lotA().toString());
        s.assertThat(lotsVus).isNotEmpty().doesNotContain(ref.lotB().toString());
        montant(s, r, totaux(PREVISION, TOTAL), "1200");
        montant(s, r, totaux(AJUSTEE, TOTAL), "2800");
        montant(s, r, totaux(REEL, DUE), "1600");
        s.assertThat(somme(r, MOIS_ESTIMATION)).isCloseTo(new BigDecimal("1200"), within(CENTIME));
        s.assertAll();
    }

    // ------------------------------------------------------------------ R12 : aucune écriture

    @Test
    void r12_lectureEtExport_aucuneEcritureEnBase() throws Exception {
        Reference ref = reference();
        RequestPostProcessor qui = admin(ref.projet());
        em.flush();
        ORDRES.set(0);
        ECRITURES.set(0);

        Reponse r = appels.lire(qui, ref.projet());
        MockHttpServletResponse export = appels.exporter(qui, ref.projet());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        s.assertThat(export.getStatus()).isEqualTo(200);
        s.assertThat(ORDRES.get()).as("ordres SQL vus (témoin du compteur)").isPositive();
        s.assertThat(ECRITURES.get()).as("INSERT / UPDATE / DELETE / MERGE").isZero();
        s.assertAll();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, T cible) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
            if (CREATION_ORDRE.contains(m.getName()) && args != null && args.length > 0 && args[0] instanceof String sql) {
                ORDRES.incrementAndGet();
                String debut = sql.stripLeading().toUpperCase(Locale.ROOT);
                if (debut.startsWith("INSERT") || debut.startsWith("UPDATE") || debut.startsWith("DELETE")
                        || debut.startsWith("MERGE")) {
                    ECRITURES.incrementAndGet();
                }
            }
            try {
                Object resultat = m.invoke(cible, args);
                return resultat instanceof Connection c ? proxy(Connection.class, c) : resultat;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
