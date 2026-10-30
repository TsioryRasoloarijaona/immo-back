package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "AdmTaskPriority")
public class AdmTaskPriority extends AuditTrieOmran {
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;
}
