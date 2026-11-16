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
 * Statut de départ, règle OMRAN : 1re transition de la gestion par {@code Sorting} dont le départ est de type « Nouveau ».
 */
@Component
public record ConfigurationPieceAchat(AdmModuleRepository modules, StrEntityRepository entites,
                                      AdmParamsGuidRepository parametres, EntityManager em) {

    private static final String ETAT_NOUVEAU = "1004DFA9-D4C9-4C61-A3AF-44F174A76D53";
    private static final String DEMARRAGE_WORKFLOW = """
            SELECT (SELECT COUNT(*) FROM BuyDocumentWorkFlow WHERE StrEntityId = :entite),
                   (SELECT TOP 1 CAST(w.StartStatusId AS varchar(36)) FROM BuyDocumentWorkFlow w
                      JOIN BuyDocumentStatus s ON s.PKey = w.StartStatusId
                    WHERE w.StrEntityId = :entite AND s.AdmWorkFlowStateTypeId = :nouveau
                    ORDER BY w.Sorting ASC, w.PKey ASC)""";

    public record Configuration(UUID module, UUID entite, UUID devise, UUID statut) {
    }

    Configuration lire(PieceAchatACreer piece) {
        UUID module = piece.entete() != null ? piece.entete().module() : module(piece.module());
        UUID entite = gestion(piece.gestion());
        return new Configuration(module, entite, devise(), statutDepart(entite, piece.sansDepart()));
    }

    UUID module(String code) {
        return modules.findByCode(code).stream().findFirst().map(AdmModule::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("Module %s absent d'AdmModule.".formatted(code)));
    }

    UUID gestion(String code) {
        return entites.findByCodeIn(List.of(code)).stream().findFirst().map(StrEntity::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity %s absente.".formatted(code)));
    }

    UUID devise() {
        return parametres.findById("DefaultCurrency").map(AdmParamsGuid::getCodeValue)
                .orElseThrow(() -> new ConfigurationManquanteException("AdmParamsGuid DefaultCurrency absent."));
    }

    /** Null si la gestion n'a aucune transition ; 500 {@code sansDepart} si aucune ne part d'un statut « Nouveau ». */
    UUID statutDepart(UUID entite, String sansDepart) {
        Object[] ligne = (Object[]) em.createNativeQuery(DEMARRAGE_WORKFLOW).setParameter("entite", entite)
                .setParameter("nouveau", ETAT_NOUVEAU).getSingleResult();
        if (((Number) ligne[0]).intValue() == 0) {
            return null;
        }
        if (ligne[1] == null) {
            throw new ConfigurationManquanteException(sansDepart);
        }
        return UUID.fromString(ligne[1].toString());
    }

    /** {@code AdmParamsGuid.DefaultMarketType}, null si absent. */
    public UUID marcheParDefaut() {
        return parametres.findById("DefaultMarketType").map(AdmParamsGuid::getCodeValue).orElse(null);
    }
}
