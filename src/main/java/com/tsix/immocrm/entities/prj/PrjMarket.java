package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lot (« marché » dans la table). Ses montants ne sont pas stockés : voir {@code domain/MontantsLot}.
 * {@code CfgSupplierId} n'a pas de FK en base : jointures LEFT obligatoires.
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarket")
public class PrjMarket {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 500)
    @Nationalized
    @Column(name = "DescriptionLong")
    private String descriptionLong;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjProjectId")
    private PrjProject prjProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjMarketTypeId")
    private PrjMarketType prjMarketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSupplierId")
    private CfgTier cfgSupplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlCategoryId")
    private StlCategory stlCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrEntityId")
    private StrEntity strEntity;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "Delay")
    private Integer delay;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @Column(name = "StartDateManual")
    private LocalDateTime startDateManual;

    @Column(name = "DelayManual")
    private Integer delayManual;

    @Column(name = "EndDateManual")
    private LocalDateTime endDateManual;

    @Column(name = "EstimatedPriceHTManual", precision = 26, scale = 6)
    private BigDecimal estimatedPriceHTManual;

    @Column(name = "EstimatedVatAmountManual", precision = 26, scale = 6)
    private BigDecimal estimatedVatAmountManual;

    @Column(name = "EstimatedPriceTTCManual", precision = 26, scale = 6)
    private BigDecimal estimatedPriceTTCManual;

    @Column(name = "WarrantyRatio", precision = 26, scale = 6)
    private BigDecimal warrantyRatio;

    @Column(name = "AdvanceRatio", precision = 26, scale = 6)
    private BigDecimal advanceRatio;

    @Column(name = "ProportionRatio", precision = 26, scale = 6)
    private BigDecimal proportionRatio;

    @Column(name = "InsuranceDECRatio", precision = 26, scale = 6)
    private BigDecimal insuranceDECRatio;

    @Column(name = "InsuranceTRCRatio", precision = 26, scale = 6)
    private BigDecimal insuranceTRCRatio;

    @Column(name = "DefaultVat", precision = 26, scale = 6)
    private BigDecimal defaultVat;

    @Column(name = "PaymentDelay")
    private Integer paymentDelay;

    @Column(name = "WarrantyInMonths", precision = 26, scale = 6)
    private BigDecimal warrantyInMonths;

    @Column(name = "IsMultiSupplier")
    private Boolean isMultiSupplier;

    @Column(name = "WithMarketView")
    private Boolean withMarketView;

    @Column(name = "IsClosed")
    private Boolean isClosed;

    @Column(name = "BlokWarrantyRestitution")
    private Boolean blokWarrantyRestitution;

    @Column(name = "LaunchOfTenderDate")
    private LocalDateTime launchOfTenderDate;

    @Column(name = "WithdrawalTenderDocumentsDate")
    private LocalDateTime withdrawalTenderDocumentsDate;

    @Column(name = "TenderMaturityDate")
    private LocalDateTime tenderMaturityDate;

    @Column(name = "TendersReportDate")
    private LocalDateTime tendersReportDate;

    @Column(name = "BidOpeningDate")
    private LocalDateTime bidOpeningDate;

    @Column(name = "TendersResortDate")
    private LocalDateTime tendersResortDate;

    @Column(name = "TenderDate")
    private LocalDateTime tenderDate;

    @Column(name = "ContractSignatureDate")
    private LocalDateTime contractSignatureDate;

    @Column(name = "ServiceOrderDate")
    private LocalDateTime serviceOrderDate;

    @Column(name = "WorkLaunchDate")
    private LocalDateTime workLaunchDate;

    @Column(name = "WorkCompletionDate")
    private LocalDateTime workCompletionDate;

    @Column(name = "ReattachmentDate")
    private LocalDateTime reattachmentDate;

    @Column(name = "CancellationDate")
    private LocalDateTime cancellationDate;

    // varchar (pas nvarchar) en base : pas de @Nationalized.
    @Size(max = 500)
    @Column(name = "CancellationCause")
    private String cancellationCause;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
