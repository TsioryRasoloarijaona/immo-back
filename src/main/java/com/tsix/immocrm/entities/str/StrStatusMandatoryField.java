package com.tsix.immocrm.entities.str;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Champ obligatoire d'un statut ; {@code StrEntityLinkedEntityId} non nul = champ d'une entité liée (tiers). */
@Getter
@Setter
@Entity
@Table(name = "StrStatusMandatoryField")
public class StrStatusMandatoryField extends LigneOmran {
    @Column(name = "StrFieldId")
    private UUID strFieldId;

    @Column(name = "StrStatusId")
    private UUID strStatusId;

    @Column(name = "StrEntityLinkedEntityId")
    private UUID strEntityLinkedEntityId;
}
