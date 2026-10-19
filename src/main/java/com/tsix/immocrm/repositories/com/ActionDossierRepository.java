package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Actions ({@code ComActionMessage}) rattachées à un dossier de vente par {@code ComFolderId}. */
public interface ActionDossierRepository extends JpaRepository<ComActionMessage, UUID> {

    @Query("SELECT m FROM ComActionMessage m LEFT JOIN FETCH m.comActionMessageType "
            + "LEFT JOIN FETCH m.comActionMessageCategory WHERE m.comFolder.id = :dossierId "
            + "ORDER BY m.docDate DESC")
    List<ComActionMessage> actionsDuDossier(UUID dossierId);

    /** Toute action portant {@code StkItemId} = l'article, dossier compris (FEATURE-patrimoine-audit-actions.md §3). */
    @Query("SELECT m FROM ComActionMessage m LEFT JOIN FETCH m.comActionMessageType "
            + "LEFT JOIN FETCH m.comActionMessageCategory WHERE m.stkItem.id = :articleId "
            + "ORDER BY m.docDate DESC")
    List<ComActionMessage> actionsDeLArticle(UUID articleId);

    Optional<ComActionMessage> findByIdAndComFolderId(UUID id, UUID dossierId);

    /** Instruction non lue adressée à un employé ({@code CfgEmployeeId} = commercial destinataire). */
    @Query("SELECT COUNT(m) > 0 FROM ComActionMessage m WHERE m.comFolder.id = :dossierId "
            + "AND m.comActionMessageType.id = :typeInstruction AND m.isNotRead = true "
            + "AND m.cfgEmployee.id = :employeId")
    boolean instructionNonLue(UUID dossierId, UUID typeInstruction, UUID employeId);
}
