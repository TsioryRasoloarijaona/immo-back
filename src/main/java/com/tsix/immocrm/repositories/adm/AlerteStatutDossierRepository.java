package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAlertData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlerteStatutDossierRepository extends JpaRepository<AdmAlertData, UUID> {

    /**
     * Alertes actives que le changement de statut éteint ({@code ActionMessage.ManageAlertForComFolder}) : actions
     * de la prospection hors échéance, dont le statut a un ordre inférieur ou égal au nouveau (sans statut : -1).
     */
    @Query(value = """
            SELECT a.* FROM AdmAlertData a
            JOIN ComActionMessage m ON m.PKey = a.RelatedKey
            LEFT JOIN StrStatus s ON s.PKey = m.StrFolderStatusId
            WHERE m.ComProspectionId = :prospection
              AND m.ComDocumentId IS NULL
              AND ISNULL(s.Sorting, -1) <= :ordre
              AND a.Active = 1""", nativeQuery = true)
    List<AdmAlertData> aEteindre(@Param("prospection") UUID prospection, @Param("ordre") int ordre);
}
