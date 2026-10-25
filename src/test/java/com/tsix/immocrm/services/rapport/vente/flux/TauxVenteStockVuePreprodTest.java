package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.services.rapport.stock.MouvementStock;
import com.tsix.immocrm.services.rapport.stock.MouvementsStockTranche;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Rapprochement du n° 24 avec la vue {@code StkMotionView} (FEATURE-tdb-vente-rapports-rythme.md §9.2) : vue absente de
 * yesRemsTest et de la CI, test ignoré là. À la main sur yesRemsProd ({@code DB_NAME=yesRemsProd},
 * {@code -Dtest=TauxVenteStockVuePreprodTest}) ; attendu : identique sur les tranches en vente des sociétés actives.
 */
@SpringBootTest
class TauxVenteStockVuePreprodTest {

    private static final String TRANCHES = """
            SELECT t.PKey FROM CfgTranche t JOIN CfgProject p ON p.PKey = t.CfgProjectId
            JOIN CfgCompany c ON c.PKey = p.CfgCompanyId AND c.IsActive = 1 WHERE t.IsForSale = 1""";

    private static final String VUE = """
            SELECT x.t, x.a, x.l, SUM(x.n) AS n
            FROM (SELECT v.CfgTrancheDescription AS t, YEAR(v.MotionDate) AS a, v.MotionCount AS n,
                         CASE WHEN v.MotionLabel = 'LANCEMENT DE LA COMMERCIALISATION' THEN 1 ELSE 0 END AS l
                  FROM StkMotionView v
                  WHERE v.CfgTrancheDescription IN (SELECT t.Description FROM CfgTranche t
                      JOIN CfgProject p ON p.PKey = t.CfgProjectId
                      JOIN CfgCompany c ON c.PKey = p.CfgCompanyId AND c.IsActive = 1 WHERE t.IsForSale = 1)) x
            GROUP BY x.t, x.a, x.l""";

    @Autowired private MouvementsStockTranche stock;
    @Autowired private JdbcTemplate jdbc;

    private boolean vueExiste() {
        return jdbc.queryForObject("SELECT CASE WHEN OBJECT_ID('dbo.StkMotionView', 'V') IS NULL THEN 0 ELSE 1 END",
                Integer.class) == 1;
    }

    /** {@code tranche|année|lancement?} → Σ des quantités. */
    private static String cle(String tranche, int annee, boolean lancement) {
        return "%s|%d|%s".formatted(tranche, annee, lancement);
    }

    @Test
    void n24_mouvementsParTrancheEtAnnee_identiquesAStkMotionView() {
        assumeTrue(vueExiste(), "StkMotionView absente (yesRemsTest, CI)");
        List<UUID> tranches = jdbc.queryForList(TRANCHES, String.class).stream().map(UUID::fromString).toList();
        Map<String, Long> calcul = new HashMap<>();
        for (MouvementStock m : stock.lire(tranches)) {
            calcul.merge(cle(m.libelleTranche(), m.date().getYear(), m.lancement()), m.quantite(), Long::sum);
        }
        Map<String, Long> vue = new HashMap<>();
        jdbc.query(VUE, rs -> {
            vue.put(cle(rs.getString("t"), rs.getInt("a"), rs.getInt("l") == 1), rs.getLong("n"));
        });

        assertThat(calcul).isNotEmpty().isEqualTo(vue);
    }
}
