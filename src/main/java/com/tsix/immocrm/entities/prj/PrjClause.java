package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
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

/** Avenant d'un lot ; les 4 retenues sont chacune au taux ou au montant ({@code …BasedOnAmount}). */
@Getter
@Setter
@Entity
@Table(name = "PrjClause")
public class PrjClause extends AuditOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "CfgTierId")
    private UUID cfgTierId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "DocRef")
    private String docRef;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHT;

    @Column(name = "AmountVat", precision = 26, scale = 6)
    private BigDecimal amountVat;

    @Column(name = "AmountTTC", precision = 26, scale = 6)
    private BigDecimal amountTTC;

    @Column(name = "AmountToPay", precision = 26, scale = 6)
    private BigDecimal amountToPay;

    @Column(name = "WarrantyBasedOnAmount")
    private Boolean warrantyBasedOnAmount;

    @Column(name = "WarrantyRatio", precision = 26, scale = 6)
    private BigDecimal warrantyRatio;

    @Column(name = "WarrantyAmount", precision = 26, scale = 6)
    private BigDecimal warrantyAmount;

    @Column(name = "ProrataBasedOnAmount")
    private Boolean prorataBasedOnAmount;

    @Column(name = "ProportionRatio", precision = 26, scale = 6)
    private BigDecimal proportionRatio;

    @Column(name = "ProportionAmount", precision = 26, scale = 6)
    private BigDecimal proportionAmount;

    @Column(name = "DECBasedOnAmount")
    private Boolean decBasedOnAmount;

    @Column(name = "InsuranceDECRatio", precision = 26, scale = 6)
    private BigDecimal insuranceDECRatio;

    @Column(name = "InsuranceDECAmount", precision = 26, scale = 6)
    private BigDecimal insuranceDECAmount;

    @Column(name = "TRCBasedOnAmount")
    private Boolean trcBasedOnAmount;

    @Column(name = "InsuranceTRCRatio", precision = 26, scale = 6)
    private BigDecimal insuranceTRCRatio;

    @Column(name = "InsuranceTRCAmount", precision = 26, scale = 6)
    private BigDecimal insuranceTRCAmount;
}
