package com.tsix.immocrm.domain.achat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LigneAchatCalculeeTest {

    private static BigDecimal nombre(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    @ParameterizedTest(name = "PU {0} × qté {1}, remise {2}, FODEC {3}, TVA {4}")
    @CsvSource(nullValues = "-", value = {
            "100, 2, 0,    0,    0.2, 100,   200,   40,     240",
            "100, 2, 0.1,  0,    0.2, 90,    180,   36,     216",
            "100, 2, 0,    0.01, 0.2, 101,   202,   40.4,   242.4",
            "100, 3, 0.1,  0.01, 0.2, 90.9,  272.7, 54.54,  327.24",
            "10,  1, 0.05, -,    -,   9.5,   9.5,   0,      9.5",
            "-,   5, 0.1,  0.01, 0.2, 0,     0,     0,      0",
            "33.33, -, 0,  0,    0.2, 33.33, 0,     0,      0",
            "0.333, 3, 0,  0,    0.07, 0.333, 0.999, 0.06993, 1.06893"})
    void de_pleineEchelle_sansArrondi_absentVautZero(String pu, String qte, String remise, String fodec, String tva,
                                                      String net, String ht, String montantTva, String ttc) {
        LigneAchatCalculee ligne = LigneAchatCalculee.de(nombre(pu), nombre(qte), nombre(remise), nombre(fodec),
                nombre(tva));

        assertThat(ligne).satisfies(
                l -> assertThat(l.prixUnitaireHTNet()).isEqualByComparingTo(net),
                l -> assertThat(l.totalHT()).isEqualByComparingTo(ht),
                l -> assertThat(l.totalTVA()).isEqualByComparingTo(montantTva),
                l -> assertThat(l.totalTTC()).isEqualByComparingTo(ttc));
    }
}
