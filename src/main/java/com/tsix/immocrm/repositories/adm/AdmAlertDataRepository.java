package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAlertData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmAlertDataRepository extends JpaRepository<AdmAlertData, UUID> {

    Optional<AdmAlertData> findByRelatedKey(UUID relatedKey);

    /**
     * Ce que le bureau désarme : son {@code ISNULL(Sorting,-1) <= -1} réécrit {@code StrFolderStatusId IS NULL}
     * (tout Sorting réel ≥ 1). En entités, pas en {@code UPDATE} de masse : les lignes passent par le
     * collecteur Hibernate vers la copie de lecture — voir FEATURE-alerte-admalertdata.md.
     */
    @Query(value = """
            SELECT a.* FROM AdmAlertData a
            JOIN ComActionMessage m ON m.PKey = a.RelatedKey
            WHERE m.ComProspectionId = :prospectionId
              AND m.ComDocumentId IS NULL
              AND m.StrFolderStatusId IS NULL
              AND a.Active = 1""", nativeQuery = true)
    List<AdmAlertData> alertesActivesDeLaProspection(@Param("prospectionId") UUID prospectionId);

    /** Alertes actives des actions propres d'un article (voir {@code ComActionMessageRepository#relancesDeLArticle}). */
    @Query(value = """
            SELECT a.* FROM AdmAlertData a
            JOIN ComActionMessage m ON m.PKey = a.RelatedKey
            WHERE m.StkItemId = :articleId
              AND m.ComProspectionId IS NULL
              AND m.ComFolderId IS NULL
              AND m.ComDocumentId IS NULL
              AND a.Active = 1""", nativeQuery = true)
    List<AdmAlertData> alertesActivesDeLArticle(@Param("articleId") UUID articleId);
}
