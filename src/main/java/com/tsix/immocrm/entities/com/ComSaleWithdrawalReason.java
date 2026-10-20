package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Motif de désistement. */
@Getter
@Setter
@Entity
@Table(name = "ComSaleWithdrawalReason")
public class ComSaleWithdrawalReason extends AuditOmran {
    @Column(name = "Sorting")
    private Integer sorting;
}
