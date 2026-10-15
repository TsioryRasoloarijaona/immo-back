package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import org.springframework.stereotype.Component;

/** Dépôts lus et écrits par les offres d'une prospection, regroupés (S107). */
@Component
public record DepotsDevis(StlQuoteRepository offres, StlQuoteItemRepository lignes, CfgTrancheRepository tranches,
                          StkItemRepository articles, CfgTierRepository tiers,
                          ComPaymentMethodRepository modesPaiement, AdmSequenceRepository sequences,
                          ComSettlementCategoryRepository categories) {
}
