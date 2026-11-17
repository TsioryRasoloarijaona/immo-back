package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
import com.tsix.immocrm.fixtures.FixtureLotAvenants.Avenant;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
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

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ASSURANCE_DEC;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.AVENANTS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.CATEGORIE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.CODE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DATE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DESIGNATION;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.FODEC;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.GARANTIE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.LIGNE_BORDEREAU;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.NOTES;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ORDRE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.PRORATA;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.PU_HT;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.QUANTITE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.REFERENCE_INTERNE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.TVA;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.UNITE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.avenant;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.decimal;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ligne;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.retenue;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.sansRetenue;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.GROUPES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotAvenants.LIGNES_AVENANT;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.LIGNE_PERIMEE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Écritures d'avenant par un Administrateur (FEATURE-lot-avenants-avances.md §3, §4.1, R2-R7, R17-R19, §9) : création séquencée,
 * validation, montants, retenues, lignes envoyées entières, avenant facturé figé, suppression, recalcul des prévisions, audit,
 * garde du bordereau définitif. Montants calculés à la main (aucun arrondi, quantité à 6 décimales).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotAvenantsEcritureTest {

    private static final String MSG_FACTURE_SUPPRESSION = "Vous ne pouvez pas supprimer un avenant avec une facture associée.";
    private static final String MSG_FACTURE_MODIFICATION = "Avenant facturé : lignes et retenues non modifiables";
    private static final String MSG_LIGNE_BORDEREAU = "Vous ne pouvez pas supprimer cette ligne";
    private static final String MSG_GROUPE_BORDEREAU = "Vous ne pouvez pas supprimer ce groupe";
    private static final String CLE = "Pkey";
    private static final String AMOUNT_HT = "AmountHT";
    private static final String AMOUNT_TTC = "AmountTTC";
    private static final String AMOUNT_TO_PAY = "AmountToPay";
    private static final String QUANTITY = "Quantity";
    private static final String DESCRIPTION_COL = "Description";
    private static final String USER_CREATE = "UserCreate";
    private static final String USER_UPDATE = "UserUpdate";
    private static final String CENT = "100";
    private static final String VINGT = "20";
    private static final String ZERO = "0";
    private static final LocalDate MARS = LocalDate.of(2026, 3, 5);
    private static final LocalDate AVRIL = LocalDate.of(2026, 4, 10);
    private static final BigDecimal MICRO = new BigDecimal("0.000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureBordereauxEcriture b;
    @Autowired private FixtureLotAvenants av;
    @Autowired private FixtureLotPrevisions prev;

    private AvenantsLotSupport appels;
    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID statut;
    private UUID categorie;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        av.tables();
        appels = new AvenantsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        av.sequenceAvenant(41);
        societe = parite.entiteJuridique("AWE-" + jeton);
        projet = parite.projet("AWP-" + jeton, societe);
        statut = parite.statut(ENTREPRENEURS, "AWS-" + jeton);
        categorie = b.categorie("AWC-" + jeton);
        admin = connecte(parite.administrateur("lot.aw." + jeton, projet), "Administrateur");
    }

    private UUID lot() {
        rang++;
        return parite.lot(projet, "AW" + rang + "-" + jeton, ENTREPRENEURS, statut);
    }

    /** Lot paramétré pour le recalcul des prévisions (comme {@code LotPrevisionsRecalculTest}), porteur d'une ligne périmée. */
    private UUID lotAvecPrevisions() {
        prev.preparer();
        UUID lot = lot();
        prev.planning(lot, PrevisionsLotSupport.DEBUT_2026, PrevisionsLotSupport.DUREE_90, PrevisionsLotSupport.DEBUT_2026,
                PrevisionsLotSupport.DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, ZERO, ZERO, ZERO);
        prev.multiFournisseur(lot, false);
        prev.lignePerimee(lot);
        return lot;
    }

    private UUID avenantEnBase(UUID lot, int ordre) {
        rang++;
        return av.avenant(new Avenant(lot, ordre, "AWA" + rang + "-" + jeton, MARS, "200", "40", "240", "216", "0.1", "24"));
    }

    private Reponse creer(UUID lot, CorpsJson corps) throws Exception {
        return appels.creer(admin, lot, AVENANTS, corps);
    }

    private Reponse cree(UUID lot, CorpsJson corps) throws Exception {
        Reponse r = creer(lot, corps);
        assertThat(r.statut()).as("POST : " + r.corps()).isEqualTo(201);
        return r;
    }

    private Reponse modifier(UUID lot, UUID avenant, CorpsJson corps) throws Exception {
        return appels.modifier(admin, lot, AVENANTS, avenant, corps);
    }

    private Map<String, Object> enBase(String table, UUID id) {
        List<Map<String, Object>> l = av.parId(table, id);
        assertThat(l).as(table + " " + id).hasSize(1);
        return l.get(0);
    }

    private static BigDecimal nombre(Object valeur) {
        return new BigDecimal(String.valueOf(valeur));
    }

    private static LocalDate jour(Object valeur) {
        return ((Timestamp) valeur).toLocalDateTime().toLocalDate();
    }

    private static UUID id(Reponse r) {
        return UUID.fromString(String.valueOf(r.valeur("$.id")));
    }

    /** Champ d'une ligne de la réponse, retrouvée par son id. */
    private static Object champLigne(Reponse r, UUID ligne, String champ) {
        List<?> valeurs = (List<?>) r.valeur("$.lignes[?(@.id == '" + ligne + "')]." + champ);
        assertThat(valeurs).as("ligne " + ligne + " : " + r.corps()).hasSize(1);
        return valeurs.get(0);
    }

    /** L1 : 2 × 100 HT, TVA 20 %, FODEC 0 ; L2 : 3 × 50 HT, TVA 10 %, FODEC 1 %. */
    private CorpsJson deuxLignes(UUID l1, UUID l2) {
        return avenant(MARS, List.of(
                ligne(l1, 1, "1", "2", CENT, VINGT, ZERO, categorie),
                ligne(l2, 2, "2", "3", "50", "10", "1", categorie)));
    }

    // ------------------------------------------------------------------ R2 : création

    @Test
    void r2_creation_codeSequence_ordreNPlus1_fournisseurDuLot_description_dateEtLot() throws Exception {
        UUID lot = lot();
        UUID fournisseur = lots.fournisseur("AWF-" + jeton);
        b.fournisseur(lot, fournisseur);
        avenantEnBase(lot, 1);

        Reponse r = cree(lot, deuxLignes(UUID.randomUUID(), UUID.randomUUID()));

        String code = String.valueOf(r.valeur("$.code"));
        Map<String, Object> enBase = enBase(FixtureLotAvenants.AVENANTS, id(r));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(code).as(r.corps()).matches("\\d{2}/00042");
            s.assertThat(enBase).containsEntry("Code", code).containsEntry("Sorting", 2)
                    .containsEntry(DESCRIPTION_COL, "Avenant N° " + code + " du 05/03/2026").containsEntry("DocRef", "REF");
            s.assertThat(String.valueOf(enBase.get("PrjMarketId"))).isEqualToIgnoringCase(lot.toString());
            s.assertThat(String.valueOf(enBase.get("CfgTierId"))).isEqualToIgnoringCase(fournisseur.toString());
            s.assertThat(jour(enBase.get("DocDate"))).isEqualTo(MARS);
            s.assertThat(r.valeur("$.lotId")).isEqualTo(lot.toString());
            s.assertThat(r.valeur("$.ordre")).isEqualTo(2);
            s.assertThat(r.valeur("$.description")).isEqualTo("Avenant N° " + code + " du 05/03/2026");
            s.assertThat(r.valeur("$.date")).isEqualTo("2026-03-05");
            s.assertThat(r.valeur("$.referenceInterne")).isEqualTo("REF");
            s.assertThat(String.valueOf(r.valeur("$.fournisseur"))).isNotBlank().isNotEqualTo("null");
        });
    }

    // R2 : deux créations → deux codes distincts, suivants de la séquence « Avenant ».
    @Test
    void r2_deuxCreations_codesUniquesSuivants() throws Exception {
        UUID lot = lot();

        Reponse premier = cree(lot, deuxLignes(UUID.randomUUID(), UUID.randomUUID()));
        Reponse second = cree(lot, deuxLignes(UUID.randomUUID(), UUID.randomUUID()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(premier.valeur("$.code"))).matches("\\d{2}/00042");
            s.assertThat(String.valueOf(second.valeur("$.code"))).matches("\\d{2}/00043");
            s.assertThat(second.valeur("$.ordre")).isEqualTo(2);
        });
    }

    @Test
    void r2_lotSansFournisseur_201_tiersNul() throws Exception {
        UUID lot = lot();

        Reponse r = cree(lot, deuxLignes(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(enBase(FixtureLotAvenants.AVENANTS, id(r)).get("CfgTierId")).isNull();
    }

    // R2 : description recalculée à chaque écriture tant que l'avenant n'est pas facturé.
    @Test
    void r2_modificationDeLaDate_descriptionRecalculee() throws Exception {
        UUID lot = lot();
        UUID cree = id(cree(lot, deuxLignes(UUID.randomUUID(), UUID.randomUUID())));
        String code = String.valueOf(enBase(FixtureLotAvenants.AVENANTS, cree).get("Code"));

        Reponse r = modifier(lot, cree, avenant(AVRIL, List.of(ligne(UUID.randomUUID(), 1, "1", "1", CENT, VINGT, ZERO, categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase(FixtureLotAvenants.AVENANTS, cree)).containsEntry(DESCRIPTION_COL, "Avenant N° " + code + " du 10/04/2026")
                    .containsEntry("Code", code);
        });
    }

    // ------------------------------------------------------------------ R3 : corps invalide → 400

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"dateManquante", "designationManquante", "ordreLigneManquant", "categorieManquante",
            "categorieInconnue", "quantiteNulle", "quantiteNegative", "prixNul", "prixNegatif", "tvaAuDessusDe100", "tvaNegative",
            "fodecAuDessusDe100", "fodecNegatif", "ligneBordereauDUnAutreLot", "ligneBordereauDeStock", "ligneBordereauEstimative",
            "ligneBordereauInconnue", "tauxRetenueAuDessusDe100", "tauxRetenueNegatif", "montantRetenueNegatif", "codeTropLong",
            "designationTropLongue", "uniteTropLongue", "notesLigneTropLongues", "notesTropLongues", "referenceInterneTropLongue",
            "idLigneEnDouble"})
    void r3_corpsInvalide_400_rienNEstCree(String cas) throws Exception {
        UUID lot = lot();
        UUID idLigne = UUID.randomUUID();
        CorpsJson l = ligne(idLigne, 1, "1", "2", CENT, VINGT, ZERO, categorie);
        List<CorpsJson> lignes = new ArrayList<>(List.of(l));
        CorpsJson corps = avenant(MARS, lignes);
        switch (cas) {
            case "dateManquante" -> corps.avec(DATE, null);
            case "designationManquante" -> l.avec(DESIGNATION, null);
            case "ordreLigneManquant" -> l.avec(ORDRE, null);
            case "categorieManquante" -> l.avec(CATEGORIE, null);
            case "categorieInconnue" -> l.avec(CATEGORIE, UUID.randomUUID());
            case "quantiteNulle" -> l.avec(QUANTITE, null);
            case "quantiteNegative" -> l.avec(QUANTITE, decimal("-1"));
            case "prixNul" -> l.avec(PU_HT, null);
            case "prixNegatif" -> l.avec(PU_HT, decimal("-0.01"));
            case "tvaAuDessusDe100" -> l.avec(TVA, decimal("100.01"));
            case "tvaNegative" -> l.avec(TVA, decimal("-1"));
            case "fodecAuDessusDe100" -> l.avec(FODEC, decimal("100.01"));
            case "fodecNegatif" -> l.avec(FODEC, decimal("-1"));
            case "ligneBordereauDUnAutreLot" -> l.avec(LIGNE_BORDEREAU, definitive(lot(), false));
            case "ligneBordereauDeStock" -> l.avec(LIGNE_BORDEREAU, definitive(lot, true));
            case "ligneBordereauEstimative" -> l.avec(LIGNE_BORDEREAU,
                    b.ligne(LIGNES_ESTIMATIF, new FixtureBordereauxEcriture.Ligne(lot, null, 1, "E", "1", CENT, categorie, false)));
            case "ligneBordereauInconnue" -> l.avec(LIGNE_BORDEREAU, UUID.randomUUID());
            case "tauxRetenueAuDessusDe100" -> corps.avec(GARANTIE, retenue(false, "100.01", null));
            case "tauxRetenueNegatif" -> corps.avec(PRORATA, retenue(false, "-1", null));
            case "montantRetenueNegatif" -> corps.avec(ASSURANCE_DEC, retenue(true, ZERO, "-1"));
            case "codeTropLong" -> l.avec(CODE, "C".repeat(256));
            case "designationTropLongue" -> l.avec(DESIGNATION, "D".repeat(501));
            case "uniteTropLongue" -> l.avec(UNITE, "U".repeat(256));
            case "notesLigneTropLongues" -> l.avec(NOTES, "N".repeat(1001));
            case "notesTropLongues" -> corps.avec(NOTES, "N".repeat(1001));
            case "referenceInterneTropLongue" -> corps.avec(REFERENCE_INTERNE, "R".repeat(256));
            case "idLigneEnDouble" -> lignes.add(ligne(idLigne, 2, "2", "1", "10", VINGT, ZERO, categorie));
            default -> throw new IllegalArgumentException(cas);
        }
        corps.avec(AvenantsLotSupport.LIGNES, lignes);

        Reponse r = creer(lot, corps);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(cas + " : " + r.corps()).isEqualTo(400);
            s.assertThat(av.compter("SELECT COUNT(*) FROM PrjClause WHERE PrjMarketId = ?", lot.toString())).isZero();
        });
    }

    // R3 (témoin) : le corps de base des cas invalides est accepté.
    @Test
    void r3_temoin_corpsDeBase_201() throws Exception {
        Reponse r = creer(lot(), avenant(MARS, List.of(ligne(UUID.randomUUID(), 1, "1", "2", CENT, VINGT, ZERO, categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    // R3 : ligne définitive (hors stock) de ce lot acceptée et stockée dans PrjMarketLineId.
    @Test
    void r3_ligneBordereauDefinitiveDuLot_201_stockee() throws Exception {
        UUID lot = lot();
        UUID definitive = definitive(lot, false);
        UUID l = UUID.randomUUID();

        Reponse r = cree(lot, avenant(MARS, List.of(ligne(l, 1, "1", "2", CENT, VINGT, ZERO, categorie).avec(LIGNE_BORDEREAU, definitive))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(enBase(LIGNES_AVENANT, l).get("PrjMarketLineId"))).isEqualToIgnoringCase(definitive.toString());
            s.assertThat(champLigne(r, l, LIGNE_BORDEREAU)).isEqualTo(definitive.toString());
        });
    }

    private UUID definitive(UUID lot, boolean stock) {
        return b.ligne(LIGNES_DEFINITIF, new FixtureBordereauxEcriture.Ligne(lot, null, 1, "D" + (++rang), "1", CENT, categorie, stock));
    }

    // ------------------------------------------------------------------ R5 : montants (calcul unique, écriture et lecture)

    // 2 lignes à 2 TVA : HT 200 + 151,5 = 351,5 ; TTC 240 + 166,65 = 406,65 ; TVA 55,15 ; sans retenue, net = TTC.
    @Test
    void r5_deuxLignesDeuxTVA_montantsExacts_reponseBaseEtLecture() throws Exception {
        UUID lot = lot();
        UUID l1 = UUID.randomUUID();
        UUID l2 = UUID.randomUUID();

        Reponse r = cree(lot, deuxLignes(l1, l2));

        UUID avenant = id(r);
        Map<String, Object> enBase = enBase(FixtureLotAvenants.AVENANTS, avenant);
        Map<String, Object> ligne2 = enBase(LIGNES_AVENANT, l2);
        Reponse lecture = appels.lire(admin, lot, AVENANTS, avenant);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.montantHT")).as(r.corps()).isEqualByComparingTo("351.5");
            s.assertThat(r.nombre("$.montantTVA")).isEqualByComparingTo("55.15");
            s.assertThat(r.nombre("$.montantTTC")).isEqualByComparingTo("406.65");
            s.assertThat(r.nombre("$.netAPayer")).isEqualByComparingTo("406.65");
            s.assertThat(nombre(champLigne(r, l1, "prixUnitaireTTC"))).isEqualByComparingTo("120");
            s.assertThat(nombre(champLigne(r, l1, "totalHT"))).isEqualByComparingTo("200");
            s.assertThat(nombre(champLigne(r, l1, "totalTTC"))).isEqualByComparingTo("240");
            s.assertThat(nombre(champLigne(r, l2, "prixUnitaireTTC"))).isEqualByComparingTo("55.55");
            s.assertThat(nombre(champLigne(r, l2, "totalHT"))).isEqualByComparingTo("151.5");
            s.assertThat(nombre(champLigne(r, l2, "totalTTC"))).isEqualByComparingTo("166.65");
            s.assertThat(nombre(champLigne(r, l2, TVA))).isEqualByComparingTo("10");
            s.assertThat(nombre(champLigne(r, l2, FODEC))).isEqualByComparingTo("1");
            s.assertThat(nombre(enBase.get(AMOUNT_HT))).isEqualByComparingTo("351.5");
            s.assertThat(nombre(enBase.get("AmountVat"))).isEqualByComparingTo("55.15");
            s.assertThat(nombre(enBase.get(AMOUNT_TTC))).isEqualByComparingTo("406.65");
            s.assertThat(nombre(enBase.get(AMOUNT_TO_PAY))).isEqualByComparingTo("406.65");
            s.assertThat(nombre(ligne2.get("VatRatio"))).as("TVA stockée en ratio").isEqualByComparingTo("0.1");
            s.assertThat(nombre(ligne2.get("FODECRatio"))).as("FODEC stocké en ratio").isEqualByComparingTo("0.01");
            s.assertThat(nombre(ligne2.get("UnitPriceTTC"))).isEqualByComparingTo("55.55");
            s.assertThat(String.valueOf(ligne2.get("PrjClauseId"))).isEqualToIgnoringCase(avenant.toString());
            s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(200);
            s.assertThat(lecture.nombre("$.montantHT")).isEqualByComparingTo("351.5");
            s.assertThat(lecture.nombre("$.montantTTC")).isEqualByComparingTo("406.65");
            s.assertThat(lecture.nombre("$.netAPayer")).isEqualByComparingTo("406.65");
            s.assertThat(lecture.valeur("$.facture")).isEqualTo(false);
        });
    }

    /*
     * R4 + R5 : TTC 406,65 ; garantie 10 % au taux = 40,665 ; prorata par montant 20 (taux = 20 / 406,65) ; DEC 1 % au taux = 4,0665 ;
     * TRC 0 → net = 406,65 − 40,665 − 20 − 4,0665 = 341,9185. Les deux colonnes (ratio, montant) stockées cohérentes.
     */
    @Test
    void r4_r5_retenuesAuTauxEtParMontant_netAPayerExact_colonnesCoherentes() throws Exception {
        UUID lot = lot();
        CorpsJson corps = deuxLignes(UUID.randomUUID(), UUID.randomUUID())
                .avec(GARANTIE, retenue(false, "10", null))
                .avec(PRORATA, retenue(true, ZERO, VINGT))
                .avec(ASSURANCE_DEC, retenue(false, "1", null))
                .avec(AvenantsLotSupport.ASSURANCE_TRC, sansRetenue());

        Reponse r = cree(lot, corps);

        Map<String, Object> enBase = enBase(FixtureLotAvenants.AVENANTS, id(r));
        BigDecimal ratioProrata = new BigDecimal("20").divide(new BigDecimal("406.65"), 10, java.math.RoundingMode.HALF_UP);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.netAPayer")).as(r.corps()).isEqualByComparingTo("341.9185");
            s.assertThat(r.nombre("$.garantie.montant")).isEqualByComparingTo("40.665");
            s.assertThat(r.valeur("$.prorata.parMontant")).isEqualTo(true);
            s.assertThat(r.nombre("$.prorata.taux")).isCloseTo(ratioProrata.movePointRight(2), within(new BigDecimal("0.0001")));
            s.assertThat(r.nombre("$.assuranceDEC.montant")).isEqualByComparingTo("4.0665");
            s.assertThat(nombre(enBase.get(AMOUNT_TO_PAY))).isEqualByComparingTo("341.9185");
            s.assertThat(nombre(enBase.get("WarrantyRatio"))).isEqualByComparingTo("0.1");
            s.assertThat(nombre(enBase.get("WarrantyAmount"))).isEqualByComparingTo("40.665");
            s.assertThat(enBase.get("WarrantyBasedOnAmount")).isEqualTo(false);
            s.assertThat(enBase.get("ProrataBasedOnAmount")).isEqualTo(true);
            s.assertThat(nombre(enBase.get("ProportionAmount"))).isEqualByComparingTo(VINGT);
            s.assertThat(nombre(enBase.get("ProportionRatio"))).isCloseTo(ratioProrata, within(MICRO));
            s.assertThat(nombre(enBase.get("InsuranceDECRatio"))).isEqualByComparingTo("0.01");
            s.assertThat(nombre(enBase.get("InsuranceDECAmount"))).isEqualByComparingTo("4.0665");
            s.assertThat(nombre(enBase.get("InsuranceTRCAmount"))).isEqualByComparingTo(ZERO);
        });
    }

    // R5 : quantité stockée à 6 décimales.
    @Test
    void r5_quantiteA6Decimales() throws Exception {
        UUID l = UUID.randomUUID();

        cree(lot(), avenant(MARS, List.of(ligne(l, 1, "1", "1.23456789", CENT, VINGT, ZERO, categorie))));

        assertThat(nombre(enBase(LIGNES_AVENANT, l).get(QUANTITY))).isEqualByComparingTo("1.234568");
    }

    // ------------------------------------------------------------------ §3 : lignes envoyées entières (PUT)

    @Test
    void put_insertionModificationSuppressionDeLignes_unSeulEnvoi_montantsRecalcules() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot, 1);
        UUID gardee = av.ligneAvenant(a, 1, "1", "2", CENT, "0.2", categorie, null);
        UUID retiree = av.ligneAvenant(a, 2, "2", "1", "50", "0.2", categorie, null);
        UUID nouvelle = UUID.randomUUID();

        Reponse r = modifier(lot, a, avenant(MARS, List.of(
                ligne(gardee, 1, "1", "5", CENT, VINGT, ZERO, categorie),
                ligne(nouvelle, 2, "3", "1", "10", VINGT, ZERO, categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.ids("$.lignes[*].id")).containsExactlyInAnyOrder(gardee.toString(), nouvelle.toString());
            s.assertThat(nombre(enBase(LIGNES_AVENANT, gardee).get(QUANTITY))).isEqualByComparingTo("5");
            s.assertThat(av.parId(LIGNES_AVENANT, retiree)).as("ligne absente de l'envoi supprimée").isEmpty();
            s.assertThat(String.valueOf(enBase(LIGNES_AVENANT, nouvelle).get("PrjClauseId"))).isEqualToIgnoringCase(a.toString());
            s.assertThat(nombre(enBase(FixtureLotAvenants.AVENANTS, a).get(AMOUNT_HT))).isEqualByComparingTo("510");
            s.assertThat(nombre(enBase(FixtureLotAvenants.AVENANTS, a).get(AMOUNT_TTC))).isEqualByComparingTo("612");
        });
    }

    // §3 : id de ligne d'un autre avenant du même lot → 400, ligne étrangère intacte.
    @Test
    void put_idLigneDUnAutreAvenant_400() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot, 1);
        UUID autre = avenantEnBase(lot, 2);
        UUID etrangere = av.ligneAvenant(autre, 1, "1", "2", CENT, "0.2", categorie, null);

        Reponse r = modifier(lot, a, avenant(MARS, List.of(ligne(etrangere, 1, "1", "9", CENT, VINGT, ZERO, categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            Map<String, Object> l = enBase(LIGNES_AVENANT, etrangere);
            s.assertThat(nombre(l.get(QUANTITY))).isEqualByComparingTo("2");
            s.assertThat(String.valueOf(l.get("PrjClauseId"))).isEqualToIgnoringCase(autre.toString());
        });
    }

    @Test
    void put_idLigneDUnAvenantDUnAutreLot_400() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot, 1);
        UUID etrangere = av.ligneAvenant(avenantEnBase(lot(), 1), 1, "1", "2", CENT, "0.2", categorie, null);

        Reponse r = modifier(lot, a, avenant(MARS, List.of(ligne(etrangere, 1, "1", "9", CENT, VINGT, ZERO, categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void put_categorieManquante_400() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot, 1);

        Reponse r = modifier(lot, a, avenant(MARS, List.of(ligne(UUID.randomUUID(), 1, "1", "1", CENT, VINGT, ZERO, null))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void put_ligneBordereauIdDUnAutreLot_400() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot, 1);
        CorpsJson l = ligne(UUID.randomUUID(), 1, "1", "1", CENT, VINGT, ZERO, categorie).avec(LIGNE_BORDEREAU, definitive(lot(), false));

        Reponse r = modifier(lot, a, avenant(MARS, List.of(l)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R5 / R6 : avenant facturé

    /** Avenant facturé : une ligne 2 × 100 HT TVA 20 %, montants stockés volontairement différents du calcul (figés). */
    private record Facture(UUID lot, UUID avenant, UUID ligne, String code) {
    }

    private Facture avenantFacture() {
        UUID lot = lot();
        rang++;
        String code = "AWZ" + rang + "-" + jeton;
        UUID a = av.avenant(new Avenant(lot, 1, code, MARS, "999", "1", "1000", "900", "0.1", CENT));
        UUID l = av.ligneAvenant(a, 1, "1", "2", CENT, "0.2", categorie, null);
        av.facturer(a, lot, projet, societe, "AWF" + rang + "-" + jeton);
        return new Facture(lot, a, l, code);
    }

    /** Corps identique à l'avenant facturé en base (lignes et retenues), date / référence / notes au choix. */
    private CorpsJson memeContenu(Facture f, LocalDate date, String reference, String notes) {
        return avenant(date, List.of(ligne(f.ligne(), 1, "1", "2", CENT, VINGT, ZERO, categorie)))
                .avec(GARANTIE, retenue(false, "10", null)).avec(REFERENCE_INTERNE, reference).avec(NOTES, notes);
    }

    @Test
    void r6_suppressionAvenantFacture_409_messageExact_rienNEstSupprime() throws Exception {
        Facture f = avenantFacture();

        Reponse r = appels.supprimer(admin, f.lot(), AVENANTS, f.avenant());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_FACTURE_SUPPRESSION);
            s.assertThat(av.parId(FixtureLotAvenants.AVENANTS, f.avenant())).hasSize(1);
            s.assertThat(av.parId(LIGNES_AVENANT, f.ligne())).hasSize(1);
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"quantiteChangee", "prixChange", "ligneAjoutee", "ligneRetiree", "garantieChangee", "prorataParMontant"})
    void r5_avenantFacture_lignesOuRetenuesChangees_409_rienNEstEcrit(String cas) throws Exception {
        Facture f = avenantFacture();
        CorpsJson corps = memeContenu(f, MARS, "REF", "Notes");
        CorpsJson l = ligne(f.ligne(), 1, "1", "2", CENT, VINGT, ZERO, categorie);
        List<CorpsJson> lignes = new ArrayList<>(List.of(l));
        switch (cas) {
            case "quantiteChangee" -> l.avec(QUANTITE, decimal("3"));
            case "prixChange" -> l.avec(PU_HT, decimal("101"));
            case "ligneAjoutee" -> lignes.add(ligne(UUID.randomUUID(), 2, "2", "1", "10", VINGT, ZERO, categorie));
            case "ligneRetiree" -> lignes.clear();
            case "garantieChangee" -> corps.avec(GARANTIE, retenue(false, "5", null));
            case "prorataParMontant" -> corps.avec(PRORATA, retenue(true, ZERO, "5"));
            default -> throw new IllegalArgumentException(cas);
        }
        corps.avec(AvenantsLotSupport.LIGNES, lignes);

        Reponse r = modifier(f.lot(), f.avenant(), corps);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(cas + " : " + r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_FACTURE_MODIFICATION);
            s.assertThat(nombre(enBase(LIGNES_AVENANT, f.ligne()).get(QUANTITY))).isEqualByComparingTo("2");
            s.assertThat(nombre(enBase(FixtureLotAvenants.AVENANTS, f.avenant()).get("WarrantyRatio"))).isEqualByComparingTo("0.1");
        });
    }

    // R5 : date, référence interne et notes restent modifiables ; montants figés (lus en base), description non recalculée.
    @Test
    void r5_avenantFacture_dateReferenceNotesSeules_200_montantsEtDescriptionInchanges() throws Exception {
        Facture f = avenantFacture();

        Reponse r = modifier(f.lot(), f.avenant(), memeContenu(f, AVRIL, "REF2", "Nouvelles notes"));

        Map<String, Object> enBase = enBase(FixtureLotAvenants.AVENANTS, f.avenant());
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase).containsEntry("Notes", "Nouvelles notes").containsEntry("DocRef", "REF2")
                    .containsEntry(DESCRIPTION_COL, "Avenant N° " + f.code());
            s.assertThat(jour(enBase.get("DocDate"))).isEqualTo(AVRIL);
            s.assertThat(nombre(enBase.get(AMOUNT_HT))).isEqualByComparingTo("999");
            s.assertThat(nombre(enBase.get(AMOUNT_TTC))).isEqualByComparingTo("1000");
            s.assertThat(nombre(enBase.get(AMOUNT_TO_PAY))).isEqualByComparingTo("900");
            s.assertThat(nombre(enBase.get("WarrantyAmount"))).isEqualByComparingTo(CENT);
            s.assertThat(r.nombre("$.montantTTC")).isEqualByComparingTo("1000");
            s.assertThat(r.nombre("$.netAPayer")).isEqualByComparingTo("900");
            s.assertThat(r.valeur("$.modifiable")).isEqualTo(false);
        });
    }

    // Ordre 4 avant 5 : avenant facturé, corps invalide et lignes changées → 400.
    @Test
    void ordre_400AvantLe409DeLAvenantFacture() throws Exception {
        Facture f = avenantFacture();
        CorpsJson corps = memeContenu(f, MARS, "REF", "Notes").avec(DATE, null)
                .avec(AvenantsLotSupport.LIGNES, List.of(ligne(f.ligne(), 1, "1", "7", CENT, VINGT, ZERO, categorie)));

        Reponse r = modifier(f.lot(), f.avenant(), corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R6 : suppression

    @Test
    void r6_suppression_204_lignesPuisAvenantSupprimes_autreAvenantIntact() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot, 1);
        UUID l1 = av.ligneAvenant(a, 1, "1", "2", CENT, "0.2", categorie, null);
        UUID l2 = av.ligneAvenant(a, 2, "2", "1", CENT, "0.2", categorie, null);
        UUID autre = avenantEnBase(lot, 2);

        Reponse r = appels.supprimer(admin, lot, AVENANTS, a);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            s.assertThat(av.parId(FixtureLotAvenants.AVENANTS, a)).isEmpty();
            s.assertThat(av.parId(LIGNES_AVENANT, l1)).isEmpty();
            s.assertThat(av.parId(LIGNES_AVENANT, l2)).isEmpty();
            s.assertThat(av.parId(FixtureLotAvenants.AVENANTS, autre)).hasSize(1);
        });
    }

    // ------------------------------------------------------------------ R7 : recalcul des prévisions

    private void recalcule(SoftAssertions s, UUID lot) {
        s.assertThat(prev.lignesPaiement(lot)).as("ligne périmée effacée par le recalcul")
                .extracting(l -> String.valueOf(l.get(DESCRIPTION_COL))).doesNotContain(LIGNE_PERIMEE);
    }

    @Test
    void r7_post_recalculeLesPrevisions() throws Exception {
        UUID lot = lotAvecPrevisions();

        Reponse r = creer(lot, deuxLignes(UUID.randomUUID(), UUID.randomUUID()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            recalcule(s, lot);
        });
    }

    @Test
    void r7_put_recalculeLesPrevisions() throws Exception {
        UUID lot = lotAvecPrevisions();
        UUID a = avenantEnBase(lot, 1);

        Reponse r = modifier(lot, a, deuxLignes(UUID.randomUUID(), UUID.randomUUID()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            recalcule(s, lot);
        });
    }

    @Test
    void r7_delete_recalculeLesPrevisions() throws Exception {
        UUID lot = lotAvecPrevisions();
        UUID a = avenantEnBase(lot, 1);

        Reponse r = appels.supprimer(admin, lot, AVENANTS, a);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            recalcule(s, lot);
        });
    }

    // ------------------------------------------------------------------ R17 : audit

    @Test
    void r17_creation_userCreateEtDate_surAvenantEtLignes() throws Exception {
        UUID l = UUID.randomUUID();

        Reponse r = cree(lot(), deuxLignes(l, UUID.randomUUID()));

        Map<String, Object> entete = enBase(FixtureLotAvenants.AVENANTS, id(r));
        Map<String, Object> ligne = enBase(LIGNES_AVENANT, l);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(entete.get(USER_CREATE))).isNotBlank().isNotEqualTo("null");
            s.assertThat(entete.get("UserCreateDateTime")).isNotNull();
            s.assertThat(String.valueOf(ligne.get(USER_CREATE))).isNotBlank().isNotEqualTo("null");
            s.assertThat(ligne.get("UserCreateDateTime")).isNotNull();
        });
    }

    @Test
    void r17_modification_userUpdateEtDate_surAvenantEtLigneModifiee() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot, 1);
        UUID l = av.ligneAvenant(a, 1, "1", "2", CENT, "0.2", categorie, null);

        Reponse r = modifier(lot, a, avenant(MARS, List.of(ligne(l, 1, "1", "4", CENT, VINGT, ZERO, categorie))));

        Map<String, Object> entete = enBase(FixtureLotAvenants.AVENANTS, a);
        Map<String, Object> ligne = enBase(LIGNES_AVENANT, l);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(String.valueOf(entete.get(USER_UPDATE))).isNotBlank().isNotEqualTo("null");
            s.assertThat(entete.get("UserUpdateDateTime")).isNotNull();
            s.assertThat(String.valueOf(ligne.get(USER_UPDATE))).isNotBlank().isNotEqualTo("null");
            s.assertThat(ligne.get("UserUpdateDateTime")).isNotNull();
        });
    }

    // ------------------------------------------------------------------ R18 : bordereau définitif

    @Test
    void r18_bordereau_suppressionDUneLigneCiteeParUnAvenant_409_ligneConservee() throws Exception {
        UUID lot = lot();
        UUID citee = definitive(lot, false);
        UUID libre = b.ligne(LIGNES_DEFINITIF, new FixtureBordereauxEcriture.Ligne(lot, null, 2, "L", "1", "50", categorie, false));
        av.ligneAvenant(avenantEnBase(lot, 1), 1, "1", "1", CENT, "0.2", categorie, citee);

        Reponse r = new BordereauxEcritureSupport(mockMvc).ecrire(admin, lot, DEFINITIF, bordereau(List.of(), List.of(
                BordereauxEcritureSupport.ligne(libre, null, 2, "L", "1", "50", categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_LIGNE_BORDEREAU);
            s.assertThat(b.parId(LIGNES_DEFINITIF, citee)).hasSize(1);
        });
    }

    @Test
    void r18_bordereau_suppressionDUnGroupeContenantUneLigneCitee_409() throws Exception {
        UUID lot = lot();
        UUID g = b.groupe(GROUPES_DEFINITIF, lot, null, 1, "1", false);
        UUID citee = b.ligne(LIGNES_DEFINITIF, new FixtureBordereauxEcriture.Ligne(lot, g, 1, "1.1", "1", CENT, categorie, false));
        av.ligneAvenant(avenantEnBase(lot, 1), 1, "1", "1", CENT, "0.2", categorie, citee);

        Reponse r = new BordereauxEcritureSupport(mockMvc).ecrire(admin, lot, DEFINITIF, BordereauxEcritureSupport.vide());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).containsAnyOf(MSG_GROUPE_BORDEREAU, MSG_LIGNE_BORDEREAU);
        });
    }

    // R18 : decomptee reste réservé aux décomptes (une ligne citée par un avenant seul n'est pas décomptée).
    @Test
    void r18_lectureBordereau_ligneCiteeParUnAvenantSeul_nonDecomptee() throws Exception {
        UUID lot = lot();
        UUID citee = definitive(lot, false);
        av.ligneAvenant(avenantEnBase(lot, 1), 1, "1", "1", CENT, "0.2", categorie, citee);

        Reponse r = new BordereauxEcritureSupport(mockMvc).lire(admin, lot, DEFINITIF);

        assertThat(String.valueOf(r.valeur("$.lignes[?(@.id == '" + citee + "')].decomptee"))).as(r.corps()).isEqualTo("[false]");
    }

    // ------------------------------------------------------------------ R7 / R19 : rien d'autre n'est écrit

    @Test
    void r19_creation_lotInchange_aucunePieceDAchat_bordereauInchange() throws Exception {
        UUID lot = lot();
        definitive(lot, false);
        List<Map<String, Object>> lotAvant = av.lignes("SELECT * FROM PrjMarket WHERE PKey = ?", lot.toString());
        List<Map<String, Object>> bordereauAvant = av.lignes("SELECT * FROM PrjMarketLine WHERE PrjMarketId = ?", lot.toString());

        Reponse r = creer(lot, deuxLignes(UUID.randomUUID(), UUID.randomUUID()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(av.lignes("SELECT * FROM PrjMarket WHERE PKey = ?", lot.toString())).as("PrjMarket").isEqualTo(lotAvant);
            s.assertThat(av.lignes("SELECT * FROM PrjMarketLine WHERE PrjMarketId = ?", lot.toString())).isEqualTo(bordereauAvant);
            s.assertThat(av.compter("SELECT COUNT(*) FROM BuyDocument WHERE PrjMarketId = ?", lot.toString())).isZero();
        });
    }
}
