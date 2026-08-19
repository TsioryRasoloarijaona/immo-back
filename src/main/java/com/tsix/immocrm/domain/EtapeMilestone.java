package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.Map;

import static com.tsix.immocrm.domain.StatutDossier.*;

/**
 * Which {@code ComFolder} date column (if any) is the "milestone date" for reaching a given
 * {@link StatutDossier}, and how confident that mapping is. Derived empirically against the real
 * database (never documented anywhere) — two kinds of evidence were used:
 * <ol>
 *   <li><b>The legacy "Fiche dossier de vente" printed report</b> (see
 *       {@code instructions/templates/FICHE DOSSIER DE VENTE.pdf}) directly pairs a French label
 *       with a {@code ComFolder} column value — e.g. it prints "Envoie notaire / Avocat" next to
 *       {@code ComFolder.AgreementDate}'s value. This is the strongest evidence (it's the legacy
 *       app itself telling us what the column means) — used for {@link #CONFIRME} entries.</li>
 *   <li><b>Cross-referencing {@code StrFolderStatusHistory}</b> (the status-change audit log)
 *       against the candidate column, on several real (non-test-looking) dossiers, checking the
 *       column's date lands on or shortly before the matching status transition timestamp.</li>
 * </ol>
 * Some statuses have no reliable column at all:
 * <ul>
 *   <li>{@link StatutDossier#RESERVATION}, {@link StatutDossier#CREDIT_EN_COURS} are transient
 *       states with no dedicated business date — {@code ComFolder.DocDate} already covers
 *       "when the dossier was created".</li>
 *   <li>{@link StatutDossier#LIVRAISON_PROVISOIRE} has <b>zero</b> occurrences in
 *       {@code StrFolderStatusHistory} across the whole database — never used in practice, no
 *       field can be confirmed.</li>
 *   <li>{@link StatutDossier#DESISTEMENT} is backed by a whole separate entity
 *       ({@code ComSaleWithdrawalCancellation} — reimbursement amount, withdrawal reason,
 *       legalization date...), not a single date column; out of scope here, tracked separately.</li>
 * </ul>
 */
public final class EtapeMilestone {

    public enum Confiance {
        /** Directly confirmed by the legacy printed report's label ↔ column pairing. */
        CONFIRME,
        /** Field exists and is thematically right, but real data shows it's often left empty
         * even once the status is reached (e.g. optional administrative paperwork date). */
        PLAUSIBLE,
        /** No column could be confirmed at all. */
        AUCUN
    }

    public record Info(String champ, String libelle, Confiance confiance) {
    }

    private static final Map<StatutDossier, Info> MILESTONES = new EnumMap<>(StatutDossier.class);

    static {
        MILESTONES.put(RESERVATION, new Info(null, "Date de création du dossier (DocDate)", Confiance.AUCUN));
        MILESTONES.put(CONCRETISATION, new Info("advanceDate", "Concrétisation", Confiance.CONFIRME));
        MILESTONES.put(MEILLEURTAUX, new Info("loanCompanyDecisionDate", "Date décision MEILLEURTAUX", Confiance.CONFIRME));
        MILESTONES.put(COMPROMIS, new Info("creditApprovalDate", "Compromis / Approbation crédit", Confiance.CONFIRME));
        MILESTONES.put(ENVOI_AU_NOTAIRE, new Info("agreementDate", "Envoi notaire / Avocat", Confiance.CONFIRME));
        MILESTONES.put(CREDIT_EN_COURS, new Info(null, "État transitoire, pas de date dédiée", Confiance.AUCUN));
        MILESTONES.put(CREDIT_DEBLOQUE, new Info("creditPossessionDate", "Attestation de consignation / Octroi crédit", Confiance.PLAUSIBLE));
        MILESTONES.put(MINUTE, new Info("agreementSavingDate", "Minute / Signature du contrat", Confiance.CONFIRME));
        MILESTONES.put(REMISE_DES_CLEFS, new Info("deliveryDateReal", "Remise des clés (effective)", Confiance.CONFIRME));
        MILESTONES.put(FORMALISE, new Info("agreementRegistryDate", "Date d'enregistrement du contrat", Confiance.CONFIRME));
        MILESTONES.put(LIVRAISON_PROVISOIRE, new Info(null, "Jamais observé en pratique (0 occurrence)", Confiance.AUCUN));
        MILESTONES.put(ANNULATION, new Info("cancelDate", "Date d'annulation (+ cause en note)", Confiance.CONFIRME));
        MILESTONES.put(DESISTEMENT, new Info(null, "Nécessite ComSaleWithdrawalCancellation (hors scope)", Confiance.AUCUN));
    }

    private EtapeMilestone() {
    }

    public static Info of(StatutDossier statut) {
        return MILESTONES.get(statut);
    }
}
