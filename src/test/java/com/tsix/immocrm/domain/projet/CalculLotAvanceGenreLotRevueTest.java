package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesLot.Somme;
import com.tsix.immocrm.dto.projet.BlocAvanceDto;
import com.tsix.immocrm.dto.projet.SuiviLotDto;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static java.math.BigDecimal.ZERO;

/** Revue L2b, S1 : AdvanceAmountValidated / AdvanceSettled = pièces ORDER_ADVANCE si IsMultiSupplier, MARKET_ADVANCE sinon. */
class CalculLotAvanceGenreLotRevueTest {

    private static SuiviLotDto suivi(boolean multi) {
        Map<Somme, BigDecimal> m = new EnumMap<>(Somme.class);
        m.put(Somme.AVANCE_VALIDEE, new BigDecimal("1000"));
        m.put(Somme.AVANCE_CMD_VALIDEE, new BigDecimal("300"));
        m.put(Somme.REGLE_AVANCE, new BigDecimal("900"));
        m.put(Somme.REGLE_AVANCE_CMD, new BigDecimal("200"));
        m.put(Somme.REGLE_AVANCE_DEC, new BigDecimal("50"));
        m.put(Somme.FAC_AVANCE, new BigDecimal("100"));
        EntreesLot.Drapeaux d = new EntreesLot.Drapeaux(multi, true, false, false, false, false);
        EntreesLot.Taux t = new EntreesLot.Taux(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
        return new CalculLot(new EntreesLot(d, t, m)).suivi();
    }

    @Test
    void lotMultiFournisseur_avanceSurCommande() {
        SuiviLotDto s = suivi(true);
        BlocAvanceDto a = s.avanceMarche();

        SoftAssertions.assertSoftly(x -> {
            x.assertThat(a.validee()).isEqualByComparingTo("300");
            x.assertThat(a.reglee()).isEqualByComparingTo("200");
            x.assertThat(a.resteAAmortir()).isEqualByComparingTo("100");
            x.assertThat(s.retenues().facture().penaliteAvance()).as("200 + 50 − 100").isEqualByComparingTo("150");
        });
    }

    @Test
    void lotSimple_avanceSurMarche() {
        SuiviLotDto s = suivi(false);
        BlocAvanceDto a = s.avanceMarche();

        SoftAssertions.assertSoftly(x -> {
            x.assertThat(a.validee()).isEqualByComparingTo("1000");
            x.assertThat(a.reglee()).isEqualByComparingTo("900");
            x.assertThat(s.retenues().facture().penaliteAvance()).as("900 + 50 − 100").isEqualByComparingTo("850");
        });
    }
}
