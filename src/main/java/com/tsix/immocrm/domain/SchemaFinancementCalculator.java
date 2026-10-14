package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.com.ComSettlementCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Calcul du « Schéma de financement » initial ({@code Sale.cs:GenerateInitialDocuments}), partagé par
 * {@code DossierService} (création, simulation) et {@code DevisService} (fiche offre) : un même (tranche, prix)
 * ne doit jamais donner deux montants. Voir FEATURE-vente-echeancier.md §1.6.
 */
public final class SchemaFinancementCalculator {

    /** Avance par défaut (1/3, décision métier) si la tranche n'a pas d'{@code AdvancePercent}. */
    private static final BigDecimal DEFAULT_ADVANCE_PERCENT =
            BigDecimal.ONE.divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);
    private static final int CENTIMES = 2;
    private static final int ECHELLE = 6;

    private SchemaFinancementCalculator() {
    }

    public record Ligne(String categorieCode, BigDecimal montant) {
    }

    /**
     * Dans l'ordre d'OMRAN : pré-réservation ; avance (montant fixe si {@code AdvanceBasedOnAmount}, sinon
     * prix × {@code AdvancePercent}) diminuée de la pré-réservation ; autofinancement = prix − avance −
     * pré-réservation ; frais de dossier ; frais de syndic ({@code SyndicAmount} × {@code SyndicPeriod}) ; lignes
     * {@code CfgTrancheSettlementCategory} d'une catégorie pas encore présente. Montant nul ⇒ pas de ligne, sauf
     * l'autofinancement, toujours écrit.
     */
    public static List<Ligne> calculer(CfgTranche tranche, BigDecimal prix) {
        List<Ligne> lignes = new ArrayList<>();
        BigDecimal preReservation = tranche != null ? montant(tranche.getBeforeReservationAmount()) : BigDecimal.ZERO;
        if (preReservation.signum() != 0) {
            lignes.add(new Ligne(RoleCategorieEcheance.PRE_RESERVATION.code(), preReservation));
        }
        BigDecimal dejaAvance = preReservation.setScale(CENTIMES, RoundingMode.HALF_UP);
        BigDecimal avanceCible = avanceCible(tranche, prix);
        BigDecimal avance = BigDecimal.ZERO;
        if (avanceCible.signum() != 0) {
            avance = avanceCible.subtract(dejaAvance);
            if (avance.signum() > 0) {
                lignes.add(new Ligne(RoleCategorieEcheance.AVANCE.code(), avance));
            }
        }
        BigDecimal avanceRetenue = avance.signum() > 0 ? avance : BigDecimal.ZERO;
        lignes.add(new Ligne(RoleCategorieEcheance.AUTO_FINANCEMENT.code(),
                prix.subtract(avanceRetenue).subtract(dejaAvance)));
        if (tranche == null) {
            return lignes;
        }
        ajouterSiNonNul(lignes, RoleCategorieEcheance.FRAIS_DOSSIER, montant(tranche.getDefaultExpenseAmount()));
        ajouterSiNonNul(lignes, RoleCategorieEcheance.FRAIS_SYNDIC, fraisSyndic(tranche));
        ajouterLignesDeTranche(lignes, tranche, prix);
        return lignes;
    }

    /** {@code AdvanceBasedOnAmount} ⇒ {@code AdvanceAmount} ; sinon prix × taux, arrondi au centime. */
    private static BigDecimal avanceCible(CfgTranche tranche, BigDecimal prix) {
        if (tranche != null && Boolean.TRUE.equals(tranche.getAdvanceBasedOnAmount())) {
            return montant(tranche.getAdvanceAmount());
        }
        BigDecimal taux = tranche != null && tranche.getAdvancePercent() != null
                ? tranche.getAdvancePercent()
                : DEFAULT_ADVANCE_PERCENT;
        return prix.multiply(taux).setScale(CENTIMES, RoundingMode.HALF_UP);
    }

    /** {@code SyndicAmount × SyndicPeriod} ; 0 si l'un manque (frais affichés par défaut à l'ajout d'une ligne). */
    public static BigDecimal fraisSyndic(CfgTranche tranche) {
        if (tranche == null || tranche.getSyndicAmount() == null || tranche.getSyndicPeriod() == null) {
            return BigDecimal.ZERO;
        }
        return tranche.getSyndicAmount().multiply(BigDecimal.valueOf(tranche.getSyndicPeriod()))
                .setScale(CENTIMES, RoundingMode.HALF_UP);
    }

    private static void ajouterSiNonNul(List<Ligne> lignes, RoleCategorieEcheance role, BigDecimal montant) {
        if (montant.signum() != 0 && role.code() != null) {
            lignes.add(new Ligne(role.code(), montant));
        }
    }

    /** {@code PercentOf} non nul ⇒ pourcentage du prix, sinon {@code AmountTTC} ; une catégorie déjà présente est sautée. */
    private static void ajouterLignesDeTranche(List<Ligne> lignes, CfgTranche tranche, BigDecimal prix) {
        List<CfgTrancheSettlementCategory> propres = tranche.getCategoriesReglement();
        if (propres == null || propres.isEmpty()) {
            return;
        }
        Set<String> presentes = new HashSet<>();
        lignes.forEach(l -> presentes.add(l.categorieCode()));
        for (CfgTrancheSettlementCategory ligneTranche : propres) {
            ComSettlementCategory categorie = ligneTranche.getComSettlementCategory();
            String code = categorie != null ? categorie.getCode() : null;
            if (code == null || !presentes.add(code)) {
                continue;
            }
            BigDecimal pourcentage = montant(ligneTranche.getPercentOf());
            BigDecimal valeur = pourcentage.signum() != 0
                    ? pourcentage.multiply(prix).setScale(ECHELLE, RoundingMode.HALF_UP)
                    : montant(ligneTranche.getAmountTTC());
            lignes.add(new Ligne(code, valeur));
        }
    }

    private static BigDecimal montant(BigDecimal valeur) {
        return Objects.requireNonNullElse(valeur, BigDecimal.ZERO);
    }
}
