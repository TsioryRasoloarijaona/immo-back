package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Nature de réception de lot ; {@code IsFinal} fait la date de réception définitive ({@code DateReceptionDefinitive}). */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketReceptionKind")
public class PrjMarketReceptionKind extends AuditOmran {
    @Column(name = "sorting")
    private Integer sorting;

    @Column(name = "IsFinal")
    private Boolean isFinal;
}
