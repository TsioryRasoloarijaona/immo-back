package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Recalculer et Enregistrer sur un décompte réel (FEATURE-decomptes-parite.md « Règles d'écriture (OMRAN) » §0-§3, §6,
 * « Vérification » et réponses Q1 / Q13 : ±0,02). Copie de {@code 26/00655} (yesRemsProd, lot {@code 26/00154}) : premier
 * décompte du lot, non facturé et dernier, 8 lignes de bordereau à 1 unité, lignes 1 et 2 avancées de 0,40 et 0,35, RG 10 %
 * sur le montant (lot), aucun autre taux ; colonnes stockées par OMRAN = {@link #OMRAN}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesPariteCalculTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String TVA = "0.2";
    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");
    private static final String[] PRIX = {"37500", "37500", "15000", "15000", "15000", "7500", "7500", "15000"};
    private static final String[] AVANCEMENT = {"0.4", "0.35", "0", "0", "0", "0", "0", "0"};

    /** Colonnes stockées de {@code 26/00655} (yesRemsProd, 11/11). */
    private static final Map<String, BigDecimal> OMRAN = omran();

    private static Map<String, BigDecimal> omran() {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("AmountHT", new BigDecimal("28125"));
        m.put("AmountVat", new BigDecimal("5625"));
        m.put("AmountTTC", new BigDecimal("33750"));
        m.put("WarrantyAmount", new BigDecimal("3375"));
        m.put("refundableRetainer", new BigDecimal("3375"));
        m.put("AdvanceAmount", BigDecimal.ZERO);
        m.put("proportionAmount", BigDecimal.ZERO);
        m.put("insuranceDECAmount", BigDecimal.ZERO);
        m.put("insuranceTRCAmount", BigDecimal.ZERO);
        m.put("nonRefundableRetainer", BigDecimal.ZERO);
        m.put("extraAmount", BigDecimal.ZERO);
        m.put("StockAmountToPay", BigDecimal.ZERO);
        m.put("FluctuationsToPay", BigDecimal.ZERO);
        m.put("amountToBePaid", new BigDecimal("33750"));
        m.put("WorkingOutAmountTTC", new BigDecimal("30375"));
        m.put("AmountTTCAfterDeductions", new BigDecimal("30375"));
        m.put("AmountToPay", new BigDecimal("30375"));
        return m;
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;

    private AppelsProjet appels;
    private String jeton;
    private UUID lot;
    private UUID fournisseur;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID projet = fixture.projet("PPK-" + jeton);
        fournisseur = fixture.fournisseur("FPK-" + jeton);
        lot = fixture.lot(projet, "LPK-" + jeton, fournisseur);
        decomptes.poser("PrjMarket", lot, "WarrantyRatio", new BigDecimal("0.1"), "AdvanceRatio", BigDecimal.ZERO,
                "ProportionRatio", BigDecimal.ZERO, "InsuranceDECRatio", BigDecimal.ZERO, "InsuranceTRCRatio",
                BigDecimal.ZERO, "InsuranceDECAmount", BigDecimal.ZERO, "InsuranceTRCAmount", BigDecimal.ZERO,
                "StockPileRatio", BigDecimal.ONE, "DefaultVat", new BigDecimal(TVA), "ExecutedQuantityBasedInsert", false,
                "ApplyWithholdingTaxOnAdditionalWork", false);
        AdmUser compte = vente.compte("prj.pk.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
    }

    /**
     * Copie de {@code 26/00655} ; {@code stockeesJustes} = colonnes de montant d'OMRAN, sinon toutes à 1 (seul un recalcul
     * peut alors les rendre justes).
     */
    private UUID copie2600655(boolean stockeesJustes) {
        UUID d = fixture.decompte(lot, "D655-" + jeton, 1, Map.of());
        List<Object> colonnes = new ArrayList<>(List.of("CfgTierId", fournisseur.toString(), "WarrantyRatio",
                new BigDecimal("0.1"), "AdvanceRatio", BigDecimal.ZERO, "proportionRatio", BigDecimal.ZERO,
                "insuranceDECRatio", BigDecimal.ZERO, "insuranceTRCRatio", BigDecimal.ZERO, "StockPileRatio", BigDecimal.ZERO,
                "WorkingOutAdvanceAmount", BigDecimal.ZERO, "ProrataRefundAmount", BigDecimal.ZERO, "Fluctuations",
                BigDecimal.ZERO, "FluctuationsOld", BigDecimal.ZERO, "StockAmount", BigDecimal.ZERO, "StockAmountOld",
                BigDecimal.ZERO, "advanceRest", BigDecimal.ZERO, "insuranceDECRest", BigDecimal.ZERO, "insuranceTRCRest",
                BigDecimal.ZERO, "InsuranceDECAmountMax", BigDecimal.ZERO, "InsuranceTRCAmountMax", BigDecimal.ZERO,
                "WorkingOutAdvanceAmountRestAtCreation", BigDecimal.ZERO, "AmountRegul", BigDecimal.ZERO,
                "AdvanceBasedOnAmount", false, "WarrantyBasedOnAmount", false, "TRCBasedOnAmount", false,
                "DECBasedOnAmount", false, "ProrataBasedOnAmount", false, "FinalWorkingOut", false));
        OMRAN.forEach((colonne, valeur) -> {
            colonnes.add(colonne);
            colonnes.add(stockeesJustes ? valeur : BigDecimal.ONE);
        });
        decomptes.poser("PrjWorkingOut", d, colonnes.toArray());
        for (int i = 0; i < PRIX.length; i++) {
            UUID ligneMarche = decomptes.ligneMarche(lot, i + 1, "1", PRIX[i], TVA);
            decomptes.ligne(d, ligneMarche, i + 1, AVANCEMENT[i], "0", PRIX[i], TVA);
        }
        return d;
    }

    private MockHttpServletResponse executer(MockHttpServletRequestBuilder requete) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(admin)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isIn(200, 204);
        return r;
    }

    private void recalculer(UUID d) throws Exception {
        executer(post(DECOMPTES + "/" + d + DecomptesPariteEcrituresTest.RECALCUL));
    }

    /** Enregistrer sans modification : la fiche relue est renvoyée telle quelle (Contrat {@code PUT /{id}}). */
    private void enregistrerSansModification(UUID d) throws Exception {
        Reponse fiche = appels.lire(admin, DECOMPTES + "/" + d);
        assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
        executer(put(DECOMPTES + "/" + d).contentType(MediaType.APPLICATION_JSON).content(fiche.corps()));
    }

    private Map<String, BigDecimal> colonnes(UUID d) {
        Map<String, Object> lu = decomptes.lire(d, OMRAN.keySet().toArray(String[]::new));
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        OMRAN.keySet().forEach(c -> {
            Object v = lu.get(c);
            m.put(c, v == null ? null : new BigDecimal(String.valueOf(v)));
        });
        return m;
    }

    private static void egales(Map<String, BigDecimal> attendu, Map<String, BigDecimal> lu) {
        SoftAssertions.assertSoftly(s -> attendu.forEach((colonne, valeur) ->
                s.assertThat(lu.get(colonne)).as(colonne).isNotNull().isCloseTo(valeur, within(TOLERANCE))));
    }

    // ------------------------------------------------------------------ §6 Recalculer

    /** « Vérification » : recalcul = valeurs OMRAN d'un décompte réel ; les colonnes faussées sont toutes réécrites. */
    @Test
    void recalcul_nominal_copie2600655_redonneLesValeursOmran() throws Exception {
        UUID d = copie2600655(false);

        recalculer(d);

        egales(OMRAN, colonnes(d));
    }

    @Test
    void recalcul_idempotent_deuxRecalculsMemesValeurs() throws Exception {
        UUID d = copie2600655(true);

        recalculer(d);
        Map<String, BigDecimal> premier = colonnes(d);
        recalculer(d);

        egales(OMRAN, premier);
        egales(premier, colonnes(d));
    }

    // ------------------------------------------------------------------ §3 Enregistrer (Q1 : test d'acceptation commun)

    /** Q1 : Enregistrer sans modification d'un décompte non verrouillé réel redonne ses colonnes stockées (±0,02). */
    @Test
    void enregistrer_sansModification_redonneLesColonnesStockees() throws Exception {
        UUID d = copie2600655(true);

        enregistrerSansModification(d);

        egales(OMRAN, colonnes(d));
    }

    /** §0 : un décompte non verrouillé est recalculé à l'enregistrement (colonnes calculées faussées → valeurs OMRAN). */
    @Test
    void enregistrer_decompteNonVerrouille_recalculeLesColonnes() throws Exception {
        UUID d = copie2600655(false);

        enregistrerSansModification(d);

        egales(OMRAN, colonnes(d));
    }

    /** Règle 6 (un seul calcul) : Enregistrer puis Recalculer ne change rien. */
    @Test
    void enregistrer_puisRecalculer_neChangeRien() throws Exception {
        UUID d = copie2600655(true);

        enregistrerSansModification(d);
        Map<String, BigDecimal> apresEnregistrer = colonnes(d);
        recalculer(d);

        egales(OMRAN, apresEnregistrer);
        egales(apresEnregistrer, colonnes(d));
    }
}
