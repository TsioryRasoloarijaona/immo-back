package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.services.rapport.vente.flux.FluxParReflexion.Flux;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Rapprochement réel avec la vue {@code ComSalePrevision} (FEATURE-tdb-vente-rapports-flux.md §12.3 test 2). La vue est absente
 * de yesRemsTest et de la base de CI : test ignoré là. Lancé à la main sur yesRemsProd ({@code DB_NAME=yesRemsProd},
 * {@code -Dtest=FluxVenteMensuelVuePreprodTest}) ; attendu au 22/10 : écart nul partout sauf ENCAISSEMENTS 2026 = −199 784,00,
 * et la ligne sans date (20 000) dans les anomalies.
 */
@SpringBootTest
class FluxVenteMensuelVuePreprodTest {

    private static final String ENCAISSEMENTS = "ENCAISSEMENTS";
    private static final BigDecimal ECART_2026 = new BigDecimal("-199784.00");

    /** Mesure du service → colonne « réel » de la vue. */
    private static final Map<String, String> COLONNES = colonnes();

    @Autowired private ApplicationContext contexte;
    @Autowired private JdbcTemplate jdbc;

    private static Map<String, String> colonnes() {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("CONCRETISATIONS", "ConcretisationQuantityReal");
        c.put("CONCRETISATIONS_NETTES", "ConcretisationQuantityRealWihoutCancelations");
        c.put("CONCRETISATIONS_MONTANT", "ConcretisationReal");
        c.put("AVANCES_ENCAISSEES", "EncaissementRealAdvance");
        c.put("CA_QUANTITE", "CAQuantityReal");
        c.put("CA_TTC", "CAReal");
        c.put("CA_HT", "CARealHT");
        c.put("LIVRAISONS", "DeliveryQuantityReal");
        c.put("DESISTEMENTS", "CancelQuantityReal");
        c.put("DESISTEMENTS_MONTANT", "CancelReal");
        c.put("ANNULATIONS", "AnnulQuantityReal");
        c.put("ANNULATIONS_MONTANT", "AnnulReal");
        c.put(ENCAISSEMENTS, "EncaissementReal");
        return c;
    }

    private boolean vueExiste() {
        return jdbc.queryForObject("SELECT CASE WHEN OBJECT_ID('dbo.ComSalePrevision', 'V') IS NULL THEN 0 ELSE 1 END",
                Integer.class) == 1;
    }

    /** {@code (tranche|année|mois) → somme de la colonne}, lignes datées seulement. */
    private Map<String, BigDecimal> vue(String colonne) {
        Map<String, BigDecimal> valeurs = new HashMap<>();
        jdbc.query("SELECT CfgTranchePkey AS t, [Year] AS a, [Month] AS m, SUM(" + colonne + ") AS v "
                + "FROM ComSalePrevision WHERE [Year] IS NOT NULL GROUP BY CfgTranchePkey, [Year], [Month]", rs -> {
                    valeurs.put(rs.getString("t").toUpperCase() + "|" + rs.getInt("a") + "|" + rs.getInt("m"),
                            rs.getBigDecimal("v"));
                });
        return valeurs;
    }

    @Test
    void vue_mesureParMesure_ecartNul_saufEncaissements2026() {
        assumeTrue(vueExiste(), "vue ComSalePrevision absente (yesRemsTest, CI) : rapprochement manuel seulement");
        List<UUID> tranches = jdbc.queryForList("SELECT PKey FROM CfgTranche WHERE IsForSale = 1", String.class)
                .stream().map(UUID::fromString).toList();
        Flux f = new FluxParReflexion(contexte).calculer(tranches, null, null,
                COLONNES.keySet().toArray(String[]::new));

        SoftAssertions soft = new SoftAssertions();
        COLONNES.forEach((mesure, colonne) -> {
            if (!ENCAISSEMENTS.equals(mesure)) {
                vue(colonne).forEach((cle, valeur) -> {
                    String[] p = cle.split("\\|");
                    soft.assertThat(f.valeur(mesure, UUID.fromString(p[0]), Integer.parseInt(p[1]),
                            Integer.parseInt(p[2])).doubleValue()).as("%s %s", mesure, cle)
                            .isCloseTo(valeur.doubleValue(), within(0.01));
                });
            }
        });
        Map<Integer, BigDecimal> encaissementsVue = new HashMap<>();
        vue("EncaissementReal").forEach((cle, valeur) ->
                encaissementsVue.merge(Integer.parseInt(cle.split("\\|")[1]), valeur, BigDecimal::add));
        List<Integer> annees = new ArrayList<>(encaissementsVue.keySet());
        assertThat(annees).isNotEmpty();
        for (Integer annee : annees) {
            BigDecimal attendu = annee == 2026 ? ECART_2026 : BigDecimal.ZERO;
            soft.assertThat(f.annee(ENCAISSEMENTS, annee).subtract(encaissementsVue.get(annee)).doubleValue())
                    .as("encaissements %d : service − vue", annee).isCloseTo(attendu.doubleValue(), within(1.0));
        }
        soft.assertThat(f.anomalies().reglementsSansDate()).as("ligne sans date de 20 000")
                .anyMatch(n -> n.compareTo(new BigDecimal("20000")) == 0);
        soft.assertAll();
    }
}
