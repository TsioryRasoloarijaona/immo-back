package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.repositories.adm.AdmModuleRepository;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Configuration de la facture de frais lue en base, jamais figée (FEATURE-patrimoine-terrains.md §3.1.5) ; une ligne
 * absente → 500 avant toute écriture. Statut de départ : début d'une transition qui n'est la fin d'aucune autre.
 */
@Component
record ConfigurationFacture(AdmModuleRepository modules, StrEntityRepository entites, AdmParamsGuidRepository parametres,
                            EntityManager em) {

    private static final String DEMARRAGE_WORKFLOW = """
            SELECT TOP 1 w.StartStatusId FROM BuyDocumentWorkFlow w WHERE w.StrEntityId = :entite
              AND w.StartStatusId IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM BuyDocumentWorkFlow x WHERE x.StrEntityId = :entite AND x.EndStatusId = w.StartStatusId)
            ORDER BY w.Sorting ASC, w.PKey ASC""";

    record Configuration(UUID module, UUID entite, UUID devise, UUID statut) {
    }

    Configuration lire() {
        UUID module = modules.findByCode("STL").stream().findFirst().map(AdmModule::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("Module STL absent d'AdmModule."));
        UUID entite = entites.findByCodeIn(List.of("StkInvoiceDebitLand")).stream().findFirst().map(StrEntity::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity StkInvoiceDebitLand absente."));
        UUID devise = parametres.findById("DefaultCurrency").map(AdmParamsGuid::getCodeValue)
                .orElseThrow(() -> new ConfigurationManquanteException("AdmParamsGuid DefaultCurrency absent."));
        List<?> depart = em.createNativeQuery(DEMARRAGE_WORKFLOW).setParameter("entite", entite).getResultList();
        if (depart.isEmpty()) {
            throw new ConfigurationManquanteException("Aucun statut de départ dans le workflow des factures de frais.");
        }
        return new Configuration(module, entite, devise, UUID.fromString(depart.get(0).toString()));
    }
}
