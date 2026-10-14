package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Type de pièce à joindre déclaré pour une entité de configuration. */
@Getter
@Setter
@Entity
@Table(name = "StrAttachment")
public class StrAttachment {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "AdmAttachedFileTypeId")
    private UUID admAttachedFileTypeId;

    @Column(name = "IsMandatory")
    private Boolean isMandatory;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;
}
