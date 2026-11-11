package com.tsix.immocrm.domain.achat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R5 — net à payer d'une pièce d'achat (FEATURE-achat-regle-commune.md §1.5, §3 R5, §7 n° 1), fonction pure
 * {@code NetAPayerAchat.calculer(TTC, composantes)} : TTC + régul + timbre + stock + prorata + fluctuations − proportion
 * − garantie − avance − TRC − DEC − avance décompte − retenue − avoirs non financiers, {@code BigDecimal} sans arrondi.
 * Lue par réflexion (la classe n'existe pas avant le lot) : contrat = classe {@code domain.achat.NetAPayerAchat}, méthode
 * statique {@code calculer(BigDecimal, <record>)}, composantes du record nommées comme {@link #COMPOSANTES}.
 */
class NetAPayerAchatTest {

    private static final String CLASSE = "com.tsix.immocrm.domain.achat.NetAPayerAchat";
    private static final BigDecimal TTC = new BigDecimal("1000");

    /** Composante du record → signe dans la formule §1.5. */
    private static final Map<String, Integer> COMPOSANTES = Map.ofEntries(
            Map.entry("amountRegul", 1),
            Map.entry("fiscalStamp", 1),
            Map.entry("stockAmountToPay", 1),
            Map.entry("prorataRefundAmount", 1),
            Map.entry("fluctuationsToPay", 1),
            Map.entry("proportionAmount", -1),
            Map.entry("warrantyAmount", -1),
            Map.entry("advanceAmount", -1),
            Map.entry("insuranceTRCAmount", -1),
            Map.entry("insuranceDECAmount", -1),
            Map.entry("workingOutAdvanceAmount", -1),
            Map.entry("retainedAmount", -1),
            Map.entry("avoirsNonFinanciers", -1));

    // ------------------------------------------------------------------ outils (réflexion)

    private static Method calculer() throws ClassNotFoundException {
        return Arrays.stream(Class.forName(CLASSE).getMethods())
                .filter(m -> m.getName().equals("calculer") && Modifier.isStatic(m.getModifiers())
                        && m.getParameterCount() == 2 && m.getParameterTypes()[0] == BigDecimal.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("static calculer(BigDecimal, composantes) absente de " + CLASSE));
    }

    /** Record des composantes : celles de {@code valeurs} posées, toute autre composante BigDecimal à 0. */
    private static Object composantes(Class<?> type, Map<String, BigDecimal> valeurs) throws ReflectiveOperationException {
        assertThat(type.isRecord()).as("composantes = record").isTrue();
        RecordComponent[] champs = type.getRecordComponents();
        List<String> noms = Arrays.stream(champs).map(RecordComponent::getName).toList();
        assertThat(noms).as("composantes du record").containsAll(valeurs.keySet());
        Object[] args = new Object[champs.length];
        for (int i = 0; i < champs.length; i++) {
            assertThat(champs[i].getType()).as(champs[i].getName()).isEqualTo(BigDecimal.class);
            args[i] = valeurs.getOrDefault(champs[i].getName(), BigDecimal.ZERO);
        }
        Constructor<?> constructeur = type.getDeclaredConstructor(
                Arrays.stream(champs).map(RecordComponent::getType).toArray(Class<?>[]::new));
        return constructeur.newInstance(args);
    }

    private static BigDecimal net(BigDecimal ttc, Map<String, BigDecimal> valeurs) throws ReflectiveOperationException {
        Method m = calculer();
        return (BigDecimal) m.invoke(null, ttc, composantes(m.getParameterTypes()[1], valeurs));
    }

    // ------------------------------------------------------------------ R5

    @Test
    void r5_composantesNommeesCommeLaFormule_toutesBigDecimal() throws Exception {
        Class<?> type = calculer().getParameterTypes()[1];

        assertThat(Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList())
                .containsAll(COMPOSANTES.keySet());
    }

    @Test
    void r5_composantesAZero_netEgalTTC() throws Exception {
        assertThat(net(new BigDecimal("8000.123456"), Map.of())).isEqualByComparingTo("8000.123456");
    }

    static Stream<Arguments> chaqueComposante() {
        return COMPOSANTES.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    /** §1.5 : chaque composante seule, avec son signe. */
    @ParameterizedTest(name = "{0} signe {1}")
    @MethodSource("chaqueComposante")
    void r5_chaqueComposante_avecSonSigne(String composante, int signe) throws Exception {
        BigDecimal valeur = new BigDecimal("123.45");

        BigDecimal net = net(TTC, Map.of(composante, valeur));

        assertThat(net).isEqualByComparingTo(TTC.add(valeur.multiply(BigDecimal.valueOf(signe))));
    }

    @Test
    void r5_avoirNonFinancier_soustrait() throws Exception {
        assertThat(net(new BigDecimal("45000"), Map.of("avoirsNonFinanciers", new BigDecimal("3183"))))
                .isEqualByComparingTo("41817");
    }

    /** Pas d'arrondi : une décimale au-delà de la 6e reste dans le résultat. */
    @Test
    void r5_sansArrondi() throws Exception {
        BigDecimal net = net(new BigDecimal("100.1234567"), Map.of("retainedAmount", new BigDecimal("0.0000003")));

        assertThat(net).isEqualByComparingTo("100.1234564");
    }

    /** Toutes les composantes ensemble : la somme algébrique exacte. */
    @Test
    void r5_toutesLesComposantes_sommeAlgebrique() throws Exception {
        Map<String, BigDecimal> valeurs = Map.ofEntries(
                Map.entry("amountRegul", new BigDecimal("1")), Map.entry("fiscalStamp", new BigDecimal("2")),
                Map.entry("stockAmountToPay", new BigDecimal("4")), Map.entry("prorataRefundAmount", new BigDecimal("8")),
                Map.entry("fluctuationsToPay", new BigDecimal("16")), Map.entry("proportionAmount", new BigDecimal("32")),
                Map.entry("warrantyAmount", new BigDecimal("64")), Map.entry("advanceAmount", new BigDecimal("128")),
                Map.entry("insuranceTRCAmount", new BigDecimal("256")), Map.entry("insuranceDECAmount", new BigDecimal("512")),
                Map.entry("workingOutAdvanceAmount", new BigDecimal("1024")),
                Map.entry("retainedAmount", new BigDecimal("2048")), Map.entry("avoirsNonFinanciers", new BigDecimal("4096")));

        assertThat(net(new BigDecimal("10000"), valeurs)).isEqualByComparingTo(String.valueOf(10000 + 31 - 8160));
    }

    /** Cas réel yesRemsProd : facture de décompte 21/00006 (garantie, avance, DEC, avance décompte) → 980 720. */
    @Test
    void r5_casReel_factureDecompteAvecGarantieEtAvance() throws Exception {
        Map<String, BigDecimal> valeurs = Map.of(
                "warrantyAmount", new BigDecimal("136800.000000"),
                "advanceAmount", new BigDecimal("136800.000000"),
                "insuranceDECAmount", new BigDecimal("13680.000000"),
                "workingOutAdvanceAmount", new BigDecimal("100000.000000"));

        assertThat(net(new BigDecimal("1368000.000000"), valeurs)).isEqualByComparingTo("980720.000000");
    }
}
