package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seule recherche d'une tranche par id pour les onglets, la copie et la suppression : une tranche d'une entité
 * inactive est masquée (404, administrateur compris), comme {@link ProgrammeVisible}.
 */
@Component
public class TrancheVisible {

    static final String TRANCHE_INTROUVABLE = "Tranche introuvable: ";

    private final CfgTrancheRepository trancheRepository;

    public TrancheVisible(CfgTrancheRepository trancheRepository) {
        this.trancheRepository = trancheRepository;
    }

    CfgTranche trouver(UUID id) {
        return trancheRepository.findDUneEntiteActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(TRANCHE_INTROUVABLE + id));
    }
}
