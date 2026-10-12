package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.repositories.act.ActAccountRepository;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStandingRepository;
import com.tsix.immocrm.repositories.lnd.LndLevelRepository;
import com.tsix.immocrm.repositories.prj.PrjProjectRepository;
import com.tsix.immocrm.repositories.stk.StkTrancheHeadingRepository;
import org.springframework.stereotype.Component;

/**
 * Dépôts des listes de la fiche programme / tranche (FEATURE-patrimoine-tranche-fiche.md §2) : lus par les
 * référentiels et par la résolution des clés étrangères à l'écriture, pour que les deux voient la même liste.
 */
@Component
public record ReferentielsTranche(CfgTrancheStandingRepository standings,
                                  LndLevelRepository niveaux,
                                  StkTrancheHeadingRepository rubriques,
                                  ActAccountRepository comptes,
                                  PrjProjectRepository projetsChantier,
                                  CfgTierRepository tiers,
                                  AdmCountryRepository pays) {
}
