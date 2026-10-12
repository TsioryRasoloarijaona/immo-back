package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.adm.AdmCountry;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "CfgTranche")
public class CfgTranche {
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

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address")
    private String address;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Email")
    private String email;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Mobile")
    private String mobile;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Phone")
    private String phone;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Fax")
    private String fax;

    @Size(max = 255)
    @Nationalized
    @Column(name = "WebSite")
    private String webSite;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address2")
    private String address2;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address3")
    private String address3;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ZipCode")
    private String zipCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "City")
    private String city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProjectId")
    private CfgProject cfgProject;

    @Column(name = "WorkStartDate")
    private LocalDateTime workStartDate;

    @Column(name = "WorkEndDate")
    private LocalDateTime workEndDate;

    @Column(name = "MarketingStartDate")
    private LocalDateTime marketingStartDate;

    @Column(name = "MarketingEndDate")
    private LocalDateTime marketingEndDate;

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

    @Column(name = "AdvancePercent", precision = 26, scale = 6)
    private BigDecimal advancePercent;

    @Column(name = "ConcretisationPercent", precision = 26, scale = 6)
    private BigDecimal concretisationPercent;

    @Column(name = "EcarttypeFinalisation", precision = 26, scale = 6)
    private BigDecimal ecarttypeFinalisation;

    @Column(name = "EcarttypeConcretisation", precision = 26, scale = 6)
    private BigDecimal ecarttypeConcretisation;

    @Column(name = "EndDateConcretisation")
    private LocalDateTime endDateConcretisation;

    @Column(name = "StartDateFinalisation")
    private LocalDateTime startDateFinalisation;

    @Column(name = "DeliveryDate")
    private LocalDateTime deliveryDate;

    @Column(name = "IsForSale")
    private Boolean isForSale;

    @Column(name = "IsForRent")
    private Boolean isForRent;

    @Column(name = "IsForProject")
    private Boolean isForProject;

    @Column(name = "IsForBuy")
    private Boolean isForBuy;

    @Column(name = "IsForSav")
    private Boolean isForSav;

    @Column(name = "OccupancyPermitsDate")
    private LocalDateTime occupancyPermitsDate;

    @Column(name = "BeforeReservationAmount", precision = 26, scale = 6)
    private BigDecimal beforeReservationAmount;

    @Column(name = "SyndicAmount", precision = 26, scale = 6)
    private BigDecimal syndicAmount;

    @Column(name = "SyndicPeriod")
    private Integer syndicPeriod;

    @Column(name = "WarrantyDelayAdded")
    private Integer warrantyDelayAdded;

    @Column(name = "WarrantyDelay")
    private Integer warrantyDelay;

    @Column(name = "AgreementFirstDelay")
    private Integer agreementFirstDelay;

    @Column(name = "AgreementSecondDelay")
    private Integer agreementSecondDelay;

    @Column(name = "PaymentBalanceDelay")
    private Integer paymentBalanceDelay;

    @Column(name = "BeforeReservationAmountDiscount", precision = 26, scale = 6)
    private BigDecimal beforeReservationAmountDiscount;

    @Column(name = "AdvancePercentDiscount", precision = 26, scale = 6)
    private BigDecimal advancePercentDiscount;

    @Column(name = "DefaultExpenseAmountDiscount", precision = 26, scale = 6)
    private BigDecimal defaultExpenseAmountDiscount;

    @Column(name = "DefaultExpenseAmount", precision = 26, scale = 6)
    private BigDecimal defaultExpenseAmount;

    @Column(name = "CancellationPenalityPercent", precision = 26, scale = 6)
    private BigDecimal cancellationPenalityPercent;

    @Column(name = "CancellationPenalityPercentDiscount", precision = 26, scale = 6)
    private BigDecimal cancellationPenalityPercentDiscount;

    @Column(name = "ArticleTransferPenality", precision = 26, scale = 6)
    private BigDecimal articleTransferPenality;

    @Column(name = "ArticleTransferPenalityDiscount", precision = 26, scale = 6)
    private BigDecimal articleTransferPenalityDiscount;

    @Column(name = "SliceTransferPenality", precision = 26, scale = 6)
    private BigDecimal sliceTransferPenality;

    @Column(name = "SliceTransferPenalityDiscount", precision = 26, scale = 6)
    private BigDecimal sliceTransferPenalityDiscount;

    @Column(name = "CustomerTransferPenality", precision = 26, scale = 6)
    private BigDecimal customerTransferPenality;

    @Column(name = "CustomerTransferPenalityDiscount", precision = 26, scale = 6)
    private BigDecimal customerTransferPenalityDiscount;

    @Column(name = "SyndicAmountDiscount", precision = 26, scale = 6)
    private BigDecimal syndicAmountDiscount;

    @Column(name = "OtherExpenseAmount", precision = 26, scale = 6)
    private BigDecimal otherExpenseAmount;

    @Column(name = "OtherExpenseAmountDiscount", precision = 26, scale = 6)
    private BigDecimal otherExpenseAmountDiscount;

    @Column(name = "CancellationPenalityBasedOnItemPrice")
    private Boolean cancellationPenalityBasedOnItemPrice;

    @Column(name = "AgreementFirstPenaltyPercent", precision = 26, scale = 6)
    private BigDecimal agreementFirstPenaltyPercent;

    @Column(name = "AgreementSecondPenaltyPercent", precision = 26, scale = 6)
    private BigDecimal agreementSecondPenaltyPercent;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CommerceTribunal")
    private String commerceTribunal;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CancellationRightCode")
    private String cancellationRightCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgClaimsManagerId")
    private CfgTier cfgClaimsManager;

    @Column(name = "CountItemBasePrevision")
    private Integer countItemBasePrevision;

    @Column(name = "TotalSaleBasePrevision", precision = 26, scale = 6)
    private BigDecimal totalSaleBasePrevision;

    @Column(name = "BaseMarketingStartDate")
    private LocalDateTime baseMarketingStartDate;

    @Column(name = "BaseEndDateConcretisation")
    private LocalDateTime baseEndDateConcretisation;

    @Column(name = "BaseStartDateFinalisation")
    private LocalDateTime baseStartDateFinalisation;

    @Column(name = "BaseMarketingEndDate")
    private LocalDateTime baseMarketingEndDate;

    @Column(name = "BaseEcarttypeConcretisation", precision = 26, scale = 6)
    private BigDecimal baseEcarttypeConcretisation;

    @Column(name = "BaseEcarttypeFinalisation", precision = 26, scale = 6)
    private BigDecimal baseEcarttypeFinalisation;

    @Column(name = "BaseConcretisationPercent", precision = 26, scale = 6)
    private BigDecimal baseConcretisationPercent;

    @Size(max = 500)
    @Nationalized
    @Column(name = "DescriptionLong", length = 500)
    private String descriptionLong;

    @Column(name = "Revival1Date")
    private LocalDateTime revival1Date;

    @Column(name = "PrevisionMarketingStartDate")
    private LocalDateTime previsionMarketingStartDate;

    @Column(name = "PrevisionEndDateConcretisation")
    private LocalDateTime previsionEndDateConcretisation;

    @Column(name = "PrevisionStartDateFinalisation")
    private LocalDateTime previsionStartDateFinalisation;

    @Column(name = "PrevisionMarketingEndDate")
    private LocalDateTime previsionMarketingEndDate;

    @Column(name = "DelayLawyerMinute")
    private Integer delayLawyerMinute;

    @Column(name = "DelayMinuteAllSettled")
    private Integer delayMinuteAllSettled;

    @Column(name = "UnitConcretisation", precision = 26, scale = 6)
    private BigDecimal unitConcretisation;

    @Column(name = "UnitFinalisation", precision = 26, scale = 6)
    private BigDecimal unitFinalisation;

    @Column(name = "UnitTurnover", precision = 26, scale = 6)
    private BigDecimal unitTurnover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierOtherId")
    private CfgTier cfgTierOther;

    @Column(name = "CoveredFloorArea", precision = 26, scale = 6)
    private BigDecimal coveredFloorArea;

    @Size(max = 255)
    @Nationalized
    @Column(name = "SavAddress")
    private String savAddress;

    @Column(name = "IsWithGiftForProposer")
    private Boolean isWithGiftForProposer;

    @Column(name = "IsWithGiftForOldCustomer")
    private Boolean isWithGiftForOldCustomer;

    @Column(name = "InterventionMinimumDelai")
    private Integer interventionMinimumDelai;

    @Column(name = "MaxVisitNumber")
    private Integer maxVisitNumber;

    @Column(name = "IsWithGiftForProspectionOrigin")
    private Boolean isWithGiftForProspectionOrigin;

    @Column(name = "AccumulatedGifts")
    private Boolean accumulatedGifts;

    @Column(name = "IsActiveAdvancePercentDiscount")
    private Boolean isActiveAdvancePercentDiscount;

    @Column(name = "ReceptionDate")
    private LocalDateTime receptionDate;

    @Column(name = "WithdrawalDelay")
    private Integer withdrawalDelay;

    @Column(name = "AdvanceAmount", precision = 26, scale = 6)
    private BigDecimal advanceAmount;

    @Column(name = "AdvanceBasedOnAmount")
    private Boolean advanceBasedOnAmount;

    @Column(name = "IsForPrm")
    private Boolean isForPrm;

    @Column(name = "IsForRom")
    private Boolean isForRom;

    @Column(name = "RaisedHandDateAFH")
    private LocalDateTime raisedHandDateAFH;

    @Column(name = "TitleDate1")
    private LocalDateTime titleDate1;

    @Column(name = "RaisedHandDateBank")
    private LocalDateTime raisedHandDateBank;

    @Column(name = "TitleDate2")
    private LocalDateTime titleDate2;

    @Column(name = "MunicipalityDateDeposit")
    private LocalDateTime municipalityDateDeposit;

    @Column(name = "MunicipalityDateWithdrawal")
    private LocalDateTime municipalityDateWithdrawal;

    @Column(name = "OTCDateNonApprovedDeposit")
    private LocalDateTime oTCDateNonApprovedDeposit;

    @Column(name = "OTCDateSettlement")
    private LocalDateTime oTCDateSettlement;

    @Column(name = "OTCDateApprovedDeposit")
    private LocalDateTime oTCDateApprovedDeposit;

    @Column(name = "OTCDateMutationWithdrawal")
    private LocalDateTime oTCDateMutationWithdrawal;

    @Column(name = "SharingDate")
    private LocalDateTime sharingDate;

    @Column(name = "SharingDateDeposit")
    private LocalDateTime sharingDateDeposit;

    @Column(name = "TitleDate")
    private LocalDateTime titleDate;

    @Column(name = "TitleDateVerification")
    private LocalDateTime titleDateVerification;

    @Column(name = "RepartitionDate")
    private LocalDateTime repartitionDate;

    @Column(name = "UnitTurnoverHT", precision = 26, scale = 6)
    private BigDecimal unitTurnoverHT;

    @Column(name = "CPFSharingSettlementDate")
    private LocalDateTime cPFSharingSettlementDate;

    @Column(name = "CPFSharingWithdrawalDate")
    private LocalDateTime cPFSharingWithdrawalDate;

    @Column(name = "IsWithVerifMaxEcheanceDelai")
    private Boolean isWithVerifMaxEcheanceDelai;

    @Column(name = "MaxEcheanceDelai")
    private Integer maxEcheanceDelai;

    @Column(name = "IsWithVerifCredit")
    private Boolean isWithVerifCredit;

    @Column(name = "CreditPercent", precision = 26, scale = 6)
    private BigDecimal creditPercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;

}