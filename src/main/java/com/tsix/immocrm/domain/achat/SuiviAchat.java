package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Suivis OMRAN des avances ({@code BuyMonitoringAdvanceView}) et des retenues de garantie ({@code BuyMonitoringWarrantyView}),
 * recalculés sur {@code BuyDocument d} (FEATURE-n7-etats-achat.md R5, R6) : une pièce donne une ligne « montant retenu »
 * (bloc a) et une ligne « nature » (bloc b) ; chaque ligne porte trois montants dans l'ordre des colonnes de l'écran.
 */
public enum SuiviAchat {

    AVANCES(List.of("Avance", "Ajustement", "Amortissement"), "Yes.Buy.Front.Tdb.BuyMonitoringAdvanceView",
            "ISNULL(d.AdvanceAmount, 0) <> 0 OR ISNULL(d.WorkingOutAdvanceAmount, 0) <> 0",
            "ISNULL(d.AdvanceAmount, 0) + ISNULL(d.WorkingOutAdvanceAmount, 0)",
            Set.of(uuid("E7999F70-7507-40D9-AA78-A2825C42B601"), uuid("E7999F70-7507-40D9-AA78-A2825C42B603"),
                    uuid("E7999F70-7507-40D9-AA78-A2825C42C004"), uuid("D35918ED-0B9F-42AE-BB6B-EBAA35321EDD"))),
    RETENUES(List.of("Retenue", "Pénalité", "Remboursé"), "Yes.Buy.Front.Tdb.BuyMonitoringWarrantyView",
            "ISNULL(d.WarrantyAmount, 0) <> 0",
            "ISNULL(d.WarrantyAmount, 0)",
            Set.of(uuid("A71B4F91-AEFC-4C70-87F7-82A6C7365CB5"), uuid("E7999F70-7507-40D9-AA78-A2825C42B602"),
                    uuid("0168074F-7507-40D9-AA78-A2825C42C857"), uuid("E7999F70-7507-40D9-AA78-A2825C42C866")));

    /** Trois montants d'une ligne, dans l'ordre des colonnes de l'état. */
    public record Montants(BigDecimal premier, BigDecimal deuxieme, BigDecimal troisieme) {

        public static final Montants ZERO = new Montants(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        public Montants plus(Montants autre) {
            return new Montants(premier.add(autre.premier), deuxieme.add(autre.deuxieme), troisieme.add(autre.troisieme));
        }
    }

    private final List<String> libelles;
    private final String droit;
    private final String condition;
    private final String montant;
    private final Set<UUID> natures;

    SuiviAchat(List<String> libelles, String droit, String condition, String montant, Set<UUID> natures) {
        this.libelles = libelles;
        this.droit = droit;
        this.condition = condition;
        this.montant = montant;
        this.natures = natures;
    }

    /** Libellés des trois montants, dans l'ordre de {@link Montants}. */
    public List<String> libelles() {
        return libelles;
    }

    /** Droit OMRAN de l'écran ; l'export exige aussi {@link #droitExport()}. */
    public String droit() {
        return droit;
    }

    public String droitExport() {
        return droit + ".toolExport";
    }

    /** SQL : pièce du bloc a (montant retenu non nul). */
    public String condition() {
        return condition;
    }

    /** SQL : montant retenu signé (avoir négatif). */
    public String montantSigne() {
        return "CASE WHEN d.DocType = 'REFUND' THEN -1 ELSE 1 END * (" + montant + ")";
    }

    public Set<UUID> natures() {
        return natures;
    }

    /**
     * Lignes d'une pièce : bloc a si {@code retenu}, bloc b si {@code deNature} ; {@code aPayer} = AmountToPay,
     * {@code regul} = AmountRegul.
     */
    public List<Montants> lignes(boolean retenu, BigDecimal montantSigne, boolean deNature, BigDecimal aPayer,
                                 BigDecimal regul) {
        Stream<Montants> a = retenu ? Stream.of(blocA(montantSigne)) : Stream.empty();
        Stream<Montants> b = deNature ? Stream.of(blocB(aPayer, regul)) : Stream.empty();
        return Stream.concat(a, b).toList();
    }

    private Montants blocA(BigDecimal montantSigne) {
        return this == AVANCES ? new Montants(BigDecimal.ZERO, BigDecimal.ZERO, montantSigne)
                : new Montants(montantSigne, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private Montants blocB(BigDecimal aPayer, BigDecimal regul) {
        return this == AVANCES ? new Montants(aPayer, regul, BigDecimal.ZERO)
                : new Montants(BigDecimal.ZERO, regul, aPayer);
    }

    private static UUID uuid(String texte) {
        return UUID.fromString(texte);
    }
}
