package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.springframework.stereotype.Component;

/** Dépôts écrits ou lus à la saisie d'un règlement, regroupés (S107). */
@Component
public record DepotsReglement(StlSettlementTypeRepository types, StlSettlementRepository reglements,
                              StlSettlementLineRepository lignes, CfgTierRepository tiers) {
}
