package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.dto.dossier.edition.ModifierDossierRequest;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.ANNULATION;
import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.COMMERCIAL;
import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.CREDIT;
import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.DATES;
import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.GESTIONNAIRES;
import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.NOTAIRE;
import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.OPTIONS;
import static com.tsix.immocrm.services.dossier.edition.GroupeEdition.PENALITES;

/**
 * Table des champs simples de {@code PATCH /api/dossiers/{id}} (dates, textes, cases, tiers référencés) ;
 * le client, les prix et la note ont leur propre traitement dans {@link DossierEditionService}.
 */
@Component
class ChampsEdition {

    private final CfgTierRepository tierRepository;
    private final EntityManager entityManager;
    private final List<Champ<?>> champs = new ArrayList<>();

    ChampsEdition(CfgTierRepository tierRepository, EntityManager entityManager) {
        this.tierRepository = tierRepository;
        this.entityManager = entityManager;
        dates();
        datesAutres();
        date("annulationDate", ANNULATION, ModifierDossierRequest::annulationDate, ComFolder::getCancelDate,
                ComFolder::setCancelDate);
        texte("annulationCauses", ANNULATION, ModifierDossierRequest::annulationCauses, ComFolder::getCancelDescription,
                ComFolder::setCancelDescription);
        tiers("commercialId", COMMERCIAL, ModifierDossierRequest::commercialId, ComFolder::getCfgCommercial,
                ComFolder::setCfgCommercial);
        tiers("gestionnaire1Id", GESTIONNAIRES, ModifierDossierRequest::gestionnaire1Id, ComFolder::getCfgSaleManager,
                ComFolder::setCfgSaleManager);
        tiers("gestionnaire2Id", GESTIONNAIRES, ModifierDossierRequest::gestionnaire2Id,
                ComFolder::getCfgSaleManager2Id, ComFolder::setCfgSaleManager2Id);
        tiers("notaireId", NOTAIRE, ModifierDossierRequest::notaireId, ComFolder::getCfgLawyer, ComFolder::setCfgLawyer);
        tiers("banqueCreditId", CREDIT, ModifierDossierRequest::banqueCreditId, ComFolder::getCreditBank,
                ComFolder::setCreditBank);
        texte("agenceCredit", CREDIT, ModifierDossierRequest::agenceCredit, ComFolder::getCreditAgency,
                ComFolder::setCreditAgency);
        date("meilleurTauxEnvoi", CREDIT, ModifierDossierRequest::meilleurTauxEnvoi, ComFolder::getLoanCompanySendDate,
                ComFolder::setLoanCompanySendDate);
        date("meilleurTauxDecisionDate", CREDIT, ModifierDossierRequest::meilleurTauxDecisionDate,
                ComFolder::getLoanCompanyDecisionDate, ComFolder::setLoanCompanyDecisionDate);
        champs.add(new Champ<>("meilleurTauxDecisionId", CREDIT, ModifierDossierRequest::meilleurTauxDecisionId,
                ComFolder::getComLoanCompanyDecisionId,
                (f, id) -> f.setComLoanCompanyDecisionId(id == null ? null : decisionExistante(id)), true));
        caseACocher("ancienClient", OPTIONS, ModifierDossierRequest::ancienClient, ComFolder::getIsOldCustomer,
                ComFolder::setIsOldCustomer);
        caseACocher("cadeauPlusTard", OPTIONS, ModifierDossierRequest::cadeauPlusTard,
                ComFolder::getIsWithGiftChoiceLater, ComFolder::setIsWithGiftChoiceLater);
        caseACocher("integrerModificationsAuPrix", OPTIONS, ModifierDossierRequest::integrerModificationsAuPrix,
                ComFolder::getPriceWithModification, ComFolder::setPriceWithModification);
        caseACocher("penalitesDesactivees", PENALITES, ModifierDossierRequest::penalitesDesactivees,
                ComFolder::getIsPenaltyDisabled, ComFolder::setIsPenaltyDisabled);
    }

    List<Champ<?>> tous() {
        return champs;
    }

    private void dates() {
        date("concretisation", ModifierDossierRequest::concretisation, ComFolder::getAdvanceDate,
                ComFolder::setAdvanceDate);
        date("promesseVente", ModifierDossierRequest::promesseVente, ComFolder::getReservationDate,
                ComFolder::setReservationDate);
        date("visiteTechnique", ModifierDossierRequest::visiteTechnique, ComFolder::getReceptionDate,
                ComFolder::setReceptionDate);
        date("envoiNotairePrevisionnel", ModifierDossierRequest::envoiNotairePrevisionnel,
                ComFolder::getAgreementDatePrevisionel, ComFolder::setAgreementDatePrevisionel);
        caseACocher("envoiNotaireConfirme", DATES, ModifierDossierRequest::envoiNotaireConfirme,
                ComFolder::getIsConfirmedAgreementDate, ComFolder::setIsConfirmedAgreementDate);
        date("enregistrementContrat", ModifierDossierRequest::enregistrementContrat,
                ComFolder::getAgreementRegistryDate, ComFolder::setAgreementRegistryDate);
        date("envoiNotaire", ModifierDossierRequest::envoiNotaire, ComFolder::getAgreementDate,
                ComFolder::setAgreementDate);
        date("minute", ModifierDossierRequest::minute, ComFolder::getAgreementSavingDate,
                ComFolder::setAgreementSavingDate);
        date("expedition", ModifierDossierRequest::expedition, ComFolder::getOwnershipCertificateDate,
                ComFolder::setOwnershipCertificateDate);
        date("relanceTitreFoncier1", ModifierDossierRequest::relanceTitreFoncier1, ComFolder::getRevival1,
                ComFolder::setRevival1);
        date("echeanceRelance1", ModifierDossierRequest::echeanceRelance1, ComFolder::getRevival1ValidityDate,
                ComFolder::setRevival1ValidityDate);
        date("relanceTitreFoncier2", ModifierDossierRequest::relanceTitreFoncier2, ComFolder::getRevival2,
                ComFolder::setRevival2);
        date("echeanceRelance2", ModifierDossierRequest::echeanceRelance2, ComFolder::getRevival2ValidityDate,
                ComFolder::setRevival2ValidityDate);
        date("ficheBancaire", ModifierDossierRequest::ficheBancaire, ComFolder::getBankRecordDate,
                ComFolder::setBankRecordDate);
        date("compromis", ModifierDossierRequest::compromis, ComFolder::getCreditApprovalDate,
                ComFolder::setCreditApprovalDate);
        date("attestation", ModifierDossierRequest::attestation, ComFolder::getCreditPossessionDate,
                ComFolder::setCreditPossessionDate);
        date("intervention", ModifierDossierRequest::intervention, ComFolder::getInterventionDate,
                ComFolder::setInterventionDate);
        date("assurance", ModifierDossierRequest::assurance, ComFolder::getInsuranceDate, ComFolder::setInsuranceDate);
        date("remiseClesEffective", ModifierDossierRequest::remiseClesEffective, ComFolder::getDeliveryDateReal,
                ComFolder::setDeliveryDateReal);
        date("actePrecision", ModifierDossierRequest::actePrecision, ComFolder::getPrecisionActDate,
                ComFolder::setPrecisionActDate);
    }

    private void datesAutres() {
        date("promesseClient", ModifierDossierRequest::promesseClient, ComFolder::getReservationDateCustomer,
                ComFolder::setReservationDateCustomer);
        date("promesseEnregistrement", ModifierDossierRequest::promesseEnregistrement,
                ComFolder::getReservationRegistryDate, ComFolder::setReservationRegistryDate);
        date("impressionContrat", ModifierDossierRequest::impressionContrat, ComFolder::getContractPrintDate,
                ComFolder::setContractPrintDate);
        date("minuteClient", ModifierDossierRequest::minuteClient, ComFolder::getAgreementSavingDateCustomer,
                ComFolder::setAgreementSavingDateCustomer);
        date("minuteAvocat", ModifierDossierRequest::minuteAvocat, ComFolder::getAgreementSavingDateLawyer,
                ComFolder::setAgreementSavingDateLawyer);
        date("minuteRemiseLegalisation", ModifierDossierRequest::minuteRemiseLegalisation,
                ComFolder::getAgreementSavingDateForLegalization, ComFolder::setAgreementSavingDateForLegalization);
        date("minuteLegalisation", ModifierDossierRequest::minuteLegalisation,
                ComFolder::getAgreementSavingDateLegalization, ComFolder::setAgreementSavingDateLegalization);
        date("minuteDepotEnregistrement", ModifierDossierRequest::minuteDepotEnregistrement,
                ComFolder::getAgreementSavingDateForRegistry, ComFolder::setAgreementSavingDateForRegistry);
        date("remiseClesClient", ModifierDossierRequest::remiseClesClient, ComFolder::getDeliveryDateRealCustomer,
                ComFolder::setDeliveryDateRealCustomer);
        date("remiseClesEnregistrement", ModifierDossierRequest::remiseClesEnregistrement,
                ComFolder::getDeliveryRegistryDate, ComFolder::setDeliveryRegistryDate);
        date("actePrecisionClient", ModifierDossierRequest::actePrecisionClient,
                ComFolder::getPrecisionActDateCustomer, ComFolder::setPrecisionActDateCustomer);
        date("actePrecisionAvocat", ModifierDossierRequest::actePrecisionAvocat, ComFolder::getPrecisionActDateLawyer,
                ComFolder::setPrecisionActDateLawyer);
        date("actePrecisionRemiseLegalisation", ModifierDossierRequest::actePrecisionRemiseLegalisation,
                ComFolder::getPrecisionActDateForLegalization, ComFolder::setPrecisionActDateForLegalization);
        date("actePrecisionLegalisation", ModifierDossierRequest::actePrecisionLegalisation,
                ComFolder::getPrecisionActDateLegalization, ComFolder::setPrecisionActDateLegalization);
        date("actePrecisionDepotEnregistrement", ModifierDossierRequest::actePrecisionDepotEnregistrement,
                ComFolder::getPrecisionActDateForRegistry, ComFolder::setPrecisionActDateForRegistry);
        date("actePrecisionEnregistrement", ModifierDossierRequest::actePrecisionEnregistrement,
                ComFolder::getPrecisionActRegistryDate, ComFolder::setPrecisionActRegistryDate);
        date("cpfDepot", ModifierDossierRequest::cpfDepot, ComFolder::getFinalTitleDateDeposit,
                ComFolder::setFinalTitleDateDeposit);
        date("cpfRetrait", ModifierDossierRequest::cpfRetrait, ComFolder::getFinalTitleDateWithdrawal,
                ComFolder::setFinalTitleDateWithdrawal);
        date("remiseCertificatPropriete", ModifierDossierRequest::remiseCertificatPropriete,
                ComFolder::getFinalTitleDate, ComFolder::setFinalTitleDate);
        texte("numeroTitre", DATES, ModifierDossierRequest::numeroTitre, ComFolder::getFinalTitleNumber,
                ComFolder::setFinalTitleNumber);
    }

    private void date(String nom, Function<ModifierDossierRequest, LocalDateTime> saisie,
                      Function<ComFolder, LocalDateTime> actuel, BiConsumer<ComFolder, LocalDateTime> ecriture) {
        date(nom, DATES, saisie, actuel, ecriture);
    }

    private void date(String nom, GroupeEdition groupe, Function<ModifierDossierRequest, LocalDateTime> saisie,
                      Function<ComFolder, LocalDateTime> actuel, BiConsumer<ComFolder, LocalDateTime> ecriture) {
        champs.add(new Champ<>(nom, groupe, saisie, actuel, ecriture, true));
    }

    /** Texte réduit de ses blancs ; un texte vide vaut absent (vider passe par {@code champsAVider}). */
    private void texte(String nom, GroupeEdition groupe, Function<ModifierDossierRequest, String> saisie,
                       Function<ComFolder, String> actuel, BiConsumer<ComFolder, String> ecriture) {
        champs.add(new Champ<>(nom, groupe, r -> sansBlancs(saisie.apply(r)), actuel, ecriture, true));
    }

    static String sansBlancs(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Une case décochée s'envoie {@code false} ; elle ne se vide pas. */
    private void caseACocher(String nom, GroupeEdition groupe, Function<ModifierDossierRequest, Boolean> saisie,
                             Function<ComFolder, Boolean> actuel, BiConsumer<ComFolder, Boolean> ecriture) {
        champs.add(new Champ<>(nom, groupe, saisie, actuel, ecriture, false));
    }

    private void tiers(String nom, GroupeEdition groupe, Function<ModifierDossierRequest, UUID> saisie,
                       Function<ComFolder, CfgTier> actuel, BiConsumer<ComFolder, CfgTier> ecriture) {
        champs.add(new Champ<>(nom, groupe, saisie, f -> Champs.de(actuel.apply(f), CfgTier::getId),
                (f, id) -> ecriture.accept(f, id == null ? null : tier(nom, id)), true));
    }

    /** Décisions MEILLEURTAUX (ACCEPTÉE, REFUSÉE) dans l'ordre d'OMRAN ; table hors entités, lue en natif. */
    List<ReferentielItemDto> decisions() {
        List<?> lignes = entityManager.createNativeQuery(
                        "SELECT PKey, Code, Description, sorting FROM ComLoanCompanyDecision ORDER BY sorting ASC")
                .getResultList();
        return lignes.stream()
                .map(Object[].class::cast)
                .map(l -> new ReferentielItemDto((UUID) l[0], (String) l[1], (String) l[2],
                        l[3] != null ? ((Number) l[3]).intValue() : null))
                .toList();
    }

    private UUID decisionExistante(UUID id) {
        Number nombre = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM ComLoanCompanyDecision WHERE PKey = :id")
                .setParameter("id", id)
                .getSingleResult();
        if (nombre.intValue() == 0) {
            throw new InvalidRequestException("meilleurTauxDecisionId : décision introuvable (%s).".formatted(id));
        }
        return id;
    }

    CfgTier tier(String champ, UUID id) {
        return tierRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("%s : tiers introuvable (%s).".formatted(champ, id)));
    }
}
