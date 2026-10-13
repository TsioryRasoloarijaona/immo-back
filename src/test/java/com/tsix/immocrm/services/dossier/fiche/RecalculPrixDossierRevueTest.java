package com.tsix.immocrm.services.dossier.fiche;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** {@code Sale.cs:RecalcDiscount} : le montant de remise est gardé, le taux suit le nouveau HT avant remise. */
class RecalculPrixDossierRevueTest {

    @Test
    void superficieQuiChange_montantDeRemiseGarde_tauxRecalcule_commeLeLegataire() {
        RecalculPrixDossier.SousArticle cave = new RecalculPrixDossier.SousArticle(UUID.randomUUID(),
                new BigDecimal("10"), new BigDecimal("0.2"), new BigDecimal("100000"));
        RecalculPrixDossier.Saisie saisie = new RecalculPrixDossier.Saisie(new BigDecimal("110"),
                new BigDecimal("0.2"), new BigDecimal("10000"), new BigDecimal("50000"),
                false, null, null, List.of(cave));

        RecalculPrixDossier.Resultat r = RecalculPrixDossier.calculer(saisie);

        assertAll(
                () -> assertThat(r.totalHT()).isEqualByComparingTo("1200000"),
                () -> assertThat(r.remiseMontantHT()).isEqualByComparingTo("50000"),
                () -> assertThat(r.remise()).isEqualByComparingTo("0.041667"),
                () -> assertThat(r.prixVenteHT()).isEqualByComparingTo("1150000"),
                () -> assertThat(r.prixVenteTTC()).isEqualByComparingTo("1380000"),
                () -> assertThat(r.prix()).isEqualByComparingTo("1380000"),
                () -> assertThat(r.superficieGlobale()).isEqualByComparingTo("120"));
    }
}
