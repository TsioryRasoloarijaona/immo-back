package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "AdmProfession")
public class AdmProfession extends AuditOmran {
    @Column(name = "IsWithSalary")
    private Boolean isWithSalary;
}
