package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DELAI;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ORDRE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.POURCENTAGE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.REMBOURSEMENTS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.decimal;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.echeance;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotAvenants.ECHEANCES;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.LIGNE_PERIMEE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Grille des échéances de remboursement de RG (FEATURE-lot-avenants-avances.md §4.2, R8-R12, R17, §9) : envoi entier, délai ↔ échéance
 * calculés depuis la date de réception définitive (R10), Σ % libre, recalcul des prévisions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotEcheancesRgEcritureTest {

    private static final String DELAY = "Delay";
    private static final String ECHEANCE_DATE = "EcheanceDate";
    private static final String RATIO = "Ratio";
    private static final String DESCRIPTION_COL = "Description";
    private static final String DATE_DEF = "$.dateReceptionDefinitive";
    private static final String CINQUANTE = "50";
    private static final LocalDate MAI = LocalDate.of(2026, 5, 1);
    private static final LocalDate JUIN = LocalDate.of(2026, 6, 30);
    private static final LocalDate AOUT = LocalDate.of(2026, 8, 15);
    private static final LocalDate SEPTEMBRE = LocalDate.of(2026, 9, 1);
    private static final LocalDate IGNOREE = LocalDate.of(2027, 1, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotSousFenetres sf;
    @Autowired private FixtureLotAvenants av;
    @Autowired private FixtureLotPrevisions prev;

    private AvenantsLotSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private UUID type;
    private UUID provisoire;
    private UUID definitif;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        av.tables();
        appels = new AvenantsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        UUID societe = parite.entiteJuridique("ERE-" + jeton);
        projet = parite.projet("ERP-" + jeton, societe);
        statut = parite.statut(ENTREPRENEURS, "ERS-" + jeton);
        type = sf.typeReception("ERT-" + jeton);
        provisoire = sf.genreReception("ERO-" + jeton, false);
        definitif = sf.genreReception("ERD-" + jeton, true);
        admin = connecte(parite.administrateur("lot.er." + jeton, projet), "Administrateur");
    }

    private UUID lot() {
        rang++;
        return parite.lot(projet, "ER" + rang + "-" + jeton, ENTREPRENEURS, statut);
    }

    private void reception(UUID lot, LocalDate date, UUID genre) {
        rang++;
        sf.reception(lot, rang, "ERR" + rang + "-" + jeton, date, false, "Chantier", type, genre);
    }

    private Reponse enregistrer(UUID lot, CorpsJson... echeances) throws Exception {
        return appels.enregistrerEcheances(admin, lot, List.of(echeances));
    }

    private Map<String, Object> enBase(UUID id) {
        List<Map<String, Object>> l = av.parId(ECHEANCES, id);
        assertThat(l).as(ECHEANCES + " " + id).hasSize(1);
        return l.get(0);
    }

    private static LocalDate jour(Object valeur) {
        return valeur == null ? null : ((Timestamp) valeur).toLocalDateTime().toLocalDate();
    }

    private static String echeanceLue(Reponse r, UUID id) {
        return String.valueOf(r.valeur("$.echeances[?(@.id == '" + id + "')].echeance"));
    }

    // ------------------------------------------------------------------ R10 / R11 : délai ↔ échéance

    // Date R10 = MAX des réceptions DEF (30/06) ; la PRO plus récente (15/08) et la fin du lot n'y changent rien.
    @Test
    void r11_delai_echeanceEgaleReceptionDefinitivePlusDelai_echeanceEnvoyeeIgnoree() throws Exception {
        UUID lot = lot();
        av.finDuLot(lot, LocalDate.of(2026, 12, 31));
        reception(lot, MAI, definitif);
        reception(lot, JUIN, definitif);
        reception(lot, AOUT, provisoire);
        UUID id = UUID.randomUUID();

        Reponse r = enregistrer(lot, echeance(id, 1, "Échéance 1", 30, IGNOREE, CINQUANTE));

        Map<String, Object> enBase = enBase(id);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(jour(enBase.get(ECHEANCE_DATE))).isEqualTo(LocalDate.of(2026, 7, 30));
            s.assertThat(enBase).containsEntry(DELAY, 30);
            s.assertThat(r.valeur(DATE_DEF)).isEqualTo("2026-06-30");
            s.assertThat(echeanceLue(r, id)).isEqualTo("[\"2026-07-30\"]");
        });
    }

    @Test
    void r11_echeanceSeule_delaiCalculeDepuisLaReceptionDefinitive() throws Exception {
        UUID lot = lot();
        reception(lot, JUIN, definitif);
        UUID id = UUID.randomUUID();

        Reponse r = enregistrer(lot, echeance(id, 1, "Échéance 1", null, LocalDate.of(2026, 8, 29), CINQUANTE));

        Map<String, Object> enBase = enBase(id);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase).containsEntry(DELAY, 60);
            s.assertThat(jour(enBase.get(ECHEANCE_DATE))).isEqualTo(LocalDate.of(2026, 8, 29));
        });
    }

    // R10 : aucune réception DEF (une PRO seulement) → repli sur PrjMarket.EndDate.
    @Test
    void r10_sansReceptionDefinitive_repliSurLaFinDuLot() throws Exception {
        UUID lot = lot();
        av.finDuLot(lot, SEPTEMBRE);
        reception(lot, AOUT, provisoire);
        UUID id = UUID.randomUUID();

        Reponse r = enregistrer(lot, echeance(id, 1, "Échéance 1", 10, null, CINQUANTE));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(jour(enBase(id).get(ECHEANCE_DATE))).isEqualTo(LocalDate.of(2026, 9, 11));
            s.assertThat(r.valeur(DATE_DEF)).isEqualTo("2026-09-01");
        });
    }

    // R11 : ni réception DEF ni fin du lot → délai et échéance stockés tels qu'envoyés ; date R10 nulle dans le DTO.
    @Test
    void r11_sansDateDeBase_delaiEtEcheanceStockesTelsQuEnvoyes() throws Exception {
        UUID lot = lot();
        av.finDuLot(lot, null);
        UUID id = UUID.randomUUID();

        Reponse r = enregistrer(lot, echeance(id, 1, "Échéance 1", 15, LocalDate.of(2026, 10, 10), CINQUANTE));

        Map<String, Object> enBase = enBase(id);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase).containsEntry(DELAY, 15);
            s.assertThat(jour(enBase.get(ECHEANCE_DATE))).isEqualTo(LocalDate.of(2026, 10, 10));
            s.assertThat(r.valeur(DATE_DEF)).isNull();
        });
    }

    // R10 : la même date dans la lecture GET /remboursements.
    @Test
    void r10_lectureRemboursements_dateReceptionDefinitiveEtTotal() throws Exception {
        UUID lot = lot();
        av.finDuLot(lot, SEPTEMBRE);
        reception(lot, JUIN, definitif);
        sf.echeanceRg(lot, 1, "Échéance 1", 30, LocalDate.of(2026, 7, 30), "0.6");
        sf.echeanceRg(lot, 2, "Échéance 2", 60, LocalDate.of(2026, 8, 29), "0.3");

        Reponse r = appels.lire(admin, lot, REMBOURSEMENTS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.valeur(DATE_DEF)).as(r.corps()).isEqualTo("2026-06-30");
            s.assertThat(r.nombre("$.totalPourcentageEcheances")).isEqualByComparingTo("90");
        });
    }

    // ------------------------------------------------------------------ R9 : Σ % libre ; stockage en ratio

    @Test
    void r9_sommeA130Pourcent_acceptee_totalRenvoye_ratiosStockes() throws Exception {
        UUID lot = lot();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();

        Reponse r = enregistrer(lot, echeance(e1, 1, "Échéance 1", 30, null, "80"), echeance(e2, 2, "Échéance 2", 60, null, CINQUANTE));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.nombre("$.totalPourcentageEcheances")).isEqualByComparingTo("130");
            s.assertThat(new BigDecimal(String.valueOf(enBase(e1).get(RATIO)))).isEqualByComparingTo("0.8");
            s.assertThat(new BigDecimal(String.valueOf(enBase(e2).get(RATIO)))).isEqualByComparingTo("0.5");
            s.assertThat(r.ids("$.echeances[*].id")).containsExactlyInAnyOrder(e1.toString(), e2.toString());
        });
    }

    // ------------------------------------------------------------------ §3 / §4.2 : envoi entier

    @Test
    void listeVide_toutesLesEcheancesDuLotSupprimees_autreLotIntact() throws Exception {
        UUID lot = lot();
        sf.echeanceRg(lot, 1, "Échéance 1", 30, null, "0.5");
        sf.echeanceRg(lot, 2, "Échéance 2", 60, null, "0.5");
        UUID autreLot = lot();
        UUID autre = sf.echeanceRg(autreLot, 1, "Échéance 1", 30, null, "0.5");

        Reponse r = enregistrer(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(av.compter("SELECT COUNT(*) FROM PrjMarketWarrantyLine WHERE PrjMarketId = ?", lot.toString())).isZero();
            s.assertThat(av.parId(ECHEANCES, autre)).hasSize(1);
        });
    }

    @Test
    void envoiEntier_insertionModificationSuppression_unSeulEnvoi() throws Exception {
        UUID lot = lot();
        UUID gardee = sf.echeanceRg(lot, 1, "Échéance 1", 30, null, "0.5");
        UUID retiree = sf.echeanceRg(lot, 2, "Échéance 2", 60, null, "0.5");
        UUID nouvelle = UUID.randomUUID();

        Reponse r = enregistrer(lot, echeance(gardee, 1, "Première", 45, null, "70"), echeance(nouvelle, 2, "Seconde", 90, null, "30"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase(gardee)).containsEntry(DESCRIPTION_COL, "Première").containsEntry(DELAY, 45);
            s.assertThat(av.parId(ECHEANCES, retiree)).isEmpty();
            s.assertThat(String.valueOf(enBase(nouvelle).get("PrjMarketId"))).isEqualToIgnoringCase(lot.toString());
        });
    }

    // ------------------------------------------------------------------ R8 : 400

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ordreManquant", "pourcentageManquant", "pourcentageAuDessusDe100", "pourcentageNegatif", "delaiNegatif",
            "descriptionTropLongue", "idEnDouble", "idDUnAutreLot"})
    void r8_corpsInvalide_400_grilleInchangee(String cas) throws Exception {
        UUID lot = lot();
        UUID existante = sf.echeanceRg(lot, 1, "Échéance 1", 30, null, "0.5");
        CorpsJson e = echeance(existante, 1, "Échéance 1", 30, null, CINQUANTE);
        List<CorpsJson> grille = new ArrayList<>(List.of(e));
        switch (cas) {
            case "ordreManquant" -> e.avec(ORDRE, null);
            case "pourcentageManquant" -> e.avec(POURCENTAGE, null);
            case "pourcentageAuDessusDe100" -> e.avec(POURCENTAGE, decimal("100.01"));
            case "pourcentageNegatif" -> e.avec(POURCENTAGE, decimal("-1"));
            case "delaiNegatif" -> e.avec(DELAI, -1);
            case "descriptionTropLongue" -> e.avec(DESCRIPTION, "D".repeat(256));
            case "idEnDouble" -> grille.add(echeance(existante, 2, "Échéance 2", 60, null, CINQUANTE));
            case "idDUnAutreLot" -> grille.add(echeance(sf.echeanceRg(lot(), 1, "Autre", 30, null, "0.5"), 2, "Autre", 60, null, CINQUANTE));
            default -> throw new IllegalArgumentException(cas);
        }

        Reponse r = appels.enregistrerEcheances(admin, lot, grille);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(cas + " : " + r.corps()).isEqualTo(400);
            s.assertThat(av.compter("SELECT COUNT(*) FROM PrjMarketWarrantyLine WHERE PrjMarketId = ?", lot.toString())).isEqualTo(1);
            s.assertThat(enBase(existante)).containsEntry(DELAY, 30);
        });
    }

    // R8 (témoin) : la grille de base des cas invalides est acceptée.
    @Test
    void r8_temoin_grilleDeBase_200() throws Exception {
        UUID lot = lot();
        UUID existante = sf.echeanceRg(lot, 1, "Échéance 1", 30, null, "0.5");

        Reponse r = enregistrer(lot, echeance(existante, 1, "Échéance 1", 30, null, CINQUANTE));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ ordre 1 : lot invisible ; aucun droit propre

    @Test
    void ordre1_lotInvisible_404_lotVisibleSansDroit_200() throws Exception {
        UUID autreProjet = parite.projet("ERX-" + jeton, parite.entiteJuridique("ERY-" + jeton));
        UUID invisible = parite.lot(autreProjet, "ERH-" + jeton, ENTREPRENEURS, statut);
        UUID existante = sf.echeanceRg(invisible, 1, "Échéance 1", 30, null, "0.5");
        UUID visible = lot();
        RequestPostProcessor sansDroit = connecte(parite.avecDroits("lot.er1." + jeton, List.of(), projet), "Commercial");

        Reponse temoin = appels.enregistrerEcheances(sansDroit, visible, List.of(echeance(UUID.randomUUID(), 1, "E", 30, null, CINQUANTE)));
        Reponse r = appels.enregistrerEcheances(sansDroit, invisible, List.of());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
            s.assertThat(av.parId(ECHEANCES, existante)).hasSize(1);
        });
    }

    // ------------------------------------------------------------------ R12 : recalcul

    @Test
    void r12_enregistrement_recalculeLesPrevisions() throws Exception {
        prev.preparer();
        UUID lot = lot();
        prev.planning(lot, PrevisionsLotSupport.DEBUT_2026, PrevisionsLotSupport.DUREE_90, PrevisionsLotSupport.DEBUT_2026,
                PrevisionsLotSupport.DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0.1", "0");
        prev.multiFournisseur(lot, false);
        prev.lignePerimee(lot);

        Reponse r = enregistrer(lot, echeance(UUID.randomUUID(), 1, "Échéance 1", 30, null, "100"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(prev.lignesPaiement(lot)).as("ligne périmée effacée par le recalcul")
                    .extracting(l -> String.valueOf(l.get(DESCRIPTION_COL))).doesNotContain(LIGNE_PERIMEE);
        });
    }

    // ------------------------------------------------------------------ R17 : audit

    @Test
    void r17_insertionEtModification_colonnesDAudit() throws Exception {
        UUID lot = lot();
        UUID modifiee = sf.echeanceRg(lot, 1, "Échéance 1", 30, null, "0.5");
        UUID inseree = UUID.randomUUID();

        Reponse r = enregistrer(lot, echeance(modifiee, 1, "Modifiée", 30, null, CINQUANTE), echeance(inseree, 2, "Neuve", 60, null, CINQUANTE));

        Map<String, Object> m = enBase(modifiee);
        Map<String, Object> i = enBase(inseree);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(String.valueOf(i.get("UserCreate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(i.get("UserCreateDateTime")).isNotNull();
            s.assertThat(String.valueOf(m.get("UserUpdate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(m.get("UserUpdateDateTime")).isNotNull();
        });
    }
}
