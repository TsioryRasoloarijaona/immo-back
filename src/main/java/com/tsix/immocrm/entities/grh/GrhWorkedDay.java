package com.tsix.immocrm.entities.grh;

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

/** Jour travaillé (Travaux chantier) ; clés étrangères en UUID simples, résolues par la lecture native. */
@Getter
@Setter
@Entity
@Table(name = "GrhWorkedDay")
public class GrhWorkedDay extends LigneOmran {
    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "TimeStart")
    private LocalDateTime timeStart;

    @Column(name = "TimeEnd")
    private LocalDateTime timeEnd;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Column(name = "CfgCompanyId")
    private UUID cfgCompanyId;

    @Column(name = "PrjProjectId")
    private UUID prjProjectId;

    @Column(name = "GrhWorkedDayTypeId")
    private UUID grhWorkedDayTypeId;

    @Column(name = "CfgEmployeeId")
    private UUID cfgEmployeeId;

    @Column(name = "CfgCustomerId")
    private UUID cfgCustomerId;

    @Column(name = "CfgTrancheLevelingDefinitionId")
    private UUID cfgTrancheLevelingDefinitionId;

    @Column(name = "CfgProjectZoneId")
    private UUID cfgProjectZoneId;

    @Column(name = "BuyItemId")
    private UUID buyItemId;

    @Column(name = "StrEntityId")
    private UUID strEntityId;

    @Column(name = "StrStatusId")
    private UUID strStatusId;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "Quantity1", precision = 26, scale = 6)
    private BigDecimal quantity1;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity1")
    private String unity1;

    @Column(name = "UnitPriceHT1", precision = 26, scale = 6)
    private BigDecimal unitPriceHT1;

    @Column(name = "VatRatio1", precision = 26, scale = 6)
    private BigDecimal vatRatio1;

    // Écrites par la facturation OMRAN seulement.
    @Column(name = "StlInvoiceCode", insertable = false, updatable = false)
    private String stlInvoiceCode;

    @Column(name = "StlInvoiceProformaCode", insertable = false, updatable = false)
    private String stlInvoiceProformaCode;
}
