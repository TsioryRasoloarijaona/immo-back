package com.tsix.immocrm.services.achat.ecriture;

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
 * Configuration d'une pièce d'achat lue en base, jamais figée ; une ligne absente → 500 avant toute écriture.
 * Statut de départ (règle D4) : début d'une transition de la gestion qui n'est la fin d'aucune autre.
 */
@Component
public record ConfigurationPieceAchat(AdmModuleRepository modules, StrEntityRepository entites,
                                      AdmParamsGuidRepository parametres, EntityManager em) {

    private static final String DEMARRAGE_WORKFLOW = """
            SELECT TOP 1 w.StartStatusId FROM BuyDocumentWorkFlow w WHERE w.StrEntityId = :entite
              AND w.StartStatusId IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM BuyDocumentWorkFlow x WHERE x.StrEntityId = :entite AND x.EndStatusId = w.StartStatusId)
            ORDER BY w.Sorting ASC, w.PKey ASC""";

    public record Configuration(UUID module, UUID entite, UUID devise, UUID statut) {
    }

    Configuration lire(PieceAchatACreer piece) {
        UUID module = modules.findByCode(piece.module()).stream().findFirst().map(AdmModule::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("Module %s absent d'AdmModule.".formatted(piece.module())));
        UUID entite = entites.findByCodeIn(List.of(piece.gestion())).stream().findFirst().map(StrEntity::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity %s absente.".formatted(piece.gestion())));
        UUID devise = parametres.findById("DefaultCurrency").map(AdmParamsGuid::getCodeValue)
                .orElseThrow(() -> new ConfigurationManquanteException("AdmParamsGuid DefaultCurrency absent."));
        List<?> depart = em.createNativeQuery(DEMARRAGE_WORKFLOW).setParameter("entite", entite).getResultList();
        if (depart.isEmpty()) {
            throw new ConfigurationManquanteException(piece.sansDepart());
        }
        return new Configuration(module, entite, devise, UUID.fromString(depart.get(0).toString()));
    }

    /** {@code AdmParamsGuid.DefaultMarketType}, null si absent. */
    public UUID marcheParDefaut() {
        return parametres.findById("DefaultMarketType").map(AdmParamsGuid::getCodeValue).orElse(null);
    }
}
