package com.tsix.immocrm.services.dossier.acquereurs;

import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.springframework.stereotype.Component;

/** Dépôts lus et écrits par l'onglet Acquéreurs (S107). */
@Component
public record DepotsAcquereurs(ComFolderCustomerRepository acquereurs, CfgTierRepository tiers,
                               ComProspectionRepository prospections) {
}
