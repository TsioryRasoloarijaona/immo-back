package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmFamilySituation;
import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.adm.AdmMatrimonialRegime;
import com.tsix.immocrm.entities.adm.AdmNationality;
import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.adm.AdmResidenceType;
import com.tsix.immocrm.entities.adm.AdmSalary;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
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
@Table(name = "CfgTier")
public class CfgTier {
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

    @Size(max = 30)
    @Nationalized
    @Column(name = "Phone", length = 30)
    private String phone;

    @Size(max = 30)
    @Nationalized
    @Column(name = "Phone1", length = 30)
    private String phone1;

    @Size(max = 30)
    @Nationalized
    @Column(name = "Phone2", length = 30)
    private String phone2;

    @Size(max = 30)
    @Nationalized
    @Column(name = "Fax", length = 30)
    private String fax;

    @Size(max = 30)
    @Nationalized
    @Column(name = "Mobile", length = 30)
    private String mobile;

    @Size(max = 100)
    @Nationalized
    @Column(name = "EMail", length = 100)
    private String eMail;

    @Size(max = 100)
    @Nationalized
    @Column(name = "WebSite", length = 100)
    private String webSite;

    @Size(max = 255)
    @Nationalized
    @Column(name = "SocialReason")
    private String socialReason;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Size(max = 255)
    @Nationalized
    @Column(name = "BankAgency")
    private String bankAgency;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Rib")
    private String rib;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Iban")
    private String iban;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FiscalCode")
    private String fiscalCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FiscalRegistry")
    private String fiscalRegistry;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FiscalDouane")
    private String fiscalDouane;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FirstName")
    private String firstName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "LastName")
    private String lastName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "BirthPlace")
    private String birthPlace;

    @Column(name = "BirthDate")
    private LocalDateTime birthDate;

    @Column(name = "NbrChildren")
    private Integer nbrChildren;

    @Column(name = "Asset", precision = 26, scale = 6)
    private BigDecimal asset;

    @Size(max = 100)
    @Nationalized
    @Column(name = "Quality", length = 100)
    private String quality;

    @Size(max = 255)
    @Nationalized
    @Column(name = "IDNumber")
    private String iDNumber;

    @Column(name = "IDDate")
    private LocalDateTime iDDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "IDPlace")
    private String iDPlace;

    @Column(name = "IDValidateDate")
    private LocalDateTime iDValidateDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "IDValidatePlace")
    private String iDValidatePlace;

    @Column(name = "IsProspect")
    private Boolean isProspect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgBankId")
    private CfgTier cfgBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCommercialId")
    private CfgTier cfgCommercial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgContactId")
    private CfgTier cfgContact;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CodeCountable")
    private String codeCountable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierTypeId")
    private CfgTierType cfgTierType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierKindId")
    private CfgTierKind cfgTierKind;

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

    @Size(max = 255)
    @Nationalized
    @Column(name = "FatherName")
    private String fatherName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "MotherName")
    private String motherName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Employer")
    private String employer;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address")
    private String address;

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

    @Column(name = "Antiquity")
    private Integer antiquity;

    @Size(max = 10)
    @Nationalized
    @Column(name = "MobileIndicatif", length = 10)
    private String mobileIndicatif;

    @Size(max = 10)
    @Nationalized
    @Column(name = "PhoneIndicatif", length = 10)
    private String phoneIndicatif;

    @Size(max = 10)
    @Nationalized
    @Column(name = "Phone1Indicatif", length = 10)
    private String phone1Indicatif;

    @Size(max = 10)
    @Nationalized
    @Column(name = "Phone2Indicatif", length = 10)
    private String phone2Indicatif;

    @Size(max = 10)
    @Nationalized
    @Column(name = "FaxIndicatif", length = 10)
    private String faxIndicatif;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CodeCountableCollective")
    private String codeCountableCollective;

    @Column(name = "IsOldCustomer")
    private Boolean isOldCustomer;

    @Column(name = "IsExtern")
    private Boolean isExtern;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ICE")
    private String ice;

    @Column(name = "IsRntCustomer")
    private Boolean isRntCustomer;

    @Column(name = "IsVisibleInList")
    private Boolean isVisibleInList;

    @Size(max = 255)
    @Nationalized
    @Column(name = "SocialSecurityNumber")
    private String socialSecurityNumber;

    @Column(name = "HireDate")
    private LocalDateTime hireDate;

    @Column(name = "AgreementDate")
    private LocalDateTime agreementDate;

    @Column(name = "FireDate")
    private LocalDateTime fireDate;

    @Column(name = "NbrChildrenHandicap")
    private Integer nbrChildrenHandicap;

    @Column(name = "SalaryBase", precision = 26, scale = 6)
    private BigDecimal salaryBase;

    @Column(name = "BaseMonthNumber", precision = 26, scale = 6)
    private BigDecimal baseMonthNumber;

    @Column(name = "BaseWorkedDaysNumber", precision = 26, scale = 6)
    private BigDecimal baseWorkedDaysNumber;

    @Column(name = "BaseVacationDaysNumber", precision = 26, scale = 6)
    private BigDecimal baseVacationDaysNumber;

    @Column(name = "RsRatio", precision = 26, scale = 6)
    private BigDecimal rsRatio;

    @Column(name = "IsFundReader")
    private Boolean isFundReader;

    @Column(name = "VATRatio", precision = 26, scale = 6)
    private BigDecimal vATRatio;

    @Column(name = "PaymentDelay")
    private Integer paymentDelay;

    // Identité de l'onglet « Général » d'Omran.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmTierTitleId")
    private AdmTierTitle admTierTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSexTypeId")
    private AdmSexType admSexType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryResidenceId")
    private AdmCountry admCountryResidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmNationalityId")
    private AdmNationality admNationality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmFamilySituationId")
    private AdmFamilySituation admFamilySituation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmResidenceTypeId")
    private AdmResidenceType admResidenceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmMatrimonialRegimeId")
    private AdmMatrimonialRegime admMatrimonialRegime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmProfessionId")
    private AdmProfession admProfession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSalaryId")
    private AdmSalary admSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmIdentityDocumentTypeId")
    private AdmIdentityDocumentType admIdentityDocumentType;
}