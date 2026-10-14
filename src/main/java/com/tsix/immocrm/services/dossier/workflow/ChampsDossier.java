package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Champs du dossier désignés par {@code StrField.Code} (nom de propriété OMRAN de {@code Yes.Com.Sales/Sale.cs}) :
 * seule table code → colonne, pour les champs obligatoires (lecture) et la date associée au statut (écriture).
 * {@code ComLoanCompanyDecisionId}, non mappé sur {@link ComFolder}, est lu à part ({@link ChampsObligatoires}).
 */
final class ChampsDossier {

    static final String DECISION_MEILLEURTAUX = "ComLoanCompanyDecisionId";

    /** {@code ecriture} nulle : le champ n'est pas une date du dossier. */
    record Champ(Function<ComFolder, Object> lecture, BiConsumer<ComFolder, LocalDateTime> ecriture) {
    }

    private static final Map<String, Champ> CHAMPS = Map.ofEntries(
            date("DocDate", ComFolder::getDocDate, ComFolder::setDocDate),
            date("AdvanceDate", ComFolder::getAdvanceDate, ComFolder::setAdvanceDate),
            date("AgreementDate", ComFolder::getAgreementDate, ComFolder::setAgreementDate),
            date("AgreementDatePrevisionel", ComFolder::getAgreementDatePrevisionel, ComFolder::setAgreementDatePrevisionel),
            date("AgreementRegistryDate", ComFolder::getAgreementRegistryDate, ComFolder::setAgreementRegistryDate),
            date("AgreementSavingDate", ComFolder::getAgreementSavingDate, ComFolder::setAgreementSavingDate),
            date("AgreementSavingDateCustomer", ComFolder::getAgreementSavingDateCustomer,
                    ComFolder::setAgreementSavingDateCustomer),
            date("AgreementSavingDateForLegalization", ComFolder::getAgreementSavingDateForLegalization,
                    ComFolder::setAgreementSavingDateForLegalization),
            date("AgreementSavingDateForRegistry", ComFolder::getAgreementSavingDateForRegistry,
                    ComFolder::setAgreementSavingDateForRegistry),
            date("AgreementSavingDateLawyer", ComFolder::getAgreementSavingDateLawyer, ComFolder::setAgreementSavingDateLawyer),
            date("AgreementSavingDateLegalization", ComFolder::getAgreementSavingDateLegalization,
                    ComFolder::setAgreementSavingDateLegalization),
            date("BankRecordDate", ComFolder::getBankRecordDate, ComFolder::setBankRecordDate),
            date("CancelDate", ComFolder::getCancelDate, ComFolder::setCancelDate),
            date("ContractPrintDate", ComFolder::getContractPrintDate, ComFolder::setContractPrintDate),
            date("CreditApprovalDate", ComFolder::getCreditApprovalDate, ComFolder::setCreditApprovalDate),
            date("CreditPossessionDate", ComFolder::getCreditPossessionDate, ComFolder::setCreditPossessionDate),
            date("DeliveryDateReal", ComFolder::getDeliveryDateReal, ComFolder::setDeliveryDateReal),
            date("DeliveryDateRealCustomer", ComFolder::getDeliveryDateRealCustomer, ComFolder::setDeliveryDateRealCustomer),
            date("DeliveryRegistryDate", ComFolder::getDeliveryRegistryDate, ComFolder::setDeliveryRegistryDate),
            date("InsuranceDate", ComFolder::getInsuranceDate, ComFolder::setInsuranceDate),
            date("InterventionDate", ComFolder::getInterventionDate, ComFolder::setInterventionDate),
            date("LoanCompanyDecisionDate", ComFolder::getLoanCompanyDecisionDate, ComFolder::setLoanCompanyDecisionDate),
            date("LoanCompanySendDate", ComFolder::getLoanCompanySendDate, ComFolder::setLoanCompanySendDate),
            date("OwnershipCertificateDate", ComFolder::getOwnershipCertificateDate, ComFolder::setOwnershipCertificateDate),
            date("PrecisionActDate", ComFolder::getPrecisionActDate, ComFolder::setPrecisionActDate),
            date("PrecisionActDateCustomer", ComFolder::getPrecisionActDateCustomer, ComFolder::setPrecisionActDateCustomer),
            date("PrecisionActDateForLegalization", ComFolder::getPrecisionActDateForLegalization,
                    ComFolder::setPrecisionActDateForLegalization),
            date("PrecisionActDateForRegistry", ComFolder::getPrecisionActDateForRegistry,
                    ComFolder::setPrecisionActDateForRegistry),
            date("PrecisionActDateLawyer", ComFolder::getPrecisionActDateLawyer, ComFolder::setPrecisionActDateLawyer),
            date("PrecisionActDateLegalization", ComFolder::getPrecisionActDateLegalization,
                    ComFolder::setPrecisionActDateLegalization),
            date("PrecisionActRegistryDate", ComFolder::getPrecisionActRegistryDate, ComFolder::setPrecisionActRegistryDate),
            date("ReservationDate", ComFolder::getReservationDate, ComFolder::setReservationDate),
            date("ReservationDateCustomer", ComFolder::getReservationDateCustomer, ComFolder::setReservationDateCustomer),
            date("ReservationRegistryDate", ComFolder::getReservationRegistryDate, ComFolder::setReservationRegistryDate),
            date("Revival1", ComFolder::getRevival1, ComFolder::setRevival1),
            date("Revival2", ComFolder::getRevival2, ComFolder::setRevival2),
            autre("CancelDescription", ComFolder::getCancelDescription),
            autre("CfgLawyerId", ComFolder::getCfgLawyer),
            autre("CreditAgency", ComFolder::getCreditAgency),
            autre("CreditBankId", ComFolder::getCreditBank),
            autre("Notes", ComFolder::getNotes),
            autre("StkItemTitleNumber", f -> f.getStkItem() != null ? f.getStkItem().getTitleNumber() : null),
            // Sale.cs:StkItemOccupancyPermitsDate lit StkItemView, qui reprend la date de la tranche du lot.
            autre("StkItemOccupancyPermitsDate", ChampsDossier::permisDHabiter));

    private ChampsDossier() {
    }

    static Optional<Champ> champ(String code) {
        return Optional.ofNullable(code).map(CHAMPS::get);
    }

    private static Map.Entry<String, Champ> date(String code, Function<ComFolder, LocalDateTime> lecture,
                                                 BiConsumer<ComFolder, LocalDateTime> ecriture) {
        return Map.entry(code, new Champ(lecture::apply, ecriture));
    }

    private static Map.Entry<String, Champ> autre(String code, Function<ComFolder, Object> lecture) {
        return Map.entry(code, new Champ(lecture, null));
    }

    private static Object permisDHabiter(ComFolder f) {
        StkItem article = f.getStkItem();
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;
        return tranche != null ? tranche.getOccupancyPermitsDate() : null;
    }
}
