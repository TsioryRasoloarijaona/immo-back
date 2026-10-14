package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Pièce à joindre pour atteindre un statut ; « juste à cocher » = une pièce cochée sans fichier suffit. */
@Getter
@Setter
@Entity
@Table(name = "StrStatusAttachment")
public class StrStatusAttachment {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "StrAttachmentId")
    private UUID strAttachmentId;

    @Column(name = "StrStatusId")
    private UUID strStatusId;

    @Column(name = "IsJustToRequiredToBeChecked")
    private Boolean isJustToRequiredToBeChecked;

    @Column(name = "IsWithTitle")
    private Boolean isWithTitle;
}
