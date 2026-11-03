package com.tsix.immocrm.entities.grh;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Congé d'un employé ({@code GrhEmployeeId} = {@code CfgTier.PKey}), lu par la fiche personnel ; jamais écrit par nous. */
@Getter
@Entity
@Immutable
@Table(name = "GrhVacation")
public class GrhVacation extends CodeLibelleOmran {
    @Column(name = "DateFrom")
    private LocalDateTime dateFrom;
    @Column(name = "DateTo")
    private LocalDateTime dateTo;
    @Column(name = "DaysNumber", precision = 26, scale = 6)
    private BigDecimal daysNumber;
    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;
    @Column(name = "GrhEmployeeId")
    private UUID grhEmployeeId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GrhVacationTypeId")
    private GrhVacationType grhVacationType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;
}
