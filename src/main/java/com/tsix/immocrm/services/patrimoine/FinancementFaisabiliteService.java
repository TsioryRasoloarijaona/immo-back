package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.faisabilite.EcheancierDemandeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.FinancementFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.RemboursementFaisabiliteDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinancementFaisabiliteService {

    public List<FinancementFaisabiliteDto> lister(UUID etudeId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public List<FinancementFaisabiliteDto> modifierFinancements(UUID etudeId, List<FinancementFaisabiliteDto> credits) {
        throw new UnsupportedOperationException();
    }

    public List<RemboursementFaisabiliteDto> echeancier(UUID etudeId, EcheancierDemandeDto demande) {
        throw new UnsupportedOperationException();
    }
}
