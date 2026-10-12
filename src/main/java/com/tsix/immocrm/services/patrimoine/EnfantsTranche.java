package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.repositories.cfg.CfgTrancheCoveredFloorRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheLevelRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheSettlementCategoryRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStatusRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStkItemRepository;
import org.springframework.stereotype.Component;

/** Dépôts des lignes enfants mappées d'une tranche (onglets et seuils), retirées avec elle. */
@Component
record EnfantsTranche(CfgTrancheStkItemRepository terrains, CfgTrancheLevelRepository etages,
                      CfgTrancheCoveredFloorRepository planchers, CfgTrancheSettlementCategoryRepository frais,
                      CfgTrancheStatusRepository seuils) {
}
