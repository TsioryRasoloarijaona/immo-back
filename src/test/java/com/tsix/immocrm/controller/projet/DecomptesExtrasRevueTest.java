package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureDecompteExtras;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.champs;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.corps;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.montant;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.nombre;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.AVANCE;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.AVANCE_DECOMPTE;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.EXTRA;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.FLUCTUATIONS;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.PRORATA;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.RETENUE_NON_REMBOURSABLE;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.RG;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.STOCK;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Revue LC6 (règle 6, argent) : R4 extras → {@code extraAmount} et net OMRAN (±0,02) relus en base, R6 retenues
 * forfaitaires → {@code nonRefundableRetainer} et net. Montants choisis où l'ordre d'arrondi ne joue pas (FODEC 0).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesExtrasRevueTest {

    private static final String RETENUES = "retenues";
    private static final String LIBELLE = "libelle";
    private static final String MONTANT = "montant";
    private static final String DESCRIPTION = "Description";
    private static final String TVA = "0.2";
    private static final String GARDEE = "Gardée";
    private static final String[] COLONNES_NET = {TTC, AVANCE, AVANCE_DECOMPTE, RG, RETENUE_NON_REMBOURSABLE, EXTRA, PRORATA,
        STOCK, FLUCTUATIONS, A_PAYER};

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private FixtureDecompteExtras extras;

    private String jeton;
    private UUID projet;
    private UUID d;
    private RequestPostProcessor modifier;

    /** Lot RG 10 %, bordereau A (PU 100, TVA 20 %) ; décompte seul du lot (dernier, non facturé), A = 4 → TTC 480. */
    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PXR-" + jeton);
        UUID lot = fixture.lot(projet, "LXR-" + jeton, fixture.fournisseur("FXR-" + jeton));
        modifier = decomptes.compte(projet, jeton, DROIT_MODIFIER);
        d = decompte(lot, "XR1-");
        decomptes.ligne(d, decomptes.ligneMarche(lot, 1, "10", "100", TVA), 1, "4", "0", "100", TVA);
    }

    private UUID decompte(UUID lot, String code) {
        return fixture.decompte(lot, code + jeton, 1, Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private MockHttpServletResponse enregistrer(String corps) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(put(DECOMPTES + "/" + d).contentType(MediaType.APPLICATION_JSON)
                .content(corps).with(modifier)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r;
    }

    private int enregistrerRetenues(Map<?, ?>... lignes) throws Exception {
        return enregistrer(corps(RETENUES, List.of(lignes))).getStatus();
    }

    private Map<String, Object> net() {
        return decomptes.lire(d, COLONNES_NET);
    }

    /** DECOUVERTES l.92 : AmountToPay = TTC − avance − avance décompte − RG − non remboursables + extras + prorata + stock + fluct. */
    private static void assertNetSelonOmran(Map<String, Object> n) {
        BigDecimal attendu = montant(n, TTC).subtract(montant(n, AVANCE)).subtract(montant(n, AVANCE_DECOMPTE))
                .subtract(montant(n, RG)).subtract(montant(n, RETENUE_NON_REMBOURSABLE)).add(montant(n, EXTRA))
                .add(montant(n, PRORATA)).add(montant(n, STOCK)).add(montant(n, FLUCTUATIONS));
        assertThat(montant(n, A_PAYER)).as("net OMRAN %s", n).isCloseTo(attendu, within(new BigDecimal("0.02")));
    }

    private static Map<String, Object> retenue(String libelle, String montant) {
        return champs(LIBELLE, libelle, MONTANT, new BigDecimal(montant));
    }

    // ------------------------------------------------------------------ R4 montant des extras

    /** R4 : 2 × 150,25 × 1,2 = 360,60 ; 3 × 10,10 × 1,1 = 33,33 → extraAmount 393,93, net selon la formule OMRAN. */
    @Test
    void r4_extrasEcrites_extraAmountEgalSommeTTC_netFormuleOmran() throws Exception {
        String corps = corps("extras", List.of(
                champs("designation", "Peinture", "quantite", new BigDecimal("2"), "prixUnitaireHT", new BigDecimal("150.25"),
                        "tauxTVA", new BigDecimal("20")),
                champs("designation", "Joints", "quantite", new BigDecimal("3"), "prixUnitaireHT", new BigDecimal("10.10"),
                        "tauxTVA", new BigDecimal("10"))));

        int statut = enregistrer(corps).getStatus();

        assertThat(statut).isEqualTo(200);
        Map<String, Object> n = net();
        assertThat(nombre(n, EXTRA)).isEqualByComparingTo("393.93");
        assertNetSelonOmran(n);
    }

    /** R4 : extra retirée par la liste → extraAmount relu à 0 (aucun second calcul : calculerEtEcrire relit les enfants). */
    @Test
    void r4_extraRetiree_extraAmountZero() throws Exception {
        extras.extra(d, 1, "Retirée");

        int statut = enregistrer(corps("extras", List.of())).getStatus();

        assertThat(statut).isEqualTo(200);
        Map<String, Object> n = net();
        assertThat(montant(n, EXTRA)).isEqualByComparingTo("0");
        assertNetSelonOmran(n);
    }

    // ------------------------------------------------------------------ R6 retenues forfaitaires

    @Test
    void r6_retenueSansId_creee() throws Exception {
        int statut = enregistrerRetenues(retenue("Pénalité de retard", "1000"));

        assertThat(statut).isEqualTo(200);
        List<Map<String, Object>> lignes = extras.retenues(d);
        assertThat(lignes).hasSize(1);
        assertThat(lignes.getFirst()).containsEntry(DESCRIPTION, "Pénalité de retard");
        assertThat(nombre(lignes.getFirst(), "Amount")).isEqualByComparingTo("1000");
    }

    /** R6 : Σ retenues forfaitaires entre dans nonRefundableRetainer ; le net baisse d'autant (Vérification : retenue 1 000 → net −1 000). */
    @Test
    void r6_retenue1000_nonRemboursablesPlus1000_netMoins1000() throws Exception {
        assertThat(enregistrer("{}").getStatus()).isEqualTo(200);
        Map<String, Object> avant = net();

        int statut = enregistrerRetenues(retenue("Pénalité", "1000"));

        assertThat(statut).isEqualTo(200);
        Map<String, Object> apres = net();
        assertThat(montant(apres, RETENUE_NON_REMBOURSABLE))
                .isEqualByComparingTo(montant(avant, RETENUE_NON_REMBOURSABLE).add(new BigDecimal("1000")));
        assertThat(montant(apres, A_PAYER)).isCloseTo(montant(avant, A_PAYER).subtract(new BigDecimal("1000")),
                within(new BigDecimal("0.02")));
        assertNetSelonOmran(apres);
    }

    @Test
    void r6_retenueAvecId_miseAJour_memeLigne() throws Exception {
        UUID r1 = extras.retenue(d, 1, "Ancienne", "10");
        Map<String, Object> ligne = retenue("Nouvelle", "25");
        ligne.put("id", r1.toString());

        int statut = enregistrerRetenues(ligne);

        assertThat(statut).isEqualTo(200);
        List<Map<String, Object>> lignes = extras.retenues(d);
        assertThat(lignes).hasSize(1);
        assertThat(lignes.getFirst()).containsEntry("id", r1.toString()).containsEntry(DESCRIPTION, "Nouvelle");
        assertThat(nombre(lignes.getFirst(), "Amount")).isEqualByComparingTo("25");
    }

    @Test
    void r6_retenueAbsenteDeLaListe_supprimee_netRecalcule() throws Exception {
        UUID r1 = extras.retenue(d, 1, GARDEE, "10");
        extras.retenue(d, 2, "Retirée", "500");
        Map<String, Object> ligne = retenue(GARDEE, "10");
        ligne.put("id", r1.toString());

        int statut = enregistrerRetenues(ligne);

        assertThat(statut).isEqualTo(200);
        assertThat(extras.retenues(d)).singleElement().satisfies(l -> assertThat(l).containsEntry("id", r1.toString()));
        assertNetSelonOmran(net());
    }

    /** R6 : null = inchangées (non-régression, passe déjà). */
    @Test
    void r6_retenuesNull_inchangees() throws Exception {
        extras.retenue(d, 1, GARDEE, "10");

        int statut = enregistrer("{\"notes\":\"x\"}").getStatus();

        assertThat(statut).isEqualTo(200);
        assertThat(extras.retenues(d)).hasSize(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void r6_libelleVide_400_rienEcrit(String libelle) throws Exception {
        int statut = enregistrerRetenues(retenue(libelle, "100"));

        assertThat(statut).isEqualTo(400);
        assertThat(extras.retenues(d)).isEmpty();
        assertThat(enregistrerRetenues(retenue("Valide", "100"))).as("témoin libellé saisi").isEqualTo(200);
    }

    /** R6 + réponse du manager : libellé > 500 → 400 ; 500 caractères passent. */
    @Test
    void r6_libelleDe501Caracteres_400_rienEcrit() throws Exception {
        int statut = enregistrerRetenues(retenue("L".repeat(501), "100"));

        assertThat(statut).isEqualTo(400);
        assertThat(extras.retenues(d)).isEmpty();
        assertThat(enregistrerRetenues(retenue("L".repeat(500), "100"))).as("témoin 500 caractères").isEqualTo(200);
    }

    /** Réponse du manager : retenues[].id d'un autre décompte → 400. */
    @Test
    void r6_idDuneRetenueDunAutreDecompte_400_rienEcrit() throws Exception {
        UUID autreLot = fixture.lot(projet, "LXS-" + jeton, fixture.fournisseur("FXS-" + jeton));
        UUID dAutre = decompte(autreLot, "XS1-");
        UUID rAutre = extras.retenue(dAutre, 1, "Autre", "10");
        Map<String, Object> pirate = retenue("Pirate", "999");
        pirate.put("id", rAutre.toString());

        int statut = enregistrerRetenues(pirate);

        assertThat(statut).isEqualTo(400);
        assertThat(extras.retenues(dAutre)).singleElement().satisfies(l -> assertThat(l).containsEntry(DESCRIPTION, "Autre"));
        assertThat(extras.retenues(d)).isEmpty();
        assertThat(enregistrerRetenues(retenue("Pirate", "999"))).as("témoin sans id étranger").isEqualTo(200);
    }
}
