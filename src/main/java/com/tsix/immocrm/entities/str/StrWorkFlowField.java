package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Champ obligatoire d'une transition ; {@code StrEntityLinkedEntityId} non nul = champ d'une entité liée (tiers). */
@Getter
@Setter
@Entity
@Table(name = "StrWorkFlowField")
public class StrWorkFlowField {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "StrFieldId")
    private UUID strFieldId;

    @Column(name = "StrWorkFlowId")
    private UUID strWorkFlowId;

    @Column(name = "StrEntityLinkedEntityId")
    private UUID strEntityLinkedEntityId;
}
