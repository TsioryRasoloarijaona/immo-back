package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.Map;

import static com.tsix.immocrm.domain.StatutDossier.*;

/**
 * Colonne date de {@code ComFolder} qui marque l'atteinte de chaque statut, avec sa confiance.
 * Déduit de la fiche PDF légataire ({@code instructions/templates/FICHE DOSSIER DE VENTE.pdf}, qui
 * associe libellé et colonne) et recoupé avec {@code StrFolderStatusHistory} sur dossiers réels.
 */
public final class EtapeMilestone {

    public enum Confiance {
        /** Confirmé par l'association libellé ↔ colonne de la fiche PDF légataire. */
        CONFIRME,
        /** Bonne colonne, mais souvent vide même une fois le statut atteint. */
        PLAUSIBLE,
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
        MILESTONES.put(DESISTEMENT, new Info("cancelDate", "Date de désistement (+ cause en note)", Confiance.CONFIRME));
    }

    private EtapeMilestone() {
    }

    public static Info of(StatutDossier statut) {
        return MILESTONES.get(statut);
    }
}
