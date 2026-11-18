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
import java.util.UUID;

/** Prévision de main-d'œuvre d'un projet par type de travaux (« Prévisions chantier » d'OMRAN) ; TVA en ratio. */
@Getter
@Setter
@Entity
@Table(name = "PrjProjectBuildingPrevision")
public class PrjProjectBuildingPrevision extends LigneOmran {
    @Column(name = "PrjProjectId")
    private UUID prjProjectId;

    @Column(name = "GrhWorkedDayTypeId")
    private UUID grhWorkedDayTypeId;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}
