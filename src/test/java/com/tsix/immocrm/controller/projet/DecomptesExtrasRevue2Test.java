package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureDecompteExtras;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.json;
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

/** Second tour LC6 (règle 6) : FODEC ≠ 0 dans un seul calcul (base, DTO, PU TTC), extras + retenues dans une requête. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesExtrasRevue2Test {

    private static final String EXTRAS = "extras";
    private static final String RETENUES = "retenues";
    private static final String DESIGNATION = "designation";
    private static final String TVA = "0.2";
    private static final String[] COLONNES_NET = {TTC, AVANCE, AVANCE_DECOMPTE, RG, RETENUE_NON_REMBOURSABLE, EXTRA, PRORATA,
        STOCK, FLUCTUATIONS, A_PAYER};

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private FixtureDecompteExtras extras;

    private UUID d;
    private RequestPostProcessor modifier;

    @BeforeEach
    void setUp() {
        String jeton = FixtureProjetLots.jeton();
        UUID projet = fixture.projet("PX2-" + jeton);
        UUID lot = fixture.lot(projet, "LX2-" + jeton, fixture.fournisseur("FX2-" + jeton));
        modifier = decomptes.compte(projet, jeton, DROIT_MODIFIER);
        d = fixture.decompte(lot, "X21-" + jeton, 1, Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
        decomptes.ligne(d, decomptes.ligneMarche(lot, 1, "10", "100", TVA), 1, "4", "0", "100", TVA);
    }

    private MockHttpServletResponse enregistrer(String corps) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(put(DECOMPTES + "/" + d).contentType(MediaType.APPLICATION_JSON)
                .content(corps).with(modifier)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r;
    }

    private static void assertNetSelonOmran(Map<String, Object> n) {
        BigDecimal attendu = montant(n, TTC).subtract(montant(n, AVANCE)).subtract(montant(n, AVANCE_DECOMPTE))
                .subtract(montant(n, RG)).subtract(montant(n, RETENUE_NON_REMBOURSABLE)).add(montant(n, EXTRA))
                .add(montant(n, PRORATA)).add(montant(n, STOCK)).add(montant(n, FLUCTUATIONS));
        assertThat(montant(n, A_PAYER)).as("net OMRAN %s", n).isCloseTo(attendu, within(new BigDecimal("0.02")));
    }

    /** 100 × 1,01 × 3 × 1,19 = 360,57 : ExtraAmount = montantTTC du DTO ; UnitPriceTTC = 100 × 1,19 (sans FODEC, D2). */
    @Test
    void r4_fodecNonNul_extraAmountEgalMontantTTCDuDto_puTTCSansFodec() throws Exception {
        String corps = json(champs(EXTRAS, List.of(champs(DESIGNATION, "Charpente", "quantite", new BigDecimal("3"),
                "prixUnitaireHT", new BigDecimal("100"), "tauxTVA", new BigDecimal("19"), "tauxFODEC", new BigDecimal("1")))));

        MockHttpServletResponse r = enregistrer(corps);

        assertThat(r.getStatus()).isEqualTo(200);
        Map<String, Object> n = decomptes.lire(d, COLONNES_NET);
        assertThat(nombre(n, EXTRA)).isEqualByComparingTo("360.57");
        Object dto = JsonPath.read(r.getContentAsString(StandardCharsets.UTF_8), "$.extras[0].montantTTC");
        assertThat(new BigDecimal(String.valueOf(dto))).isEqualByComparingTo("360.57");
        Map<String, Object> ligne = extras.extras(d).getFirst();
        assertThat(nombre(ligne, "UnitPriceTTC")).isEqualByComparingTo("119");
        assertThat(nombre(ligne, "FODECRatio")).isEqualByComparingTo("0.01");
        assertNetSelonOmran(n);
    }

    /** Extra TTC 120 et retenue 50 dans le même PUT : non remboursables +50, extras 120, net OMRAN. */
    @Test
    void r4r6_extrasEtRetenuesEnsemble_netCoherent() throws Exception {
        assertThat(enregistrer("{}").getStatus()).isEqualTo(200);
        Map<String, Object> avant = decomptes.lire(d, COLONNES_NET);
        String corps = json(champs(
                EXTRAS, List.of(champs(DESIGNATION, "Dalle", "quantite", BigDecimal.ONE, "prixUnitaireHT", new BigDecimal("100"),
                        "tauxTVA", new BigDecimal("20"))),
                RETENUES, List.of(champs("libelle", "Pénalité", "montant", new BigDecimal("50")))));

        assertThat(enregistrer(corps).getStatus()).isEqualTo(200);

        Map<String, Object> apres = decomptes.lire(d, COLONNES_NET);
        assertThat(montant(apres, RETENUE_NON_REMBOURSABLE))
                .isEqualByComparingTo(montant(avant, RETENUE_NON_REMBOURSABLE).add(new BigDecimal("50")));
        assertThat(montant(apres, EXTRA)).isEqualByComparingTo("120");
        assertThat(montant(apres, A_PAYER)).isCloseTo(montant(avant, A_PAYER).add(new BigDecimal("70")),
                within(new BigDecimal("0.02")));
        assertNetSelonOmran(apres);
    }

    /** Extras valides + retenue invalide → 400 et aucune extra écrite (validation des deux listes avant écriture). */
    @Test
    void retenueInvalide_extrasValides_400_aucuneExtraEcrite() throws Exception {
        extras.extra(d, 1, "Existante");
        String corps = json(champs(
                EXTRAS, List.of(champs(DESIGNATION, "Nouvelle", "quantite", BigDecimal.ONE, "prixUnitaireHT", BigDecimal.TEN)),
                RETENUES, List.of(champs("libelle", " ", "montant", BigDecimal.ONE))));

        assertThat(enregistrer(corps).getStatus()).isEqualTo(400);

        List<Map<String, Object>> lignes = extras.extras(d);
        assertThat(lignes).hasSize(1);
        assertThat(lignes.getFirst()).containsEntry("Description", "Existante");
        assertThat(extras.retenues(d)).isEmpty();
    }
}
