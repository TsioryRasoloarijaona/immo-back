package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Rubrique d'une tranche. */
@Getter
@Setter
@Entity
@Table(name = "StkTrancheHeading")
public class StkTrancheHeading extends AuditTrieOmran {
    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;
}
