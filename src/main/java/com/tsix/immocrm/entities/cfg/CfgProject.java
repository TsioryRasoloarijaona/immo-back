package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.adm.AdmCountry;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "CfgProject")
public class CfgProject {
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

    @Size(max = 255)
    @Nationalized
    @Column(name = "SignatureCity")
    private String signatureCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSignatoryId")
    private CfgTier admSignatory;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTechnicalManagerId")
    private CfgTier cfgTechnicalManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSalesManagerId")
    private CfgTier cfgSalesManager;

    @Size(max = 500)
    @Nationalized
    @Column(name = "DescriptionLong", length = 500)
    private String descriptionLong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;
}