package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TrancheService {

    private final CfgTrancheRepository trancheRepository;

    public TrancheService(CfgTrancheRepository trancheRepository) {
        this.trancheRepository = trancheRepository;
    }

    public TrancheDetailDto findById(UUID id) {
        return trancheRepository.findById(id)
                .map(TrancheService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Tranche introuvable: " + id));
    }

    private static TrancheDetailDto toDetail(CfgTranche tranche) {
        return new TrancheDetailDto(
                tranche.getId(),
                tranche.getCfgProject() != null ? tranche.getCfgProject().getId() : null,
                tranche.getCode(),
                tranche.getDescription(),
                tranche.getDescriptionLong(),
                tranche.getAddress(),
                tranche.getAddress2(),
                tranche.getAddress3(),
                tranche.getZipCode(),
                tranche.getCity(),
                tranche.getPhone(),
                tranche.getMobile(),
                tranche.getFax(),
                tranche.getEmail(),
                tranche.getWebSite(),
                tranche.getWorkStartDate(),
                tranche.getWorkEndDate(),
                tranche.getMarketingStartDate(),
                tranche.getMarketingEndDate(),
                tranche.getDeliveryDate(),
                tranche.getIsForSale(),
                tranche.getIsForRent(),

                tranche.getCancellationPenalityPercent(),
                tranche.getCancellationPenalityPercentDiscount(),
                tranche.getArticleTransferPenality(),
                tranche.getArticleTransferPenalityDiscount(),
                tranche.getSliceTransferPenality(),
                tranche.getSliceTransferPenalityDiscount(),
                tranche.getCustomerTransferPenality(),
                tranche.getCustomerTransferPenalityDiscount(),
                tranche.getAgreementFirstPenaltyPercent(),
                tranche.getAgreementSecondPenaltyPercent(),

                tranche.getWarrantyDelay(),
                tranche.getWarrantyDelayAdded(),
                tranche.getAgreementFirstDelay(),
                tranche.getAgreementSecondDelay(),
                tranche.getPaymentBalanceDelay(),
                tranche.getDelayLawyerMinute(),
                tranche.getDelayMinuteAllSettled(),
                tranche.getWithdrawalDelay(),
                tranche.getInterventionMinimumDelai(),
                tranche.getMaxEcheanceDelai(),

                tranche.getAdvancePercent(),
                tranche.getConcretisationPercent(),
                tranche.getMaxVisitNumber(),
                tranche.getIsWithVerifCredit(),
                tranche.getCreditPercent()
        );
    }
}
