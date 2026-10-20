package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.dto.patrimoine.plans.PlanArticleDto;
import com.tsix.immocrm.dto.patrimoine.plans.PlanDto;
import com.tsix.immocrm.dto.patrimoine.plans.TraceDto;
import com.tsix.immocrm.dto.patrimoine.plans.TrancheAvecPlansDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PlansService {

    public record ImagePlan(String contentType, byte[] octets) {
    }

    public List<TrancheAvecPlansDto> tranchesAvecPlans() {
        throw new UnsupportedOperationException();
    }

    public List<PlanDto> plansDeLaTranche(UUID trancheId) {
        throw new UnsupportedOperationException();
    }

    public String etagImage(UUID planId) {
        throw new UnsupportedOperationException();
    }

    public ImagePlan image(UUID planId) {
        throw new UnsupportedOperationException();
    }

    public List<TraceDto> traces(UUID planId) {
        throw new UnsupportedOperationException();
    }

    public List<PlanArticleDto> plansDeLArticle(UUID articleId) {
        throw new UnsupportedOperationException();
    }
}
