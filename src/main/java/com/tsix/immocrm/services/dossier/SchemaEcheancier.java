package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComSettlementCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Seul calcul du « schéma » d'un dossier ({@code Sale.cs:AdvanceAmount}, {@code CreditAmount},
 * {@code SelfFinancingAmount}, {@code GetSchemaDifference}) et de la ligne qui absorbe un écart
 * ({@code ComDocument.cs:ManageDocument}). Les loyers d'une vente en location-accession ne sont pas gérés : 0.
 */
public final class SchemaEcheancier {

    private static final int CENTIMES = 2;

    private SchemaEcheancier() {
    }

    /** Sommes signées par nature : une catégorie au débit ({@code IsCredit = 0}) se retranche. */
    public record Natures(BigDecimal avance, BigDecimal credit, BigDecimal autofinancement) {
        public BigDecimal total() {
            return avance.add(credit).add(autofinancement);
        }
    }

    public static Natures natures(List<ComDocument> lignes) {
        return new Natures(
                somme(lignes, c -> Boolean.TRUE.equals(c.getIsSaleAdvance())),
                somme(lignes, c -> Boolean.TRUE.equals(c.getIsSaleCredit())),
                somme(lignes, c -> Boolean.TRUE.equals(c.getIsSaleAutoFinancment())));
    }

    /** {@code GetSchemaDifference} : prix après remise − (avance + crédit + autofinancement), au centime. */
    public static BigDecimal ecart(BigDecimal prixApresRemise, List<ComDocument> lignes) {
        return Objects.requireNonNullElse(prixApresRemise, BigDecimal.ZERO)
                .subtract(natures(lignes).total())
                .setScale(CENTIMES, RoundingMode.HALF_UP);
    }

    /**
     * Ligne qui reçoit l'écart : le dernier autofinancement (date décroissante), sinon le dernier crédit, au crédit et
     * de catégorie qui ne « met pas à jour le schéma » ; jamais la ligne modifiée elle-même.
     */
    public static Optional<ComDocument> ligneDeReport(List<ComDocument> lignes, UUID ligneModifiee) {
        Optional<ComDocument> autofinancement = derniere(lignes, ligneModifiee,
                c -> Boolean.TRUE.equals(c.getIsSaleAutoFinancment()));
        return autofinancement.isPresent() ? autofinancement
                : derniere(lignes, ligneModifiee, c -> Boolean.TRUE.equals(c.getIsSaleCredit()));
    }

    /** Reste de l'autofinancement : montant signé − réglé signé, ligne par ligne. */
    public static BigDecimal autofinancementReste(List<ComDocument> lignes, Map<UUID, BigDecimal> regleParLigne) {
        BigDecimal reste = BigDecimal.ZERO;
        for (ComDocument ligne : lignes) {
            ComSettlementCategory c = ligne.getComSettlementCategory();
            if (c == null || !Boolean.TRUE.equals(c.getIsSaleAutoFinancment())) {
                continue;
            }
            BigDecimal net = Objects.requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO)
                    .subtract(regleParLigne.getOrDefault(ligne.getId(), BigDecimal.ZERO));
            reste = reste.add(auCredit(c) ? net : net.negate());
        }
        return reste;
    }

    /** {@code IsCredit} NULL compte au crédit, comme {@link MontantsDossier#montantSigne}. */
    static boolean auCredit(ComSettlementCategory c) {
        return !Boolean.FALSE.equals(c.getIsCredit());
    }

    public static boolean metAJourLeSchema(ComDocument ligne) {
        ComSettlementCategory c = ligne.getComSettlementCategory();
        return c != null && Boolean.TRUE.equals(c.getCanUpdateDocuments());
    }

    private static Optional<ComDocument> derniere(List<ComDocument> lignes, UUID exclue,
                                                  Predicate<ComSettlementCategory> nature) {
        return lignes.stream()
                .filter(d -> !d.getId().equals(exclue))
                .filter(d -> {
                    ComSettlementCategory c = d.getComSettlementCategory();
                    return c != null && nature.test(c) && auCredit(c)
                            && !Boolean.TRUE.equals(c.getCanUpdateDocuments());
                })
                .max(Comparator.comparing(ComDocument::getDocDate, Comparator.nullsFirst(LocalDateTime::compareTo)));
    }

    private static BigDecimal somme(List<ComDocument> lignes, Predicate<ComSettlementCategory> nature) {
        BigDecimal total = BigDecimal.ZERO;
        for (ComDocument ligne : lignes) {
            ComSettlementCategory c = ligne.getComSettlementCategory();
            if (c == null || !nature.test(c)) {
                continue;
            }
            BigDecimal montant = Objects.requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO);
            total = total.add(auCredit(c) ? montant : montant.negate());
        }
        return total;
    }
}
