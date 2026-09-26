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
     * Les alertes <b>encore actives</b> d'une prospection, telles que le logiciel de bureau les
     * cible quand il les désarme — son prédicat, relevé dans sa trace d'audit :
     *
     * <pre>{@code
     * UPDATE AdmAlertData SET Active=0 WHERE RelatedKey IN (
     *   SELECT PKey FROM ComActionMessage LEFT JOIN StrStatus …
     *   WHERE ISNULL(StrStatus.Sorting,'-1') <= -1 AND ComDocumentId IS NULL
     *     AND ComProspectionId = …)
     * }</pre>
     *
     * <p>{@code ISNULL(Sorting, -1) <= -1} ne retient que les interactions <b>sans statut de
     * dossier</b> : tous les {@code StrStatus.Sorting} réels valent 1 ou plus. On l'écrit donc
     * directement {@code StrFolderStatusId IS NULL}, sans jointure. {@code ComDocumentId IS NULL}
     * épargne les rappels de paiement rattachés à une échéance.
     *
     * <p>Renvoyée en entités, pas en {@code UPDATE} de masse : chaque ligne modifiée passe ainsi
     * par le collecteur Hibernate et redescend dans la copie de lecture avec la transaction,
     * au lieu d'attendre le cycle de synchronisation suivant.
     */
    @Query(value = """
            SELECT a.* FROM AdmAlertData a
            JOIN ComActionMessage m ON m.PKey = a.RelatedKey
            WHERE m.ComProspectionId = :prospectionId
              AND m.ComDocumentId IS NULL
              AND m.StrFolderStatusId IS NULL
              AND a.Active = 1""", nativeQuery = true)
    List<AdmAlertData> alertesActivesDeLaProspection(@Param("prospectionId") UUID prospectionId);
}
