package com.tsix.immocrm.services.dossier.workflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * « Avec vérification du schéma de financement » ({@code StrStatus.BoolField012}, {@code Sale.cs:VerifSchema}) :
 * prix après remise = avance + crédit + auto-financement au centime près, modifications couvertes, et échéances
 * crédit / auto-financement non soldées postérieures à l'envoi au notaire. Pur.
 */
public final class ControleSchema {

    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SCHEMA_DIFFERENT = "Votre schéma de financement ne correspond pas au montant contractuel : ";

    private ControleSchema() {
    }

    /** Champs du dossier lus par le contrôle. */
    public record Dossier(BigDecimal prixApresRemise, BigDecimal modifications, boolean prixAvecModifications,
                          LocalDateTime envoiNotaire, LocalDateTime envoiNotairePrevisionnel) {
    }

    public static Optional<String> controler(Dossier dossier, SituationFinanciere situation) {
        if (!dossier.prixAvecModifications()) {
            BigDecimal ecartModifications = SituationFinanciere.auCentime(
                    nonNul(dossier.modifications()).subtract(situation.modificationsAuSchema()));
            if (ecartModifications.signum() != 0) {
                return Optional.of("Votre schéma de financement pour les modifications ne correspond pas au montant "
                        + "contractuel : différence de " + FormatsOmran.montant(ecartModifications) + " !");
            }
        }
        Optional<String> desequilibre = messageEcart(SituationFinanciere.auCentime(
                nonNul(dossier.prixApresRemise()).subtract(situation.financement())));
        if (desequilibre.isPresent()) {
            return desequilibre;
        }
        if (dossier.envoiNotaire() != null) {
            return datesEcheances(situation, dossier.envoiNotaire(), "Envoie au notaire / Avocat");
        }
        if (dossier.envoiNotairePrevisionnel() != null) {
            return datesEcheances(situation, dossier.envoiNotairePrevisionnel(), "Envoie au notaire prévisonnelle");
        }
        return Optional.empty();
    }

    /** Message OMRAN d'un écart prix − schéma (montant à ajouter ou à soustraire) ; vide si l'écart est nul. */
    public static Optional<String> messageEcart(BigDecimal ecart) {
        if (ecart.signum() > 0) {
            return Optional.of(SCHEMA_DIFFERENT + "vous devez ajouter dans le schéma de financement un montant de "
                    + FormatsOmran.montant(ecart) + " !");
        }
        if (ecart.signum() < 0) {
            return Optional.of(SCHEMA_DIFFERENT + "vous devez soustraire dans le schéma de financement un montant de "
                    + FormatsOmran.montant(ecart) + " !");
        }
        return Optional.empty();
    }

    /** {@code Sale.cs:verifAgreement}. */
    private static Optional<String> datesEcheances(SituationFinanciere situation, LocalDateTime reference, String libelle) {
        String suffixe = " avec un reste à régler soient supérieur ou égal à la date '" + libelle + ": "
                + JJ_MM_AAAA.format(reference) + "'.";
        if (anterieureNonSoldee(situation, reference, true)) {
            return Optional.of("il faut que les dates de schéma de financement pour les crédits" + suffixe);
        }
        if (anterieureNonSoldee(situation, reference, false)) {
            return Optional.of("il faut que les dates de schéma de financement pour les auto financements" + suffixe);
        }
        return Optional.empty();
    }

    private static boolean anterieureNonSoldee(SituationFinanciere situation, LocalDateTime reference,
                                               boolean creditBancaire) {
        return situation.echeances().stream()
                .filter(e -> creditBancaire ? e.creditBancaire() : e.autoFinancement())
                .anyMatch(e -> e.date() != null && e.date().toLocalDate().isBefore(reference.toLocalDate())
                        && e.reste().signum() > 0);
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}
