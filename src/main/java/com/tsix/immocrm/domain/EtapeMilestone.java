package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.Map;

import static com.tsix.immocrm.domain.StatutDossier.*;

/**
 * Repli de la « date associée » d'un statut quand {@code StrStatus.StrFieldDateId} n'est pas renseigné (base de
 * test) : copie du paramétrage de production, mesuré ({@code FEATURE-vente-workflow.md} §7). Champ en camelCase.
 */
public final class EtapeMilestone {

    public enum Confiance {
        /** Date associée paramétrée dans OMRAN (ou recopiée de ce paramétrage). */
        CONFIRME,
        /** Bonne colonne, mais souvent vide même une fois le statut atteint. */
        PLAUSIBLE,
        AUCUN
    }

    public record Info(String champ, String libelle, Confiance confiance) {
    }

    private static final Map<StatutDossier, Info> MILESTONES = new EnumMap<>(StatutDossier.class);

    static {
        MILESTONES.put(RESERVATION, new Info("docDate", "Date réservation", Confiance.CONFIRME));
        MILESTONES.put(CONCRETISATION, new Info("advanceDate", "Date concrétisation", Confiance.CONFIRME));
        MILESTONES.put(MEILLEURTAUX, new Info("loanCompanySendDate", "Date d'envoi MEILLEURTAUX", Confiance.CONFIRME));
        MILESTONES.put(COMPROMIS, new Info(null, "Aucune date associée", Confiance.AUCUN));
        MILESTONES.put(ENVOI_AU_NOTAIRE, new Info("agreementDate", "Date envoie au notaire / avocat", Confiance.CONFIRME));
        MILESTONES.put(CREDIT_EN_COURS, new Info(null, "Aucune date associée", Confiance.AUCUN));
        MILESTONES.put(CREDIT_DEBLOQUE, new Info("creditApprovalDate", "Date compromis / Approbation crédit",
                Confiance.CONFIRME));
        MILESTONES.put(MINUTE, new Info("agreementSavingDate", "Date Minute / Signature contrat", Confiance.CONFIRME));
        MILESTONES.put(REMISE_DES_CLEFS, new Info("deliveryDateReal", "Date Remise de Clés", Confiance.CONFIRME));
        MILESTONES.put(FORMALISE, new Info("ownershipCertificateDate", "Date d'Expédition / Certificat de propriété",
                Confiance.CONFIRME));
        MILESTONES.put(LIVRAISON_PROVISOIRE, new Info(null, "Aucune date associée", Confiance.AUCUN));
        MILESTONES.put(ANNULATION, new Info("cancelDate", "Date Annulation / Résiliation", Confiance.CONFIRME));
        MILESTONES.put(DESISTEMENT, new Info("cancelDate", "Date Annulation / Résiliation", Confiance.CONFIRME));
    }

    private EtapeMilestone() {
    }

    public static Info of(StatutDossier statut) {
        return MILESTONES.get(statut);
    }
}
