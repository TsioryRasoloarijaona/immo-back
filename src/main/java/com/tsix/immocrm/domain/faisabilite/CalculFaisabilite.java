package com.tsix.immocrm.domain.faisabilite;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bloc A et phases d'une étude ({@code omran-src/Yes.Rems/Yes.Stk/StkFeasibilityStudy.cs} l.531-1700) : seule
 * implémentation des formules, appelée à chaque écriture et par l'aperçu (FEATURE-patrimoine-faisabilite.md §2.2).
 */
public final class CalculFaisabilite {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final int ECHELLE = 6;

    private CalculFaisabilite() {
    }

    /** Coefficient et montant d'une paire à deux sens : montant = coef × base. */
    private record Paire(BigDecimal coef, BigDecimal montant) {
    }

    /**
     * Entrées lues dans {@code e} (les champs calculés y sont ignorés) ; {@code maroc} = {@code CountryFormule} MA.
     * Paire : le coefficient s'il est donné, sinon le montant (gardé tel quel) ; division par zéro → 0 comme OMRAN.
     */
    public static ValeursFaisabilite calculer(ValeursFaisabilite e, boolean maroc) {
        BigDecimal superficie = z(e.superficie());
        Paire equipements = paire(e.coefEquipements(), e.superficieEquipements(), superficie);
        BigDecimal nette = superficie.subtract(equipements.montant());
        Paire emprise = paire(e.coefEmprise(), e.empriseAuSol(), maroc ? nette : superficie);
        Paire couvert = paire(e.cuf(), e.plancherCouvert(), maroc ? nette : superficie.multiply(emprise.coef(), MC));
        Paire vendable = paire(e.coefVendable(), e.plancherVendable(), couvert.montant());
        BigDecimal nonBatie = superficie.subtract(emprise.montant());

        Paire prix = paire(e.prixAchatM2(), e.prixTerrain(), superficie);
        BigDecimal prixM2Ht = diviser(prix.coef(), BigDecimal.ONE.add(z(e.tauxTvaTerrain())));
        BigDecimal prixHt = prixM2Ht.multiply(superficie, MC);
        BigDecimal tauxEnregistrement = z(e.tauxFraisEnregistrement());
        BigDecimal enregistrement = tauxEnregistrement.multiply(prix.montant(), MC);
        BigDecimal enregistrementHt = tauxEnregistrement.multiply(prixHt, MC);
        BigDecimal tauxAutres = z(e.tauxAutresFraisTerrain());
        BigDecimal autres = tauxAutres.multiply(prix.montant(), MC);
        BigDecimal autresHt = tauxAutres.multiply(prixHt, MC);

        BigDecimal coutBati = z(e.coutConstructionM2Bati()).multiply(couvert.montant(), MC);
        BigDecimal coutNonBati = z(e.coutConstructionM2NonBati()).multiply(nonBatie, MC);
        BigDecimal autresExecution = z(e.tauxAutresFraisExecution()).multiply(coutBati.add(coutNonBati), MC);

        List<LocalDateTime> d = dates(e);
        return new ValeursFaisabilite(r(superficie), r(equipements.coef()), r(equipements.montant()), r(nette),
                r(emprise.coef()), r(emprise.montant()), r(couvert.coef()), r(couvert.montant()),
                r(vendable.coef()), r(vendable.montant()), r(nonBatie), r(z(e.tauxTvaTerrain())),
                r(prix.coef()), r(prixM2Ht), r(prix.coef().subtract(prixM2Ht)),
                r(prix.montant()), r(prixHt), r(prix.montant().subtract(prixHt)),
                r(tauxEnregistrement), r(enregistrement), r(enregistrementHt), r(enregistrement.subtract(enregistrementHt)),
                r(tauxAutres), r(autres), r(autresHt), r(autres.subtract(autresHt)),
                r(autres.add(prix.montant()).add(enregistrement)), r(autresHt.add(prixHt).add(enregistrementHt)),
                r(z(e.coutConstructionM2Bati())), r(coutBati), r(z(e.coutConstructionM2NonBati())), r(coutNonBati),
                r(z(e.tauxAutresFraisExecution())), r(autresExecution),
                e.debutAchatTerrain(), z(e.dureeAchatTerrain()), d.get(0),
                z(e.delaiAchatTerrainPeriodeValidation()), d.get(1), z(e.dureePeriodeValidation()), d.get(2),
                z(e.delaiPeriodeValidationPermisBatir()), d.get(3), z(e.dureePermisBatir()), d.get(4),
                z(e.delaiPermisBatirEtudes()), d.get(5), z(e.dureeEtudes()), d.get(6),
                z(e.delaiPermisBatirTravaux()), d.get(7), z(e.dureeTravaux()), d.get(8),
                z(e.delaiTravauxConcretisation()), d.get(9), z(e.dureeConcretisation()), d.get(10),
                z(e.delaiTravauxFinalisation()), d.get(11), z(e.dureeFinalisation()), d.get(12),
                new ArrayList<>());
    }

    /**
     * Les 13 dates calculées, dans l'ordre du record (fin d'achat … fin de finalisation), nulles sans début d'achat.
     * Setters OMRAN (mois, {@code AddMonths}) : études = DÉBUT du permis + délai, concrétisation = DÉBUT des travaux +
     * délai, finalisation = fin des travaux + délai (§8 ter.4).
     */
    private static List<LocalDateTime> dates(ValeursFaisabilite e) {
        LocalDateTime finAchat = plus(e.debutAchatTerrain(), e.dureeAchatTerrain());
        LocalDateTime debutValidation = plus(finAchat, e.delaiAchatTerrainPeriodeValidation());
        LocalDateTime finValidation = plus(debutValidation, e.dureePeriodeValidation());
        LocalDateTime debutPermis = plus(finValidation, e.delaiPeriodeValidationPermisBatir());
        LocalDateTime finPermis = plus(debutPermis, e.dureePermisBatir());
        LocalDateTime debutEtudes = plus(debutPermis, e.delaiPermisBatirEtudes());
        LocalDateTime debutTravaux = plus(finPermis, e.delaiPermisBatirTravaux());
        LocalDateTime finTravaux = plus(debutTravaux, e.dureeTravaux());
        LocalDateTime debutConcretisation = plus(debutTravaux, e.delaiTravauxConcretisation());
        LocalDateTime debutFinalisation = plus(finTravaux, e.delaiTravauxFinalisation());
        return Arrays.asList(finAchat, debutValidation, finValidation, debutPermis, finPermis,
                debutEtudes, plus(debutEtudes, e.dureeEtudes()), debutTravaux, finTravaux,
                debutConcretisation, plus(debutConcretisation, e.dureeConcretisation()),
                debutFinalisation, plus(debutFinalisation, e.dureeFinalisation()));
    }

    /** Un montant dont le coefficient arrondi est {@code coef} est gardé : réécrire une étude ne fait pas dériver ses montants. */
    private static Paire paire(BigDecimal coef, BigDecimal montant, BigDecimal base) {
        if (coef != null && montant != null && base.signum() != 0 && r(diviser(montant, base)).compareTo(r(coef)) == 0) {
            return new Paire(coef, montant);
        }
        if (coef != null) {
            return new Paire(coef, coef.multiply(base, MC));
        }
        if (montant != null) {
            return new Paire(diviser(montant, base), montant);
        }
        return new Paire(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static BigDecimal diviser(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? BigDecimal.ZERO : a.divide(b, MC);
    }

    private static LocalDateTime plus(LocalDateTime date, Integer mois) {
        return date == null ? null : date.plusMonths(z(mois));
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int z(Integer v) {
        return v == null ? 0 : v;
    }

    /** Échelle des colonnes {@code DECIMAL(26,6)}. */
    private static BigDecimal r(BigDecimal v) {
        return v.setScale(ECHELLE, RoundingMode.HALF_UP);
    }
}
