package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyShareholderRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderLineRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderRepository;
import com.tsix.immocrm.repositories.cfg.CfgShareholderRepository;
import com.tsix.immocrm.repositories.stl.StlCurrencyRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Dépôts de l'écran Entités juridiques, actionnaires et groupes (FEATURE-entites-juridiques.md). */
@Component
public record DepotsEntites(CfgCompanyRepository entites, CfgCompanyShareholderRepository repartitions,
                            CfgShareholderRepository actionnaires, CfgGroupShareholderRepository groupes,
                            CfgGroupShareholderLineRepository lignesGroupe, StlCurrencyRepository devises,
                            AdmSequenceRepository compteurs) {

    CfgShareholder actionnaire(UUID id) {
        return actionnaires.findById(id).orElseThrow(() -> new ResourceNotFoundException("Actionnaire introuvable: " + id));
    }

    CfgGroupShareholder groupe(UUID id) {
        return groupes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Groupe d'actionnaires introuvable: " + id));
    }
}
