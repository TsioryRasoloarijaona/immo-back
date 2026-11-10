package com.tsix.immocrm.domain.achat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Calcul unique du solde d'une pièce d'achat — FEATURE-achat-impayes.md §3.1, §8 test 1.
 * Les méthodes du lot A3 sont appelées par réflexion : la classe compile avant le code, les noms restent ceux du §3.1.
 */
class SoldeDocumentAchatTest {

    private static final String A_DEDUIRE = "aDeduire";
    private static final String TROP_REGLE = "tropRegle";
    private static final String A_REGLER = "aRegler";

    private static SoldeDocumentAchat de(String aPayer, String regle) {
        return SoldeDocumentAchat.de(new BigDecimal(aPayer), new BigDecimal(regle));
    }

    private static boolean etat(SoldeDocumentAchat solde, String methode) throws ReflectiveOperationException {
        Method m = SoldeDocumentAchat.class.getMethod(methode);
        return (Boolean) m.invoke(solde);
    }

    private static SoldeDocumentAchat total(Collection<SoldeDocumentAchat> soldes) throws ReflectiveOperationException {
        Method m = SoldeDocumentAchat.class.getMethod("total", Collection.class);
        assertThat(Modifier.isStatic(m.getModifiers())).as("total est statique").isTrue();
        return (SoldeDocumentAchat) m.invoke(null, soldes);
    }

    // §8.1 : à payer −2 000 000, réglé 0 (récupération d'avance)
    @Test
    void s31_aPayerNegatif_aDeduireVrai_tropRegleFaux_aReglerFaux() throws Exception {
        SoldeDocumentAchat solde = de("-2000000", "0");

        assertAll(
                () -> assertThat(etat(solde, A_DEDUIRE)).isTrue(),
                () -> assertThat(etat(solde, TROP_REGLE)).isFalse(),
                () -> assertThat(etat(solde, A_REGLER)).isFalse());
    }

    // §8.1 : à payer 3 000, réglé 3 020 (cas 22/00050)
    @Test
    void s31_regleAuDelaDUnAPayerPositif_tropRegleVrai_aDeduireFaux_aReglerFaux() throws Exception {
        SoldeDocumentAchat solde = de("3000", "3020");

        assertAll(
                () -> assertThat(etat(solde, TROP_REGLE)).isTrue(),
                () -> assertThat(etat(solde, A_DEDUIRE)).isFalse(),
                () -> assertThat(etat(solde, A_REGLER)).isFalse());
    }

    // §8.1 : reste 0,05 → ni aRegler ni tropRegle ; reste 0,06 → aRegler (seuils stricts du §3.1)
    @ParameterizedTest(name = "à payer {0}, réglé {1} → aRegler {2}, tropRegle {3}")
    @CsvSource({
            "500, 499.95, false, false",
            "500, 499.94, true, false",
            "500, 500.05, false, false",
            "500, 500.06, false, true"})
    void s31_seuils005_aReglerEtTropRegle(String aPayer, String regle, boolean aRegler, boolean tropRegle)
            throws Exception {
        SoldeDocumentAchat solde = de(aPayer, regle);

        assertAll(
                () -> assertThat(etat(solde, A_REGLER)).isEqualTo(aRegler),
                () -> assertThat(etat(solde, TROP_REGLE)).isEqualTo(tropRegle),
                () -> assertThat(etat(solde, A_DEDUIRE)).isFalse());
    }

    // §3.1 : aDeduire = montantAPayer < −0,05 (strict)
    @ParameterizedTest(name = "à payer {0} → aDeduire {1}")
    @CsvSource({"-0.05, false", "-0.06, true", "0, false"})
    void s31_seuilADeduire(String aPayer, boolean attendu) throws Exception {
        SoldeDocumentAchat solde = de(aPayer, "0");

        assertAll(
                () -> assertThat(etat(solde, A_DEDUIRE)).isEqualTo(attendu),
                () -> assertThat(etat(solde, TROP_REGLE)).isFalse());
    }

    // §8.1 : total de deux soldes = de(Σ à payer, Σ réglé)
    @Test
    void s31_total_deDeuxSoldes_egaleLaSomme() throws Exception {
        SoldeDocumentAchat t = total(List.of(de("100", "30"), de("-40", "0")));

        assertThat(t).isEqualTo(SoldeDocumentAchat.de(new BigDecimal("60"), new BigDecimal("30")));
    }

    @Test
    void s31_total_resteEtSoldeRecalculesSurLesSommes() throws Exception {
        SoldeDocumentAchat t = total(List.of(de("3000", "3020"), de("20", "0")));

        assertAll(
                () -> assertThat(t.montantAPayer()).isEqualByComparingTo("3020"),
                () -> assertThat(t.montantRegle()).isEqualByComparingTo("3020"),
                () -> assertThat(t.resteARegler()).isEqualByComparingTo("0"),
                () -> assertThat(t.solde()).isTrue());
    }

    // §3.1 : SQL_REGLE, constante publique partagée par toute requête qui lit un réglé d'achat
    @Test
    void s31_sqlRegle_constantePubliqueStatique() throws Exception {
        Field f = SoldeDocumentAchat.class.getField("SQL_REGLE");

        assertAll(
                () -> assertThat(Modifier.isStatic(f.getModifiers())).isTrue(),
                () -> assertThat(f.getType()).isEqualTo(String.class),
                () -> assertThat((String) f.get(null)).contains("StlSettlementLine", "BuyDocumentId"));
    }
}
