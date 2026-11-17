package com.tsix.immocrm.entities.prj;

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

/** Échéance de remboursement de la retenue de garantie d'un lot. */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketWarrantyLine")
public class PrjMarketWarrantyLine extends LigneOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Delay")
    private Integer delay;

    @Column(name = "EcheanceDate")
    private LocalDateTime echeanceDate;

    @Column(name = "Ratio", precision = 26, scale = 6)
    private BigDecimal ratio;
}
