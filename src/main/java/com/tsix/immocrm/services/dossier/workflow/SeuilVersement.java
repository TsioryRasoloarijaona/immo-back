package com.tsix.immocrm.services.dossier.workflow;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Seuil de versement d'un statut ({@code Sale.cs:VerifThreshold}) : seule règle, pour le refus d'une transition comme
 * pour la suggestion de passage automatique. Pur : la configuration et la situation sont fournies.
 */
public final class SeuilVersement {

    /** {@code ComFolderStatusPaymentType} « Toujours » / « Au comptant » / « À crédit » (PKey de production). */
    public static final UUID TOUJOURS = UUID.fromString("BC72ECEA-66A5-4EC0-847B-1B55ADB0A2C5");
    public static final UUID AU_COMPTANT = UUID.fromString("F344426B-CB63-4DFD-A357-BC5BBD7AAAF3");
    public static final UUID A_CREDIT = UUID.fromString("DF53ECBD-BA90-402F-9E5F-39EB9F0BA6D3");

    static final int SANS_SEUIL = 0;
    static final int AU_MOINS = 1;
    static final int STRICTEMENT = 2;

    private static final String SOIT = ".\r\nSoit un montant de ";

    private SeuilVersement() {
    }

    /** Ligne {@code CfgTrancheStatus} retenue (tranche, sinon globale). */
    public record Configuration(int operateur, BigDecimal ratio, UUID typePaiement) {
    }

    /** Ce que le statut cible et la tranche imposent en plus du seuil. */
    public record Regles(boolean venteAuComptant, boolean venteACredit, boolean verifCreditMax,
                         BigDecimal pourcentageCreditMax) {
    }

    /** Message OMRAN du premier contrôle qui échoue ; vide si la transition respecte le seuil. */
    public static Optional<String> controler(Configuration config, Regles regles, SituationFinanciere situation,
                                             BigDecimal prixApresRemise, BigDecimal prix) {
        BigDecimal credit = situation.credit();
        if (regles.verifCreditMax() && credit.signum() != 0
                && credit.compareTo(nonNul(prix).multiply(nonNul(regles.pourcentageCreditMax()))) > 0) {
            return Optional.of("Vous avez dépassez le montant max du crédit !");
        }
        if (config == null || config.operateur() == SANS_SEUIL) {
            return Optional.empty();
        }
        if (regles.venteACredit() && credit.signum() == 0 && !regles.venteAuComptant()) {
            return Optional.of("Il faut que le montant de crédit soit strictement supérieur à 0");
        }
        if (regles.venteAuComptant() && credit.signum() != 0 && !regles.venteACredit()) {
            return Optional.of("Il faut que le montant de crédit soit strictement égale à 0");
        }
        BigDecimal ratio = nonNul(config.ratio());
        BigDecimal seuil = SituationFinanciere.auCentime(ratio.multiply(nonNul(prixApresRemise)));
        BigDecimal verse = situation.versements();
        if (TOUJOURS.equals(config.typePaiement()) || (AU_COMPTANT.equals(config.typePaiement()) && credit.signum() == 0)) {
            return comparer(config.operateur(), verse, seuil, "", ratio, seuil);
        }
        if (A_CREDIT.equals(config.typePaiement()) && credit.signum() != 0) {
            // Sale.cs compare l'avance réglée au montant du crédit et affiche ratio × crédit : repris tel quel.
            BigDecimal avanceReglee = SituationFinanciere.auCentime(situation.avanceReglee());
            return comparer(config.operateur(), avanceReglee, credit, " du crédit", ratio, ratio.multiply(credit));
        }
        return Optional.empty();
    }

    private static Optional<String> comparer(int operateur, BigDecimal verse, BigDecimal borne, String nature,
                                             BigDecimal ratio, BigDecimal montantAffiche) {
        if (operateur == AU_MOINS && verse.compareTo(borne) < 0) {
            return Optional.of("Il faut que la sommes des versements" + nature + " soit au moins égal à "
                    + FormatsOmran.pourcentage(ratio) + SOIT + FormatsOmran.montant(montantAffiche));
        }
        if (operateur == STRICTEMENT && verse.compareTo(borne) <= 0) {
            return Optional.of("Il faut que la sommes des versements" + nature + " soit strictement supérieur à "
                    + FormatsOmran.pourcentage(ratio) + SOIT + FormatsOmran.montant(montantAffiche));
        }
        return Optional.empty();
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}
