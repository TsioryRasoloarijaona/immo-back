package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Crédit d'une étude (« Déblocage de crédit »). */
@Getter
@Setter
@Entity
@Table(name = "FSFunding")
public class FSFunding extends LigneOmran {
    @Column(name = "FeasibilityStudyId")
    private UUID feasibilityStudyId;

    @Column(name = "paymentDate")
    private LocalDateTime paymentDate;

    @Column(name = "amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @Column(name = "monthsCount")
    private Integer monthsCount;

    @Column(name = "monthsStep")
    private Integer monthsStep;

    @Column(name = "gracePeriod")
    private Integer gracePeriod;

    @Column(name = "chargeRatio", precision = 26, scale = 6)
    private BigDecimal chargeRatio;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;
}
