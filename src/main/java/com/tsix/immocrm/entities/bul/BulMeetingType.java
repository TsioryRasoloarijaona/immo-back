package com.tsix.immocrm.entities.bul;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Type de réunion de chantier ; Code et Description en 25 caractères. */
@Getter
@Setter
@Entity
@Table(name = "BulMeetingType")
public class BulMeetingType extends AuditOmran {
    @Column(name = "Sorting")
    private Integer sorting;
}
