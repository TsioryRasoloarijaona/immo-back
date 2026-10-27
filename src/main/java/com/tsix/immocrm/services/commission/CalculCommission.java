package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.ComSalesCommission;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.utils.Champs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Seul calcul d'un montant de commission (C7) : choix de la règle (R-S) puis montant (R-M), FEATURE-vente-commissions.md
 * §2 ; d'après {@code CommissionTranche.cs:CalculateConcretisationCommissionForFolder}, sans arrondi.
 */
public final class CalculCommission {

    static final String MODE_MONTANT = "Montant";

    /** Ce que la règle compare : la personne commissionnée, l'action commerciale, la catégorie, le jour du fait. */
    public record Fait(UUID personneId, UUID actionId, UUID categorieId, LocalDate jour) {
    }

    public record Montant(BigDecimal valeur, String mode) {
    }

    private CalculCommission() {
    }

    /** Règle applicable : sans employé de support, fenêtre contenant le jour, la plus spécifique, puis le départage. */
    public static Optional<ComSalesCommission> regle(List<ComSalesCommission> regles, Fait fait) {
        return regles.stream()
                .filter(r -> r.getComEmployee() == null)
                .filter(r -> dansLaFenetre(r, fait.jour()))
                .filter(r -> correspond(id(r.getCfgTier()), fait.personneId())
                        && correspond(Champs.de(r.getComAction(), ComAction::getId), fait.actionId())
                        && correspond(Champs.de(r.getStkItemCategory(), StkItemCategory::getId), fait.categorieId()))
                .min(Comparator.comparingInt(CalculCommission::niveau)
                        .thenComparingInt(CalculCommission::bornes)
                        .thenComparing(ComSalesCommission::getUserCreateDateTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(r -> r.getId().toString()));
    }

    /** Montant fixe ou pourcentage (ratio) du prix de vente TTC ; colonnes « Extern » pour un prestataire. */
    public static Montant montant(ComSalesCommission r, TypeCommission type, boolean externe, BigDecimal prix) {
        Bloc b = bloc(r, type, externe);
        if (Boolean.TRUE.equals(b.surMontant())) {
            return new Montant(nonNul(b.montant()), MODE_MONTANT);
        }
        BigDecimal taux = nonNul(b.taux());
        String pourcentage = taux.movePointRight(2).stripTrailingZeros().toPlainString();
        return new Montant(nonNul(prix).multiply(taux), pourcentage + " % du prix de vente TTC");
    }

    private record Bloc(Boolean surMontant, BigDecimal montant, BigDecimal taux) {
    }

    private static Bloc bloc(ComSalesCommission r, TypeCommission type, boolean externe) {
        return switch (type) {
            case CONCRETISATION -> externe
                    ? new Bloc(r.getSaleCommissionExternBasedOnAmount(), r.getSaleCommissionAmountExtern(),
                    r.getSaleCommissionPercentExtern())
                    : new Bloc(r.getSaleCommissionBasedOnAmount(), r.getSaleCommissionAmount(), r.getSaleCommissionPercent());
            case FINALISATION_1 -> externe
                    ? new Bloc(r.getDeliveryCommissionExternBasedOnAmount(), r.getDeliveryCommissionAmountExtern(),
                    r.getDeliveryCommissionPercentExtern())
                    : new Bloc(r.getDeliveryCommissionBasedOnAmount(), r.getDeliveryCommissionAmount(),
                    r.getDeliveryCommissionPercent());
            case FINALISATION_2 -> externe
                    ? new Bloc(r.getDeliveryCommissionExtern2BasedOnAmount(), r.getDeliveryCommissionAmountExtern2(),
                    r.getDeliveryCommissionPercentExtern2())
                    : new Bloc(r.getDeliveryCommission2BasedOnAmount(), r.getDeliveryCommissionAmount2(),
                    r.getDeliveryCommissionPercent2());
            case DESISTEMENT -> throw new IllegalArgumentException("Un désistement reprend des lignes, il ne se calcule pas.");
        };
    }

    /** Critère absent = toute valeur ; critère posé = la valeur du fait. */
    private static boolean correspond(UUID critere, UUID valeur) {
        return critere == null || critere.equals(valeur);
    }

    /** 0 = commercial + action + catégorie … 7 = aucun critère (ordre de l'aide OMRAN). */
    private static int niveau(ComSalesCommission r) {
        return (r.getCfgTier() == null ? 4 : 0) + (r.getComAction() == null ? 2 : 0)
                + (r.getStkItemCategory() == null ? 1 : 0);
    }

    /** 0 = deux bornes, 1 = début seul, 2 = fin seule, 3 = aucune. */
    private static int bornes(ComSalesCommission r) {
        if (r.getStartDate() != null) {
            return r.getEndDate() != null ? 0 : 1;
        }
        return r.getEndDate() != null ? 2 : 3;
    }

    private static boolean dansLaFenetre(ComSalesCommission r, LocalDate jour) {
        return (r.getStartDate() == null || !jour.isBefore(r.getStartDate().toLocalDate()))
                && (r.getEndDate() == null || !jour.isAfter(r.getEndDate().toLocalDate()));
    }

    private static UUID id(CfgTier tier) {
        return Champs.de(tier, CfgTier::getId);
    }

    private static BigDecimal nonNul(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }

    static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }
}
