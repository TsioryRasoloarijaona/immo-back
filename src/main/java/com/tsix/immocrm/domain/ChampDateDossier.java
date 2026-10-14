package com.tsix.immocrm.domain;

/**
 * Date visée par la période de {@code GET /api/dossiers} : les 46 dates d'OMRAN
 * ({@code SaleMonitoringList.GetDatesFilterWithoutCancelDate}) plus l'annulation, dans l'ordre d'OMRAN.
 * Un chemin pointé passe par des jointures externes. {@link #RESERVATION} lit {@code DocDate}.
 */
public enum ChampDateDossier {
    RESERVATION("docDate", "Date réservation"),
    PROSPECTION("comProspection.docDate", "Date prospection"),
    VALIDITE_STATUT("statusValidityDate", "Date validité du statut"),
    CONCRETISATION("advanceDate", "Date concrétisation"),
    PROMESSE_VENTE("reservationDate", "Date promesse de vente"),
    PROMESSE_VENTE_CLIENT("reservationDateCustomer", "Date promesse de vente client"),
    ENREGISTREMENT_RESERVATION("reservationRegistryDate", "Date enregistrement de réservation"),
    VISITE_TECHNIQUE("receptionDate", "Date fiche technique / réception"),
    ENVOI_NOTAIRE_PREVU("agreementDatePrevisionel", "Date envoi au notaire prévisionnelle"),
    ENVOI_NOTAIRE("agreementDate", "Date envoi notaire / avocat"),
    MINUTE("agreementSavingDate", "Date minute / signature contrat"),
    MINUTE_CLIENT("agreementSavingDateCustomer", "Date minute / signature contrat client"),
    FORMALISATION("agreementRegistryDate", "Date enregistrement contrat"),
    IMPRESSION_CONTRAT("contractPrintDate", "Date impression contrat rés./prom."),
    EXPEDITION("ownershipCertificateDate", "Date expédition / certificat de propriété"),
    RELANCE_1("revival1", "Date relance 1"),
    ECHEANCE_RELANCE_1("revival1ValidityDate", "Date échéance relance 1 titre foncier"),
    RELANCE_2("revival2", "Date relance 2"),
    ECHEANCE_RELANCE_2("revival2ValidityDate", "Date échéance relance 2 titre foncier"),
    FICHE_BANCAIRE("bankRecordDate", "Date fiche bancaire"),
    ENVOI_MEILLEUR_TAUX("loanCompanySendDate", "Date envoi MEILLEURTAUX"),
    DECISION_MEILLEUR_TAUX("loanCompanyDecisionDate", "Date décision MEILLEURTAUX"),
    COMPROMIS("creditApprovalDate", "Date compromis / approbation crédit"),
    CREDIT_DEBLOQUE("creditPossessionDate", "Date attestation de consignation / octroi crédit"),
    FICHE_INTERVENTION("interventionDate", "Date fiche intervention"),
    FICHE_ASSURANCE("insuranceDate", "Date fiche assurance"),
    REMISE_DES_CLES_PREVUE("stkItem.stkHierarchy.deliveryDate", "Date remise des clés prévue"),
    REMISE_DES_CLES("deliveryDateReal", "Date remise des clés"),
    REMISE_DES_CLES_CLIENT("deliveryDateRealCustomer", "Date remise des clés client"),
    ENREGISTREMENT_REMISE_DES_CLES("deliveryRegistryDate", "Date enregistrement de remise des clés"),
    MODIFICATION_REMISE("discountChangeDate", "Date de modification de la remise"),
    PERMIS_HABITER("stkItem.stkHierarchy.occupancyPermitsDate", "Date permis d'habiter"),
    ACTE_PRECISION("precisionActDate", "Date acte de précision"),
    ACTE_PRECISION_CLIENT("precisionActDateCustomer", "Date acte de précision client"),
    ENREGISTREMENT_ACTE_PRECISION("precisionActRegistryDate", "Date enregistrement acte de précision"),
    MINUTE_AVOCAT("agreementSavingDateLawyer", "Date minute / signature contrat avocat"),
    REMISE_LEGALISATION_CONTRAT("agreementSavingDateForLegalization", "Date remise contrat pour légalisation"),
    LEGALISATION_CONTRAT("agreementSavingDateLegalization", "Date légalisation contrat"),
    DEPOT_ENREGISTREMENT_CONTRAT("agreementSavingDateForRegistry", "Date de dépôt pour enregistrement contrat"),
    REMISE_LEGALISATION_ACTE_PRECISION("precisionActDateForLegalization",
            "Date remise pour légalisation acte de précision"),
    DEPOT_ENREGISTREMENT_ACTE_PRECISION("precisionActDateForRegistry",
            "Date de dépôt pour enregistrement acte de précision"),
    DEPOT_CPF("finalTitleDateDeposit", "Date de dépôt à la CPF"),
    RETRAIT_CPF("finalTitleDateWithdrawal", "Date de retrait du dossier client de la CPF"),
    REMISE_CERTIFICAT_PROPRIETE("finalTitleDate", "Date de remise du certificat de propriété au client"),
    CREATION("userCreateDateTime", "Date création"),
    MODIFICATION("userUpdateDateTime", "Date modification"),
    ANNULATION("cancelDate", "Date d'annulation");

    private final String champEntite;
    private final String libelle;

    ChampDateDossier(String champEntite, String libelle) {
        this.champEntite = champEntite;
        this.libelle = libelle;
    }

    /** Chemin JPA depuis {@code ComFolder}, pointé si la date vit sur une association. */
    public String getChampEntite() {
        return champEntite;
    }

    public String getLibelle() {
        return libelle;
    }
}
