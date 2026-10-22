package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.Categories;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prévisions (le « Bilan ») d'une étude : saisie de l'arbre, refus, recalcul unique — FEATURE-patrimoine-faisabilite-workflow.md
 * §2.1-§2.4, §3 et invariants I3, I4, I7, I8. Exemple chiffré YESTEST (§2.4) : unité 2 970, « Gros oeuvres » 2 000 × 2 970,
 * « Architecte » 2,5 % de « Gros oeuvres », TVA 0,20 → Dépense 6 088 500 HT / 7 306 200 TTC, terrain exclu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionFaisabiliteControllerTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.00001");
    private static final String PREVISIONS_ETUDE = "SELECT COUNT(*) FROM FSPrevision WHERE FeasibilityStudyId = :id";
    private static final String PAR_TRANCHE_ETUDE = "SELECT COUNT(*) FROM StkFSPrevisionStkTranche WHERE FeasibilityStudyId = :id";
    private static final String TVA = "0.2";
    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;

    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;
    private Categories cat;
    private UUID etude;
    private UUID uniteBase;
    private UUID unitePv;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
        cat = finance.categoriesDeTest(jeton);
        etude = api.etude(yestest(fiche("PRV-" + jeton, societe.getId())));
        uniteBase = api.unite("PV-" + jeton, false, null);
        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("2970")))).andExpect(status().isOk());
        unitePv = fixture.ligneUnite(etude, uniteBase);
        api.tranches(etude, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("T1"), d("1")),
                ventilation(unitePv, d("2970"))))).andExpect(status().isOk());
    }

    private UUID trancheDeLaSociete(String code) {
        return patrimoine.tranche(code + "-" + jeton, patrimoine.projet("PRJ-" + code + "-" + jeton, societe)).getId();
    }

    private CorpsJson grosOeuvres() {
        return parUnite(cat.grosOeuvres(), unitePv, "2000", TVA);
    }

    private CorpsJson architecte() {
        return enPourcentage(cat.architecte(), cat.grosOeuvres(), "0.025", TVA);
    }

    private String enregistrer(CorpsJson... lignes) throws Exception {
        return api.remplacer(PREVISIONS, etude, lignes(lignes)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal lu(String json, UUID categorie, String champ) {
        List<Object> valeurs = JsonPath.read(json, "$.lignes[?(@.categorieId == '" + categorie + "')]." + champ);
        assertThat(valeurs).as("%s de %s", champ, categorie).hasSize(1);
        return new BigDecimal(String.valueOf(valeurs.get(0)));
    }

    private static BigDecimal bilan(String json, String champ) {
        return new BigDecimal(String.valueOf((Object) JsonPath.read(json, "$.bilan." + champ)));
    }

    // ------------------------------------------------------------------ §2.4 exemple chiffré YESTEST

    @Test
    void r2_4_yestest_colonnesStockees_6088500Ht_7306200Ttc_coutRevient2460() throws Exception {
        enregistrer(grosOeuvres(), architecte());

        Tuple e = fixture.etude(etude);
        assertAll(
                () -> assertThat(decimal(e.get("TotalCostHT"))).isEqualByComparingTo("6088500"),
                () -> assertThat(decimal(e.get("TotalCost"))).isEqualByComparingTo("7306200"),
                () -> assertThat(decimal(e.get("TotalCostExecutionHT"))).isEqualByComparingTo("6088500"),
                () -> assertThat(decimal(e.get("TotalCostExecution"))).isEqualByComparingTo("7306200"),
                () -> assertThat(decimal(e.get("UnitBackCost"))).isEqualByComparingTo("2460"));
    }

    /** FSPrevision stocke le montant PROPRE ; UnityId = ligne d'unité de l'étude ; ParentId = catégorie de référence. */
    @Test
    void r2_4_yestest_lignesFSPrevision_montantsPropres_uniteEtReference() throws Exception {
        enregistrer(grosOeuvres(), architecte());

        Tuple go = finance.prevision(etude, cat.grosOeuvres());
        Tuple arc = finance.prevision(etude, cat.architecte());
        assertAll(
                () -> assertThat(decimal(go.get("AmountHT"))).isEqualByComparingTo("5940000"),
                () -> assertThat(decimal(go.get("Amount"))).isEqualByComparingTo("7128000"),
                () -> assertThat(decimal(go.get("Ratio"))).isEqualByComparingTo("2000"),
                () -> assertThat(decimal(go.get("VatRatio"))).isEqualByComparingTo(TVA),
                () -> assertThat(guid(go.get("unite"))).isEqualTo(guid(unitePv)),
                () -> assertThat(go.get("reference")).isNull(),
                () -> assertThat(decimal(arc.get("AmountHT"))).isEqualByComparingTo("148500"),
                () -> assertThat(decimal(arc.get("Amount"))).isEqualByComparingTo("178200"),
                () -> assertThat(decimal(arc.get("Ratio"))).isEqualByComparingTo("0.025"),
                () -> assertThat(guid(arc.get("reference"))).isEqualTo(guid(cat.grosOeuvres())),
                () -> assertThat(arc.get("unite")).isNull());
    }

    /** §2.1 : défauts — IsForProject vrai, GlobalRatio 0, Note '', modèle de marché NULL, pas indivise. */
    @Test
    void r2_1_ligne_valeursParDefaut() throws Exception {
        enregistrer(grosOeuvres());

        Tuple go = finance.prevision(etude, cat.grosOeuvres());
        assertAll(
                () -> assertThat(go.get("IsForProject")).isEqualTo(Boolean.TRUE),
                () -> assertThat(decimal(go.get("GlobalRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(go.get("Note")).isEqualTo(""),
                () -> assertThat(go.get("modele")).isNull(),
                () -> assertThat(go.get("Undivided")).isNotEqualTo(Boolean.TRUE),
                () -> assertThat(go.get("ApplyOnDate")).isNull());
    }

    /** §2.1 : TVA absente du corps → TVA par défaut de l'étude (0,20 à la création). */
    @Test
    void r2_1_tvaAbsente_tvaParDefautDeLEtude() throws Exception {
        enregistrer(grosOeuvres().sans(TAUX_TVA));

        assertThat(decimal(finance.prevision(etude, cat.grosOeuvres()).get("VatRatio"))).isEqualByComparingTo("0.20");
    }

    /** §2.1 : le serveur ajoute les ancêtres jusqu'à la racine ; chaque nœud est écrit, montant propre nul. */
    @Test
    void r2_1_ancetresAjoutes_jusquALaRacine() throws Exception {
        enregistrer(grosOeuvres(), architecte());

        assertAll(
                () -> assertThat(finance.prevision(etude, cat.travaux())).isNotNull()
                        .satisfies(t -> assertThat(decimal(t.get("Amount"))).isEqualByComparingTo("0")),
                () -> assertThat(finance.prevision(etude, cat.honoraires())).isNotNull(),
                () -> assertThat(finance.prevision(etude, RACINE_DEPENSE)).isNotNull(),
                () -> assertThat(fixture.compter(PREVISIONS_ETUDE, etude)).isEqualTo(5));
    }

    /** §2.3 lecture : total d'un nœud = propre + enfants (I3) ; poids = total / racine ; ventilation sur la tranche unique. */
    @Test
    void i3_get_totauxDesNoeuds_poids_ventilation() throws Exception {
        enregistrer(grosOeuvres(), architecte());

        String json = api.lire(PREVISIONS, etude).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Object> ventilationRacine = JsonPath.read(json,
                "$.lignes[?(@.categorieId == '" + RACINE_DEPENSE.toString().toLowerCase() + "')].ventilation[*].montantHt");
        assertAll(
                () -> assertThat(lu(json, cat.travaux(), "montantHtPropre")).isEqualByComparingTo("0"),
                () -> assertThat(lu(json, cat.travaux(), MONTANT_HT)).isCloseTo(d("5940000"), within(TOLERANCE)),
                () -> assertThat(lu(json, cat.grosOeuvres(), MONTANT)).isCloseTo(d("7128000"), within(TOLERANCE)),
                () -> assertThat(lu(json, cat.grosOeuvres(), POIDS)).isCloseTo(d("0.975609756"), within(d("0.000001"))),
                () -> assertThat(lu(json, RACINE_DEPENSE, MONTANT_HT)).isCloseTo(d("6088500"), within(TOLERANCE)),
                () -> assertThat(lu(json, RACINE_DEPENSE, MONTANT)).isCloseTo(d("7306200"), within(TOLERANCE)),
                () -> assertThat(lu(json, RACINE_DEPENSE, POIDS)).isEqualByComparingTo("1"),
                () -> assertThat(ventilationRacine).singleElement()
                        .satisfies(v -> assertThat(new BigDecimal(v.toString())).isCloseTo(d("6088500"), within(TOLERANCE))),
                () -> assertThat(bilan(json, "coutTotalHt")).isCloseTo(d("6088500"), within(TOLERANCE)));
    }

    @Test
    void r2_3_get_parentIdDArbre() throws Exception {
        enregistrer(grosOeuvres());

        api.lire(PREVISIONS, etude)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[?(@.categorieId == '" + cat.grosOeuvres() + "')].parentId")
                        .value(cat.travaux().toString()));
    }

    /** §2.3 : l'aperçu calcule montants et bilan sans rien écrire. */
    @Test
    void r2_3_calcul_apercu_sansEcriture() throws Exception {
        String json = api.envoyer(CALCUL_PREVISIONS, etude, lignes(grosOeuvres(), architecte())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertAll(
                () -> assertThat(bilan(json, "coutTotalHt")).isCloseTo(d("6088500"), within(TOLERANCE)),
                () -> assertThat(bilan(json, "coutTotal")).isCloseTo(d("7306200"), within(TOLERANCE)),
                () -> assertThat(fixture.compter(PREVISIONS_ETUDE, etude)).isZero(),
                () -> assertThat(fixture.compter(PAR_TRANCHE_ETUDE, etude)).isZero());
    }

    // ------------------------------------------------------------------ §2.2 saisie à deux sens

    @Test
    void r2_2_montantHtSeul_ratioRecalcule() throws Exception {
        enregistrer(grosOeuvres().sans(RATIO).avec(MONTANT_HT, d("5940000")));

        Tuple go = finance.prevision(etude, cat.grosOeuvres());
        assertAll(
                () -> assertThat(decimal(go.get("Ratio"))).isEqualByComparingTo("2000"),
                () -> assertThat(decimal(go.get("Amount"))).isEqualByComparingTo("7128000"));
    }

    @Test
    void r2_2_montantTtcSeul_htEtRatioRecalcules() throws Exception {
        enregistrer(grosOeuvres().sans(RATIO).avec(MONTANT, d("7128000")));

        Tuple go = finance.prevision(etude, cat.grosOeuvres());
        assertAll(
                () -> assertThat(decimal(go.get("AmountHT"))).isEqualByComparingTo("5940000"),
                () -> assertThat(decimal(go.get("Ratio"))).isEqualByComparingTo("2000"));
    }

    /** Priorité ratio &gt; montantHt &gt; montant. */
    @Test
    void r2_2_lesTroisDiscordants_leRatioGagne() throws Exception {
        enregistrer(grosOeuvres().avec(MONTANT_HT, d("1")).avec(MONTANT, d("2")));

        Tuple go = finance.prevision(etude, cat.grosOeuvres());
        assertAll(
                () -> assertThat(decimal(go.get("AmountHT"))).isEqualByComparingTo("5940000"),
                () -> assertThat(decimal(go.get("Amount"))).isEqualByComparingTo("7128000"));
    }

    @Test
    void r2_2_montantHtEtMontant_leMontantHtGagne() throws Exception {
        enregistrer(grosOeuvres().sans(RATIO).avec(MONTANT_HT, d("5940000")).avec(MONTANT, d("2")));

        assertThat(decimal(finance.prevision(etude, cat.grosOeuvres()).get("Amount"))).isEqualByComparingTo("7128000");
    }

    /** §2.1 : en pourcentage, la base est le total de la ligne de référence, enfants compris. */
    @Test
    void r2_1_pourcentage_baseTotalDeLaReferenceEnfantsCompris() throws Exception {
        enregistrer(grosOeuvres(), enPourcentage(cat.architecte(), cat.travaux(), "0.1", TVA));

        assertThat(decimal(finance.prevision(etude, cat.architecte()).get("AmountHT"))).isEqualByComparingTo("594000");
    }

    // ------------------------------------------------------------------ §2.3 refus (400)

    private void refuse(CorpsJson... lignes) throws Exception {
        api.remplacer(PREVISIONS, etude, lignes(lignes)).andExpect(status().isBadRequest());
        assertThat(fixture.compter(PREVISIONS_ETUDE, etude)).isZero();
    }

    private void refuse(String message, CorpsJson... lignes) throws Exception {
        api.remplacer(PREVISIONS, etude, lignes(lignes))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(message)));
        assertThat(fixture.compter(PREVISIONS_ETUDE, etude)).isZero();
    }

    @Test
    void r2_3_400_categorieAbsente() throws Exception {
        refuse(grosOeuvres().sans(CATEGORIE_ID));
    }

    @Test
    void r2_3_400_categorieInconnue() throws Exception {
        refuse(parUnite(UUID.randomUUID(), unitePv, "1", TVA));
    }

    @Test
    void r2_3_400_categorieSansPrevision() throws Exception {
        refuse(parUnite(cat.sansPrevision(), unitePv, "1", TVA));
    }

    @Test
    void r2_3_400_uniteDUneAutreEtude() throws Exception {
        UUID autre = api.etude(yestest(fiche("AUT-" + jeton, societe.getId())));
        api.unites(autre, lignes(ligneUniteCorps(1, uniteBase, d("10")))).andExpect(status().isOk());

        refuse(parUnite(cat.grosOeuvres(), fixture.ligneUnite(autre, uniteBase), "1", TVA));
    }

    @Test
    void r2_3_400_tvaSuperieureA1() throws Exception {
        refuse(parUnite(cat.grosOeuvres(), unitePv, "1", "1.5"));
    }

    @Test
    void r2_3_400_tvaNegative() throws Exception {
        refuse(parUnite(cat.grosOeuvres(), unitePv, "1", "-0.1"));
    }

    @Test
    void r2_3_400_ratioNegatif() throws Exception {
        refuse(parUnite(cat.grosOeuvres(), unitePv, "-1", TVA));
    }

    @Test
    void r2_3_400_montantNegatif() throws Exception {
        refuse(grosOeuvres().sans(RATIO).avec(MONTANT, d("-1")));
    }

    @Test
    void r2_3_400_uniteEtReferenceALaFois() throws Exception {
        refuse(grosOeuvres(), architecte().avec(UNITE_ETUDE_ID, unitePv));
    }

    // ------------------------------------------------------------------ I8 : trois messages de hiérarchie

    @Test
    void i8_referenceHorsDuBilan_400_message() throws Exception {
        refuse(MSG_HORS_BILAN, enPourcentage(cat.architecte(), cat.divers(), "0.1", TVA));
    }

    @Test
    void i8_referenceAncetre_400_messageHierarchie() throws Exception {
        refuse(MSG_HIERARCHIE, grosOeuvres(), enPourcentage(cat.architecte(), cat.honoraires(), "0.1", TVA));
    }

    @Test
    void i8_referenceDescendant_400_messageHierarchie() throws Exception {
        refuse(MSG_HIERARCHIE, grosOeuvres(), enPourcentage(cat.travaux(), cat.grosOeuvres(), "0.1", TVA));
    }

    @Test
    void i8_cycleDeReferences_400_messageBoucle() throws Exception {
        refuse(MSG_BOUCLE, enPourcentage(cat.grosOeuvres(), cat.architecte(), "0.1", TVA),
                enPourcentage(cat.architecte(), cat.grosOeuvres(), "0.1", TVA));
    }

    // ------------------------------------------------------------------ 404, 409

    @Test
    void r2_3_404_etudeInconnue() throws Exception {
        api.lire(PREVISIONS, UUID.randomUUID()).andExpect(status().isNotFound());
        api.remplacer(PREVISIONS, UUID.randomUUID(), lignes(new String[0])).andExpect(status().isNotFound());
    }

    @Test
    void r2_3_409_etudeBloquee_rienEcrit() throws Exception {
        api.bloquer(etude).andExpect(status().is2xxSuccessful());

        api.remplacer(PREVISIONS, etude, lignes(grosOeuvres()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MSG_BLOQUEE)));

        assertThat(fixture.compter(PREVISIONS_ETUDE, etude)).isZero();
    }

    /** §2.3 : retirer une unité de base utilisée par une ligne → 409 « Vous ne pouvez pas supprimer cette ligne! ». */
    @Test
    void r2_3_retraitDUneUniteUtilisee_409_ligneConservee() throws Exception {
        enregistrer(grosOeuvres());

        api.unites(etude, lignes(new String[0]))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MSG_LIGNE_UTILISEE)));

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkFSBaseUnitStkFeasibilityStudy WHERE FeasibilityStudyId = :id",
                etude)).isEqualTo(1);
    }

    /** §2.3 : retirer une tranche devient permis ; les lignes par tranche sont reconstruites (aucune sans tranche). */
    @Test
    void r2_3_retraitDUneTranche_permis_lignesParTrancheReconstruites() throws Exception {
        enregistrer(grosOeuvres());
        int avant = fixture.compter(PAR_TRANCHE_ETUDE, etude);

        api.tranches(etude, lignes(new String[0])).andExpect(status().isOk());

        assertAll(
                () -> assertThat(avant).isPositive(),
                () -> assertThat(fixture.compter(PAR_TRANCHE_ETUDE, etude)).isZero(),
                () -> assertThat(finance.prevision(etude, cat.grosOeuvres())).isNotNull());
    }

    // ------------------------------------------------------------------ §2.1 lignes verrouillées (relations configurées)

    private UUID terrainRelie() {
        UUID terrain = finance.depense("TER-" + jeton, RACINE_DEPENSE);
        finance.relier(RELATION_PRIX_TERRAIN, terrain);
        return terrain;
    }

    /** Relation « prix du terrain » liée : ligne ajoutée par le serveur, montant imposé (4 500 000 sur YESTEST), dans la racine. */
    @Test
    void r2_1_relationTerrainConfiguree_ligneAjoutee_montantImpose_verrouillee() throws Exception {
        UUID terrain = terrainRelie();

        String json = enregistrer(grosOeuvres(), architecte());

        assertAll(
                () -> assertThat(decimal(finance.prevision(etude, terrain).get("AmountHT"))).isEqualByComparingTo("4500000"),
                () -> assertThat(JsonPath.<List<Object>>read(json,
                        "$.lignes[?(@.categorieId == '" + terrain + "')]." + VERROUILLEE)).containsExactly(true),
                () -> assertThat(decimal(fixture.etude(etude).get("TotalCostHT"))).isEqualByComparingTo("10588500"));
    }

    @Test
    void r2_3_ligneVerrouilleeModifiee_400() throws Exception {
        UUID terrain = terrainRelie();

        refuse(grosOeuvres(), corps().avec(CATEGORIE_ID, terrain).avec(MONTANT_HT, d("1")));
    }

    // ------------------------------------------------------------------ §3 recalcul unique

    /** I7 : deux enregistrements identiques de suite = mêmes lignes dérivées. */
    @Test
    void i7_recalculIdempotent_memesLignesDerivees() throws Exception {
        enregistrer(grosOeuvres(), architecte());
        int parTranche = fixture.compter(PAR_TRANCHE_ETUDE, etude);
        List<String> tresorerie = finance.tresorerie(etude).stream()
                .map(t -> t.get("type") + "|" + t.get("DocDate") + "|" + decimal(t.get("Amount")).stripTrailingZeros()).toList();

        enregistrer(grosOeuvres(), architecte());

        assertAll(
                () -> assertThat(fixture.compter(PAR_TRANCHE_ETUDE, etude)).isEqualTo(parTranche),
                () -> assertThat(finance.tresorerie(etude).stream()
                        .map(t -> t.get("type") + "|" + t.get("DocDate") + "|" + decimal(t.get("Amount")).stripTrailingZeros())
                        .toList()).isEqualTo(tresorerie),
                () -> assertThat(fixture.compter(PREVISIONS_ETUDE, etude)).isEqualTo(5));
    }

    /** §3 : PUT unites recalcule dans la même transaction (quantité 2 970 → 1 485 : coût HT 3 044 250). */
    @Test
    void r3_putUnites_recalculeLeBilan() throws Exception {
        enregistrer(grosOeuvres(), architecte());

        api.tranches(etude, lignes(new String[0])).andExpect(status().isOk());
        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("1485")).avec(ID, unitePv))).andExpect(status().isOk());
        api.tranches(etude, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, trancheDeLaSociete("T2"), d("1")),
                ventilation(unitePv, d("1485"))))).andExpect(status().isOk());

        assertAll(
                () -> assertThat(decimal(finance.prevision(etude, cat.grosOeuvres()).get("AmountHT")))
                        .isEqualByComparingTo("2970000"),
                () -> assertThat(decimal(fixture.etude(etude).get("TotalCostHT"))).isEqualByComparingTo("3044250"));
    }

    /** I4 : deux tranches 0,25 / 0,75 → Σ ventilation d'un nœud = son total ; Amount = AmountHT × (1 + TVA) (écart 7c). */
    @Test
    void i4_ventilationDeuxTranches_sommeEgaleTotal_amountCorrige() throws Exception {
        api.tranches(etude, lignes(
                tranche(ligneTrancheCorps("Tranche A", 1, trancheDeLaSociete("TA"), d("0.25")), ventilation(unitePv, d("742.5"))),
                tranche(ligneTrancheCorps("Tranche B", 2, trancheDeLaSociete("TB"), d("0.75")),
                        ventilation(unitePv, d("2227.5"))))).andExpect(status().isOk());

        enregistrer(grosOeuvres(), architecte());

        List<Tuple> go = finance.parTranche(etude, cat.grosOeuvres());
        BigDecimal racine = finance.parTranche(etude, RACINE_DEPENSE).stream().map(t -> decimal(t.get("AmountHT")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertAll(
                () -> assertThat(go).extracting(t -> decimal(t.get("AmountHT")).stripTrailingZeros().toPlainString())
                        .containsExactly("1485000", "4455000"),
                () -> assertThat(go).extracting(t -> decimal(t.get("Amount")).stripTrailingZeros().toPlainString())
                        .containsExactly("1782000", "5346000"),
                () -> assertThat(racine).isCloseTo(d("6088500"), within(TOLERANCE)));
    }
}
