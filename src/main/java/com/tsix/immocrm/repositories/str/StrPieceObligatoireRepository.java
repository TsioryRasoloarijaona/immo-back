package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Pièces à joindre d'une transition ou d'un statut. Tuple : [AdmAttachedFileTypeId, libellé du type,
 * IsJustToRequiredToBeChecked, IsWithTitle, StrAttachment.IsMandatory].
 */
public interface StrPieceObligatoireRepository extends JpaRepository<StrAttachment, UUID> {

    @Query("SELECT a.admAttachedFileTypeId, t.description, wa.isJustToRequiredToBeChecked, wa.isWithTitle, "
            + "a.isMandatory FROM StrWorkFlowAttachment wa JOIN StrAttachment a ON a.id = wa.strAttachmentId "
            + "LEFT JOIN AdmAttachedFileType t ON t.id = a.admAttachedFileTypeId WHERE wa.strWorkFlowId = :transition")
    List<Object[]> deTransition(UUID transition);

    @Query("SELECT a.admAttachedFileTypeId, t.description, sa.isJustToRequiredToBeChecked, sa.isWithTitle, "
            + "a.isMandatory FROM StrStatusAttachment sa JOIN StrAttachment a ON a.id = sa.strAttachmentId "
            + "LEFT JOIN AdmAttachedFileType t ON t.id = a.admAttachedFileTypeId WHERE sa.strStatusId = :statut")
    List<Object[]> deStatut(UUID statut);

    /** Pièces du dossier ; tuple [AdmAttachedFileTypeId, IsCheckedButNotAttached] (colonne non mappée). */
    @Query(value = "SELECT AdmAttachedFileTypeId, IsCheckedButNotAttached FROM AdmAttachedFile "
            + "WHERE CategoryId = :porteur AND AdmAttachedFileTypeId IS NOT NULL", nativeQuery = true)
    List<Object[]> piecesDuPorteur(@Param("porteur") UUID porteur);
}
