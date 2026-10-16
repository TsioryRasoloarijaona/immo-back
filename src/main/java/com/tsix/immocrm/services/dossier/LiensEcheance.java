package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.repositories.adm.AdmAlertDataRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Ce qui rattache une échéance au reste de la base, pour sa suppression ({@code ComDocument.cs:VerifyLinksForDeletion},
 * {@code BeforeDelete}, {@code Sale.cs:DeleteDocument}). Tables lues en SQL : pas d'entité pour les décomptes et factures.
 */
@Component
public class LiensEcheance {

    /** Autorisations de versement, règlements (annulés compris), lignes de facture, dossier de recouvrement. */
    private static final String COMPTER_LIENS = """
            SELECT (SELECT COUNT(*) FROM StlPaymentAuthorizationLine WHERE ComDocumentId = :id)
                 + (SELECT COUNT(*) FROM StlSettlementLine WHERE ComDocumentId = :id)
                 + (SELECT COUNT(*) FROM StlDocumentLine WHERE ComDocumentId = :id)
                 + (SELECT COUNT(*) FROM ComDocument WHERE PKey = :id AND StlRecoveryFolderId IS NOT NULL)""";

    private final EntityManager entityManager;
    private final ComActionMessageRepository actionRepository;
    private final AdmAlertDataRepository alerteRepository;

    public LiensEcheance(EntityManager entityManager, ComActionMessageRepository actionRepository,
                         AdmAlertDataRepository alerteRepository) {
        this.entityManager = entityManager;
        this.actionRepository = actionRepository;
        this.alerteRepository = alerteRepository;
    }

    public boolean estLiee(UUID echeanceId) {
        Number total = (Number) entityManager.createNativeQuery(COMPTER_LIENS)
                .setParameter("id", echeanceId)
                .getSingleResult();
        return total.longValue() > 0;
    }

    /**
     * Supprime les actions de l'échéance et leurs alertes, en entités (le collecteur Hibernate les retire de la copie
     * de lecture), puis détache les lignes qui la désignaient comme pénalité.
     */
    public void detacher(ComDocument echeance) {
        List<ComActionMessage> actions = entityManager.createQuery(
                        "SELECT m FROM ComActionMessage m WHERE m.comDocument.id = :id", ComActionMessage.class)
                .setParameter("id", echeance.getId())
                .getResultList();
        for (ComActionMessage action : actions) {
            alerteRepository.deleteAll(alertesDe(action.getId()));
        }
        actionRepository.deleteAll(actions);
        entityManager.createQuery("SELECT d FROM ComDocument d WHERE d.comPenaltyDocument.id = :id", ComDocument.class)
                .setParameter("id", echeance.getId())
                .getResultList()
                .forEach(d -> d.setComPenaltyDocument(null));
    }

    /** Plusieurs alertes peuvent viser la même action : {@code findByRelatedKey} lèverait. */
    private List<AdmAlertData> alertesDe(UUID actionId) {
        return entityManager.createQuery("SELECT a FROM AdmAlertData a WHERE a.relatedKey = :id", AdmAlertData.class)
                .setParameter("id", actionId)
                .getResultList();
    }
}
