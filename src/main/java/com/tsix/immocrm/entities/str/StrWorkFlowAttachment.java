package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Pièce à joindre pour exécuter une transition ; même sémantique que {@link StrStatusAttachment}. */
@Getter
@Setter
@Entity
@Table(name = "StrWorkFlowAttachment")
public class StrWorkFlowAttachment {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "StrAttachmentId")
    private UUID strAttachmentId;

    @Column(name = "StrWorkFlowId")
    private UUID strWorkFlowId;

    @Column(name = "IsJustToRequiredToBeChecked")
    private Boolean isJustToRequiredToBeChecked;

    @Column(name = "IsWithTitle")
    private Boolean isWithTitle;
}
