package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Type de réception de lot (PAR, TOT…), cité par {@code PrjMarketReception.PrjMarketReceptionTypeId}. */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketReceptionType")
public class PrjMarketReceptionType extends AuditOmran {
    @Column(name = "sorting")
    private Integer sorting;
}
