package com.tsix.immocrm.domain.foncier;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Surfaces d'une prospection terrain ({@code Prospection.cs:1141-1548}) et d'une zone ({@code LndLandZone.cs:80-500}) :
 * seule implémentation, appelée à chaque écriture et par l'aperçu (FEATURE-patrimoine-prospections-terrain.md §3).
 */
public final class CalculSurfacesTerrain {

    private static final MathContext MC = MathContext.DECIMAL128;

    private CalculSurfacesTerrain() {
    }

    private record Paire(BigDecimal coef, BigDecimal montant) {
    }

    /**
     * Par paire, le montant gagne s'il est {@code champModifie} ou donné sans son coefficient ; sinon montant = coef × base.
     * {@code maroc} = {@code CountryFormule} MA ; division par zéro → 0 comme OMRAN.
     */
    public static SurfacesTerrain prospection(SurfacesTerrain e, String champModifie, boolean maroc) {
        return calculer(e, champModifie, maroc, false);
    }

    /** Zone : sans voies, formules MA ; plancher couvert donné → {@code Cuf = AreaCuf / (Area × Cos)} (piège OMRAN gardé). */
    public static SurfacesTerrain zone(SurfacesTerrain e) {
        return calculer(e, null, true, true);
    }

    private static SurfacesTerrain calculer(SurfacesTerrain e, String champ, boolean maroc, boolean zone) {
        BigDecimal superficie = z(e.superficie());
        BigDecimal voies = z(e.superficieEquipements());
        BigDecimal nette = superficie.subtract(voies).subtract(z(e.superficieEquipementACeder()))
                .subtract(z(e.superficieARegler())).subtract(z(e.superficieACeder()));
        BigDecimal baseEmprise = maroc ? nette : superficie;
        Paire emprise = paire(e.cos(), e.superficieCos(), "superficieCos", champ, baseEmprise, baseEmprise);
        BigDecimal empriseBrute = superficie.multiply(emprise.coef(), MC);
        BigDecimal baseCouvert = maroc ? nette : empriseBrute;
        Paire couvert = paire(e.cuf(), e.superficieCuf(), "superficieCuf", champ, baseCouvert,
                zone ? empriseBrute : baseCouvert);
        Paire vendable = paire(e.coefVendable(), e.superficieVendable(), "superficieVendable", champ, couvert.montant(),
                couvert.montant());
        Paire commerce = paire(e.coefCommerce(), e.superficieCommerce(), "superficieCommerce", champ, superficie, superficie);
        Paire bureau = paire(e.coefBureau(), e.superficieBureau(), "superficieBureau", champ, superficie, superficie);
        Paire parking = paire(e.coefParking(), e.superficieParking(), "superficieParking", champ, superficie, superficie);
        Paire prix = paire(e.prixM2(), e.prix(), "prix", champ, superficie, superficie);
        BigDecimal habitat = vendable.montant().subtract(bureau.montant()).subtract(commerce.montant());
        return new SurfacesTerrain(r(superficie), r(voies), r(z(e.superficieEquipementACeder())),
                r(z(e.superficieARegler())), r(z(e.superficieACeder())), r(diviser(voies, nette)), r(nette),
                r(emprise.coef()), r(emprise.montant()), r(couvert.coef()), r(couvert.montant()),
                r(vendable.coef()), r(vendable.montant()), r(commerce.coef()), r(commerce.montant()),
                r(bureau.coef()), r(bureau.montant()), r(habitat), r(parking.coef()), r(parking.montant()),
                r(prix.coef()), r(prix.montant()));
    }

    private static Paire paire(BigDecimal coef, BigDecimal montant, String nomMontant, String champ, BigDecimal base,
                               BigDecimal baseInverse) {
        if (montant != null && (nomMontant.equals(champ) || coef == null)) {
            return new Paire(diviser(montant, baseInverse), montant);
        }
        BigDecimal c = z(coef);
        return new Paire(c, c.multiply(base, MC));
    }

    private static BigDecimal diviser(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? BigDecimal.ZERO : a.divide(b, MC);
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Échelle des colonnes {@code DECIMAL(26,6)}. */
    private static BigDecimal r(BigDecimal v) {
        return v.setScale(6, RoundingMode.HALF_UP);
    }
}
