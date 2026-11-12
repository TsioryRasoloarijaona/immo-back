package com.tsix.immocrm.domain.achat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Q1 : {@code impaye()} = {@code ROUND(reste, 2) > 0} comme OMRAN, indépendant de {@code aRegler()} (tolérance 0,05
 * inchangée) — FEATURE-achat-impayes-soldes.md, réponses du manager (12/11). Appel par réflexion : méthode à écrire.
 */
class SoldeDocumentAchatImpayeTest {

    private static boolean impaye(SoldeDocumentAchat solde) throws Exception {
        Method m = SoldeDocumentAchat.class.getMethod("impaye");
        return (Boolean) m.invoke(solde);
    }

    @ParameterizedTest(name = "à payer {0}, réglé {1} -> impayé {2}")
    @CsvSource({
            "500, 499.99, true",
            "500, 499.96, true",
            "500, 499.95, true",
            "500, 499.995, true",
            "500, 499.996, false",
            "500, 500, false",
            "500, 500.01, false",
            "3000, 3020, false",
            "-40, 0, false"
    })
    void q1_impaye_resteArrondiA2StrictementPositif(String aPayer, String regle, boolean attendu) throws Exception {
        SoldeDocumentAchat solde = SoldeDocumentAchat.de(new BigDecimal(aPayer), new BigDecimal(regle));

        assertThat(impaye(solde)).isEqualTo(attendu);
    }
}
